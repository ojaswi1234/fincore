package com.fincore.repository;

import com.fincore.model.TransactionRecord;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

@Repository
public interface TransactionRepository extends JpaRepository<TransactionRecord, Long> {

    Optional<TransactionRecord> findByReferenceNumber(String referenceNumber);

    Optional<TransactionRecord> findByIdempotencyKey(String idempotencyKey);

    @Query("SELECT t FROM TransactionRecord t WHERE t.sourceAccount.id = :accId OR t.destinationAccount.id = :accId ORDER BY t.createdAt DESC")
    Page<TransactionRecord> findByAccountId(@Param("accId") Long accId, Pageable pageable);

    @Query("SELECT t FROM TransactionRecord t WHERE t.sourceAccount.user.id = :userId OR t.destinationAccount.user.id = :userId ORDER BY t.createdAt DESC")
    Page<TransactionRecord> findByUserId(@Param("userId") Long userId, Pageable pageable);

    @Query("SELECT t FROM TransactionRecord t WHERE t.sourceAccount.user.id = :userId OR t.destinationAccount.user.id = :userId ORDER BY t.createdAt DESC")
    List<TransactionRecord> findTop10ByUserId(@Param("userId") Long userId);

    List<TransactionRecord> findTop20ByOrderByCreatedAtDesc();

    @Query("SELECT count(t) FROM TransactionRecord t WHERE t.sourceAccount.user.id = :userId AND t.createdAt >= :since")
    long countRecentTransfersByUser(@Param("userId") Long userId, @Param("since") Instant since);
}
