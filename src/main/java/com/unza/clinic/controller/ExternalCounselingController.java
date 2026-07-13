package com.unza.clinic.controller;

import com.unza.clinic.config.CounselingProperties;
import com.unza.clinic.service.CounselingTokenService;
import org.springframework.http.*;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.server.ResponseStatusException;

import java.util.*;

/**
 * Proxy bridge between the UNZA Clinic system and the Counseling system
 * at https://counselling.unza.ac.zm.
 *
 * All frontend counselingApi calls hit /api/external/counseling/* here.
 * This controller forwards them to /api/clinic/* on the counseling system,
 * translates field names / status values, and returns the result.
 *
 * Authentication is handled transparently by CounselingTokenService:
 *   - On first call it logs in with the configured service-account credentials.
 *   - The JWT (1-hour expiry) is cached and silently refreshed every 55 minutes.
 *   - If a static override token is set in app.counseling.service-token it is
 *     used directly without auto-login.
 *
 * Graceful offline: when the counseling system URL is not configured or is
 * unreachable every endpoint returns a safe empty response (no 5xx to the
 * clinic frontend).
 */
@RestController
@RequestMapping("/api/external/counseling")
public class ExternalCounselingController {

    private final RestTemplate rest = new RestTemplate();
    private final CounselingProperties props;
    private final CounselingTokenService tokenService;

    public ExternalCounselingController(CounselingProperties props,
                                        CounselingTokenService tokenService) {
        this.props        = props;
        this.tokenService = tokenService;
    }

    // ------------------------------------------------------------------
    // Referrals
    // ------------------------------------------------------------------

    @GetMapping("/referrals")
    public ResponseEntity<?> listReferrals(@RequestParam(required = false) String direction) {
        requireAuth();
        if (!props.isConfigured()) return ResponseEntity.ok(List.of());
        try {
            ResponseEntity<Object> resp = rest.exchange(
                    url("/api/clinic/referrals"),
                    HttpMethod.GET, tokenService.withAuth(), Object.class);
            return ResponseEntity.ok(resp.getBody());
        } catch (HttpClientErrorException e) {
            return ResponseEntity.status(e.getStatusCode()).body(err(e.getMessage()));
        } catch (ResourceAccessException e) {
            return ResponseEntity.ok(List.of());
        }
    }

    @PostMapping("/referrals")
    public ResponseEntity<?> createReferral(@RequestBody Map<String, Object> body) {
        requireAuth();
        if (!props.isConfigured()) return ResponseEntity.status(503).body(err("Counseling system not configured"));
        try {
            ResponseEntity<Object> resp = rest.postForEntity(
                    url("/api/clinic/referrals"),
                    tokenService.withAuth(toCouns(body)), Object.class);
            return ResponseEntity.status(resp.getStatusCode()).body(resp.getBody());
        } catch (HttpClientErrorException e) {
            return ResponseEntity.status(e.getStatusCode()).body(err(e.getMessage()));
        } catch (ResourceAccessException e) {
            return ResponseEntity.status(503).body(err("Counseling system unavailable"));
        }
    }

    @GetMapping("/referrals/{id}")
    public ResponseEntity<?> getReferral(@PathVariable String id) {
        requireAuth();
        if (!props.isConfigured()) return ResponseEntity.notFound().build();
        try {
            ResponseEntity<Object> resp = rest.exchange(
                    url("/api/clinic/referrals/" + id),
                    HttpMethod.GET, tokenService.withAuth(), Object.class);
            return ResponseEntity.ok(resp.getBody());
        } catch (HttpClientErrorException e) {
            return ResponseEntity.status(e.getStatusCode()).build();
        } catch (ResourceAccessException e) {
            return ResponseEntity.status(503).body(err("Counseling system unavailable"));
        }
    }

    /**
     * Frontend sends PUT {status: "pending|in_progress|completed|cancelled"}.
     * Counseling system expects PATCH …?status=PENDING|SENT|COMPLETED|DECLINED.
     */
    @PutMapping("/referrals/{id}/status")
    public ResponseEntity<?> updateReferralStatus(@PathVariable String id,
                                                  @RequestBody Map<String, Object> body) {
        requireAuth();
        if (!props.isConfigured()) return ResponseEntity.status(503).body(err("Counseling system not configured"));
        try {
            String counselingStatus = mapStatus((String) body.get("status"));
            ResponseEntity<Object> resp = rest.exchange(
                    url("/api/clinic/referrals/" + id + "/status?status=" + counselingStatus),
                    HttpMethod.PATCH, tokenService.withAuth(), Object.class);
            return ResponseEntity.ok(resp.getBody());
        } catch (HttpClientErrorException e) {
            return ResponseEntity.status(e.getStatusCode()).body(err(e.getMessage()));
        } catch (ResourceAccessException e) {
            return ResponseEntity.status(503).body(err("Counseling system unavailable"));
        }
    }

