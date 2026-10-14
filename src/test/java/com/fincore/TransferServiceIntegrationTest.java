package com.fincore;

import com.fincore.dto.TransferRequestDto;
import com.fincore.dto.TransferResponseDto;
import com.fincore.exception.*;
import com.fincore.model.*;
import com.fincore.repository.AccountRepository;
import com.fincore.repository.JournalEntryRepository;
import com.fincore.repository.TransactionRepository;
import com.fincore.repository.UserRepository;
import com.fincore.service.LedgerService;
import com.fincore.service.TransferService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

@SpringBootTest
public class TransferServiceIntegrationTest {

    @Autowired
    private TransferService transferService;

    @Autowired
    private AccountRepository accountRepository;

    @Autowired
    private TransactionRepository transactionRepository;

    @Autowired
    private JournalEntryRepository journalEntryRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private LedgerService ledgerService;

    private User senderUser;
    private User recipientUser;
    private Account senderAccount;
    private Account recipientAccount;

    @BeforeEach
    void setUp() {
        senderUser = userRepository.findByUsername("john_doe").orElseThrow();
        recipientUser = userRepository.findByUsername("jane_smith").orElseThrow();

        senderAccount = accountRepository.findByAccountNumber("ACC-10001").orElseThrow();
        recipientAccount = accountRepository.findByAccountNumber("ACC-20001").orElseThrow();

        // Reset balances for deterministic tests
        senderAccount.setBalance(new BigDecimal("50000.00"));
        recipientAccount.setBalance(new BigDecimal("20000.00"));
        accountRepository.save(senderAccount);
        accountRepository.save(recipientAccount);
    }

    @Test
    @DisplayName("Should successfully transfer funds, update balances, and create balanced double-entry ledger entries")
    void testSuccessfulTransfer_UpdatesBalancesAndCreatesBalancedLedger() {
        BigDecimal transferAmount = new BigDecimal("10000.00");
        String idempKey = "TEST-IDEMP-" + UUID.randomUUID();

        TransferRequestDto request = new TransferRequestDto(
                senderAccount.getAccountNumber(),
                recipientAccount.getAccountNumber(),
                transferAmount,
                "Integration test payment",
                idempKey
        );

        TransferResponseDto response = transferService.executeTransfer(request, "john_doe", "127.0.0.1");

        assertThat(response).isNotNull();
        assertThat(response.getStatus()).isEqualTo("COMPLETED");
        assertThat(response.getAmount()).isEqualByComparingTo(transferAmount);

        // Verify updated account balances
        Account updatedSender = accountRepository.findByAccountNumber(senderAccount.getAccountNumber()).orElseThrow();
        Account updatedRecipient = accountRepository.findByAccountNumber(recipientAccount.getAccountNumber()).orElseThrow();

        assertThat(updatedSender.getBalance()).isEqualByComparingTo(new BigDecimal("40000.00"));
        assertThat(updatedRecipient.getBalance()).isEqualByComparingTo(new BigDecimal("30000.00"));

        // Verify transaction record
        TransactionRecord tx = transactionRepository.findByReferenceNumber(response.getReferenceNumber()).orElseThrow();
        assertThat(tx.getStatus()).isEqualTo(TransactionStatus.COMPLETED);

        // Verify double-entry ledger journal entries
        List<JournalEntry> entries = journalEntryRepository.findByTransactionId(tx.getId());
        assertThat(entries).hasSize(2);

        JournalEntry debitEntry = entries.stream().filter(e -> e.getEntryType() == EntryType.DEBIT).findFirst().orElseThrow();
        JournalEntry creditEntry = entries.stream().filter(e -> e.getEntryType() == EntryType.CREDIT).findFirst().orElseThrow();

        assertThat(debitEntry.getAmount()).isEqualByComparingTo(transferAmount);
        assertThat(debitEntry.getBalanceAfter()).isEqualByComparingTo(new BigDecimal("40000.00"));
        assertThat(creditEntry.getAmount()).isEqualByComparingTo(transferAmount);
        assertThat(creditEntry.getBalanceAfter()).isEqualByComparingTo(new BigDecimal("30000.00"));

        // Verify ledger balance invariant (Debits == Credits)
        assertThat(ledgerService.verifyDoubleEntryBalance(tx.getId())).isTrue();
    }

    @Test
    @DisplayName("Should throw InsufficientFundsException and leave balances unchanged when funds are insufficient")
    void testInsufficientFunds_ThrowsExceptionAndPreservesBalances() {
        BigDecimal excessiveAmount = new BigDecimal("999999.00");
        TransferRequestDto request = new TransferRequestDto(
                senderAccount.getAccountNumber(),
                recipientAccount.getAccountNumber(),
                excessiveAmount,
                "Overdraft test",
                null
        );

        assertThrows(InsufficientFundsException.class, () ->
                transferService.executeTransfer(request, "john_doe", "127.0.0.1")
        );

        // Balances must remain unchanged
        Account currentSender = accountRepository.findByAccountNumber(senderAccount.getAccountNumber()).orElseThrow();
        assertThat(currentSender.getBalance()).isEqualByComparingTo(new BigDecimal("50000.00"));
    }

