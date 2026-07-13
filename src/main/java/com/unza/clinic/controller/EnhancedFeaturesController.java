package com.unza.clinic.controller;

import com.unza.clinic.model.*;
import com.unza.clinic.service.*;
import com.unza.clinic.repository.*;
import com.unza.clinic.util.JwtUtil;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.http.*;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.io.IOException;
import java.math.BigDecimal;
import java.net.MalformedURLException;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Controller for all enhanced features added in V3:
 *  - Feature 2:  Patient document upload / download
 *  - Feature 4:  Drug interaction + allergy safety check
 *  - Feature 5:  Vital sign alerts (acknowledge / list)
 *  - Feature 6:  Lab reference ranges CRUD + result approval
 *  - Feature 7:  Financial reporting + insurance claim lifecycle
 *  - Feature 8:  JWT refresh-token rotation + logout
 *  - Feature 9:  Excel + PDF export
 *  - Feature 11: Controlled-substance register CRUD
 */
@RestController
@RequestMapping("/api")
public class EnhancedFeaturesController {

    private final ClinicDataStore dataStore;
    private final DocumentStorageService docService;
    private final AllergySafetyService safetyService;
    private final VitalAlertService vitalAlertService;
    private final ExportService exportService;
    private final RefreshTokenService refreshTokenService;
    private final JwtUtil jwtUtil;
    private final BillingRepository billingRepo;
    private final InsuranceClaimRepository claimRepo;
    private final LabTestRepository labTestRepo;
    private final PatientRepository patientRepo;

    public EnhancedFeaturesController(
            ClinicDataStore dataStore,
            DocumentStorageService docService,
            AllergySafetyService safetyService,
            VitalAlertService vitalAlertService,
            ExportService exportService,
            RefreshTokenService refreshTokenService,
            JwtUtil jwtUtil,
            BillingRepository billingRepo,
            InsuranceClaimRepository claimRepo,
            LabTestRepository labTestRepo,
            PatientRepository patientRepo) {
        this.dataStore = dataStore;
        this.docService = docService;
        this.safetyService = safetyService;
        this.vitalAlertService = vitalAlertService;
        this.exportService = exportService;
        this.refreshTokenService = refreshTokenService;
        this.jwtUtil = jwtUtil;
        this.billingRepo = billingRepo;
        this.claimRepo = claimRepo;
        this.labTestRepo = labTestRepo;
        this.patientRepo = patientRepo;
    }

    // ================================================================
    // FEATURE 8 — Auth / Refresh Tokens
    // POST /api/auth/refresh   → exchange refresh token for new access token
    // POST /api/auth/logout    → revoke all refresh tokens for the current user
    // ================================================================

