package com.unza.clinic.service;

import com.unza.clinic.config.HrProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.Map;
import java.util.concurrent.locks.ReentrantLock;

/**
 * Manages the Bearer token used when the clinic backend calls the UNZA HR
 * system (devhr.unza.zm) for staff and dependents metadata.
 *
 * The HR system's JWT expiry is 1 hour (iat/exp 3600s apart). This service
 * caches the token and re-authenticates when fewer than 5 minutes remain, so
 * every outbound call to the HR system always carries a valid token.
 *
 * Priority:
 *   1. If app.hr.service-token is set, use it as-is (manual/static override).
 *   2. Otherwise call /api/auth-login with service-username + service-password
 *      and cache the resulting token for TOKEN_TTL_MS.
 *
 * Thread-safe: a ReentrantLock prevents simultaneous re-logins under concurrent load.
 */
@Service
public class HrTokenService {

    private static final Logger log = LoggerFactory.getLogger(HrTokenService.class);

    /** Refresh 5 minutes before the 1-hour HR system JWT expires. */
    private static final long TOKEN_TTL_MS = 55 * 60 * 1000L;

    private final HrProperties props;
    private final RestTemplate rest = new RestTemplate();
    private final ReentrantLock lock = new ReentrantLock();

    private volatile String cachedToken;
    private volatile long   tokenExpiresAt = 0;

    public HrTokenService(HrProperties props) {
        this.props = props;
    }

    /**
     * Returns a valid Bearer token string (without the "Bearer " prefix).
     * Returns null when the HR system is not configured.
     */
    public String getToken() {
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

    @SuppressWarnings("unchecked")
    private String refreshToken() {
        lock.lock();
        try {
            if (isTokenValid()) return cachedToken;

            if (props.getServiceUsername().isBlank() || props.getServicePassword().isBlank()) {
                log.warn("HR service credentials not configured — skipping auto-login");
                return null;
            }

            String loginUrl = props.getBaseUrl().stripTrailing() + "/api/auth-login";
            Map<String, String> creds = Map.of(
                    "username", props.getServiceUsername(),
                    "password", props.getServicePassword()
            );
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            HttpEntity<Map<String, String>> req = new HttpEntity<>(creds, headers);

            // Docs describe this call as GET with a JSON body, but Java's default HTTP client
            // (HttpURLConnection) silently drops the body on GET requests. The HR server accepts
            // POST with the identical payload/response shape, so we use POST here for reliability.
            ResponseEntity<Map> resp = rest.postForEntity(loginUrl, req, Map.class);
            if (resp.getStatusCode().is2xxSuccessful() && resp.getBody() != null) {
                String token = extractToken(resp.getBody());
                if (token != null) {
                    cachedToken    = token;
                    tokenExpiresAt = System.currentTimeMillis() + TOKEN_TTL_MS;
                    log.info("HR system token refreshed; next refresh in ~55 min");
                    return cachedToken;
                }
            }
            log.warn("HR system login returned no token (status {})", resp.getStatusCode());
            return null;

        } catch (Exception e) {
            log.warn("Failed to obtain HR system token: {}", e.getMessage());
            return null;
        } finally {
            lock.unlock();
        }
    }

    /** Unwraps { response: { status, message, data: { token } } }. */
    @SuppressWarnings("unchecked")
    private String extractToken(Map<String, Object> body) {
        Object responseObj = body.getOrDefault("response", body);
        if (!(responseObj instanceof Map)) return null;
        Map<String, Object> responseMap = (Map<String, Object>) responseObj;
        Object dataObj = responseMap.get("data");
        if (!(dataObj instanceof Map)) return null;
        Object token = ((Map<String, Object>) dataObj).get("token");
        return token != null ? token.toString() : null;
    }
}
