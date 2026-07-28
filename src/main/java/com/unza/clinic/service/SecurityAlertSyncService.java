package com.unza.clinic.service;

import com.unza.clinic.config.CounselingProperties;
import com.unza.clinic.dto.SecurityAlertInboundRequest;
import com.unza.clinic.dto.SecurityAlertStatusUpdateRequest;
import com.unza.clinic.model.SecurityAlert;
import com.unza.clinic.model.SecuritySystem;
import com.unza.clinic.repository.SecurityAlertRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.Map;

/**
 * Pushes locally-created/updated SecurityAlerts to the counselling system.
 *
 * Uses a simple shared static API key (X-Service-Api-Key header) rather than the
 * bearer-login flow that CounselingTokenService provides for the older
 * referrals/visits integration — this is a separate, simpler service-to-service
 * mechanism, deliberately kept independent of that flow.
 *
 * All failures are caught and logged; sync is best-effort and must never break
 * the local security-alert workflow (create/acknowledge/resolve always succeed
 * locally even if the counselling system is unreachable).
 */
@Service
public class SecurityAlertSyncService {

    private static final Logger log = LoggerFactory.getLogger(SecurityAlertSyncService.class);

    private final CounselingProperties props;
    private final SecurityAlertRepository repository;
    private final RestTemplate rest = new RestTemplate();

    @Value("${app.cross-system.api-key:}")
    private String apiKey;

    public SecurityAlertSyncService(CounselingProperties props, SecurityAlertRepository repository) {
        this.props = props;
        this.repository = repository;
    }

    /**
     * Push a newly-created local (CLINIC-origin) alert to counselling.
     * On success, remembers counselling's own id as our externalAlertId so later
     * acknowledge/resolve actions can be mirrored back over there.
     */
    public void syncCreate(SecurityAlert alert) {
        if (!props.isConfigured()) return;
        try {
            SecurityAlertInboundRequest payload = new SecurityAlertInboundRequest(
                    String.valueOf(alert.getId()),
                    alert.getCategory(),
                    alert.getSeverity(),
                    alert.getSourceType(),
                    alert.getSubjectStudentId(),
                    alert.getSubjectName(),
                    alert.getReportedByName(),
                    alert.getDescription(),
                    alert.getLatitude(),
                    alert.getLongitude(),
                    alert.getOccurredAt() != null ? alert.getOccurredAt().toString() : null
            );
            HttpEntity<SecurityAlertInboundRequest> req = new HttpEntity<>(payload, headers());
            ResponseEntity<Map> resp = rest.postForEntity(
                    url("/api/clinic/security-alerts/inbound"), req, Map.class);
            if (resp.getStatusCode().is2xxSuccessful() && resp.getBody() != null) {
                Object localAlertId = resp.getBody().get("localAlertId");
                if (localAlertId != null) {
                    alert.setExternalAlertId(String.valueOf(localAlertId));
                    alert.setExternalSystem(SecuritySystem.COUNSELLING);
                    repository.save(alert);
                }
            } else {
                log.warn("Counselling security-alert sync (create) returned status {}", resp.getStatusCode());
            }
        } catch (Exception e) {
            log.warn("Failed to sync new security alert {} to counselling: {}", alert.getId(), e.getMessage());
        }
    }

    /** Push an acknowledge/resolve status change to counselling for an alert already linked to it. */
    public void syncStatus(SecurityAlert alert, String status, String actorName, String resolutionNotes) {
        if (!props.isConfigured()) return;
        String externalId = alert.getExternalAlertId();
        if (externalId == null || externalId.isBlank()) return;
        try {
            SecurityAlertStatusUpdateRequest payload =
                    new SecurityAlertStatusUpdateRequest(status, actorName, resolutionNotes);
            HttpEntity<SecurityAlertStatusUpdateRequest> req = new HttpEntity<>(payload, headers());
            rest.exchange(
                    url("/api/clinic/security-alerts/inbound/" + externalId + "/status"),
                    HttpMethod.PATCH, req, Void.class);
        } catch (Exception e) {
            log.warn("Failed to sync status update for security alert {} to counselling: {}",
                    alert.getId(), e.getMessage());
        }
    }

    private String url(String path) {
        return props.getBaseUrl().stripTrailing() + path;
    }

    private HttpHeaders headers() {
        HttpHeaders h = new HttpHeaders();
        h.setContentType(MediaType.APPLICATION_JSON);
        if (apiKey != null && !apiKey.isBlank()) {
            h.set("X-Service-Api-Key", apiKey);
        }
        return h;
    }
}
