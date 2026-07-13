package com.unza.clinic.controller;

import com.unza.clinic.config.CounselingProperties;
import com.unza.clinic.model.*;
import com.unza.clinic.service.ClinicDataStore;
import com.unza.clinic.service.CounselingTokenService;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestClientException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Supplemental endpoints: patient medical history timeline, expiring-drug query,
 * and morbidity report. Kept separate from ApiController to avoid further growth.
 */
@RestController
@RequestMapping("/api")
public class ClinicSupplementController {

    private final ClinicDataStore dataStore;
    private final CounselingProperties counselingProps;
    private final CounselingTokenService counselingTokenService;
    private final RestTemplate counselingRest = new RestTemplate();

    public ClinicSupplementController(ClinicDataStore dataStore,
                                      CounselingProperties counselingProps,
                                      CounselingTokenService counselingTokenService) {
        this.dataStore              = dataStore;
        this.counselingProps        = counselingProps;
        this.counselingTokenService = counselingTokenService;
    }

    // ------------------------------------------------------------------
    // GET /api/patients/{id}/history  — unified medical timeline
    // ------------------------------------------------------------------
    @GetMapping("/patients/{id}/history")
    public ResponseEntity<?> getPatientHistory(@PathVariable String id) {
        requireAuthenticated();
        Patient patient = dataStore.getPatientByPatientId(id);
        if (patient == null) {
            // try numeric DB id
            try {
                long dbId = Long.parseLong(id);
                patient = dataStore.getPatients().stream()
                        .filter(p -> p.getId() != null && p.getId() == dbId)
                        .findFirst().orElse(null);
            } catch (NumberFormatException ignored) {}
        }
        if (patient == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Patient not found");
        }

        final String pid = patient.getPatientId();
        List<Map<String, Object>> timeline = new ArrayList<>();

        // Triage visits
        dataStore.getTriageRecords().stream()
                .filter(t -> pid.equalsIgnoreCase(t.getPatientId()))
                .forEach(t -> {
                    Map<String, Object> e = new LinkedHashMap<>();
                    e.put("type", "triage");
                    e.put("date", nvl(t.getArrivalTime()));
                    e.put("summary", "Triage: " + nvl(t.getChiefComplaint()) + " [" + nvl(t.getLevel()) + "]");
                    e.put("detail", Map.of(
                            "level", nvl(t.getLevel()),
                            "chiefComplaint", nvl(t.getChiefComplaint()),
                            "vitalSigns", nvl(t.getVitalSigns()),
                            "bloodPressure", nvl(t.getBloodPressure()),
                            "status", nvl(t.getStatus()),
                            "nurseName", nvl(t.getNurseName())
                    ));
                    timeline.add(e);
                });

        // Encounters
        dataStore.getEncounterRecords().stream()
                .filter(enc -> pid.equalsIgnoreCase(enc.getPatientId()))
                .forEach(enc -> {
                    Map<String, Object> e = new LinkedHashMap<>();
                    e.put("type", "encounter");
                    e.put("date", nvl(enc.getCreatedAt()));
                    e.put("summary", "Encounter " + nvl(enc.getEncounterId()) + " — " + nvl(enc.getCurrentStage()));
                    e.put("detail", Map.of(
                            "encounterId", nvl(enc.getEncounterId()),
                            "stage", nvl(enc.getCurrentStage()),
                            "paymentStatus", nvl(enc.getPaymentStatus()),
                            "notes", nvl(enc.getNotes())
                    ));
                    timeline.add(e);
                });

        // Lab tests
        dataStore.getLabTests().stream()
                .filter(lt -> pid.equalsIgnoreCase(lt.getPatientId()))
                .forEach(lt -> {
                    Map<String, Object> e = new LinkedHashMap<>();
                    e.put("type", "lab_test");
                    e.put("date", nvl(lt.getDate()));
                    e.put("summary", "Lab: " + nvl(lt.getTest()) + " [" + nvl(lt.getStatus()) + "]");
                    e.put("detail", Map.of(
                            "testId", nvl(lt.getTestId()),
                            "test", nvl(lt.getTest()),
                            "category", nvl(lt.getCategory()),
                            "results", nvl(lt.getResults()),
                            "interpretation", nvl(lt.getInterpretation()),
                            "status", nvl(lt.getStatus())
                    ));
                    timeline.add(e);
                });

        // Prescriptions
        dataStore.getPrescriptions().stream()
                .filter(rx -> pid.equalsIgnoreCase(rx.getPatientId()))
                .forEach(rx -> {
                    Map<String, Object> e = new LinkedHashMap<>();
                    e.put("type", "prescription");
                    e.put("date", nvl(rx.getDate()));
                    e.put("summary", "Rx " + nvl(rx.getRxId()) + ": " + nvl(rx.getDrugName()) + " [" + nvl(rx.getStatus()) + "]");
                    e.put("detail", Map.of(
                            "rxId", nvl(rx.getRxId()),
                            "drugName", nvl(rx.getDrugName()),
                            "dosage", nvl(rx.getDosage()),
                            "duration", nvl(rx.getDuration()),
                            "instructions", nvl(rx.getInstructions()),
                            "status", nvl(rx.getStatus()),
                            "doctor", nvl(rx.getDoctor())
                    ));
                    timeline.add(e);
                });

        // Admissions
        dataStore.getAdmissions().stream()
                .filter(adm -> pid.equalsIgnoreCase(adm.getPatientId()))
                .forEach(adm -> {
                    Map<String, Object> e = new LinkedHashMap<>();
                    e.put("type", "admission");
                    e.put("date", nvl(adm.getAdmittedOn()));
                    e.put("summary", "Admitted: " + nvl(adm.getWard()) + " / " + nvl(adm.getBed()) + " [" + nvl(adm.getStatus()) + "]");
                    e.put("detail", Map.of(
                            "admissionId", nvl(adm.getAdmissionId()),
                            "ward", nvl(adm.getWard()),
                            "bed", nvl(adm.getBed()),
                            "diagnosis", nvl(adm.getDiagnosis()),
                            "doctor", nvl(adm.getDoctor()),
                            "status", nvl(adm.getStatus())
                    ));
                    timeline.add(e);
                });

        // Referrals
        dataStore.getReferralRecords().stream()
                .filter(ref -> pid.equalsIgnoreCase(ref.getPatientId()))
                .forEach(ref -> {
                    Map<String, Object> e = new LinkedHashMap<>();
                    e.put("type", "referral");
                    e.put("date", nvl(ref.getDate()));
                    e.put("summary", "Referral: " + nvl(ref.getFromDept()) + " → " + nvl(ref.getToDept()) + " (" + nvl(ref.getUrgency()) + ")");
                    e.put("detail", Map.of(
                            "referralId", nvl(ref.getReferralId()),
                            "reason", nvl(ref.getReason()),
                            "fromDept", nvl(ref.getFromDept()),
                            "toDept", nvl(ref.getToDept()),
                            "urgency", nvl(ref.getUrgency()),
                            "status", nvl(ref.getStatus())
                    ));
                    timeline.add(e);
                });

        // Imaging
        dataStore.getImagingRequests().stream()
                .filter(img -> pid.equalsIgnoreCase(img.getPatientId()))
                .forEach(img -> {
                    Map<String, Object> e = new LinkedHashMap<>();
                    e.put("type", "imaging");
                    e.put("date", nvl(img.getRequestDate()));
                    e.put("summary", "Imaging: " + nvl(img.getType()) + " / " + nvl(img.getBodyPart()) + " [" + nvl(img.getStatus()) + "]");
                    e.put("detail", Map.of(
                            "requestId", nvl(img.getRequestId()),
                            "type", nvl(img.getType()),
                            "bodyPart", nvl(img.getBodyPart()),
                            "findings", nvl(img.getFindings()),
                            "status", nvl(img.getStatus())
                    ));
                    timeline.add(e);
                });

        // Counseling system clinic visits (graceful: skipped when counseling system is not connected)
        if (counselingProps.isConfigured()) {
            try {
                ResponseEntity<List> resp = counselingRest.exchange(
                        counselingProps.getBaseUrl().stripTrailing() + "/api/clinic/visits/client/" + pid,
                        HttpMethod.GET,
                        counselingTokenService.withAuth(), List.class);
                List<?> visits = resp.getBody();
                if (visits != null) {
                    for (Object item : visits) {
                        if (!(item instanceof Map)) continue;
                        @SuppressWarnings("unchecked")
                        Map<String, Object> v = (Map<String, Object>) item;
                        Map<String, Object> e = new LinkedHashMap<>();
                        e.put("type", "counseling_visit");
                        e.put("date", nvl(String.valueOf(v.getOrDefault("visitDate", ""))));
                        e.put("summary", "Counseling Visit: " + nvl(String.valueOf(v.getOrDefault("visitType", "")))
                                + " — " + nvl(String.valueOf(v.getOrDefault("visitPurpose", ""))));
                        e.put("detail", Map.of(
                                "visitType",    nvl(String.valueOf(v.getOrDefault("visitType", ""))),
                                "visitPurpose", nvl(String.valueOf(v.getOrDefault("visitPurpose", ""))),
                                "notes",        nvl(String.valueOf(v.getOrDefault("notes", ""))),
                                "source",       "counseling_system"
                        ));
                        timeline.add(e);
                    }
                }
            } catch (RuntimeException ignored) {
                // Counseling system unavailable or returned unexpected data — continue with local timeline
            }
        }

        // Sort descending by date string (ISO dates sort lexicographically)
        timeline.sort((a, b) -> {
            String da = (String) a.getOrDefault("date", "");
            String db = (String) b.getOrDefault("date", "");
            return db.compareTo(da);
        });

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("patientId", patient.getPatientId());
        result.put("patientName", patient.getName());
        result.put("totalEvents", timeline.size());
        result.put("timeline", timeline);
        return ResponseEntity.ok(result);
    }

