package com.fincore.event;

import com.fincore.dto.WebSocketNotificationDto;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import java.util.UUID;

@Component
public class TransferEventListener {

    private static final Logger log = LoggerFactory.getLogger(TransferEventListener.class);
    private final SimpMessagingTemplate messagingTemplate;

    public TransferEventListener(SimpMessagingTemplate messagingTemplate) {
        this.messagingTemplate = messagingTemplate;
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handleTransferCompleted(TransferCompletedEvent event) {
        log.info("Transaction committed successfully. Dispatching real-time WebSocket events for ref: {}", event.getReferenceNumber());

        // 1. Notify Sender via private STOMP user destination
        WebSocketNotificationDto senderNotification = new WebSocketNotificationDto(
                UUID.randomUUID().toString(),
                "TRANSFER_DEBIT",
                event.getReferenceNumber(),
                "Transfer Successful",
                String.format("Transferred %s %s to %s. New balance: %s",
                        event.getAmount(), event.getCurrency(), event.getDestinationAccountNumber(), event.getSenderNewBalance()),
                event.getAmount(),
                event.getCurrency(),
                event.getSourceAccountNumber(),
                event.getDestinationAccountNumber(),
                event.getTimestamp()
        );
        messagingTemplate.convertAndSendToUser(
                event.getSenderUsername(),
                "/queue/notifications",
                senderNotification
        );

        // 2. Notify Receiver via private STOMP user destination
        WebSocketNotificationDto receiverNotification = new WebSocketNotificationDto(
                UUID.randomUUID().toString(),
                "TRANSFER_CREDIT",
                event.getReferenceNumber(),
                "Funds Received",
                String.format("Received %s %s from %s. New balance: %s",
                        event.getAmount(), event.getCurrency(), event.getSourceAccountNumber(), event.getReceiverNewBalance()),
                event.getAmount(),
                event.getCurrency(),
                event.getSourceAccountNumber(),
                event.getDestinationAccountNumber(),
                event.getTimestamp()
        );
        messagingTemplate.convertAndSendToUser(
                event.getReceiverUsername(),
                "/queue/notifications",
                receiverNotification
        );

        // 3. Broadcast to Admin Activity Feed
        WebSocketNotificationDto adminNotification = new WebSocketNotificationDto(
                UUID.randomUUID().toString(),
                event.isFlagged() ? "FLAGGED_TRANSACTION" : "TRANSACTION_EXECUTED",
                event.getReferenceNumber(),
                event.isFlagged() ? "High-Risk Transfer Flagged" : "Transfer Executed",
                String.format("[%s -> %s] %s %s (Sender: %s, Receiver: %s)%s",
                        event.getSourceAccountNumber(), event.getDestinationAccountNumber(),
                        event.getAmount(), event.getCurrency(), event.getSenderUsername(), event.getReceiverUsername(),
                        event.isFlagged() ? " - Alert: " + event.getFraudReason() : ""),
                event.getAmount(),
                event.getCurrency(),
                event.getSourceAccountNumber(),
                event.getDestinationAccountNumber(),
                event.getTimestamp()
        );
        messagingTemplate.convertAndSend("/topic/admin/activity", adminNotification);

        log.info("WebSocket notifications successfully delivered for ref: {}", event.getReferenceNumber());
    }
}
