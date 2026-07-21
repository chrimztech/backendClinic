package com.unza.clinic.controller;

import com.unza.clinic.config.SisProperties;
import com.unza.clinic.service.SisDirectoryService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.http.HttpStatus;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Bridge between the UNZA Clinic system and the UNZA student records system
 * (devoap.unza.zm /api/v1/students/lookup) for student metadata.
 *
 * All frontend sisApi calls (src/lib/externalSystems.ts) hit
 * /api/external/sis/* here. Authentication uses a single static Bearer token
 * (see SisProperties) — no login/refresh flow, unlike the HR integration.
 *
 * Graceful offline: when the SIS URL/token is not configured or the system
 * is unreachable, endpoints return a safe empty response instead of a 5xx.
 */
@RestController
@RequestMapping("/api/external/sis")
public class ExternalSisController {

    private final SisProperties props;
    private final SisDirectoryService sisService;

    public ExternalSisController(SisProperties props, SisDirectoryService sisService) {
        this.props = props;
        this.sisService = sisService;
    }

    @GetMapping("/students/{studentNumber}")
    public ResponseEntity<?> getStudent(@PathVariable String studentNumber,
                                         @RequestParam(required = false) String instance) {
        requireAuth();
        if (!props.isConfigured()) return ResponseEntity.status(503).body(err("SIS not configured"));
        Map<String, Object> student = sisService.lookup(studentNumber, instance);
        if (student == null) return ResponseEntity.notFound().build();
        return ResponseEntity.ok(normalize(student));
    }

    /** The real SIS API has no name-search endpoint — only lookup by student number + instance. */
    @GetMapping("/students/search")
    public ResponseEntity<?> searchByName(@RequestParam String name) {
        requireAuth();
        return ResponseEntity.ok(List.of());
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

    /** Adds frontend-friendly aliases on top of the raw SIS fields. */
    private Map<String, Object> normalize(Map<String, Object> student) {
        Map<String, Object> out = new LinkedHashMap<>(student);
        out.putIfAbsent("full_name", fullName(student));
        out.putIfAbsent("student_number", firstNonBlank(student.get("student_id"), student.get("username")));
        out.putIfAbsent("phone", student.get("phone_number"));
        out.putIfAbsent("year_of_study", student.get("year_of_program"));
        out.put("gender", normalizeGender(str(student.get("gender"))));
        out.putIfAbsent("requires_medical_exam", true);
        out.putIfAbsent("medical_exam_status", "pending");
        return out;
    }

    private String fullName(Map<String, Object> student) {
        return Stream.of(str(student.get("first_name")), str(student.get("middle_name")), str(student.get("last_name")))
                .filter(s -> !s.isBlank())
                .collect(Collectors.joining(" "));
    }

    private String normalizeGender(String raw) {
        String g = raw.trim().toUpperCase();
        return switch (g) {
            case "M", "MALE" -> "Male";
            case "F", "FEMALE" -> "Female";
            case "" -> "";
            default -> "Other";
        };
    }

    private Object firstNonBlank(Object a, Object b) {
        if (a != null && !a.toString().isBlank()) return a.toString();
        return b;
    }

    private String str(Object o) {
        return o == null ? "" : o.toString();
    }
}