    // ------------------------------------------------------------------
    // GET /api/pharmacy/expiring-drugs?days=30
    // ------------------------------------------------------------------
    @GetMapping("/pharmacy/expiring-drugs")
    public ResponseEntity<?> getExpiringDrugs(
            @RequestParam(defaultValue = "30") int days) {
        requireAuthenticated();

        LocalDate today = LocalDate.now();

        List<Map<String, Object>> results = new ArrayList<>();
        for (Drug drug : dataStore.getDrugs()) {
            if (drug.getExpiry() == null || drug.getExpiry().isBlank()) continue;
            try {
                LocalDate expiry = LocalDate.parse(drug.getExpiry());
                long daysLeft = ChronoUnit.DAYS.between(today, expiry);
                if (daysLeft <= days) {
                    Map<String, Object> row = new LinkedHashMap<>();
                    row.put("id", drug.getId());
                    row.put("name", nvl(drug.getName()));
                    row.put("batchNumber", nvl(drug.getBatchNumber()));
                    row.put("expiry", expiry.toString());
                    row.put("daysUntilExpiry", daysLeft);
                    row.put("status", daysLeft < 0 ? "expired" : "expiring_soon");
                    row.put("stock", drug.getStock());
                    row.put("unit", nvl(drug.getUnit()));
                    results.add(row);
                }
            } catch (DateTimeParseException ignored) {}
        }

        results.sort(Comparator.comparingLong(r -> (Long) ((Map<?, ?>) r).get("daysUntilExpiry")));

        return ResponseEntity.ok(Map.of(
                "daysAhead", days,
                "count", results.size(),
                "drugs", results
        ));
    }

