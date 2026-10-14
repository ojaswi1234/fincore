package com.fincore.controller.api;

import com.fincore.dto.TransactionDto;
import com.fincore.exception.AccountNotFoundException;
import com.fincore.exception.UnauthorizedAccountAccessException;
import com.fincore.model.Account;
import com.fincore.model.TransactionRecord;
import com.fincore.repository.AccountRepository;
import com.fincore.repository.TransactionRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/transactions")
public class TransactionRestController {

    private final TransactionRepository transactionRepository;
    private final AccountRepository accountRepository;

    public TransactionRestController(TransactionRepository transactionRepository, AccountRepository accountRepository) {
        this.transactionRepository = transactionRepository;
        this.accountRepository = accountRepository;
    }

    @GetMapping("/{referenceNumber}")
    public ResponseEntity<TransactionDto> getTransactionByRef(
            @PathVariable String referenceNumber,
            @AuthenticationPrincipal UserDetails userDetails) {

        TransactionRecord tx = transactionRepository.findByReferenceNumber(referenceNumber)
                .orElseThrow(() -> new AccountNotFoundException("Transaction not found for reference: " + referenceNumber));

        boolean isAdmin = userDetails.getAuthorities().stream().anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));
        boolean isOwner = (tx.getSourceAccount() != null && tx.getSourceAccount().getUser().getUsername().equalsIgnoreCase(userDetails.getUsername())) ||
                (tx.getDestinationAccount() != null && tx.getDestinationAccount().getUser().getUsername().equalsIgnoreCase(userDetails.getUsername()));

        if (!isAdmin && !isOwner) {
            throw new UnauthorizedAccountAccessException("You do not have permission to view transaction: " + referenceNumber);
        }

        return ResponseEntity.ok(toDto(tx));
    }

    @GetMapping("/account/{accountNumber}")
    public ResponseEntity<Page<TransactionDto>> getAccountTransactions(
            @PathVariable String accountNumber,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @AuthenticationPrincipal UserDetails userDetails) {

        Account account = accountRepository.findByAccountNumber(accountNumber)
                .orElseThrow(() -> new AccountNotFoundException("Account not found: " + accountNumber));

        boolean isAdmin = userDetails.getAuthorities().stream().anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));
        if (!isAdmin && !account.getUser().getUsername().equalsIgnoreCase(userDetails.getUsername())) {
            throw new UnauthorizedAccountAccessException("You are not authorized to view transactions for account " + accountNumber);
        }

        Page<TransactionRecord> records = transactionRepository.findByAccountId(
                account.getId(),
                PageRequest.of(page, size, Sort.by("createdAt").descending())
        );

        return ResponseEntity.ok(records.map(this::toDto));
    }

    private TransactionDto toDto(TransactionRecord tx) {
        return new TransactionDto(
                tx.getId(),
                tx.getReferenceNumber(),
                tx.getSourceAccount() != null ? tx.getSourceAccount().getAccountNumber() : "EXTERNAL",
                tx.getDestinationAccount() != null ? tx.getDestinationAccount().getAccountNumber() : "EXTERNAL",
                tx.getTransactionType().name(),
                tx.getAmount(),
                tx.getCurrency(),
                tx.getStatus().name(),
                tx.getFailureReason(),
                tx.getDescription(),
                tx.getCreatedAt(),
                tx.getCompletedAt()
        );
    }
}
