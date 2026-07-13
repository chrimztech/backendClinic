package com.unza.clinic.service;

import com.unza.clinic.model.Drug;
import com.unza.clinic.model.NotificationItem;
import com.unza.clinic.repository.DrugRepository;
import com.unza.clinic.repository.NotificationRepository;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.time.temporal.ChronoUnit;
import java.util.List;

/**
 * Scheduled service that scans drug inventory for upcoming expiry and low stock,
 * persists NotificationItem records, and broadcasts via WebSocket.
 */
@Service
public class PharmacyAlertService {

    private final DrugRepository drugRepo;
    private final NotificationRepository notificationRepo;
    private final WebSocketNotificationService wsService;

    public PharmacyAlertService(DrugRepository drugRepo,
                                 NotificationRepository notificationRepo,
                                 WebSocketNotificationService wsService) {
        this.drugRepo = drugRepo;
        this.notificationRepo = notificationRepo;
        this.wsService = wsService;
    }

    // Run every 6 hours
    @Scheduled(fixedDelayString = "${app.pharmacy-alerts.check-interval-ms:21600000}")
    public void scanDrugInventory() {
        checkExpiringDrugs(30);
        checkLowStock();
    }

    public void checkExpiringDrugs(int daysAhead) {
        LocalDate threshold = LocalDate.now().plusDays(daysAhead);
        LocalDate today = LocalDate.now();

        for (Drug drug : drugRepo.findAll()) {
            if (drug.getExpiry() == null || drug.getExpiry().isBlank()) continue;
            try {
                LocalDate expiry = LocalDate.parse(drug.getExpiry());
                long daysLeft = ChronoUnit.DAYS.between(today, expiry);

                if (daysLeft < 0) {
                    // Already expired
                    persist("error",
                            "Drug Expired: " + drug.getName(),
                            drug.getName() + " (Batch " + nvl(drug.getBatchNumber()) + ") expired on " + expiry + ". Remove from stock immediately.");
                } else if (daysLeft <= daysAhead) {
                    // Expiring soon
                    persist("warning",
                            "Drug Expiring Soon: " + drug.getName(),
                            drug.getName() + " (Batch " + nvl(drug.getBatchNumber()) + ") expires in " + daysLeft + " day(s) on " + expiry + ".");
                }
            } catch (DateTimeParseException ignored) {}
        }
    }

    public void checkLowStock() {
        for (Drug drug : drugRepo.findAll()) {
            if (drug.getStock() == null || drug.getReorderLevel() == null) continue;
            if ("inactive".equalsIgnoreCase(drug.getStatus())) continue;

            if (drug.getStock() == 0) {
                persist("error",
                        "Drug Out of Stock: " + drug.getName(),
                        drug.getName() + " is completely out of stock. Reorder required.");
            } else if (drug.getStock() <= drug.getReorderLevel()) {
                persist("warning",
                        "Low Stock: " + drug.getName(),
                        drug.getName() + " has only " + drug.getStock() + " " + nvl(drug.getUnit()) + " remaining (reorder level: " + drug.getReorderLevel() + ").");
            }
        }
    }

    private void persist(String type, String title, String message) {
        // Avoid duplicate notifications: skip if an unread one with same title exists
        boolean exists = notificationRepo.findAll().stream()
                .anyMatch(n -> !n.isRead() && title.equals(n.getTitle()));
        if (exists) return;

        NotificationItem notif = new NotificationItem();
        notif.setType(type);
        notif.setTitle(title);
        notif.setMessage(message);
        notif.setTime(java.time.LocalDateTime.now().toString());
        notif.setRead(false);
        notif = notificationRepo.save(notif);
        wsService.broadcastNotification(notif);
    }

    private String nvl(String s) { return s != null ? s : ""; }
}