    // ------------------------------------------------------------------
    // Sessions / Visits
    // ------------------------------------------------------------------

    /** Maps "sessions" calls to counseling visit records per client. */
    @GetMapping("/sessions")
    public ResponseEntity<?> getSessions(@RequestParam(defaultValue = "all") String patientId) {
        requireAuth();
        if (!props.isConfigured() || "all".equals(patientId)) return ResponseEntity.ok(List.of());
        try {
            ResponseEntity<Object> resp = rest.exchange(
                    url("/api/clinic/visits/client/" + patientId),
                    HttpMethod.GET, tokenService.withAuth(), Object.class);
            return ResponseEntity.ok(resp.getBody());
        } catch (Exception e) {
            return ResponseEntity.ok(List.of());
        }
    }

    @GetMapping("/visits/frequency")
    public ResponseEntity<?> getVisitFrequency(@RequestParam String clientId) {
        requireAuth();
        if (!props.isConfigured()) return ResponseEntity.ok(Map.of("totalVisits", 0, "clientId", clientId));
        try {
            ResponseEntity<Object> resp = rest.exchange(
                    url("/api/clinic/visits/client/" + clientId + "/frequency"),
                    HttpMethod.GET, tokenService.withAuth(), Object.class);
            return ResponseEntity.ok(resp.getBody());
        } catch (Exception e) {
            return ResponseEntity.ok(Map.of("totalVisits", 0, "clientId", clientId));
        }
    }

    @GetMapping("/visits/frequent-visitors")
    public ResponseEntity<?> getFrequentVisitors(
            @RequestParam(defaultValue = "3") int threshold,
            @RequestParam(defaultValue = "90") int withinDays) {
        requireAuth();
        if (!props.isConfigured()) return ResponseEntity.ok(List.of());
        try {
            ResponseEntity<Object> resp = rest.exchange(
                    url("/api/clinic/visits/frequent-visitors?threshold=" + threshold + "&withinDays=" + withinDays),
                    HttpMethod.GET, tokenService.withAuth(), Object.class);
            return ResponseEntity.ok(resp.getBody());
        } catch (Exception e) {
            return ResponseEntity.ok(List.of());
        }
    }

    // ------------------------------------------------------------------
    // Inbound webhook — counseling system pushes visits back here
    // Configure this URL in the counseling system:
    //   POST https://<clinic-host>/api/external/counseling/inbound/visit
    // ------------------------------------------------------------------
    @PostMapping("/inbound/visit")
    public ResponseEntity<?> receiveInboundVisit(@RequestBody Map<String, Object> body) {
        Map<String, Object> ack = new LinkedHashMap<>();
        ack.put("received", true);
        ack.put("clientId", body.get("clientId"));
        ack.put("visitDate", body.get("visitDate"));
        ack.put("message", "Visit notification acknowledged");
        return ResponseEntity.ok(ack);
    }

    // ------------------------------------------------------------------
    // Helpers
    // ------------------------------------------------------------------

    private String url(String path) {
        return props.getBaseUrl().stripTrailing() + path;
    }

    private void requireAuth() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated() || "anonymousUser".equals(auth.getPrincipal())) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Authentication required");
        }
    }

    private Map<String, Object> err(String msg) {
        return Map.of("error", msg);
    }

    /** Translates clinic referral payload → counseling system fields. */
    private Map<String, Object> toCouns(Map<String, Object> src) {
        Map<String, Object> out = new LinkedHashMap<>();
        out.put("clientId",   src.get("patientId"));
        out.put("urgency",    mapUrgency((String) src.get("urgency")));
        out.put("reason",     src.getOrDefault("reason", ""));
        out.put("notes",      src.getOrDefault("notes", ""));
        out.put("referredBy", src.getOrDefault("referredBy", "Clinic"));
        out.put("externalId", src.get("patientId"));
        return out;
    }

    /** low/medium/high → NORMAL/URGENT/EMERGENCY */
    private String mapUrgency(String u) {
        if (u == null) return "NORMAL";
        return switch (u.toLowerCase()) {
            case "high"   -> "EMERGENCY";
            case "medium" -> "URGENT";
            default       -> "NORMAL";
        };
    }

    /** Clinic status → counseling lifecycle value */
    private String mapStatus(String s) {
        if (s == null) return "PENDING";
        return switch (s.toLowerCase()) {
            case "in_progress" -> "SENT";
            case "completed"   -> "COMPLETED";
            case "cancelled"   -> "DECLINED";
            default            -> "PENDING";
        };
    }
}
