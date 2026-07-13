package com.unza.clinic.service;

import com.unza.clinic.model.*;
import com.unza.clinic.repository.*;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.*;

/**
 * Periodically scans recent triage records for abnormal vital signs and
 * persists VitalAlert records.  Also broadcasts alerts via WebSocket.
 *
 * Thresholds are based on WHO/ATLS emergency criteria.
 */
@Service
public class VitalAlertService {

    private final TriageRepository triageRepo;
    private final VitalAlertRepository alertRepo;
    private final NotificationRepository notificationRepo;
    private final WebSocketNotificationService wsService;

    public VitalAlertService(TriageRepository triageRepo,
                              VitalAlertRepository alertRepo,
                              NotificationRepository notificationRepo,
                              WebSocketNotificationService wsService) {
        this.triageRepo = triageRepo;
        this.alertRepo = alertRepo;
        this.notificationRepo = notificationRepo;
        this.wsService = wsService;
    }

    // Run every 5 minutes
    @Scheduled(fixedDelayString = "${app.vital-alerts.check-interval-ms:300000}")
    public void scanForAbnormalVitals() {
        List<TriageRecord> recent = triageRepo.findAll();
        for (TriageRecord t : recent) {
            checkBloodPressure(t);
            checkOxygenSaturation(t);
            checkTemperature(t);
            checkPulseRate(t);
            checkRandomBloodSugar(t);
        }
    }

    public List<VitalAlert> getAlertsForPatient(String patientId) {
        return alertRepo.findByPatientIdOrderByCreatedAtDesc(patientId);
    }

    public List<VitalAlert> getUnacknowledgedAlerts() {
        return alertRepo.findByAcknowledged(false);
    }

    public VitalAlert acknowledge(String alertId, String acknowledgedBy) {
        VitalAlert alert = alertRepo.findByAlertId(alertId).orElse(null);
        if (alert == null) return null;
        alert.setAcknowledged(true);
        alert.setAcknowledgedBy(acknowledgedBy);
        alert.setAcknowledgedAt(LocalDateTime.now());
        return alertRepo.save(alert);
    }

    // ----------------------------------------------------------------
    // Private checkers
    // ----------------------------------------------------------------

    private void checkBloodPressure(TriageRecord t) {
        String bp = t.getBloodPressure();
        if (bp == null || bp.isBlank()) return;
        try {
            String[] parts = bp.split("[/\\\\]");
            if (parts.length < 2) return;
            int systolic = Integer.parseInt(parts[0].trim());
            int diastolic = Integer.parseInt(parts[1].trim());

            if (systolic >= 180 || diastolic >= 110) {
                createAlertIfNew(t, "BLOOD_PRESSURE", bp, "< 180/110", "CRITICAL",
                        "Hypertensive crisis: BP " + bp + " for patient " + t.getPatientName() + ". Immediate assessment required.");
            } else if (systolic >= 140 || diastolic >= 90) {
                createAlertIfNew(t, "BLOOD_PRESSURE", bp, "< 140/90", "WARNING",
                        "Stage 2 hypertension: BP " + bp + " for patient " + t.getPatientName() + ".");
            } else if (systolic < 90) {
                createAlertIfNew(t, "BLOOD_PRESSURE", bp, "> 90 systolic", "CRITICAL",
                        "Hypotension: BP " + bp + " for patient " + t.getPatientName() + ". Shock protocol may be needed.");
            }
        } catch (NumberFormatException ignored) {}
    }

    private void checkOxygenSaturation(TriageRecord t) {
        Integer spo2 = t.getOxygenSaturation();
        if (spo2 == null) return;
        if (spo2 < 90) {
            createAlertIfNew(t, "O2_SATURATION", spo2 + "%", ">= 94%", "CRITICAL",
                    "Critical low O2 saturation: " + spo2 + "% for patient " + t.getPatientName() + ". Supplemental oxygen required.");
        } else if (spo2 < 94) {
            createAlertIfNew(t, "O2_SATURATION", spo2 + "%", ">= 94%", "WARNING",
                    "Low O2 saturation: " + spo2 + "% for patient " + t.getPatientName() + ".");
        }
    }

