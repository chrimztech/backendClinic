package com.unza.clinic.service;

import com.unza.clinic.config.CounselingProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.Map;
import java.util.concurrent.locks.ReentrantLock;

/**
 * Manages the Bearer token used when the clinic backend calls the counseling system.
 *
 * The counseling system's JWT expiry is 1 hour (3 600 000 ms). This service caches
 * the token and silently re-authenticates when fewer than 5 minutes remain, so
 * every outbound call to counselling.unza.ac.zm always carries a valid token.
 *
 * Priority:
 *   1. If app.counseling.service-token is set, use it as-is (manual/static override).
 *   2. Otherwise POST to /api/auth/login with service-username + service-password
 *      and cache the resulting token for TOKEN_TTL_MS.
 *
 * Thread-safe: a ReentrantLock prevents simultaneous re-logins under concurrent load.
 */
@Service
public class CounselingTokenService {

    private static final Logger log = LoggerFactory.getLogger(CounselingTokenService.class);

    /** Refresh 5 minutes before the 1-hour counseling JWT expires. */
    private static final long TOKEN_TTL_MS = 55 * 60 * 1000L;

    private final CounselingProperties props;
    private final RestTemplate rest = new RestTemplate();
    private final ReentrantLock lock = new ReentrantLock();

    private volatile String cachedToken;
    private volatile long   tokenExpiresAt = 0;

    public CounselingTokenService(CounselingProperties props) {
        this.props = props;
    }

    /**
     * Returns a valid Bearer token string (without the "Bearer " prefix).
     * Returns null when the counseling system is not configured.
     */
    public String getToken() {
        // Static override — skip auto-login
        if (!props.getServiceToken().isBlank()) {
            return props.getServiceToken();
        }
        if (!props.isConfigured()) {
            return null;
        }
        if (isTokenValid()) {
            return cachedToken;
        }
        return refreshToken();
    }

    /** Builds an HttpEntity with the Authorization header set. */
    public <T> HttpEntity<T> withAuth(T body) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        String token = getToken();
        if (token != null) {
            headers.setBearerAuth(token);
        }
        return new HttpEntity<>(body, headers);
    }

    /** Convenience overload for GET/DELETE (no body). */
    public HttpEntity<Void> withAuth() {
        return withAuth(null);
    }

    // ------------------------------------------------------------------

    private boolean isTokenValid() {
        return cachedToken != null && System.currentTimeMillis() < tokenExpiresAt;
    }

    private String refreshToken() {
        lock.lock();
        try {
            // Double-check inside the lock
            if (isTokenValid()) return cachedToken;

            if (props.getServiceUsername().isBlank() || props.getServicePassword().isBlank()) {
                log.warn("Counseling service credentials not configured — skipping auto-login");
                return null;
            }

            String loginUrl = props.getBaseUrl().stripTrailing() + "/api/auth/login";
            Map<String, String> creds = Map.of(
                    "username", props.getServiceUsername(),
                    "password", props.getServicePassword()
            );
            HttpEntity<Map<String, String>> req = new HttpEntity<>(creds,
                    jsonHeaders());

            ResponseEntity<Map> resp = rest.postForEntity(loginUrl, req, Map.class);
            if (resp.getStatusCode().is2xxSuccessful() && resp.getBody() != null) {
                // Counseling system returns { token: "...", type: "Bearer" }
                // or { accessToken: "..." } depending on implementation
                Object raw = resp.getBody().getOrDefault("token",
                             resp.getBody().get("accessToken"));
                if (raw != null) {
                    cachedToken    = raw.toString();
                    tokenExpiresAt = System.currentTimeMillis() + TOKEN_TTL_MS;
                    log.info("Counseling system token refreshed; next refresh in ~55 min");
                    return cachedToken;
                }
            }
            log.warn("Counseling system login returned no token (status {})", resp.getStatusCode());
            return null;

        } catch (Exception e) {
            log.warn("Failed to obtain counseling system token: {}", e.getMessage());
            return null;
        } finally {
            lock.unlock();
        }
    }

    private HttpHeaders jsonHeaders() {
        HttpHeaders h = new HttpHeaders();
        h.setContentType(MediaType.APPLICATION_JSON);
        return h;
    }
}
