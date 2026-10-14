package com.fincore.repository;

import com.fincore.model.FraudAlert;
import com.fincore.model.FraudAlertStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface FraudAlertRepository extends JpaRepository<FraudAlert, Long> {
    List<FraudAlert> findByStatusOrderByCreatedAtDesc(FraudAlertStatus status);
    Page<FraudAlert> findAllByOrderByCreatedAtDesc(Pageable pageable);
    long countByStatus(FraudAlertStatus status);
}
