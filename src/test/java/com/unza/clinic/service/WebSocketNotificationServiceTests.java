package com.unza.clinic.service;

import org.junit.jupiter.api.Test;
import org.springframework.messaging.simp.SimpMessagingTemplate;

import java.util.Map;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

class WebSocketNotificationServiceTests {

    @Test
    void handoffNotifiesDestinationWithDataAndSourceWithRefreshSignal() {
        SimpMessagingTemplate messagingTemplate = mock(SimpMessagingTemplate.class);
        WebSocketNotificationService service = new WebSocketNotificationService(messagingTemplate);
        Map<String, Object> encounter = Map.of("id", 42L, "current_stage", "CHECKOUT");
        Map<String, Object> signal = Map.of("event", "QUEUE_CHANGED", "stage", "CHECKOUT");

        service.broadcastQueueUpdate("CONSULTATION", "CHECKOUT", encounter);

        verify(messagingTemplate).convertAndSend("/topic/queue", signal);
        verify(messagingTemplate).convertAndSend("/topic/queue/CHECKOUT", encounter);
        verify(messagingTemplate).convertAndSend("/topic/queue/CONSULTATION", signal);
    }
}
