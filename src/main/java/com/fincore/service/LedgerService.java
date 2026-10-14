package com.fincore.service;

import com.fincore.exception.FinCoreException;
import com.fincore.model.Account;
import com.fincore.model.EntryType;
import com.fincore.model.JournalEntry;
import com.fincore.model.TransactionRecord;
import com.fincore.repository.JournalEntryRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

@Service
public class LedgerService {

    private static final Logger log = LoggerFactory.getLogger(LedgerService.class);
    private final JournalEntryRepository journalEntryRepository;

    public LedgerService(JournalEntryRepository journalEntryRepository) {
        this.journalEntryRepository = journalEntryRepository;
    }

    @Transactional(propagation = Propagation.MANDATORY)
    public void recordDoubleEntryTransfer(TransactionRecord tx, Account sourceAccount, Account destAccount,
                                          BigDecimal amount, BigDecimal sourceNewBalance, BigDecimal destNewBalance) {
        // Double-entry record 1: Debit to Source Account
        JournalEntry debitEntry = new JournalEntry(
                tx,
                sourceAccount,
                EntryType.DEBIT,
                amount,
                sourceNewBalance
        );
        journalEntryRepository.save(debitEntry);

        // Double-entry record 2: Credit to Destination Account
        JournalEntry creditEntry = new JournalEntry(
                tx,
                destAccount,
                EntryType.CREDIT,
                amount,
                destNewBalance
        );
        journalEntryRepository.save(creditEntry);

        log.debug("Recorded double-entry ledger records for tx ref: {}. Debit Acc: {}, Credit Acc: {}, Amount: {}",
                tx.getReferenceNumber(), sourceAccount.getAccountNumber(), destAccount.getAccountNumber(), amount);
    }

    @Transactional(readOnly = true)
    public boolean verifyDoubleEntryBalance(Long transactionId) {
        List<JournalEntry> entries = journalEntryRepository.findByTransactionId(transactionId);
        if (entries.isEmpty()) {
            throw new FinCoreException("No ledger journal entries found for transaction ID: " + transactionId);
        }

        BigDecimal totalDebit = BigDecimal.ZERO;
        BigDecimal totalCredit = BigDecimal.ZERO;

        for (JournalEntry entry : entries) {
            if (entry.getEntryType() == EntryType.DEBIT) {
                totalDebit = totalDebit.add(entry.getAmount());
            } else if (entry.getEntryType() == EntryType.CREDIT) {
                totalCredit = totalCredit.add(entry.getAmount());
            }
        }

        boolean isBalanced = totalDebit.compareTo(totalCredit) == 0;
        if (!isBalanced) {
            log.error("CRITICAL LEDGER INTEGRITY FAILURE: Transaction ID {} is unbalanced! Total Debit: {}, Total Credit: {}",
                    transactionId, totalDebit, totalCredit);
            throw new FinCoreException(String.format("Ledger imbalance detected! Debit: %s, Credit: %s", totalDebit, totalCredit));
        }

        return true;
    }
}