    @Test
    @DisplayName("Should reject transfer when destination account does not exist")
    void testInvalidDestinationAccount_ThrowsAccountNotFoundException() {
        TransferRequestDto request = new TransferRequestDto(
                senderAccount.getAccountNumber(),
                "ACC-DOES-NOT-EXIST",
                new BigDecimal("500.00"),
                "Invalid dest",
                null
        );

        assertThrows(AccountNotFoundException.class, () ->
                transferService.executeTransfer(request, "john_doe", "127.0.0.1")
        );
    }

    @Test
    @DisplayName("Should reject transfer when user attempts unauthorized debit from another user's account")
    void testUnauthorizedAccountAccess_ThrowsException() {
        TransferRequestDto request = new TransferRequestDto(
                senderAccount.getAccountNumber(), // John's account
                recipientAccount.getAccountNumber(),
                new BigDecimal("500.00"),
                "Unauthorized theft attempt",
                null
        );

        // Jane attempts to transfer out of John's account
        assertThrows(UnauthorizedAccountAccessException.class, () ->
                transferService.executeTransfer(request, "jane_smith", "127.0.0.1")
        );
    }

    @Test
    @DisplayName("Should reject duplicate idempotency key on subsequent requests")
    void testDuplicateIdempotencyKey_ThrowsException() {
        String idempKey = "IDEMP-DUPLICATE-CHECK-" + UUID.randomUUID();

        TransferRequestDto request1 = new TransferRequestDto(
                senderAccount.getAccountNumber(),
                recipientAccount.getAccountNumber(),
                new BigDecimal("100.00"),
                "First call",
                idempKey
        );
        transferService.executeTransfer(request1, "john_doe", "127.0.0.1");

        // Second call with identical idempotency key
        TransferRequestDto request2 = new TransferRequestDto(
                senderAccount.getAccountNumber(),
                recipientAccount.getAccountNumber(),
                new BigDecimal("100.00"),
                "Second call",
                idempKey
        );

        assertThrows(DuplicateIdempotencyException.class, () ->
                transferService.executeTransfer(request2, "john_doe", "127.0.0.1")
        );
    }

    @Test
    @DisplayName("Should prohibit transfer to the exact same account")
    void testTransferToSameAccount_ThrowsInvalidTransferException() {
        TransferRequestDto request = new TransferRequestDto(
                senderAccount.getAccountNumber(),
                senderAccount.getAccountNumber(),
                new BigDecimal("100.00"),
                "Self transfer",
                null
        );

        assertThrows(InvalidTransferException.class, () ->
                transferService.executeTransfer(request, "john_doe", "127.0.0.1")
        );
    }

    @Test
    @DisplayName("Concurrent transfers must prevent account overspending via pessimistic locking")
    void testConcurrentTransfers_PreventOverspending() throws InterruptedException {
        // Create an isolated account with exactly 1,000 INR
        Account limitedAccount = new Account(
                "ACC-CONCUR-" + UUID.randomUUID().toString().substring(0, 6),
                senderUser,
                AccountType.CHECKING,
                "INR",
                new BigDecimal("1000.00"),
                AccountStatus.ACTIVE
        );
        accountRepository.save(limitedAccount);

        int numberOfThreads = 2;
        ExecutorService executorService = Executors.newFixedThreadPool(numberOfThreads);
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch doneLatch = new CountDownLatch(numberOfThreads);

        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger failureCount = new AtomicInteger(0);

        for (int i = 0; i < numberOfThreads; i++) {
            executorService.submit(() -> {
                try {
                    startLatch.await(); // ensure both threads start concurrently
                    TransferRequestDto request = new TransferRequestDto(
                            limitedAccount.getAccountNumber(),
                            recipientAccount.getAccountNumber(),
                            new BigDecimal("800.00"), // 2 * 800 = 1600 > 1000 balance!
                            "Concurrent spend",
                            UUID.randomUUID().toString()
                    );
                    transferService.executeTransfer(request, "john_doe", "127.0.0.1");
                    successCount.incrementAndGet();
                } catch (InsufficientFundsException ex) {
                    failureCount.incrementAndGet();
                } catch (Exception ex) {
                    // Any other exception
                } finally {
                    doneLatch.countDown();
                }
            });
        }

        startLatch.countDown();
        doneLatch.await(5, TimeUnit.SECONDS);
        executorService.shutdown();

        // Exactly 1 must succeed and 1 must fail due to insufficient funds!
        assertThat(successCount.get()).isEqualTo(1);
        assertThat(failureCount.get()).isEqualTo(1);

        // Account balance must be exactly 200.00, never negative!
        Account finalAccount = accountRepository.findByAccountNumber(limitedAccount.getAccountNumber()).orElseThrow();
        assertThat(finalAccount.getBalance()).isEqualByComparingTo(new BigDecimal("200.00"));
    }
}
