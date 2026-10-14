package com.fincore.dto;

import java.math.BigDecimal;
import java.time.Instant;

public class TransferResponseDto {

    private String referenceNumber;
    private String status;
    private String sourceAccountNumber;
    private String destinationAccountNumber;
    private BigDecimal amount;
    private String currency;
    private BigDecimal sourceNewBalance;
    private String description;
    private Instant createdAt;
    private boolean flagged;
    private String message;

    public TransferResponseDto() {
    }

    public TransferResponseDto(String referenceNumber, String status, String sourceAccountNumber,
                               String destinationAccountNumber, BigDecimal amount, String currency,
                               BigDecimal sourceNewBalance, String description, Instant createdAt,
                               boolean flagged, String message) {
        this.referenceNumber = referenceNumber;
        this.status = status;
        this.sourceAccountNumber = sourceAccountNumber;
        this.destinationAccountNumber = destinationAccountNumber;
        this.amount = amount;
        this.currency = currency;
        this.sourceNewBalance = sourceNewBalance;
        this.description = description;
        this.createdAt = createdAt;
        this.flagged = flagged;
        this.message = message;
    }

    public String getReferenceNumber() {
        return referenceNumber;
    }

    public void setReferenceNumber(String referenceNumber) {
        this.referenceNumber = referenceNumber;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getSourceAccountNumber() {
        return sourceAccountNumber;
    }

    public void setSourceAccountNumber(String sourceAccountNumber) {
        this.sourceAccountNumber = sourceAccountNumber;
    }

    public String getDestinationAccountNumber() {
        return destinationAccountNumber;
    }

    public void setDestinationAccountNumber(String destinationAccountNumber) {
        this.destinationAccountNumber = destinationAccountNumber;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public void setAmount(BigDecimal amount) {
        this.amount = amount;
    }

    public String getCurrency() {
        return currency;
    }

    public void setCurrency(String currency) {
        this.currency = currency;
    }

    public BigDecimal getSourceNewBalance() {
        return sourceNewBalance;
    }

    public void setSourceNewBalance(BigDecimal sourceNewBalance) {
        this.sourceNewBalance = sourceNewBalance;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }

    public boolean isFlagged() {
        return flagged;
    }

    public void setFlagged(boolean flagged) {
        this.flagged = flagged;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }
}
