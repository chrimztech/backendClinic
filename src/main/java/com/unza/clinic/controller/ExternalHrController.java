package com.unza.clinic.controller;

import com.unza.clinic.config.HrProperties;
import com.unza.clinic.service.HrDirectoryService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.http.HttpStatus;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Bridge between the UNZA Clinic system and the UNZA HR system
 * (devhr.unza.zm) for staff and dependents metadata.
 *
 * All frontend hrApi calls (src/lib/externalSystems.ts) hit
 * /api/external/hr/* here. Authentication against the HR system is handled
 * transparently by HrTokenService (auto-login + 55-minute token refresh).
 *
 * Graceful offline: when the HR system URL is not configured or is
 * unreachable, endpoints return a safe empty response instead of a 5xx.
 */
@RestController
@RequestMapping("/api/external/hr")
public class ExternalHrController {

    private final HrProperties props;
    private final HrDirectoryService hrService;

    public ExternalHrController(HrProperties props, HrDirectoryService hrService) {
        this.props = props;
        this.hrService = hrService;
    }

    @GetMapping("/staff/{staffNumber}")
    public ResponseEntity<?> getStaff(@PathVariable String staffNumber) {
        requireAuth();
        if (!props.isConfigured()) return ResponseEntity.status(503).body(err("HR system not configured"));
        Map<String, Object> staff = hrService.findByManNumber(staffNumber);
        if (staff == null) return ResponseEntity.notFound().build();
        return ResponseEntity.ok(normalize(staff));
    }

    @GetMapping("/staff/search")
    public ResponseEntity<?> searchByName(@RequestParam String name) {
        requireAuth();
        if (!props.isConfigured()) return ResponseEntity.ok(List.of());
        return ResponseEntity.ok(hrService.searchByName(name).stream().map(this::normalize).toList());
    }

    @GetMapping("/staff/{staffNumber}/details")
    public ResponseEntity<?> getStaffDetails(@PathVariable String staffNumber) {
        requireAuth();
        if (!props.isConfigured()) return ResponseEntity.status(503).body(err("HR system not configured"));
        Map<String, Object> staff = hrService.findByManNumber(staffNumber);
        if (staff == null) return ResponseEntity.notFound().build();
        return ResponseEntity.ok(normalize(staff));
    }

    @GetMapping("/staff/{staffNumber}/spouse")
    public ResponseEntity<?> getSpouse(@PathVariable String staffNumber) {
        requireAuth();
        if (!props.isConfigured()) return ResponseEntity.ok(Map.of());
        Map<String, Object> staff = hrService.findByManNumber(staffNumber);
        if (staff == null) return ResponseEntity.ok(Map.of());
        Map<String, Object> spouse = HrDirectoryService.dependents(staff).stream()
                .filter(d -> "Spouse".equalsIgnoreCase(String.valueOf(d.get("relationship"))))
                .findFirst().orElse(null);
        return ResponseEntity.ok(spouse != null ? spouse : Map.of());
    }

    @GetMapping("/staff/{staffNumber}/dependents")
    public ResponseEntity<?> getDependents(@PathVariable String staffNumber) {
        requireAuth();
        if (!props.isConfigured()) return ResponseEntity.ok(List.of());
        Map<String, Object> staff = hrService.findByManNumber(staffNumber);
        return ResponseEntity.ok(staff == null ? List.of() : HrDirectoryService.dependents(staff));
    }

    // ------------------------------------------------------------------

    private void requireAuth() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated() || "anonymousUser".equals(auth.getPrincipal())) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Authentication required");
        }
    }

    private Map<String, Object> err(String msg) {
        return Map.of("error", msg);
    }

    /** Adds frontend-friendly aliases on top of the raw HR fields (full_name, position, staff_number). */
    private Map<String, Object> normalize(Map<String, Object> staff) {
        Map<String, Object> out = new LinkedHashMap<>(staff);
        out.putIfAbsent("full_name", HrDirectoryService.fullName(staff));
        out.putIfAbsent("position", staff.get("job_title"));
        out.putIfAbsent("requires_medical_exam", true);
        return out;
    }
}