    @PostMapping("/auth/refresh")
    public Map<String, Object> refreshToken(@RequestBody Map<String, String> body) {
        String refreshToken = body.get("refreshToken");
        if (refreshToken == null || refreshToken.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "refreshToken is required");
        }
        try {
            String userId = refreshTokenService.rotateToken(refreshToken, null);
            AppUser user = dataStore.getUsers().stream()
                    .filter(u -> userId.equals(u.getUserId()))
                    .findFirst()
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "User not found"));

            String newAccessToken = jwtUtil.generateToken(user.getUserId(), user.getRole(),
                    user.getDepartment(), user.getName(), user.getEmail());
            String newRefreshToken = refreshTokenService.createToken(user.getUserId(), user.getEmail());

            return Map.of(
                    "accessToken", newAccessToken,
                    "refreshToken", newRefreshToken,
                    "expiresIn", 900
            );
        } catch (IllegalArgumentException e) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, e.getMessage());
        }
    }

    @PostMapping("/auth/logout")
    public Map<String, Object> logout(HttpServletRequest request) {
        AuthUserDetails user = extractUser(request);
        if (user != null) {
            refreshTokenService.revokeAll(user.getUserId());
        }
        return Map.of("success", true, "message", "Logged out — all sessions revoked");
    }

    // ================================================================
    // FEATURE 2 — Patient Documents
    // POST   /api/patients/{id}/documents         → upload file
    // GET    /api/patients/{id}/documents         → list documents
    // GET    /api/documents/{docId}/download      → download file
    // DELETE /api/documents/{docId}               → soft-delete
    // ================================================================

    @PostMapping("/patients/{patientId}/documents")
    public Map<String, Object> uploadDocument(
            HttpServletRequest request,
            @PathVariable String patientId,
            @RequestParam("file") MultipartFile file,
            @RequestParam(value = "documentType", defaultValue = "GENERAL") String documentType,
            @RequestParam(value = "description", required = false) String description,
            @RequestParam(value = "encounterId", required = false) String encounterId,
            @RequestParam(value = "labTestId", required = false) String labTestId,
            @RequestParam(value = "imagingRequestId", required = false) String imagingRequestId) {

        requireAnyPermission(request, List.of("patients.view", "patients.manage"));
        Patient patient = patientRepo.findAll().stream()
                .filter(p -> patientId.equalsIgnoreCase(p.getPatientId()) || patientId.equalsIgnoreCase(p.getClinicNumber()))
                .findFirst()
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Patient not found"));

        String uploadedBy = extractUserName(request);
        try {
            PatientDocument doc = docService.store(file, patient.getPatientId(), patient.getName(),
                    documentType, description, uploadedBy, encounterId, labTestId, imagingRequestId);
            return Map.of("success", true, "document", toDocumentResponse(doc));
        } catch (IOException e) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "File upload failed: " + e.getMessage());
        }
    }

    @GetMapping("/patients/{patientId}/documents")
    public List<Map<String, Object>> listDocuments(HttpServletRequest request, @PathVariable String patientId) {
        requireAnyPermission(request, List.of("patients.view", "patients.manage"));
        return dataStore.getDocumentsByPatient(patientId).stream().map(this::toDocumentResponse).toList();
    }

    @GetMapping("/documents/{docId}/download")
    public ResponseEntity<Resource> downloadDocument(HttpServletRequest request, @PathVariable String docId) {
        requireAnyPermission(request, List.of("patients.view", "patients.manage", "laboratory.view", "radiology.view"));
        PatientDocument doc = dataStore.getDocumentById(docId);
        if (doc == null || "deleted".equals(doc.getStatus())) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Document not found");
        }
        try {
            Path filePath = docService.resolveFilePath(doc);
            Resource resource = new UrlResource(filePath.toUri());
            if (!resource.exists()) throw new ResponseStatusException(HttpStatus.NOT_FOUND, "File not found on server");
            String contentType = doc.getContentType() != null ? doc.getContentType() : "application/octet-stream";
            return ResponseEntity.ok()
                    .contentType(MediaType.parseMediaType(contentType))
                    .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + doc.getFileName() + "\"")
                    .body(resource);
        } catch (MalformedURLException e) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Could not resolve file path");
        }
    }

    @DeleteMapping("/documents/{docId}")
    public Map<String, Object> deleteDocument(HttpServletRequest request, @PathVariable String docId) {
        requireAnyPermission(request, List.of("patients.manage"));
        PatientDocument doc = dataStore.getDocumentById(docId);
        if (doc == null) throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Document not found");
        try {
            docService.delete(doc);
            return Map.of("success", true);
        } catch (IOException e) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Delete failed: " + e.getMessage());
        }
    }

    // ================================================================
    // FEATURE 4 — Drug Interactions & Allergy Safety
    // POST /api/prescriptions/safety-check         → check before prescribing
    // GET  /api/drugs/interactions                  → list all known interactions
    // POST /api/drugs/interactions                  → create interaction
    // PUT  /api/drugs/interactions/{id}             → update interaction
    // DELETE /api/drugs/interactions/{id}           → delete interaction
    // ================================================================

    @PostMapping("/prescriptions/safety-check")
    public Map<String, Object> safetyCheck(HttpServletRequest request, @RequestBody Map<String, Object> body) {
        requireAnyPermission(request, List.of("prescriptions.view", "pharmacy.view"));

        @SuppressWarnings("unchecked")
        List<String> drugNames = (List<String>) body.getOrDefault("drugNames", Collections.emptyList());
        String patientId = (String) body.get("patientId");

        String allergies = "";
        if (patientId != null) {
            Patient p = patientRepo.findAll().stream()
                    .filter(pt -> patientId.equalsIgnoreCase(pt.getPatientId()))
                    .findFirst().orElse(null);
            if (p != null) allergies = p.getAllergies() != null ? p.getAllergies() : "";
        }

        return safetyService.fullSafetyCheck(drugNames, allergies);
    }

    @GetMapping("/drugs/interactions")
    public List<Map<String, Object>> getDrugInteractions(HttpServletRequest request) {
        requireAnyPermission(request, List.of("pharmacy.view", "prescriptions.view"));
        return dataStore.getDrugInteractions().stream().map(this::toDrugInteractionResponse).toList();
    }

    @PostMapping("/drugs/interactions")
    public Map<String, Object> createDrugInteraction(HttpServletRequest request, @RequestBody Map<String, String> body) {
        requirePermission(request, "pharmacy.view");
        DrugInteraction di = new DrugInteraction();
        di.setInteractionId("DI-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase());
        di.setDrugA(required(body, "drugA"));
        di.setDrugB(required(body, "drugB"));
        di.setSeverity(body.getOrDefault("severity", "MODERATE"));
        di.setDescription(body.get("description"));
        di.setClinicalEffect(body.get("clinicalEffect"));
        di.setManagement(body.get("management"));
        di.setCreatedAt(LocalDateTime.now());
        di.setUpdatedAt(LocalDateTime.now());
        return Map.of("success", true, "entry", toDrugInteractionResponse(dataStore.saveDrugInteraction(di)));
    }

    @PutMapping("/drugs/interactions/{interactionId}")
    public Map<String, Object> updateDrugInteraction(HttpServletRequest request,
                                                       @PathVariable String interactionId,
                                                       @RequestBody Map<String, String> body) {
        requirePermission(request, "pharmacy.view");
        DrugInteraction di = dataStore.getDrugInteraction(interactionId);
        if (di == null) throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Interaction not found");
        if (body.containsKey("drugA")) di.setDrugA(body.get("drugA"));
        if (body.containsKey("drugB")) di.setDrugB(body.get("drugB"));
        if (body.containsKey("severity")) di.setSeverity(body.get("severity"));
        if (body.containsKey("description")) di.setDescription(body.get("description"));
        if (body.containsKey("clinicalEffect")) di.setClinicalEffect(body.get("clinicalEffect"));
        if (body.containsKey("management")) di.setManagement(body.get("management"));
        di.setUpdatedAt(LocalDateTime.now());
        return Map.of("success", true, "entry", toDrugInteractionResponse(dataStore.saveDrugInteraction(di)));
    }

    @DeleteMapping("/drugs/interactions/{interactionId}")
    public Map<String, Object> deleteDrugInteraction(HttpServletRequest request, @PathVariable String interactionId) {
        requirePermission(request, "pharmacy.view");
        DrugInteraction di = dataStore.getDrugInteraction(interactionId);
        if (di == null) throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Interaction not found");
        dataStore.deleteDrugInteraction(di);
        return Map.of("success", true);
    }

    // ================================================================
    // FEATURE 5 — Vital Alerts
    // GET  /api/vital-alerts                → all unacknowledged alerts
    // GET  /api/patients/{id}/vital-alerts  → alerts for one patient
    // PUT  /api/vital-alerts/{id}/acknowledge
    // ================================================================

    @GetMapping("/vital-alerts")
    public List<Map<String, Object>> getUnacknowledgedAlerts(HttpServletRequest request) {
        requireAnyPermission(request, List.of("triage.view", "patients.view"));
        return vitalAlertService.getUnacknowledgedAlerts().stream().map(this::toAlertResponse).toList();
    }

    @GetMapping("/patients/{patientId}/vital-alerts")
    public List<Map<String, Object>> getPatientAlerts(HttpServletRequest request, @PathVariable String patientId) {
        requireAnyPermission(request, List.of("triage.view", "patients.view"));
        return vitalAlertService.getAlertsForPatient(patientId).stream().map(this::toAlertResponse).toList();
    }

    @PutMapping("/vital-alerts/{alertId}/acknowledge")
    public Map<String, Object> acknowledgeAlert(HttpServletRequest request, @PathVariable String alertId) {
        requireAnyPermission(request, List.of("triage.view", "patients.view"));
        String acknowledgedBy = extractUserName(request);
        VitalAlert alert = vitalAlertService.acknowledge(alertId, acknowledgedBy);
        if (alert == null) throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Alert not found");
        return Map.of("success", true, "entry", toAlertResponse(alert));
    }

    // ================================================================
    // FEATURE 6 — Lab Reference Ranges + Result Approval
    // GET    /api/lab/reference-ranges             → list all ranges
    // POST   /api/lab/reference-ranges             → create range
    // PUT    /api/lab/reference-ranges/{rangeId}   → update range
    // DELETE /api/lab/reference-ranges/{rangeId}   → delete range
    // GET    /api/lab/reference-ranges/lookup       → find by testName + gender + age
    // PUT    /api/lab-tests/{testId}/approve        → approve lab result
    // ================================================================

    @GetMapping("/lab/reference-ranges")
    public List<Map<String, Object>> getReferenceRanges(HttpServletRequest request) {
        requireAnyPermission(request, List.of("laboratory.view"));
        return dataStore.getLabReferenceRanges().stream().map(this::toRangeResponse).toList();
    }

    @GetMapping("/lab/reference-ranges/lookup")
    public List<Map<String, Object>> lookupReferenceRange(HttpServletRequest request,
                                                           @RequestParam String testName,
                                                           @RequestParam(defaultValue = "ALL") String gender,
                                                           @RequestParam(defaultValue = "25") int age) {
        requireAnyPermission(request, List.of("laboratory.view"));
        return dataStore.findRangesForTest(testName, gender, age).stream().map(this::toRangeResponse).toList();
    }

    @PostMapping("/lab/reference-ranges")
    public Map<String, Object> createReferenceRange(HttpServletRequest request, @RequestBody Map<String, Object> body) {
        requirePermission(request, "laboratory.view");
        LabReferenceRange r = buildRangeFromBody(null, body);
        r.setCreatedAt(LocalDateTime.now());
        r.setUpdatedAt(LocalDateTime.now());
        return Map.of("success", true, "entry", toRangeResponse(dataStore.saveLabReferenceRange(r)));
    }

    @PutMapping("/lab/reference-ranges/{rangeId}")
    public Map<String, Object> updateReferenceRange(HttpServletRequest request,
                                                      @PathVariable String rangeId,
                                                      @RequestBody Map<String, Object> body) {
        requirePermission(request, "laboratory.view");
        LabReferenceRange existing = dataStore.getLabReferenceRange(rangeId);
        if (existing == null) throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Reference range not found");
        buildRangeFromBody(existing, body);
        existing.setUpdatedAt(LocalDateTime.now());
        return Map.of("success", true, "entry", toRangeResponse(dataStore.saveLabReferenceRange(existing)));
    }

    @DeleteMapping("/lab/reference-ranges/{rangeId}")
    public Map<String, Object> deleteReferenceRange(HttpServletRequest request, @PathVariable String rangeId) {
        requirePermission(request, "laboratory.view");
        LabReferenceRange r = dataStore.getLabReferenceRange(rangeId);
        if (r == null) throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Reference range not found");
        dataStore.deleteLabReferenceRange(r);
        return Map.of("success", true);
    }

    @PutMapping("/lab-tests/{testId}/approve")
    public Map<String, Object> approveLabResult(HttpServletRequest request,
                                                  @PathVariable String testId,
                                                  @RequestBody Map<String, String> body) {
        requirePermission(request, "laboratory.view");
        LabTest test = labTestRepo.findAll().stream()
                .filter(t -> testId.equalsIgnoreCase(t.getTestId()))
                .findFirst()
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Lab test not found"));

        if (!"COMPLETED".equalsIgnoreCase(test.getStatus())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Only completed tests can be approved");
        }
        String approvedBy = body.getOrDefault("approvedBy", extractUserName(request));
        test.setApprovedBy(approvedBy);
        test.setApprovedAt(LocalDateTime.now().toString());
        test.setStatus("APPROVED");
        if (body.containsKey("interpretation")) test.setInterpretation(body.get("interpretation"));
        labTestRepo.save(test);
        return Map.of("success", true, "testId", test.getTestId(), "approvedBy", approvedBy, "approvedAt", test.getApprovedAt());
    }

    // ================================================================
    // FEATURE 7 — Financial Reporting + Insurance Claim Lifecycle
    // GET /api/reports/financial          → revenue summary
    // GET /api/reports/outstanding        → unpaid invoices
    // PUT /api/insurance-claims/{id}/status → update claim status
    // ================================================================

    @GetMapping("/reports/financial")
    public Map<String, Object> financialReport(HttpServletRequest request,
                                                @RequestParam(required = false) String fromDate,
                                                @RequestParam(required = false) String toDate,
                                                @RequestParam(required = false) String department) {
        requireAnyPermission(request, List.of("billing.view", "billing.payments"));

        List<BillingInvoice> all = billingRepo.findAll();
        List<BillingInvoice> filtered = all.stream()
                .filter(inv -> inDateRange(inv.getDueDate(), fromDate, toDate))
                .toList();

        double totalRevenue = filtered.stream()
                .filter(inv -> "PAID".equalsIgnoreCase(inv.getStatus()))
                .mapToDouble(inv -> inv.getTotal() != null ? inv.getTotal() : 0)
                .sum();
        double totalOutstanding = filtered.stream()
                .filter(inv -> !"PAID".equalsIgnoreCase(inv.getStatus()) && !"CANCELLED".equalsIgnoreCase(inv.getStatus()))
                .mapToDouble(inv -> inv.getTotal() != null ? inv.getTotal() : 0)
                .sum();
        long paidCount  = filtered.stream().filter(inv -> "PAID".equalsIgnoreCase(inv.getStatus())).count();
        long pendingCount = filtered.stream().filter(inv -> "PENDING".equalsIgnoreCase(inv.getStatus())).count();
        long overdueCount = filtered.stream().filter(inv -> "OVERDUE".equalsIgnoreCase(inv.getStatus())).count();

        // Revenue by payment method
        Map<String, Double> byPaymentMethod = filtered.stream()
                .filter(inv -> "PAID".equalsIgnoreCase(inv.getStatus()) && inv.getPaymentMethod() != null)
                .collect(Collectors.groupingBy(inv -> inv.getPaymentMethod(),
                        Collectors.summingDouble(inv -> inv.getTotal() != null ? inv.getTotal() : 0)));

        // Insurance claims summary
        List<InsuranceClaim> claims = claimRepo.findAll().stream()
                .filter(c -> inDateRange(c.getSubmitted() != null ? c.getSubmitted() : "", fromDate, toDate))
                .toList();
        Map<String, Long> claimsByStatus = claims.stream()
                .collect(Collectors.groupingBy(c -> nvl(c.getStatus()), Collectors.counting()));

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("period", Map.of("from", nvl(fromDate), "to", nvl(toDate)));
        result.put("totalInvoices", filtered.size());
        result.put("paidCount", paidCount);
        result.put("pendingCount", pendingCount);
        result.put("overdueCount", overdueCount);
        result.put("totalRevenue", Math.round(totalRevenue * 100.0) / 100.0);
        result.put("totalOutstanding", Math.round(totalOutstanding * 100.0) / 100.0);
        result.put("revenueByPaymentMethod", byPaymentMethod);
        result.put("insuranceClaims", Map.of("total", claims.size(), "byStatus", claimsByStatus));
        result.put("generatedAt", LocalDateTime.now().toString());
        return result;
    }

    @GetMapping("/reports/outstanding")
    public List<Map<String, Object>> outstandingInvoices(HttpServletRequest request,
                                                          @RequestParam(required = false) String fromDate,
                                                          @RequestParam(required = false) String toDate) {
        requireAnyPermission(request, List.of("billing.view", "billing.payments"));
        return billingRepo.findAll().stream()
                .filter(inv -> !"PAID".equalsIgnoreCase(inv.getStatus()) && !"CANCELLED".equalsIgnoreCase(inv.getStatus()))
                .filter(inv -> inDateRange(inv.getDueDate(), fromDate, toDate))
                .map(inv -> {
                    Map<String, Object> m = new LinkedHashMap<>();
                    m.put("invoiceId", inv.getInvoiceId());
                    m.put("patientName", nvl(inv.getPatientName()));
                    m.put("total", inv.getTotal());
                    m.put("status", nvl(inv.getStatus()));
                    m.put("dueDate", nvl(inv.getDueDate()));
                    return m;
                }).toList();
    }

    @PutMapping("/insurance-claims/{claimId}/status")
    public Map<String, Object> updateClaimStatus(HttpServletRequest request,
                                                   @PathVariable String claimId,
                                                   @RequestBody Map<String, String> body) {
        requireAnyPermission(request, List.of("insurance.view"));
        InsuranceClaim claim = claimRepo.findAll().stream()
                .filter(c -> claimId.equalsIgnoreCase(c.getClaimId()))
                .findFirst()
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Claim not found"));

        String newStatus = required(body, "status");
        // Lifecycle: DRAFT → SUBMITTED → APPROVED / REJECTED → PAID
        claim.setStatus(newStatus);
        if (body.containsKey("notes")) claim.setNotes(body.get("notes"));
        if ("APPROVED".equalsIgnoreCase(newStatus) || "PAID".equalsIgnoreCase(newStatus)) {
            if (body.containsKey("approvedAmount")) claim.setApprovedAmount(body.get("approvedAmount"));
        }
        claimRepo.save(claim);
        return Map.of("success", true, "claimId", claimId, "status", newStatus);
    }

    // ================================================================
    // FEATURE 9 — Data Export
    // GET /api/export/patients.xlsx
    // GET /api/export/billing.xlsx
    // GET /api/export/inventory.xlsx
    // GET /api/export/lab-results.xlsx
    // GET /api/export/billing/{invoiceId}.pdf
    // GET /api/export/patients/{patientId}/summary.pdf
    // ================================================================

    @GetMapping("/export/patients.xlsx")
    public ResponseEntity<byte[]> exportPatients(HttpServletRequest request) throws IOException {
        requireAnyPermission(request, List.of("patients.view", "patients.manage"));
        byte[] data = exportService.exportPatientsExcel();
        return excelResponse(data, "patients.xlsx");
    }

    @GetMapping("/export/billing.xlsx")
    public ResponseEntity<byte[]> exportBilling(HttpServletRequest request,
                                                  @RequestParam(required = false) String from,
                                                  @RequestParam(required = false) String to) throws IOException {
        requireAnyPermission(request, List.of("billing.view", "billing.payments"));
        byte[] data = exportService.exportBillingExcel(from, to);
        return excelResponse(data, "billing.xlsx");
    }

    @GetMapping("/export/inventory.xlsx")
    public ResponseEntity<byte[]> exportInventory(HttpServletRequest request) throws IOException {
        requireAnyPermission(request, List.of("pharmacy.view", "inventory.view"));
        byte[] data = exportService.exportInventoryExcel();
        return excelResponse(data, "drug-inventory.xlsx");
    }

    @GetMapping("/export/lab-results.xlsx")
    public ResponseEntity<byte[]> exportLabResults(HttpServletRequest request,
                                                     @RequestParam(required = false) String from,
                                                     @RequestParam(required = false) String to) throws IOException {
        requireAnyPermission(request, List.of("laboratory.view"));
        byte[] data = exportService.exportLabResultsExcel(from, to);
        return excelResponse(data, "lab-results.xlsx");
    }

    @GetMapping("/export/billing/{invoiceId}.pdf")
    public ResponseEntity<byte[]> exportInvoicePdf(HttpServletRequest request,
                                                     @PathVariable String invoiceId) throws Exception {
        requireAnyPermission(request, List.of("billing.view", "billing.payments"));
        BillingInvoice inv = billingRepo.findAll().stream()
                .filter(i -> invoiceId.equalsIgnoreCase(i.getInvoiceId()))
                .findFirst()
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Invoice not found"));
        byte[] data = exportService.exportBillingInvoicePdf(inv);
        return pdfResponse(data, "invoice-" + invoiceId + ".pdf");
    }

    @GetMapping("/export/patients/{patientId}/summary.pdf")
    public ResponseEntity<byte[]> exportPatientSummaryPdf(HttpServletRequest request,
                                                            @PathVariable String patientId) throws Exception {
        requireAnyPermission(request, List.of("patients.view"));
        Patient patient = patientRepo.findAll().stream()
                .filter(p -> patientId.equalsIgnoreCase(p.getPatientId()) || patientId.equalsIgnoreCase(p.getClinicNumber()))
                .findFirst()
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Patient not found"));
        List<LabTest> tests = labTestRepo.findAll().stream()
                .filter(t -> patient.getPatientId().equalsIgnoreCase(t.getPatientId()))
                .toList();
        List<BillingInvoice> invoices = billingRepo.findAll().stream()
                .filter(i -> patient.getPatientId().equalsIgnoreCase(i.getPatientId()))
                .toList();
        byte[] data = exportService.exportPatientSummaryPdf(patient, tests, invoices);
        return pdfResponse(data, "patient-summary-" + patientId + ".pdf");
    }

    // ================================================================
    // FEATURE 11 — Controlled Substance Register
    // GET  /api/pharmacy/controlled-register  → handled by ApiController#getControlledRegister
    // POST /api/pharmacy/controlled-register  → create entry (requires dual sign-off)
    // ================================================================

    @PostMapping("/pharmacy/controlled-register")
    public Map<String, Object> addControlledEntry(HttpServletRequest request, @RequestBody Map<String, Object> body) {
        requirePermission(request, "pharmacy.view");

        // Dual sign-off: dispensedBy + authorizedBy both required
        String dispensedBy = required(body, "dispensedBy");
        String authorizedBy = required(body, "authorizedBy");
        if (dispensedBy.equalsIgnoreCase(authorizedBy)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Dispensing pharmacist and authorizing person must be different individuals");
        }

        ControlledDrugRegister entry = new ControlledDrugRegister();
        entry.setEntryId("CDR-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase());
        entry.setDrugName(required(body, "drugName"));
        entry.setSchedule((String) body.get("schedule"));
        entry.setRxId((String) body.get("rxId"));
        entry.setPatientId((String) body.get("patientId"));
        entry.setPatientName((String) body.get("patientName"));
        entry.setQuantityDispensed(toInt(body.get("quantityDispensed")));
        entry.setUnit((String) body.get("unit"));
        entry.setBalanceBefore(toInt(body.get("balanceBefore")));
        Integer qty = entry.getQuantityDispensed();
        Integer before = entry.getBalanceBefore();
        entry.setBalanceAfter((before != null && qty != null) ? before - qty : null);
        entry.setDispensedBy(dispensedBy);
        entry.setAuthorizedBy(authorizedBy);
        entry.setDateDispensed((String) body.getOrDefault("dateDispensed", LocalDateTime.now().toLocalDate().toString()));
        entry.setWitness((String) body.get("witness"));
        entry.setNotes((String) body.get("notes"));
        entry.setCreatedAt(LocalDateTime.now());

        return Map.of("success", true, "entry", toControlledDrugResponse(dataStore.saveControlledDrugEntry(entry)));
    }

    // ================================================================
    // Private helpers
    // ================================================================

    private Map<String, Object> toDocumentResponse(PatientDocument doc) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("documentId", doc.getDocumentId());
        m.put("patientId", doc.getPatientId());
        m.put("patientName", nvl(doc.getPatientName()));
        m.put("documentType", doc.getDocumentType());
        m.put("fileName", doc.getFileName());
        m.put("fileSize", doc.getFileSize());
        m.put("contentType", nvl(doc.getContentType()));
        m.put("description", nvl(doc.getDescription()));
        m.put("uploadedBy", nvl(doc.getUploadedBy()));
        m.put("uploadedAt", doc.getUploadedAt() != null ? doc.getUploadedAt().toString() : "");
        m.put("encounterId", nvl(doc.getEncounterId()));
        m.put("labTestId", nvl(doc.getLabTestId()));
        m.put("status", nvl(doc.getStatus()));
        return m;
    }

    private Map<String, Object> toDrugInteractionResponse(DrugInteraction di) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("interactionId", di.getInteractionId());
        m.put("drugA", di.getDrugA());
        m.put("drugB", di.getDrugB());
        m.put("severity", di.getSeverity());
        m.put("description", nvl(di.getDescription()));
        m.put("clinicalEffect", nvl(di.getClinicalEffect()));
        m.put("management", nvl(di.getManagement()));
        m.put("updatedAt", di.getUpdatedAt() != null ? di.getUpdatedAt().toString() : "");
        return m;
    }

    private Map<String, Object> toAlertResponse(VitalAlert alert) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("alertId", alert.getAlertId());
        m.put("patientId", alert.getPatientId());
        m.put("patientName", nvl(alert.getPatientName()));
        m.put("vitalType", alert.getVitalType());
        m.put("value", alert.getValueText());
        m.put("threshold", nvl(alert.getThresholdText()));
        m.put("severity", alert.getSeverity());
        m.put("message", nvl(alert.getMessage()));
        m.put("acknowledged", alert.isAcknowledged());
        m.put("acknowledgedBy", nvl(alert.getAcknowledgedBy()));
        m.put("acknowledgedAt", alert.getAcknowledgedAt() != null ? alert.getAcknowledgedAt().toString() : "");
        m.put("createdAt", alert.getCreatedAt() != null ? alert.getCreatedAt().toString() : "");
        return m;
    }

    private Map<String, Object> toRangeResponse(LabReferenceRange r) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("rangeId", r.getRangeId());
        m.put("testName", r.getTestName());
        m.put("category", nvl(r.getCategory()));
        m.put("gender", nvl(r.getGender()));
        m.put("minAge", r.getMinAge());
        m.put("maxAge", r.getMaxAge());
        m.put("minValue", r.getMinValue());
        m.put("maxValue", r.getMaxValue());
        m.put("criticalLow", r.getCriticalLow());
        m.put("criticalHigh", r.getCriticalHigh());
        m.put("unit", nvl(r.getUnit()));
        m.put("interpretationLow", nvl(r.getInterpretationLow()));
        m.put("interpretationHigh", nvl(r.getInterpretationHigh()));
        return m;
    }

    private Map<String, Object> toControlledDrugResponse(ControlledDrugRegister e) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("entryId", e.getEntryId());
        m.put("drugName", e.getDrugName());
        m.put("schedule", nvl(e.getSchedule()));
        m.put("rxId", nvl(e.getRxId()));
        m.put("patientId", nvl(e.getPatientId()));
        m.put("patientName", nvl(e.getPatientName()));
        m.put("quantityDispensed", e.getQuantityDispensed());
        m.put("unit", nvl(e.getUnit()));
        m.put("balanceBefore", e.getBalanceBefore());
        m.put("balanceAfter", e.getBalanceAfter());
        m.put("dispensedBy", nvl(e.getDispensedBy()));
        m.put("authorizedBy", nvl(e.getAuthorizedBy()));
        m.put("dateDispensed", nvl(e.getDateDispensed()));
        m.put("witness", nvl(e.getWitness()));
        m.put("notes", nvl(e.getNotes()));
        m.put("createdAt", e.getCreatedAt() != null ? e.getCreatedAt().toString() : "");
        return m;
    }

    private LabReferenceRange buildRangeFromBody(LabReferenceRange existing, Map<String, Object> body) {
        LabReferenceRange r = existing != null ? existing : new LabReferenceRange();
        if (r.getRangeId() == null) r.setRangeId("LR-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase());
        if (body.containsKey("testName")) r.setTestName((String) body.get("testName"));
        if (body.containsKey("category")) r.setCategory((String) body.get("category"));
        if (body.containsKey("gender")) r.setGender((String) body.get("gender"));
        if (body.containsKey("minAge")) r.setMinAge(toInt(body.get("minAge")));
        if (body.containsKey("maxAge")) r.setMaxAge(toInt(body.get("maxAge")));
        if (body.containsKey("minValue") && body.get("minValue") != null) r.setMinValue(new BigDecimal(body.get("minValue").toString()));
        if (body.containsKey("maxValue") && body.get("maxValue") != null) r.setMaxValue(new BigDecimal(body.get("maxValue").toString()));
        if (body.containsKey("criticalLow") && body.get("criticalLow") != null) r.setCriticalLow(new BigDecimal(body.get("criticalLow").toString()));
        if (body.containsKey("criticalHigh") && body.get("criticalHigh") != null) r.setCriticalHigh(new BigDecimal(body.get("criticalHigh").toString()));
        if (body.containsKey("unit")) r.setUnit((String) body.get("unit"));
        if (body.containsKey("interpretationLow")) r.setInterpretationLow((String) body.get("interpretationLow"));
        if (body.containsKey("interpretationHigh")) r.setInterpretationHigh((String) body.get("interpretationHigh"));
        return r;
    }

    private ResponseEntity<byte[]> excelResponse(byte[] data, String filename) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"));
        headers.setContentDisposition(ContentDisposition.attachment().filename(filename).build());
        return ResponseEntity.ok().headers(headers).body(data);
    }

    private ResponseEntity<byte[]> pdfResponse(byte[] data, String filename) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_PDF);
        headers.setContentDisposition(ContentDisposition.attachment().filename(filename).build());
        return ResponseEntity.ok().headers(headers).body(data);
    }

    private void requirePermission(HttpServletRequest request, String permission) {
        AuthUserDetails user = extractUser(request);
        if (user == null || !user.getPermissions().contains(permission)) {
            throw new AccessDeniedException("Permission required: " + permission);
        }
    }

    private void requireAnyPermission(HttpServletRequest request, List<String> permissions) {
        AuthUserDetails user = extractUser(request);
        if (user == null) throw new AccessDeniedException("Authentication required");
        boolean hasAny = permissions.stream().anyMatch(p -> user.getPermissions().contains(p));
        if (!hasAny) throw new AccessDeniedException("One of these permissions required: " + permissions);
    }

    private AuthUserDetails extractUser(HttpServletRequest request) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.getPrincipal() instanceof AuthUserDetails) {
            return (AuthUserDetails) auth.getPrincipal();
        }
        return null;
    }

    private String extractUserName(HttpServletRequest request) {
        AuthUserDetails user = extractUser(request);
        return user != null ? user.getName() : "System";
    }

    private boolean inDateRange(String date, String from, String to) {
        if (date == null || date.isBlank()) return true;
        if (from != null && !from.isBlank() && date.compareTo(from) < 0) return false;
        if (to   != null && !to.isBlank()   && date.compareTo(to)   > 0) return false;
        return true;
    }

    private String nvl(String s) { return s != null ? s : ""; }

    private String required(Map<String, ?> body, String key) {
        Object val = body.get(key);
        if (val == null || val.toString().isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "'" + key + "' is required");
        }
        return val.toString();
    }

    private Integer toInt(Object val) {
        if (val == null) return null;
        try { return Integer.parseInt(val.toString()); } catch (NumberFormatException e) { return null; }
    }
}