    // ------------------------------------------------------------------
    // GET /api/reports/morbidity  — top diagnoses / chief complaints
    // ------------------------------------------------------------------
    @GetMapping("/reports/morbidity")
    public ResponseEntity<?> getMorbidityReport(
            @RequestParam(defaultValue = "20") int top) {
        requireAuthenticated();

        Map<String, Long> byComplaint = dataStore.getTriageRecords().stream()
                .map(TriageRecord::getChiefComplaint)
                .filter(c -> c != null && !c.isBlank())
                .collect(Collectors.groupingBy(
                        c -> normalise(c),
                        Collectors.counting()
                ));

        List<Map<String, Object>> ranked = byComplaint.entrySet().stream()
                .sorted(Map.Entry.<String, Long>comparingByValue().reversed())
                .limit(top)
                .map(entry -> {
                    Map<String, Object> row = new LinkedHashMap<>();
                    row.put("complaint", entry.getKey());
                    row.put("count", entry.getValue());
                    return row;
                })
                .collect(Collectors.toList());

        long total = byComplaint.values().stream().mapToLong(Long::longValue).sum();

        return ResponseEntity.ok(Map.of(
                "reportDate", LocalDate.now().toString(),
                "totalTriageRecords", dataStore.getTriageRecords().size(),
                "totalWithComplaint", total,
                "topDiagnoses", ranked
        ));
    }

    // ------------------------------------------------------------------
    // helpers
    // ------------------------------------------------------------------
    private void requireAuthenticated() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated() || "anonymousUser".equals(auth.getPrincipal())) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Authentication required");
        }
    }

    private String nvl(String s) { return s != null ? s : ""; }

    private String normalise(String s) {
        if (s == null) return "";
        return s.trim().toLowerCase(Locale.ROOT)
                .replaceAll("\\s+", " ");
    }
}
