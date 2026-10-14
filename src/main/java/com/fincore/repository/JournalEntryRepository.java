package com.fincore.repository;

import com.fincore.model.JournalEntry;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface JournalEntryRepository extends JpaRepository<JournalEntry, Long> {
    List<JournalEntry> findByTransactionId(Long transactionId);
    Page<JournalEntry> findByAccountIdOrderByCreatedAtDesc(Long accountId, Pageable pageable);
    List<JournalEntry> findTop20ByAccountIdOrderByCreatedAtDesc(Long accountId);
}