    private void checkTemperature(TriageRecord t) {
        Double temp = t.getTemperature();
        if (temp == null) return;
        if (temp >= 39.5) {
            createAlertIfNew(t, "TEMPERATURE", temp + "°C", "< 39.5°C", "CRITICAL",
                    "High fever: " + temp + "°C for patient " + t.getPatientName() + ". Sepsis workup may be indicated.");
        } else if (temp >= 38.0) {
            createAlertIfNew(t, "TEMPERATURE", temp + "°C", "< 38.0°C", "WARNING",
                    "Fever: " + temp + "°C for patient " + t.getPatientName() + ".");
        } else if (temp < 35.5) {
            createAlertIfNew(t, "TEMPERATURE", temp + "°C", "> 35.5°C", "CRITICAL",
                    "Hypothermia: " + temp + "°C for patient " + t.getPatientName() + ".");
        }
    }

    private void checkPulseRate(TriageRecord t) {
        Integer pulse = t.getPulseRate();
        if (pulse == null) return;
        if (pulse > 120) {
            createAlertIfNew(t, "PULSE_RATE", pulse + " bpm", "60-100 bpm", "CRITICAL",
                    "Tachycardia: " + pulse + " bpm for patient " + t.getPatientName() + ".");
        } else if (pulse < 50) {
            createAlertIfNew(t, "PULSE_RATE", pulse + " bpm", "60-100 bpm", "CRITICAL",
                    "Bradycardia: " + pulse + " bpm for patient " + t.getPatientName() + ".");
        }
    }

    private void checkRandomBloodSugar(TriageRecord t) {
        Double rbs = t.getRandomBloodSugar();
        if (rbs == null) return;
        if (rbs < 3.0) {
            createAlertIfNew(t, "BLOOD_GLUCOSE", rbs + " mmol/L", "> 3.0 mmol/L", "CRITICAL",
                    "Severe hypoglycaemia: " + rbs + " mmol/L for patient " + t.getPatientName() + ". Immediate glucose required.");
        } else if (rbs > 14.0) {
            createAlertIfNew(t, "BLOOD_GLUCOSE", rbs + " mmol/L", "< 14.0 mmol/L", "WARNING",
                    "Hyperglycaemia: " + rbs + " mmol/L for patient " + t.getPatientName() + ".");
        }
    }

    private void createAlertIfNew(TriageRecord triage, String vitalType, String value,
                                   String threshold, String severity, String message) {
        // Avoid duplicate alerts for the same triage record + vital type
        boolean exists = alertRepo.findByPatientId(triage.getPatientId()).stream()
                .anyMatch(a -> vitalType.equals(a.getVitalType())
                        && triage.getId() != null && triage.getId().equals(a.getTriageId())
                        && !a.isAcknowledged());
        if (exists) return;

        VitalAlert alert = new VitalAlert();
        alert.setAlertId("VA-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase());
        alert.setPatientId(triage.getPatientId());
        alert.setPatientName(triage.getPatientName());
        alert.setTriageId(triage.getId());
        alert.setVitalType(vitalType);
        alert.setValueText(value);
        alert.setThresholdText(threshold);
        alert.setSeverity(severity);
        alert.setMessage(message);
        alert.setAcknowledged(false);
        alert.setCreatedAt(LocalDateTime.now());
        alert = alertRepo.save(alert);

        // Persist as a system notification too
        NotificationItem notif = new NotificationItem();
        notif.setType("CRITICAL".equals(severity) ? "error" : "warning");
        notif.setTitle(("CRITICAL".equals(severity) ? "Critical " : "") + "Vital Alert — " + triage.getPatientName());
        notif.setMessage(message);
        notif.setTime(LocalDateTime.now().toString());
        notif.setRead(false);
        notificationRepo.save(notif);

        wsService.broadcastVitalAlert(alert);
    }
}
