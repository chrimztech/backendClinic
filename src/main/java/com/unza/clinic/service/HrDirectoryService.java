package com.unza.clinic.service;

import com.unza.clinic.config.HrProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Fetches staff and dependents metadata from the UNZA HR system
 * (devhr.unza.zm /api/staff-directory).
 *
 * The full directory holds 4000+ records, so it is fetched on demand and
 * cached in memory for DIRECTORY_CACHE_TTL_MS to support name search without
 * hammering the HR system. Single man-number lookups always hit the HR
 * system directly (the API supports server-side filtering for that case).
 */
@Service
public class HrDirectoryService {

    private static final Logger log = LoggerFactory.getLogger(HrDirectoryService.class);
    private static final long DIRECTORY_CACHE_TTL_MS = 30 * 60 * 1000L;

    private final HrProperties props;
    private final HrTokenService tokenService;
    private final RestTemplate rest;

    private volatile List<Map<String, Object>> cachedDirectory;
    private volatile long cacheExpiresAt = 0;

    public HrDirectoryService(HrProperties props, HrTokenService tokenService) {
        this.props = props;
        this.tokenService = tokenService;
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(8000);
        factory.setReadTimeout(20000);
        this.rest = new RestTemplate(factory);
    }

    public boolean isConfigured() {
        return props.isConfigured();
    }

    /** Looks up a single staff record (with dependents) by HR man/staff number. Null if not found or unreachable. */
    public Map<String, Object> findByManNumber(String manNumber) {
        if (!isConfigured() || manNumber == null || manNumber.isBlank()) return null;
        try {
            String url = props.getBaseUrl().stripTrailing() + "/api/staff-directory?man_number="
                    + URLEncoder.encode(manNumber.trim(), StandardCharsets.UTF_8);
            ResponseEntity<Map> resp = rest.exchange(url, HttpMethod.GET, tokenService.withAuth(), Map.class);
            List<Map<String, Object>> staff = extractStaffList(resp.getBody());
            return staff.isEmpty() ? null : staff.get(0);
        } catch (Exception e) {
            log.warn("HR staff-directory lookup failed for man_number {}: {}", manNumber, e.getMessage());
            return null;
        }
    }

    /** Full directory, cached to avoid repeatedly pulling thousands of records. */
    public List<Map<String, Object>> getAllCached() {
        if (!isConfigured()) return List.of();
        if (cachedDirectory != null && System.currentTimeMillis() < cacheExpiresAt) {
            return cachedDirectory;
        }
        return refreshCache();
    }

    public List<Map<String, Object>> searchByName(String name) {
        if (name == null || name.trim().isEmpty()) return List.of();
        String needle = name.trim().toLowerCase();
        return getAllCached().stream()
                .filter(s -> fullName(s).toLowerCase().contains(needle))
                .limit(25)
                .toList();
    }

    public static List<Map<String, Object>> dependents(Map<String, Object> staff) {
        Object raw = staff == null ? null : staff.get("dependents");
        if (raw instanceof List<?> list) {
            @SuppressWarnings("unchecked")
            List<Map<String, Object>> deps = (List<Map<String, Object>>) list;
            return deps;
        }
        return List.of();
    }

    public static String fullName(Map<String, Object> staff) {
        return Stream.of(str(staff.get("first_name")), str(staff.get("middle_name")), str(staff.get("last_name")))
                .filter(s -> !s.isBlank())
                .collect(Collectors.joining(" "));
    }

    // ------------------------------------------------------------------

    private synchronized List<Map<String, Object>> refreshCache() {
        if (cachedDirectory != null && System.currentTimeMillis() < cacheExpiresAt) {
            return cachedDirectory;
        }
        try {
            String url = props.getBaseUrl().stripTrailing() + "/api/staff-directory";
            ResponseEntity<Map> resp = rest.exchange(url, HttpMethod.GET, tokenService.withAuth(), Map.class);
            List<Map<String, Object>> staff = extractStaffList(resp.getBody());
            cachedDirectory = staff;
            cacheExpiresAt = System.currentTimeMillis() + DIRECTORY_CACHE_TTL_MS;
            log.info("HR staff directory cached: {} records", staff.size());
            return staff;
        } catch (Exception e) {
            log.warn("Failed to fetch HR staff directory: {}", e.getMessage());
            return cachedDirectory != null ? cachedDirectory : List.of();
        }
    }

    @SuppressWarnings("unchecked")
    private List<Map<String, Object>> extractStaffList(Map body) {
        if (body == null) return List.of();
        Object responseObj = body.getOrDefault("response", body);
        if (!(responseObj instanceof Map)) return List.of();
        Object dataObj = ((Map<String, Object>) responseObj).get("data");
        if (!(dataObj instanceof Map)) return List.of();
        Object staffObj = ((Map<String, Object>) dataObj).get("staff");
        if (!(staffObj instanceof List)) return List.of();
        return (List<Map<String, Object>>) staffObj;
    }

    private static String str(Object o) {
        return o == null ? "" : o.toString();
    }
}
