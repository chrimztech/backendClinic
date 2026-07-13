package com.unza.clinic.service;

import com.unza.clinic.model.NotificationItem;
import com.unza.clinic.model.VitalAlert;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

import java.util.Map;

/**
 * Broadcasts real-time events to connected WebSocket clients via STOMP topics.
 * Frontend subscribes to /topic/notifications, /topic/queue, /topic/lab-results,
 * /topic/vital-alerts, and /topic/ward-status.
 */
@Service
public class WebSocketNotificationService {

    private final SimpMessagingTemplate messagingTemplate;

    public WebSocketNotificationService(SimpMessagingTemplate messagingTemplate) {
        this.messagingTemplate = messagingTemplate;
    }

    public void broadcastNotification(NotificationItem notification) {
        messagingTemplate.convertAndSend("/topic/notifications", notification);
    }

    public void broadcastQueueUpdate(Object queueData) {
        messagingTemplate.convertAndSend("/topic/queue", queueData);
    }

    public void broadcastLabResult(Object labResult) {
        messagingTemplate.convertAndSend("/topic/lab-results", labResult);
    }

    public void broadcastVitalAlert(VitalAlert alert) {
        messagingTemplate.convertAndSend("/topic/vital-alerts", Map.of(
                "alertId", alert.getAlertId(),
                "patientId", alert.getPatientId(),
                "patientName", nvl(alert.getPatientName()),
                "vitalType", alert.getVitalType(),
                "value", alert.getValueText(),
                "severity", alert.getSeverity(),
                "message", nvl(alert.getMessage()),
                "createdAt", alert.getCreatedAt() != null ? alert.getCreatedAt().toString() : ""
        ));
    }

    public void broadcastWardStatus(Object wardStatus) {
        messagingTemplate.convertAndSend("/topic/ward-status", wardStatus);
    }

    public void broadcastPharmacyQueue(Object rxData) {
        messagingTemplate.convertAndSend("/topic/pharmacy-queue", rxData);
    }

    private String nvl(String s) {
        return s != null ? s : "";
    }
}
