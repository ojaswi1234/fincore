package com.fincore;

import com.fincore.dto.WebSocketNotificationDto;
import com.fincore.event.TransferCompletedEvent;
import com.fincore.event.TransferEventListener;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.messaging.simp.SimpMessagingTemplate;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
public class TransferEventListenerTest {

    @Mock
    private SimpMessagingTemplate messagingTemplate;

    @InjectMocks
    private TransferEventListener listener;

    @Test
    @DisplayName("Should publish STOMP notifications to sender user destination, recipient user destination, and admin topic")
    void testHandleTransferCompleted_PublishesWebSocketMessages() {
        TransferCompletedEvent event = new TransferCompletedEvent(
                "TX-REF-12345",
                "john_doe",
                "jane_smith",
                "ACC-10001",
                "ACC-20001",
                new BigDecimal("5000.00"),
                "INR",
                new BigDecimal("45000.00"),
                new BigDecimal("25000.00"),
                false,
                null
        );

        listener.handleTransferCompleted(event);

        // Verify sender destination
        ArgumentCaptor<WebSocketNotificationDto> senderCaptor = ArgumentCaptor.forClass(WebSocketNotificationDto.class);
        verify(messagingTemplate).convertAndSendToUser(eq("john_doe"), eq("/queue/notifications"), senderCaptor.capture());
        assertThat(senderCaptor.getValue().getEventType()).isEqualTo("TRANSFER_DEBIT");
        assertThat(senderCaptor.getValue().getReferenceNumber()).isEqualTo("TX-REF-12345");

        // Verify receiver destination
        ArgumentCaptor<WebSocketNotificationDto> receiverCaptor = ArgumentCaptor.forClass(WebSocketNotificationDto.class);
        verify(messagingTemplate).convertAndSendToUser(eq("jane_smith"), eq("/queue/notifications"), receiverCaptor.capture());
        assertThat(receiverCaptor.getValue().getEventType()).isEqualTo("TRANSFER_CREDIT");

        // Verify admin broadcast topic
        ArgumentCaptor<WebSocketNotificationDto> adminCaptor = ArgumentCaptor.forClass(WebSocketNotificationDto.class);
        verify(messagingTemplate).convertAndSend(eq("/topic/admin/activity"), adminCaptor.capture());
        assertThat(adminCaptor.getValue().getEventType()).isEqualTo("TRANSACTION_EXECUTED");
    }
}
