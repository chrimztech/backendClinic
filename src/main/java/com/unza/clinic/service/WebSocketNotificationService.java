package com.unza.clinic.service;

import com.unza.clinic.model.NotificationItem;
import com.unza.clinic.model.VitalAlert;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.Locale;

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

    public void broadcastQueueUpdate(String currentStage, Object queueData) {
        broadcastQueueUpdate(null, currentStage, queueData);
    }

    /**
     * Identifiable queue data is sent only to the destination department. The
     * source department receives a metadata-only refresh after a hand-off.
     */
    public void broadcastQueueUpdate(String previousStage, String currentStage, Object queueData) {
        String destinationStage = safeStage(currentStage);
        if (destinationStage.isBlank()) destinationStage = "UNKNOWN";
        Map<String, Object> signal = Map.of("event", "QUEUE_CHANGED", "stage", destinationStage);
        messagingTemplate.convertAndSend("/topic/queue", signal);
        messagingTemplate.convertAndSend("/topic/queue/" + destinationStage, queueData);

        String sourceStage = safeStage(previousStage);
        if (!sourceStage.isBlank() && !sourceStage.equals(destinationStage)) {
            messagingTemplate.convertAndSend("/topic/queue/" + sourceStage, signal);
        }
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

    private String safeStage(String stage) {
        String normalized = nvl(stage).trim().replace('-', '_').replace(' ', '_').toUpperCase(Locale.ROOT);
        return normalized.matches("[A-Z_]+") ? normalized : "";
    }
}
