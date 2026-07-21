package com.unza.clinic.service;

import com.unza.clinic.config.SisProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Looks up student records from the UNZA Online Application Portal / student
 * records system (devoap.unza.zm /api/v1/students/lookup).
 *
 * Each student number belongs to exactly one "instance" (campus/programme
 * track: UG, PG, GSB, IDE, ZOU, ecampus) and the API requires that instance
 * to be named in the request. When the caller doesn't know it, {@link
 * #lookup} fans the request out across all configured instances in
 * parallel and returns the first match.
 */
@Service
public class SisDirectoryService {

    private static final Logger log = LoggerFactory.getLogger(SisDirectoryService.class);

    private final SisProperties props;
    private final RestTemplate rest;
    private final ExecutorService executor = Executors.newCachedThreadPool();

    public SisDirectoryService(SisProperties props) {
        this.props = props;
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(6000);
        factory.setReadTimeout(10000);
        this.rest = new RestTemplate(factory);
    }

    public boolean isConfigured() {
        return props.isConfigured();
    }

    /**
     * Looks up a student by number. If {@code instance} is blank, tries every
     * configured instance in parallel and returns the first hit. Returns null
     * if not found in any instance, unconfigured, or unreachable.
     */
    public Map<String, Object> lookup(String studentNumber, String instance) {
        if (!isConfigured() || studentNumber == null || studentNumber.isBlank()) return null;
        if (instance != null && !instance.isBlank()) {
            return lookupInstance(studentNumber.trim(), instance.trim());
        }
        List<CompletableFuture<Map<String, Object>>> futures = props.getInstances().stream()
                .map(inst -> CompletableFuture.supplyAsync(() -> lookupInstance(studentNumber.trim(), inst), executor))
                .toList();
        return futures.stream()
                .map(CompletableFuture::join)
                .filter(result -> result != null)
                .findFirst()
                .orElse(null);
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> lookupInstance(String studentNumber, String instance) {
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.setBearerAuth(props.getToken());
            Map<String, String> body = Map.of("student_number", studentNumber, "instance", instance);
            HttpEntity<Map<String, String>> req = new HttpEntity<>(body, headers);

            ResponseEntity<Map> resp = rest.postForEntity(
                    props.getBaseUrl().stripTrailing() + "/api/v1/students/lookup", req, Map.class);
            return extractUser(resp.getBody(), instance);
        } catch (HttpClientErrorException.NotFound e) {
            return null;
        } catch (HttpClientErrorException e) {
            if (e.getStatusCode() == HttpStatusCode.valueOf(401)) {
                log.warn("SIS system rejected the configured token (401 Unauthenticated)");
            } else {
                log.warn("SIS lookup failed for instance {}: {} {}", instance, e.getStatusCode(), e.getMessage());
            }
            return null;
        } catch (Exception e) {
            log.warn("SIS lookup failed for instance {}: {}", instance, e.getMessage());
            return null;
        }
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> extractUser(Map<String, Object> body, String requestedInstance) {
        if (body == null || !Boolean.TRUE.equals(body.get("success"))) return null;
        Object dataObj = body.get("data");
        if (!(dataObj instanceof Map)) return null;
        Object userObj = ((Map<String, Object>) dataObj).get("user");
        if (!(userObj instanceof Map)) return null;
        Map<String, Object> user = (Map<String, Object>) userObj;
        user.putIfAbsent("instance", requestedInstance);
        return user;
    }
}
