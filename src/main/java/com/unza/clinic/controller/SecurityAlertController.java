package com.unza.clinic.controller;

import com.unza.clinic.dto.SecurityAlertCreateRequest;
import com.unza.clinic.dto.SecurityAlertResolveRequest;
import com.unza.clinic.model.AuthUserDetails;
import com.unza.clinic.model.SecurityAlert;
import com.unza.clinic.model.SecurityAlertSourceType;
import com.unza.clinic.model.SecurityAlertStatus;
import com.unza.clinic.model.SecuritySystem;
import com.unza.clinic.repository.SecurityAlertRepository;
import com.unza.clinic.service.SecurityAlertSyncService;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Local REST API for sensitive-case Security alerts (self-harm, suicide,
 * rape/sexual assault, physical attack, panic button, other), visible to
 * university Security staff. Alerts raised here are also mirrored to the
 * counselling system's own SecurityAlert store (best-effort, see
 * SecurityAlertSyncService); alerts raised over there arrive here via
 * ExternalCounselingSecurityAlertController's inbound webhook.
 *
 * Per-endpoint permission gating happens primarily in
 * JwtAuthenticationFilter.resolveRequiredPermissions() (security.view /
 * security.report / security.acknowledge / security.resolve). The check
 * repeated here mirrors the existing ApiController#requirePermission pattern
 * used across this codebase (defense in depth + a place to read the current user).
 */
@RestController
@RequestMapping("/api/security-alerts")
public class SecurityAlertController {

    private static final Logger log = LoggerFactory.getLogger(SecurityAlertController.class);

    private final SecurityAlertRepository repository;
    private final SecurityAlertSyncService syncService;

    public SecurityAlertController(SecurityAlertRepository repository, SecurityAlertSyncService syncService) {
        this.repository = repository;
        this.syncService = syncService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public Map<String, Object> create(@Valid @RequestBody SecurityAlertCreateRequest request) {
        AuthUserDetails user = requirePermission("security.report");

        SecurityAlert alert = new SecurityAlert();
        alert.setCategory(request.category());
        alert.setSeverity(request.severity());
        alert.setSubjectStudentId(request.subjectStudentId());
        alert.setSubjectName(request.subjectName());
        alert.setDescription(request.description());
        alert.setLatitude(request.latitude());
        alert.setLongitude(request.longitude());
        alert.setReportedByUserId(user.getUserId());
        alert.setReportedByName(user.getName());
        alert.setOriginSystem(SecuritySystem.CLINIC);
        alert.setSourceType(SecurityAlertSourceType.MANUAL_REPORT);
        alert.setStatus(SecurityAlertStatus.NEW);
        alert.setOccurredAt(LocalDateTime.now());

        alert = repository.save(alert);

        // Best-effort mirror to counselling; never let a sync failure fail the local report.
        try {
            syncService.syncCreate(alert);
        } catch (Exception e) {
            log.warn("Security alert {} saved locally but sync to counselling failed: {}", alert.getId(), e.getMessage());
        }

        return toResponse(alert);
    }

    @GetMapping
    public List<Map<String, Object>> list(@RequestParam(required = false) String status,
                                           @RequestParam(required = false) String category) {
        requirePermission("security.view");
        return repository.findAllByOrderByOccurredAtDesc().stream()
                .filter(a -> status == null || status.isBlank() || a.getStatus().name().equalsIgnoreCase(status))
                .filter(a -> category == null || category.isBlank() || a.getCategory().name().equalsIgnoreCase(category))
                .map(this::toResponse)
                .toList();
    }

    @PostMapping("/{id}/acknowledge")
    public Map<String, Object> acknowledge(@PathVariable Long id) {
        AuthUserDetails user = requirePermission("security.acknowledge");
        SecurityAlert alert = requireAlert(id);

        alert.setStatus(SecurityAlertStatus.ACKNOWLEDGED);
        alert.setAcknowledgedByName(user.getName());
        alert.setAcknowledgedAt(LocalDateTime.now());
        alert = repository.save(alert);

        syncStatusSafely(alert, alert.getStatus().name(), user.getName(), null);
        return toResponse(alert);
    }

    @PostMapping("/{id}/resolve")
    public Map<String, Object> resolve(@PathVariable Long id,
                                        @RequestBody(required = false) SecurityAlertResolveRequest request) {
        AuthUserDetails user = requirePermission("security.resolve");
        SecurityAlert alert = requireAlert(id);

        boolean falsePositive = request != null && Boolean.TRUE.equals(request.falsePositive());
        String notes = request != null ? request.resolutionNotes() : null;

        alert.setStatus(falsePositive ? SecurityAlertStatus.FALSE_POSITIVE : SecurityAlertStatus.RESOLVED);
        alert.setResolvedByName(user.getName());
        alert.setResolvedAt(LocalDateTime.now());
        alert.setResolutionNotes(notes);
        alert = repository.save(alert);

        syncStatusSafely(alert, alert.getStatus().name(), user.getName(), notes);
        return toResponse(alert);
    }

    // ------------------------------------------------------------------
    // Helpers
    // ------------------------------------------------------------------

    private void syncStatusSafely(SecurityAlert alert, String status, String actorName, String notes) {
        try {
            syncService.syncStatus(alert, status, actorName, notes);
        } catch (Exception e) {
            log.warn("Security alert {} updated locally but sync to counselling failed: {}", alert.getId(), e.getMessage());
        }
    }

    private SecurityAlert requireAlert(Long id) {
        return repository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Security alert not found"));
    }

    private AuthUserDetails requirePermission(String permission) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated() || !(auth.getPrincipal() instanceof AuthUserDetails userDetails)) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Missing signed-in user context");
        }
        List<String> permissions = userDetails.getPermissions();
        if (permissions == null || !permissions.contains(permission)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "You do not have permission to perform this action");
        }
        return userDetails;
    }

    private Map<String, Object> toResponse(SecurityAlert a) {
        Map<String, Object> out = new LinkedHashMap<>();
        out.put("id", a.getId());
        out.put("category", a.getCategory());
        out.put("severity", a.getSeverity());
        out.put("originSystem", a.getOriginSystem());
        out.put("sourceType", a.getSourceType());
        out.put("subjectStudentId", a.getSubjectStudentId());
        out.put("subjectName", a.getSubjectName());
        out.put("reportedByUserId", a.getReportedByUserId());
        out.put("reportedByName", a.getReportedByName());
        out.put("description", a.getDescription());
        out.put("latitude", a.getLatitude());
        out.put("longitude", a.getLongitude());
        out.put("status", a.getStatus());
        out.put("acknowledgedByName", a.getAcknowledgedByName());
        out.put("acknowledgedAt", a.getAcknowledgedAt());
        out.put("resolvedByName", a.getResolvedByName());
        out.put("resolvedAt", a.getResolvedAt());
        out.put("resolutionNotes", a.getResolutionNotes());
        out.put("externalAlertId", a.getExternalAlertId());
        out.put("externalSystem", a.getExternalSystem());
        out.put("occurredAt", a.getOccurredAt());
        out.put("createdAt", a.getCreatedAt());
        out.put("updatedAt", a.getUpdatedAt());
        return out;
    }
}
