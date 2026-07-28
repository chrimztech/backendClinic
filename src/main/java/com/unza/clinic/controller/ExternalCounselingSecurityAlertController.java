package com.unza.clinic.controller;

import com.unza.clinic.dto.SecurityAlertInboundRequest;
import com.unza.clinic.dto.SecurityAlertStatusUpdateRequest;
import com.unza.clinic.model.SecurityAlert;
import com.unza.clinic.model.SecurityAlertStatus;
import com.unza.clinic.model.SecuritySystem;
import com.unza.clinic.repository.SecurityAlertRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Inbound side of the security-alert cross-system sync: the counselling system
 * pushes its own alerts (and status changes to them) here.
 *
 * Both endpoints are service-to-service calls with no user JWT available on the
 * caller side, so they:
 *  - are listed under SecurityConfig's permitAll (bypassing JwtAuthenticationFilter
 *    entirely — see isPublicEndpoint() there for the matching path check), and
 *  - instead authenticate via the shared X-Service-Api-Key header, checked as the
 *    first step of each method here.
 *
 * Outbound direction (clinic -> counselling) lives in SecurityAlertSyncService,
 * which POSTs/PATCHes the mirror-image endpoints on the counselling system.
 */
@RestController
@RequestMapping("/api/external/counseling/security-alerts")
public class ExternalCounselingSecurityAlertController {

    private final SecurityAlertRepository repository;

    @Value("${app.cross-system.api-key:}")
    private String crossSystemApiKey;

    public ExternalCounselingSecurityAlertController(SecurityAlertRepository repository) {
        this.repository = repository;
    }

    @PostMapping("/inbound")
    public ResponseEntity<?> receiveInbound(@RequestHeader(value = "X-Service-Api-Key", required = false) String apiKey,
                                             @RequestBody SecurityAlertInboundRequest request) {
        if (!isValidServiceApiKey(apiKey)) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(err("Invalid or missing X-Service-Api-Key"));
        }

        SecurityAlert alert = new SecurityAlert();
        alert.setCategory(request.category());
        alert.setSeverity(request.severity());
        alert.setSourceType(request.sourceType());
        alert.setSubjectStudentId(request.subjectStudentId());
        alert.setSubjectName(request.subjectName());
        alert.setReportedByName(request.reportedByName());
        alert.setDescription(request.description());
        alert.setLatitude(request.latitude());
        alert.setLongitude(request.longitude());
        alert.setOriginSystem(SecuritySystem.COUNSELLING);
        alert.setStatus(SecurityAlertStatus.NEW);
        alert.setExternalAlertId(request.externalAlertId());
        alert.setExternalSystem(SecuritySystem.COUNSELLING);
        alert.setOccurredAt(parseOccurredAt(request.occurredAt()));

        alert = repository.save(alert);

        Map<String, Object> ack = new LinkedHashMap<>();
        ack.put("localAlertId", String.valueOf(alert.getId()));
        return ResponseEntity.ok(ack);
    }

    /**
     * {id} is OUR local id (the one returned as localAlertId from /inbound above).
     * Updates the row directly at the repository level — deliberately NOT via
     * SecurityAlertController's acknowledge/resolve flow, since that would trigger
     * an outbound sync back to counselling and create an infinite update loop.
     */
    @PatchMapping("/inbound/{id}/status")
    public ResponseEntity<?> updateInboundStatus(@RequestHeader(value = "X-Service-Api-Key", required = false) String apiKey,
                                                  @PathVariable Long id,
                                                  @RequestBody SecurityAlertStatusUpdateRequest request) {
        if (!isValidServiceApiKey(apiKey)) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(err("Invalid or missing X-Service-Api-Key"));
        }

        SecurityAlert alert = repository.findById(id).orElse(null);
        if (alert == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(err("Security alert not found"));
        }

        SecurityAlertStatus status = parseStatus(request.status());
        if (status == null) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(err("Invalid status: " + request.status()));
        }

        alert.setStatus(status);
        LocalDateTime now = LocalDateTime.now();
        switch (status) {
            case ACKNOWLEDGED -> {
                alert.setAcknowledgedByName(request.actorName());
                alert.setAcknowledgedAt(now);
            }
            case RESOLVED, FALSE_POSITIVE -> {
                alert.setResolvedByName(request.actorName());
                alert.setResolvedAt(now);
                if (request.resolutionNotes() != null) {
                    alert.setResolutionNotes(request.resolutionNotes());
                }
            }
            default -> { /* NEW: nothing extra to set */ }
        }
        repository.save(alert);

        Map<String, Object> ack = new LinkedHashMap<>();
        ack.put("updated", true);
        return ResponseEntity.ok(ack);
    }

    // ------------------------------------------------------------------
    // Helpers
    // ------------------------------------------------------------------

    private boolean isValidServiceApiKey(String provided) {
        return crossSystemApiKey != null && !crossSystemApiKey.isBlank()
                && crossSystemApiKey.equals(provided);
    }

    private SecurityAlertStatus parseStatus(String raw) {
        if (raw == null || raw.isBlank()) return null;
        try {
            return SecurityAlertStatus.valueOf(raw.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    private LocalDateTime parseOccurredAt(String raw) {
        if (raw == null || raw.isBlank()) return LocalDateTime.now();
        try {
            return LocalDateTime.ofInstant(Instant.parse(raw), ZoneId.systemDefault());
        } catch (Exception e) {
            try {
                return LocalDateTime.parse(raw);
            } catch (Exception e2) {
                return LocalDateTime.now();
            }
        }
    }

    private Map<String, Object> err(String msg) {
        return Map.of("error", msg);
    }
}
