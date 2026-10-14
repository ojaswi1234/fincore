package com.fincore.event;

import java.math.BigDecimal;
import java.time.Instant;

public class TransferCompletedEvent {

    private final String referenceNumber;
    private final String senderUsername;
    private final String receiverUsername;
    private final String sourceAccountNumber;
    private final String destinationAccountNumber;
    private final BigDecimal amount;
    private final String currency;
    private final BigDecimal senderNewBalance;
    private final BigDecimal receiverNewBalance;
    private final boolean flagged;
    private final String fraudReason;
    private final Instant timestamp;

    public TransferCompletedEvent(String referenceNumber, String senderUsername, String receiverUsername,
                                  String sourceAccountNumber, String destinationAccountNumber,
                                  BigDecimal amount, String currency, BigDecimal senderNewBalance,
                                  BigDecimal receiverNewBalance, boolean flagged, String fraudReason) {
        this.referenceNumber = referenceNumber;
        this.senderUsername = senderUsername;
        this.receiverUsername = receiverUsername;
        this.sourceAccountNumber = sourceAccountNumber;
        this.destinationAccountNumber = destinationAccountNumber;
        this.amount = amount;
        this.currency = currency;
        this.senderNewBalance = senderNewBalance;
        this.receiverNewBalance = receiverNewBalance;
        this.flagged = flagged;
        this.fraudReason = fraudReason;
        this.timestamp = Instant.now();
    }

    public String getReferenceNumber() {
        return referenceNumber;
    }

    public String getSenderUsername() {
        return senderUsername;
    }

    public String getReceiverUsername() {
        return receiverUsername;
    }

    public String getSourceAccountNumber() {
        return sourceAccountNumber;
    }

    public String getDestinationAccountNumber() {
        return destinationAccountNumber;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public String getCurrency() {
        return currency;
    }

    public BigDecimal getSenderNewBalance() {
        return senderNewBalance;
    }

    public BigDecimal getReceiverNewBalance() {
        return receiverNewBalance;
    }

    public boolean isFlagged() {
        return flagged;
    }

    public String getFraudReason() {
        return fraudReason;
    }

    public Instant getTimestamp() {
        return timestamp;
    }
}
