package com.fincore.service;

import com.fincore.dto.TransferRequestDto;
import com.fincore.dto.TransferResponseDto;
import com.fincore.event.TransferCompletedEvent;
import com.fincore.exception.*;
import com.fincore.model.*;
import com.fincore.repository.AccountRepository;
import com.fincore.repository.IdempotencyRecordRepository;
import com.fincore.repository.TransactionRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.util.UUID;

@Service
public class TransferService {

    private static final Logger log = LoggerFactory.getLogger(TransferService.class);

    private final AccountRepository accountRepository;
    private final TransactionRepository transactionRepository;
    private final IdempotencyRecordRepository idempotencyRecordRepository;
    private final LedgerService ledgerService;
    private final FraudDetectionService fraudDetectionService;
    private final NotificationService notificationService;
    private final AuditService auditService;
    private final ApplicationEventPublisher eventPublisher;

    public TransferService(AccountRepository accountRepository,
                           TransactionRepository transactionRepository,
                           IdempotencyRecordRepository idempotencyRecordRepository,
                           LedgerService ledgerService,
                           FraudDetectionService fraudDetectionService,
                           NotificationService notificationService,
                           AuditService auditService,
                           ApplicationEventPublisher eventPublisher) {
        this.accountRepository = accountRepository;
        this.transactionRepository = transactionRepository;
        this.idempotencyRecordRepository = idempotencyRecordRepository;
        this.ledgerService = ledgerService;
        this.fraudDetectionService = fraudDetectionService;
        this.notificationService = notificationService;
        this.auditService = auditService;
        this.eventPublisher = eventPublisher;
    }

