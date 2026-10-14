package com.fincore.service;

import com.fincore.model.FraudAlert;
import com.fincore.model.FraudAlertStatus;
import com.fincore.model.FraudSeverity;
import com.fincore.model.TransactionRecord;
import com.fincore.model.User;
import com.fincore.repository.FraudAlertRepository;
import com.fincore.repository.TransactionRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.temporal.ChronoUnit;

@Service
public class FraudDetectionService {

    private static final Logger log = LoggerFactory.getLogger(FraudDetectionService.class);

    private final FraudAlertRepository fraudAlertRepository;
    private final TransactionRepository transactionRepository;

    @Value("${fincore.fraud.high-value-threshold:200000.00}")
    private BigDecimal highValueThreshold;

    @Value("${fincore.fraud.velocity-time-window-seconds:60}")
    private int velocityWindowSeconds;

    @Value("${fincore.fraud.velocity-max-transfers:3}")
    private int velocityMaxTransfers;

    public FraudDetectionService(FraudAlertRepository fraudAlertRepository, TransactionRepository transactionRepository) {
        this.fraudAlertRepository = fraudAlertRepository;
        this.transactionRepository = transactionRepository;
    }

    public static class FraudCheckResult {
        private final boolean flagged;
        private final FraudSeverity severity;
        private final String reason;

        public FraudCheckResult(boolean flagged, FraudSeverity severity, String reason) {
            this.flagged = flagged;
            this.severity = severity;
            this.reason = reason;
        }

        public static FraudCheckResult pass() {
            return new FraudCheckResult(false, null, null);
        }

        public boolean isFlagged() {
            return flagged;
        }

        public FraudSeverity getSeverity() {
            return severity;
        }

        public String getReason() {
            return reason;
        }
    }

    @Transactional(readOnly = true)
    public FraudCheckResult evaluateTransfer(User sender, BigDecimal amount) {
        // Rule 1: High-value transfer
        if (amount.compareTo(highValueThreshold) >= 0) {
            return new FraudCheckResult(
                    true,
                    FraudSeverity.HIGH,
                    String.format("Transfer amount %s exceeds high-value threshold of %s INR", amount, highValueThreshold)
            );
        }

        // Rule 2: Velocity check (rapid bursts of transfers)
        Instant windowStart = Instant.now().minus(velocityWindowSeconds, ChronoUnit.SECONDS);
        long recentCount = transactionRepository.countRecentTransfersByUser(sender.getId(), windowStart);
        if (recentCount >= velocityMaxTransfers) {
            return new FraudCheckResult(
                    true,
                    FraudSeverity.MEDIUM,
                    String.format("High transaction velocity: %d transfers submitted in the last %d seconds",
                            recentCount + 1, velocityWindowSeconds)
            );
        }

        return FraudCheckResult.pass();
    }

    @Transactional(propagation = Propagation.MANDATORY)
    public FraudAlert recordFraudAlert(TransactionRecord transaction, User user, FraudCheckResult result) {
        FraudAlert alert = new FraudAlert(
                transaction,
                user,
                "AUTOMATED_RULE_FLAG",
                result.getSeverity(),
                result.getReason()
        );
        alert.setStatus(FraudAlertStatus.REVIEW_REQUIRED);
        log.warn("Generated fraud review alert for User {}: {}", user.getUsername(), result.getReason());
        return fraudAlertRepository.save(alert);
    }
}
