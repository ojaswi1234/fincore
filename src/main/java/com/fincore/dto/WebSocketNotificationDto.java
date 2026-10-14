package com.fincore.dto;

import java.math.BigDecimal;
import java.time.Instant;

public class WebSocketNotificationDto {

    private String eventId;
    private String eventType;
    private String referenceNumber;
    private String title;
    private String message;
    private BigDecimal amount;
    private String currency;
    private String sourceAccountNumber;
    private String destinationAccountNumber;
    private Instant timestamp;

    public WebSocketNotificationDto() {
    }

    public WebSocketNotificationDto(String eventId, String eventType, String referenceNumber,
                                    String title, String message, BigDecimal amount,
                                    String currency, String sourceAccountNumber,
                                    String destinationAccountNumber, Instant timestamp) {
        this.eventId = eventId;
        this.eventType = eventType;
        this.referenceNumber = referenceNumber;
        this.title = title;
        this.message = message;
        this.amount = amount;
        this.currency = currency;
        this.sourceAccountNumber = sourceAccountNumber;
        this.destinationAccountNumber = destinationAccountNumber;
        this.timestamp = timestamp != null ? timestamp : Instant.now();
    }

    public String getEventId() {
        return eventId;
    }

    public void setEventId(String eventId) {
        this.eventId = eventId;
    }

    public String getEventType() {
        return eventType;
    }

    public void setEventType(String eventType) {
        this.eventType = eventType;
    }

    public String getReferenceNumber() {
        return referenceNumber;
    }

    public void setReferenceNumber(String referenceNumber) {
        this.referenceNumber = referenceNumber;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
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

    public Instant getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(Instant timestamp) {
        this.timestamp = timestamp;
    }
}