    @Transactional(isolation = Isolation.READ_COMMITTED)
    public TransferResponseDto executeTransfer(TransferRequestDto request, String authenticatedUsername, String clientIp) {
        log.info("Processing transfer request: {} -> {} amount: {} (User: {})",
                request.getSourceAccountNumber(), request.getDestinationAccountNumber(), request.getAmount(), authenticatedUsername);

        // 1. Validate inputs
        if (request.getSourceAccountNumber() == null || request.getDestinationAccountNumber() == null) {
            throw new InvalidTransferException("Source and destination account numbers are required");
        }
        if (request.getSourceAccountNumber().trim().equalsIgnoreCase(request.getDestinationAccountNumber().trim())) {
            throw new InvalidTransferException("Transfers to the same account are prohibited");
        }
        if (request.getAmount() == null || request.getAmount().compareTo(BigDecimal.ZERO) <= 0) {
            throw new InvalidTransferException("Transfer amount must be strictly greater than 0");
        }

        BigDecimal transferAmount = request.getAmount().setScale(2, RoundingMode.HALF_EVEN);

        // 2. Idempotency Check
        String idempKey = request.getIdempotencyKey();
        if (idempKey != null && !idempKey.isBlank()) {
            if (idempotencyRecordRepository.findByIdempotencyKey(idempKey).isPresent()) {
                throw new DuplicateIdempotencyException("A transfer with idempotency key '" + idempKey + "' has already been executed");
            }
        }

        // 3. Resolve account primary IDs first (scalar query without caching stale entity in L1 persistence context)
        Long srcId = accountRepository.findIdByAccountNumber(request.getSourceAccountNumber().trim())
                .orElseThrow(() -> new AccountNotFoundException("Source account not found: " + request.getSourceAccountNumber()));

        Long dstId = accountRepository.findIdByAccountNumber(request.getDestinationAccountNumber().trim())
                .orElseThrow(() -> new AccountNotFoundException("Destination account not found: " + request.getDestinationAccountNumber()));

        // 4. Deadlock-Free Pessimistic Write Locking (acquire write locks in strict ascending ID order)
        Long firstId = Math.min(srcId, dstId);
        Long secondId = Math.max(srcId, dstId);

        Account lockedFirst = accountRepository.findByIdForUpdate(firstId)
                .orElseThrow(() -> new AccountNotFoundException("Account ID not found during locking: " + firstId));
        Account lockedSecond = accountRepository.findByIdForUpdate(secondId)
                .orElseThrow(() -> new AccountNotFoundException("Account ID not found during locking: " + secondId));

        Account source = (lockedFirst.getId().equals(srcId)) ? lockedFirst : lockedSecond;
        Account destination = (lockedFirst.getId().equals(dstId)) ? lockedFirst : lockedSecond;

        // 5. Ownership verification at service layer
        if (authenticatedUsername != null && !authenticatedUsername.equalsIgnoreCase("admin")) {
            if (!source.getUser().getUsername().equalsIgnoreCase(authenticatedUsername)) {
                auditService.recordEvent(
                        source.getUser().getId(),
                        authenticatedUsername,
                        "UNAUTHORIZED_TRANSFER_ATTEMPT",
                        "ACCOUNT",
                        source.getAccountNumber(),
                        clientIp,
                        "Attempted unauthorized debit on foreign account"
                );
                throw new UnauthorizedAccountAccessException("You are not authorized to initiate transfers from account " + source.getAccountNumber());
            }
        }

        // 6. Validate eligibility and funds
        if (source.getStatus() != AccountStatus.ACTIVE) {
            throw new AccountStatusException("Source account is not active. Current status: " + source.getStatus());
        }
        if (destination.getStatus() != AccountStatus.ACTIVE) {
            throw new AccountStatusException("Destination account is not active. Current status: " + destination.getStatus());
        }
        if (source.getBalance().compareTo(transferAmount) < 0) {
            throw new InsufficientFundsException(String.format("Insufficient funds in account %s. Available: %s INR, Requested: %s INR",
                    source.getAccountNumber(), source.getBalance(), transferAmount));
        }

        // 7. Execute monetary debit and credit
        BigDecimal sourceNewBalance = source.getBalance().subtract(transferAmount);
        BigDecimal destNewBalance = destination.getBalance().add(transferAmount);

        source.setBalance(sourceNewBalance);
        destination.setBalance(destNewBalance);

        accountRepository.save(source);
        accountRepository.save(destination);

        // 8. Generate immutable TransactionRecord
        String referenceNumber = "TX-" + UUID.randomUUID().toString().replace("-", "").substring(0, 12).toUpperCase();
        TransactionRecord tx = new TransactionRecord(
                referenceNumber,
                source,
                destination,
                TransactionType.TRANSFER,
                transferAmount,
                source.getCurrency(),
                TransactionStatus.COMPLETED,
                idempKey,
                request.getDescription() != null ? request.getDescription() : "Fund transfer"
        );
        tx.setCompletedAt(Instant.now());
        TransactionRecord savedTx = transactionRepository.save(tx);

        // 9. Record and verify balanced Double-Entry Ledger journal entries
        ledgerService.recordDoubleEntryTransfer(savedTx, source, destination, transferAmount, sourceNewBalance, destNewBalance);
        ledgerService.verifyDoubleEntryBalance(savedTx.getId());

        // 10. Fraud evaluation simulation
        FraudDetectionService.FraudCheckResult fraudResult = fraudDetectionService.evaluateTransfer(source.getUser(), transferAmount);
        boolean isFlagged = fraudResult.isFlagged();
        if (isFlagged) {
            fraudDetectionService.recordFraudAlert(savedTx, source.getUser(), fraudResult);
            savedTx.setStatus(TransactionStatus.FLAGGED);
            transactionRepository.save(savedTx);
        }

        // 11. Record in-app notifications
        notificationService.createNotification(
                source.getUser(),
                "Funds Transferred",
                String.format("Successfully sent %s %s to account %s. Ref: %s",
                        transferAmount, source.getCurrency(), destination.getAccountNumber(), referenceNumber),
                "TRANSFER_DEBIT"
        );
        notificationService.createNotification(
                destination.getUser(),
                "Funds Received",
                String.format("Received %s %s from account %s. Ref: %s",
                        transferAmount, destination.getCurrency(), source.getAccountNumber(), referenceNumber),
                "TRANSFER_CREDIT"
        );

        // 12. Record Audit Event
        auditService.recordEvent(
                source.getUser().getId(),
                authenticatedUsername,
                "TRANSFER_COMPLETED",
                "TRANSACTION",
                referenceNumber,
                clientIp,
                String.format("Debit: %s, Credit: %s, Amount: %s", source.getAccountNumber(), destination.getAccountNumber(), transferAmount)
        );

        // 13. Record Idempotency key if supplied
        if (idempKey != null && !idempKey.isBlank()) {
            IdempotencyRecord idempRecord = new IdempotencyRecord(
                    idempKey,
                    null,
                    200,
                    referenceNumber
            );
            idempotencyRecordRepository.save(idempRecord);
        }

        // 14. Publish event for AFTER_COMMIT STOMP notification dispatch
        eventPublisher.publishEvent(new TransferCompletedEvent(
                referenceNumber,
                source.getUser().getUsername(),
                destination.getUser().getUsername(),
                source.getAccountNumber(),
                destination.getAccountNumber(),
                transferAmount,
                source.getCurrency(),
                sourceNewBalance,
                destNewBalance,
                isFlagged,
                isFlagged ? fraudResult.getReason() : null
        ));

        return new TransferResponseDto(
                referenceNumber,
                savedTx.getStatus().name(),
                source.getAccountNumber(),
                destination.getAccountNumber(),
                transferAmount,
                source.getCurrency(),
                sourceNewBalance,
                savedTx.getDescription(),
                savedTx.getCreatedAt(),
                isFlagged,
                isFlagged ? "Transfer completed and flagged for operational review: " + fraudResult.getReason()
                        : "Transfer executed successfully"
        );
    }
}
