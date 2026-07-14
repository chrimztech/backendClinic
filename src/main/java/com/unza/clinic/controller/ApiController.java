package com.unza.clinic.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.core.type.TypeReference;
import com.unza.clinic.model.*;
import com.unza.clinic.dto.*;
import com.unza.clinic.model.LoginAuditLog;
import com.unza.clinic.service.ClinicDataStore;
import com.unza.clinic.service.DataBackupService;
import com.unza.clinic.service.LoginRateLimiter;
import com.unza.clinic.service.RefreshTokenService;
import com.unza.clinic.service.WebSocketNotificationService;
import com.unza.clinic.model.AuthUserDetails;
import com.unza.clinic.util.JwtUtil;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import org.springframework.jdbc.core.JdbcTemplate;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.TextStyle;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.ArrayList;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

@RestController
@RequestMapping("/api")
public class ApiController {

     private final ClinicDataStore dataStore;
     private final DataBackupService backupService;
     private final JwtUtil jwtUtil;
     private final JdbcTemplate jdbc;
     private final com.unza.clinic.repository.PrescriptionItemRepository prescriptionItemRepository;
     private final WebSocketNotificationService wsService;
     private final LoginRateLimiter rateLimiter;
     private final RefreshTokenService refreshTokenService;
     private final ObjectMapper objectMapper = new ObjectMapper();

      public ApiController(ClinicDataStore dataStore, DataBackupService backupService, JwtUtil jwtUtil, JdbcTemplate jdbc,
                           com.unza.clinic.repository.PrescriptionItemRepository prescriptionItemRepository,
                           WebSocketNotificationService wsService, LoginRateLimiter rateLimiter,
                           RefreshTokenService refreshTokenService) {
          this.dataStore = dataStore;
          this.backupService = backupService;
          this.jwtUtil = jwtUtil;
          this.jdbc = jdbc;
          this.prescriptionItemRepository = prescriptionItemRepository;
          this.wsService = wsService;
          this.rateLimiter = rateLimiter;
          this.refreshTokenService = refreshTokenService;
      }

     @GetMapping("/")
     public Map<String, Object> root() {
         return Map.of("message", "UNZA Clinic API", "status", "running", "endpoints", Map.of("health", "/api/health", "patients", "/api/patients", "docs", "Use /api/* for all endpoints"));
     }

    @GetMapping("/health")
    public Map<String, Object> health() {
        return Map.of("status", "ok", "timestamp", LocalDateTime.now().toString(), "service", "unza-clinic-springboot");
    }

    @GetMapping("/patients")
    public Object getPatients(HttpServletRequest httpRequest,
                              @RequestParam(required = false) Integer page,
                              @RequestParam(required = false) Integer size) {
        requirePermission(httpRequest, "patients.view");
        List<Map<String, Object>> all = dataStore.getPatients().stream().map(this::toPatientResponse).toList();
        return paginate(all, page, size);
    }

    @GetMapping("/patients/{id}")
    public Map<String, Object> getPatientById(HttpServletRequest httpRequest, @PathVariable String id) {
        requirePermission(httpRequest, "patients.view");
        Patient patient = resolvePatient(id);
        if (patient == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Patient not found");
        }
        return toPatientResponse(patient);
    }

    @PutMapping("/patients/{id}")
    public Map<String, Object> updatePatient(
            HttpServletRequest httpRequest,
            @PathVariable String id,
            @Valid @RequestBody PatientCreateRequest request
    ) {
        requireAnyPermission(httpRequest, List.of("walkin.view", "patients.manage"));
        Patient patient = resolvePatient(id);
        if (patient == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Patient not found");
        }
        String previousPatientIdentifier = stringValue(patient.getPatientId()).trim();
        String previousClinicNumber = stringValue(patient.getClinicNumber()).trim();

        String patientType = normalizePatientType(request.patientType());
        patient.setPatientType(patientType);
        patient.setName(request.name());
        patient.setAge(request.age());
        patient.setGender(request.gender());
        patient.setDob(request.dob());
        patient.setPhone(request.phone());
        patient.setEmail(request.email());
        patient.setAddress(request.address());
        patient.setBloodGroup(request.bloodGroup());
        patient.setStudentId(isStudentPatientType(patientType) ? stringValue(request.studentId()) : "");
        patient.setManNumber(isStaffLinkedPatientType(patientType) ? stringValue(request.manNumber()) : "");
        patient.setProgram(isStudentPatientType(patientType) ? stringValue(request.program()) : "");
        patient.setSchool(isStudentPatientType(patientType) ? stringValue(request.school()) : "");
        patient.setYear(isStudentPatientType(patientType) ? request.year() : null);
        patient.setHostel(isStudentPatientType(patientType) ? stringValue(request.hostel()) : "");
        patient.setEmergencyContact(request.emergencyContact());
        patient.setEmergencyPhone(request.emergencyPhone());
        patient.setEmergencyRelation(request.emergencyRelation());
        patient.setAllergies(request.allergies());
        patient.setConditions(request.conditions());
        patient.setInsurance(request.insurance());

        if (hasText(request.clinicNumber())) {
            String requestedClinicNumber = request.clinicNumber().trim().toUpperCase(Locale.ROOT);
            Long currentPatientInternalId = patient.getId();
            boolean clinicNumberInUse = dataStore.getPatients().stream()
                    .filter(entry -> !Objects.equals(entry.getId(), currentPatientInternalId))
                    .anyMatch(entry ->
                            requestedClinicNumber.equalsIgnoreCase(stringValue(entry.getClinicNumber()).trim())
                                    || requestedClinicNumber.equalsIgnoreCase(stringValue(entry.getPatientId()).trim()));
            if (clinicNumberInUse) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Clinic number already exists");
            }
            patient.setPatientId(requestedClinicNumber);
            patient.setClinicNumber(requestedClinicNumber);
        }

        patient = dataStore.updatePatient(patient);
        syncPatientIdentifierReferences(previousPatientIdentifier, previousClinicNumber, stringValue(patient.getPatientId()).trim());
        writeAuditLog("System", "Admin", "update", "Updated patient " + patient.getName() + " (" + patient.getPatientId() + ").", "127.0.0.1");
        return Map.of("success", true, "entry", toPatientResponse(patient));
    }

    @PutMapping("/patients/{id}/graduate")
    public Map<String, Object> graduatePatient(HttpServletRequest httpRequest, @PathVariable String id) {
        requirePermission(httpRequest, "patients.manage");
        Patient patient = resolvePatient(id);
        if (patient == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Patient not found");
        }
        if (!isStudentPatientType(patient.getPatientType())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Only student patient records can be graduated");
        }
        patient.setStatus("graduated");
        patient = dataStore.updatePatient(patient);
        writeAuditLog("System", "Admin", "update", "Marked patient " + patient.getName() + " as graduated.", "127.0.0.1");
        return Map.of("success", true, "entry", toPatientResponse(patient));
    }

    @DeleteMapping("/patients/{id}")
    public Map<String, Object> deletePatient(HttpServletRequest httpRequest, @PathVariable String id) {
        requirePermission(httpRequest, "patients.manage");
        Patient patient = resolvePatient(id);
        if (patient == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Patient not found");
        }
        dataStore.deletePatient(patient);
        writeAuditLog("System", "Admin", "delete", "Deleted patient " + patient.getName() + " (" + patient.getPatientId() + ").", "127.0.0.1");
        return Map.of("success", true);
    }

    @PostMapping("/patients")
    public Map<String, Object> createPatient(@Valid @RequestBody PatientCreateRequest request) {
        Patient patient = new Patient();
        String patientType = normalizePatientType(request.patientType());
        String clinicNumber = resolveClinicNumber(request.clinicNumber(), patientType, stringValue(request.studentId()), stringValue(request.manNumber()));
        patient.setPatientId(clinicNumber);
        patient.setClinicNumber(clinicNumber);
        patient.setPatientType(patientType);
        patient.setName(request.name());
        patient.setAge(request.age());
        patient.setGender(request.gender());
        patient.setDob(request.dob());
        patient.setPhone(request.phone());
        patient.setEmail(request.email());
        patient.setAddress(request.address());
        patient.setBloodGroup(request.bloodGroup());
        patient.setStudentId(isStudentPatientType(patientType) ? stringValue(request.studentId()) : "");
        patient.setManNumber(isStaffLinkedPatientType(patientType) ? stringValue(request.manNumber()) : "");
        patient.setProgram(isStudentPatientType(patientType) ? stringValue(request.program()) : "");
        patient.setSchool(isStudentPatientType(patientType) ? stringValue(request.school()) : "");
        patient.setYear(isStudentPatientType(patientType) ? request.year() : null);
        patient.setHostel(isStudentPatientType(patientType) ? stringValue(request.hostel()) : "");
        patient.setEmergencyContact(request.emergencyContact());
        patient.setEmergencyPhone(request.emergencyPhone());
        patient.setEmergencyRelation(request.emergencyRelation());
        patient.setAllergies(request.allergies());
        patient.setConditions(request.conditions());
        patient.setInsurance(request.insurance());
        patient.setStatus("active");
        patient = dataStore.addPatient(patient);
        writeAuditLog("System", "create", "Registered patient " + patient.getName() + " (" + patient.getClinicNumber() + ")", "127.0.0.1");
        return Map.of("success", true, "patient_id", patient.getPatientId(), "clinic_number", patient.getClinicNumber());
    }

    @GetMapping("/staff")
    public List<Map<String, Object>> getStaff(HttpServletRequest httpRequest) {
        requirePermission(httpRequest, "staff.view");
        return dataStore.getStaffMembers().stream().map(this::toStaffResponse).toList();
    }

    /** HR System lookup — find staff by man number so registration can pre-fill details */
    @GetMapping("/staff/lookup/man-number/{manNumber}")
    public Map<String, Object> lookupStaffByManNumber(HttpServletRequest httpRequest, @PathVariable String manNumber) {
        requireAnyPermission(httpRequest, List.of("walkin.view", "patients.view", "staff.view"));
        StaffMember member = dataStore.getStaffMemberByManNumber(manNumber.trim());
        if (member == null) {
            // Also check the patients table for an existing staff patient record
            Patient existing = dataStore.getPatients().stream()
                    .filter(p -> manNumber.trim().equalsIgnoreCase(stringValue(p.getManNumber())))
                    .findFirst().orElse(null);
            if (existing != null) {
                return Map.of("found", true, "source", "patient_record",
                        "name", stringValue(existing.getName()),
                        "department", "",
                        "phone", stringValue(existing.getPhone()),
                        "email", stringValue(existing.getEmail()),
                        "clinic_number", stringValue(existing.getClinicNumber()),
                        "already_registered", true);
            }
            return Map.of("found", false);
        }
        Patient existingPatient = dataStore.getPatients().stream()
                .filter(p -> manNumber.trim().equalsIgnoreCase(stringValue(p.getManNumber())))
                .findFirst().orElse(null);
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("found", true);
        result.put("source", "hr_system");
        result.put("name", stringValue(member.getName()));
        result.put("department", stringValue(member.getDepartment()));
        result.put("phone", stringValue(member.getPhone()));
        result.put("email", stringValue(member.getEmail()));
        result.put("role", stringValue(member.getRole()));
        result.put("already_registered", existingPatient != null);
        result.put("clinic_number", existingPatient != null ? stringValue(existingPatient.getClinicNumber()) : null);
        return result;
    }

    /** SIS lookup — find student by computer number so registration can pre-fill details */
    @GetMapping("/patients/lookup/student-id/{studentId}")
    public Map<String, Object> lookupStudentById(HttpServletRequest httpRequest, @PathVariable String studentId) {
        requireAnyPermission(httpRequest, List.of("walkin.view", "patients.view"));
        Patient existing = dataStore.getPatients().stream()
                .filter(p -> studentId.trim().equalsIgnoreCase(stringValue(p.getStudentId())))
                .findFirst().orElse(null);
        if (existing != null) {
            return Map.of("found", true, "source", "sis",
                    "name", stringValue(existing.getName()),
                    "program", stringValue(existing.getProgram()),
                    "school", stringValue(existing.getSchool()),
                    "phone", stringValue(existing.getPhone()),
                    "email", stringValue(existing.getEmail()),
                    "already_registered", true,
                    "clinic_number", stringValue(existing.getClinicNumber()));
        }
        return Map.of("found", false, "already_registered", false);
    }

    @PostMapping("/staff")
    public Map<String, Object> createStaff(HttpServletRequest httpRequest, @Valid @RequestBody StaffCreateRequest request) {
        requirePermission(httpRequest, "staff.manage");
        StaffMember member = new StaffMember();
        member.setStaffId("STF-" + String.format("%03d", dataStore.getStaffMembers().size() + 1));
        member.setManNumber(hasText(request.manNumber()) ? request.manNumber().trim() : "MAN-" + String.format("%03d", dataStore.getStaffMembers().size() + 1));
        member.setName(request.name());
        member.setRole(request.role());
        member.setDepartment(request.department());
        member.setPhone(request.phone());
        member.setEmail(request.email());
        member.setSpecialization(request.specialization());
        member.setStatus("active");
        member = dataStore.addStaffMember(member);
        writeAuditLog("System", "create", "Added staff member " + member.getName() + " (" + member.getStaffId() + ")", "127.0.0.1");
        return Map.of("success", true, "staff_id", member.getStaffId(), "entry", toStaffResponse(member));
    }

    @PutMapping("/staff/{staffId}/graduate")
    public Map<String, Object> graduateStaff(HttpServletRequest httpRequest, @PathVariable String staffId) {
        requirePermission(httpRequest, "staff.manage");
        StaffMember member = resolveStaffMember(staffId);
        if (member == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Staff member not found");
        }
        throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Staff members are managed by section, role, and account status. Graduation applies only to student patient records.");
    }

    @DeleteMapping("/staff/{staffId}")
    public Map<String, Object> deleteStaff(HttpServletRequest httpRequest, @PathVariable String staffId) {
        requirePermission(httpRequest, "staff.manage");
        StaffMember member = resolveStaffMember(staffId);
        if (member == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Staff member not found");
        }
        AppUser linkedUser = dataStore.findUserByStaffId(member.getStaffId());
        if (linkedUser == null && hasText(member.getManNumber())) {
            linkedUser = dataStore.findUserByManNumber(member.getManNumber());
        }
        if (linkedUser != null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Delete the linked user account first before removing this staff record");
        }
        dataStore.deleteStaffMember(member);
        writeAuditLog("System", "Admin", "delete", "Deleted staff member " + member.getName() + " (" + member.getStaffId() + ").", "127.0.0.1");
        return Map.of("success", true);
    }

    private StaffMember resolveStaffMember(String identifier) {
        String normalizedIdentifier = stringValue(identifier).trim();
        if (isBlank(normalizedIdentifier)) {
            return null;
        }
        StaffMember member = dataStore.getStaffMemberByStaffId(normalizedIdentifier);
        if (member != null) {
            return member;
        }
        if (hasText(normalizedIdentifier)) {
            member = dataStore.getStaffMemberByManNumber(normalizedIdentifier);
            if (member != null) {
                return member;
            }
            try {
                return dataStore.getStaffMember(Long.parseLong(normalizedIdentifier));
            } catch (NumberFormatException ignored) {
                return null;
            }
        }
        return null;
    }

    private Patient resolvePatient(String identifier) {
        String normalizedIdentifier = stringValue(identifier).trim();
        if (isBlank(normalizedIdentifier)) {
            return null;
        }

        Patient patient = dataStore.getPatientByPatientId(normalizedIdentifier);
        if (patient != null) {
            return patient;
        }

        for (Patient entry : dataStore.getPatients()) {
            if (matchesPatientIdentifier(entry, normalizedIdentifier)) {
                return entry;
            }
        }

        try {
            Long numericId = Long.parseLong(normalizedIdentifier);
            return dataStore.getPatients().stream()
                    .filter(entry -> Objects.equals(entry.getId(), numericId))
                    .findFirst()
                    .orElse(null);
        } catch (NumberFormatException ignored) {
            return null;
        }
    }

    private boolean matchesPatientIdentifier(Patient patient, String identifier) {
        return identifier.equalsIgnoreCase(stringValue(patient.getPatientId()).trim())
                || identifier.equalsIgnoreCase(stringValue(patient.getClinicNumber()).trim())
                || identifier.equalsIgnoreCase(stringValue(patient.getStudentId()).trim())
                || identifier.equalsIgnoreCase(stringValue(patient.getManNumber()).trim());
    }

    private String normalizePatientType(String patientType) {
        if (!hasText(patientType)) {
            return "GENERAL";
        }
        return patientType.trim().toUpperCase(Locale.ROOT);
    }

    private String resolveClinicNumber(String requestedClinicNumber, String patientType, String studentId, String manNumber) {
        if (hasText(requestedClinicNumber)) {
            String normalized = requestedClinicNumber.trim().toUpperCase(Locale.ROOT);
            if (clinicNumberExists(normalized)) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Clinic number already exists");
            }
            return normalized;
        }
        return generateClinicNumber(patientType, studentId, manNumber);
    }

    private String generateClinicNumber(String patientType, String studentId, String manNumber) {
        String normalizedType = normalizePatientType(patientType);

        // Students: tie clinic number to their computer/registration number
        if (("STUDENT".equals(normalizedType) || "FIRST_TIME_STUDENT".equals(normalizedType)) && hasText(studentId)) {
            String base = "STU-" + sanitizeInstitutionalId(studentId);
            if (!clinicNumberExists(base)) return base;
            // Returning student with same ID — return the existing record's number
            return base;
        }

        // Staff: tie clinic number to their man number
        if ("STAFF".equals(normalizedType) && hasText(manNumber)) {
            String base = "STA-" + sanitizeInstitutionalId(manNumber);
            if (!clinicNumberExists(base)) return base;
            return base;
        }

        // Staff dependants: tie to the staff member's man number with DEP prefix
        if ("STAFF_DEPENDANT".equals(normalizedType) && hasText(manNumber)) {
            String base = "DEP-" + sanitizeInstitutionalId(manNumber);
            if (!clinicNumberExists(base)) return base;
            // Multiple dependants share the same man number — append a suffix
            for (int suffix = 2; suffix <= 20; suffix++) {
                String candidate = base + "-" + suffix;
                if (!clinicNumberExists(candidate)) return candidate;
            }
            return base + "-" + System.currentTimeMillis() % 1000;
        }

        // Sequence-based fallback for NON_UNZA, GENERAL, or when institutional ID is missing
        // No year in the number — runs as a permanent global counter (EXT-00001, CLN-00001, etc.)
        String prefix = switch (normalizedType) {
            case "NON_UNZA" -> "EXT";
            case "STUDENT", "FIRST_TIME_STUDENT" -> "STU";
            case "STAFF" -> "STA";
            case "STAFF_DEPENDANT" -> "DEP";
            default -> "CLN";
        };
        int nextSequence = dataStore.getPatients().stream()
                .map(patient -> extractClinicSequence(patient.getClinicNumber(), prefix))
                .filter(Objects::nonNull)
                .mapToInt(Integer::intValue)
                .max()
                .orElse(0) + 1;
        return prefix + "-" + String.format("%05d", nextSequence);
    }

    private String sanitizeInstitutionalId(String id) {
        return id.trim().toUpperCase(Locale.ROOT)
                .replaceAll("[^A-Z0-9]", "-")
                .replaceAll("-{2,}", "-")
                .replaceAll("^-|-$", "");
    }

    private Integer extractClinicSequence(String clinicNumber, String prefix) {
        String normalized = stringValue(clinicNumber).trim().toUpperCase(Locale.ROOT);
        String expectedPrefix = prefix + "-";
        if (!normalized.startsWith(expectedPrefix)) return null;
        String suffix = normalized.substring(expectedPrefix.length());
        // Only parse pure numeric suffixes (skip year-based ones like "2026-0001")
        if (!suffix.matches("\\d+")) return null;
        try {
            return Integer.parseInt(suffix);
        } catch (NumberFormatException ex) {
            return null;
        }
    }

    private boolean clinicNumberExists(String clinicNumber) {
        return dataStore.getPatients().stream().anyMatch(patient ->
                clinicNumber.equalsIgnoreCase(stringValue(patient.getClinicNumber()).trim())
                        || clinicNumber.equalsIgnoreCase(stringValue(patient.getPatientId()).trim()));
    }

    private boolean isStudentPatientType(String patientType) {
        String normalizedType = normalizePatientType(patientType);
        return "STUDENT".equals(normalizedType) || "FIRST_TIME_STUDENT".equals(normalizedType);
    }

    private boolean isStaffLinkedPatientType(String patientType) {
        String normalizedType = normalizePatientType(patientType);
        return "STAFF".equals(normalizedType) || "STAFF_DEPENDANT".equals(normalizedType);
    }

    @GetMapping("/departments")
    public List<Map<String, Object>> getDepartments(HttpServletRequest httpRequest) {
        requireAuthenticatedUser(httpRequest);
        return dataStore.getDepartments().stream().map(this::toDepartmentResponse).toList();
    }

    @PostMapping("/departments")
    public Map<String, Object> createDepartment(HttpServletRequest httpRequest, @Valid @RequestBody DepartmentCreateRequest request) {
        requirePermission(httpRequest, "departments.manage");
        Department department = new Department();
        department.setCode("DEPT-" + request.name().substring(0, Math.min(2, request.name().length())).toUpperCase(Locale.ROOT) + "-" + String.format("%02d", dataStore.getDepartments().size() + 1));
        department.setName(request.name());
        department.setHead(request.head());
        department.setDoctors(defaultInt(request.doctors()));
        department.setNurses(defaultInt(request.nurses()));
        department.setBeds(defaultInt(request.beds()));
        department.setLocation(request.location());
        department.setPhone(request.phone());
        department.setStatus("Active");
        department = dataStore.addDepartment(department);
        return Map.of("success", true, "code", department.getCode(), "entry", toDepartmentResponse(department));
    }

    @PutMapping("/departments/{code}")
    public Map<String, Object> updateDepartment(
            HttpServletRequest httpRequest,
            @PathVariable String code,
            @Valid @RequestBody DepartmentCreateRequest request
    ) {
        requirePermission(httpRequest, "departments.manage");
        Department department = dataStore.getDepartmentByCode(stringValue(code).trim());
        if (department == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Department not found");
        }
        department.setName(request.name());
        department.setHead(request.head());
        department.setDoctors(defaultInt(request.doctors()));
        department.setNurses(defaultInt(request.nurses()));
        department.setBeds(defaultInt(request.beds()));
        department.setLocation(request.location());
        department.setPhone(request.phone());
        department = dataStore.updateDepartment(department);
        writeAuditLog("System", "Admin", "update", "Updated department " + department.getName() + " (" + department.getCode() + ").", "127.0.0.1");
        return Map.of("success", true, "entry", toDepartmentResponse(department));
    }

    // ==================== EXTERNAL SYSTEM INTEGRATION ====================

    // Student Information System (SIS) Integration
    @GetMapping("/external/sis/students/{studentNumber}")
    public Map<String, Object> getStudentFromSIS(@PathVariable String studentNumber) {
        // In production, this would call the actual SIS API
        // For now, return mock data based on student number pattern
        Map<String, Object> student = new LinkedHashMap<>();
        student.put("student_number", studentNumber);
        student.put("full_name", "Student " + studentNumber);
        student.put("first_name", "Student");
        student.put("last_name", studentNumber);
        student.put("date_of_birth", "2000-01-15");
        student.put("gender", "Male");
        student.put("email", studentNumber.toLowerCase() + "@unza.ac.zm");
        student.put("phone", "+260971234567");
        student.put("address", "Great East Road Campus, Lusaka");
        student.put("program", "Bachelor of Medicine");
        student.put("year_of_study", 3);
        student.put("requires_medical_exam", true);
        student.put("medical_exam_status", "pending");
        return student;
    }

    @GetMapping("/external/sis/students/search")
    public List<Map<String, Object>> searchStudentsByName(@RequestParam String name) {
        List<Map<String, Object>> results = new ArrayList<>();
        Map<String, Object> student = new LinkedHashMap<>();
        student.put("student_number", "2023123456");
        student.put("full_name", name);
        student.put("date_of_birth", "2000-01-15");
        student.put("gender", "Male");
        student.put("email", name.toLowerCase().replace(" ", ".") + "@unza.ac.zm");
        student.put("phone", "+260971234567");
        student.put("program", "Bachelor of Medicine");
        student.put("year_of_study", 3);
        student.put("requires_medical_exam", true);
        student.put("medical_exam_status", "pending");
        results.add(student);
        return results;
    }

    @GetMapping("/external/sis/students/{studentNumber}/medical-status")
    public Map<String, Object> getStudentMedicalStatus(@PathVariable String studentNumber) {
        Patient patient = resolvePatient(studentNumber);
        return buildMedicalStatusResponse(
                patient,
                stringValue(studentNumber).trim(),
                "student",
                true,
                "pending"
        );
    }

    @PutMapping("/external/sis/students/{studentNumber}/medical-record")
    public Map<String, Object> updateStudentMedicalRecord(@PathVariable String studentNumber, @RequestBody Map<String, Object> body) {
        writeAuditLog("System", "External", "update", "Updated medical record status for student " + studentNumber, "127.0.0.1");
        return Map.of("success", true, "message", "Medical record updated for student " + studentNumber);
    }

    // HR System Integration
    @GetMapping("/external/hr/staff/{staffNumber}")
    public Map<String, Object> getStaffFromHR(@PathVariable String staffNumber) {
        Map<String, Object> staff = new LinkedHashMap<>();
        staff.put("staff_number", staffNumber);
        staff.put("full_name", "Staff Member " + staffNumber);
        staff.put("first_name", "Staff");
        staff.put("last_name", staffNumber);
        staff.put("date_of_birth", "1980-05-20");
        staff.put("gender", "Female");
        staff.put("email", "staff" + staffNumber + "@unza.ac.zm");
        staff.put("phone", "+260977654321");
        staff.put("address", "Great East Road Campus, Lusaka");
        staff.put("department", "Computer Science");
        staff.put("position", "Lecturer");
        staff.put("requires_medical_exam", false);
        staff.put("medical_exam_status", "exempted");
        return staff;
    }

    @GetMapping("/external/hr/staff/search")
    public List<Map<String, Object>> searchStaffByName(@RequestParam String name) {
        List<Map<String, Object>> results = new ArrayList<>();
        Map<String, Object> staff = new LinkedHashMap<>();
        staff.put("staff_number", "EMP12345");
        staff.put("full_name", name);
        staff.put("date_of_birth", "1980-05-20");
        staff.put("gender", "Female");
        staff.put("email", name.toLowerCase().replace(" ", ".") + "@unza.ac.zm");
        staff.put("phone", "+260977654321");
        staff.put("department", "Computer Science");
        staff.put("position", "Lecturer");
        staff.put("requires_medical_exam", false);
        staff.put("medical_exam_status", "exempted");
        results.add(staff);
        return results;
    }

    @GetMapping("/external/hr/staff/{staffNumber}/medical-status")
    public Map<String, Object> getStaffMedicalStatus(@PathVariable String staffNumber) {
        Patient patient = resolvePatient(staffNumber);
        return buildMedicalStatusResponse(
                patient,
                stringValue(staffNumber).trim(),
                "staff",
                false,
                "exempted"
        );
    }

    @GetMapping("/external/hr/staff/{staffNumber}/details")
    public Map<String, Object> getStaffDetailsFromHR(@PathVariable String staffNumber) {
        Map<String, Object> staff = getStaffFromHR(staffNumber);
        List<Map<String, Object>> dependents = new ArrayList<>();
        Map<String, Object> dependent = new LinkedHashMap<>();
        dependent.put("name", "Child One");
        dependent.put("date_of_birth", "2010-03-10");
        dependent.put("relationship", "Child");
        dependents.add(dependent);
        staff.put("dependents", dependents);
        return staff;
    }

    @GetMapping("/external/hr/staff/{staffNumber}/spouse")
    public Map<String, Object> getStaffSpouse(@PathVariable String staffNumber) {
        Map<String, Object> spouse = new LinkedHashMap<>();
        spouse.put("name", "Spouse Name");
        spouse.put("date_of_birth", "1985-08-25");
        spouse.put("phone", "+260955111222");
        return spouse;
    }

    @GetMapping("/external/hr/staff/{staffNumber}/dependents")
    public List<Map<String, Object>> getStaffDependents(@PathVariable String staffNumber) {
        List<Map<String, Object>> dependents = new ArrayList<>();
        Map<String, Object> dependent1 = new LinkedHashMap<>();
        dependent1.put("name", "Child One");
        dependent1.put("date_of_birth", "2010-03-10");
        dependent1.put("relationship", "Child");
        dependents.add(dependent1);
        Map<String, Object> dependent2 = new LinkedHashMap<>();
        dependent2.put("name", "Child Two");
        dependent2.put("date_of_birth", "2012-07-15");
        dependent2.put("relationship", "Child");
        dependents.add(dependent2);
        return dependents;
    }

    @PutMapping("/external/hr/staff/{staffNumber}/medical-record")
    public Map<String, Object> updateStaffMedicalRecord(@PathVariable String staffNumber, @RequestBody Map<String, Object> body) {
        writeAuditLog("System", "External", "update", "Updated medical record status for staff " + staffNumber, "127.0.0.1");
        return Map.of("success", true, "message", "Medical record updated for staff " + staffNumber);
    }

    @GetMapping("/appointments")
    public List<Map<String, Object>> getAppointments(HttpServletRequest httpRequest) {
        requirePermission(httpRequest, "schedules.view");
        return dataStore.getAppointments().stream().map(this::toAppointmentResponse).toList();
    }

    @PostMapping("/appointments")
    public Map<String, Object> createAppointment(HttpServletRequest httpRequest, @Valid @RequestBody AppointmentCreateRequest request) {
        requirePermission(httpRequest, "schedules.view");
        Patient linkedPatient = resolvePatient(request.patientId());
        Appointment appointment = new Appointment();
        appointment.setAppointmentId("APT-" + LocalDateTime.now().getYear() + String.format("%05d", dataStore.getAppointments().size() + 1));
        appointment.setPatientId(resolveCanonicalPatientId(request.patientId()));
        appointment.setPatientName(resolveCanonicalPatientName(linkedPatient, request.patientName()));
        appointment.setDoctorId(request.doctorId());
        appointment.setDoctorName(request.doctorName());
        appointment.setDepartment(request.department());
        appointment.setDate(request.date());
        appointment.setTime(request.time());
        appointment.setType(request.type());
        appointment.setStatus("scheduled");
        appointment.setNotes(request.notes());
        appointment = dataStore.addAppointment(appointment);
        return Map.of("success", true, "appointment_id", appointment.getAppointmentId(), "entry", toAppointmentResponse(appointment));
    }

    @GetMapping("/prescriptions")
    public Object getPrescriptions(HttpServletRequest httpRequest,
                                   @RequestParam(required = false) Integer page,
                                   @RequestParam(required = false) Integer size) {
        requireAnyPermission(httpRequest, List.of("prescriptions.view", "pharmacy.view", "pharmacy.dispense"));
        List<Map<String, Object>> all = dataStore.getPrescriptions().stream().map(this::toPrescriptionResponse).toList();
        return paginate(all, page, size);
    }

    @PostMapping("/prescriptions")
    public Map<String, Object> createPrescription(HttpServletRequest httpRequest, @Valid @RequestBody PrescriptionCreateRequest request) {
        AppUser actor = requirePermission(httpRequest, "prescriptions.view");
        Patient linkedPatient = resolvePatient(request.patientId());

        List<PrescriptionItemDto> drugItems = request.drugItems() != null ? request.drugItems() : List.of();
        if (drugItems.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "At least one drug item is required");
        }

        String itemsSummary = drugItems.stream()
                .map(item -> stringValue(item.drugName()) + " × " + (item.quantity() == null ? 1 : item.quantity()))
                .collect(Collectors.joining(", "));

        PrescriptionItemDto first = drugItems.get(0);

        Prescription prescription = new Prescription();
        prescription.setRxId("RX-" + LocalDateTime.now().getYear() + "-" + String.format("%03d", dataStore.getPrescriptions().size() + 1));
        prescription.setPatientId(resolveCanonicalPatientId(request.patientId()));
        prescription.setPatientName(resolveCanonicalPatientName(linkedPatient, request.patientName()));
        prescription.setPatientIdNum(resolveCanonicalPatientId(firstNonBlank(request.patientIdNum(), request.patientId()).toString()));
        prescription.setDoctor(resolveActorName(actor, request.doctor(), "Current Doctor"));
        prescription.setDate(LocalDate.now().toString());
        prescription.setItems(itemsSummary);
        prescription.setDrugName(stringValue(first.drugName()));
        prescription.setQuantity(first.quantity() == null || first.quantity() <= 0 ? 1 : first.quantity());
        prescription.setDosage(stringValue(first.dosage()));
        prescription.setDuration(stringValue(first.duration()));
        prescription.setInstructions(stringValue(first.instructions()));
        prescription.setMedicationClass(stringValue(first.medicationClass()));
        prescription.setProgram(stringValue(request.program()));
        prescription.setStatus("pending");
        prescription = dataStore.addPrescription(prescription);

        Long prescriptionId = prescription.getId();
        for (PrescriptionItemDto item : drugItems) {
            PrescriptionItem pi = new PrescriptionItem();
            pi.setPrescriptionId(prescriptionId);
            pi.setDrugName(stringValue(item.drugName()));
            pi.setQuantity(item.quantity() == null || item.quantity() <= 0 ? 1 : item.quantity());
            pi.setDosage(stringValue(item.dosage()));
            pi.setDuration(stringValue(item.duration()));
            pi.setInstructions(stringValue(item.instructions()));
            pi.setMedicationClass(stringValue(item.medicationClass()));
            prescriptionItemRepository.save(pi);
        }

        return Map.of("success", true, "rx_id", prescription.getRxId(), "entry", toPrescriptionResponse(prescription));
    }

    @PostMapping("/prescriptions/{id}/dispense")
    public Map<String, Object> dispensePrescription(HttpServletRequest httpRequest, @PathVariable Long id) {
        AppUser actor = requireAnyPermission(httpRequest, List.of("pharmacy.dispense", "pharmacy.view"));
        Prescription prescription = dataStore.getPrescription(id);
        if (prescription == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Prescription not found");
        }
        if (isEqualIgnoreCase(prescription.getStatus(), "dispensed")) {
            return Map.of("success", true, "entry", toPrescriptionResponse(prescription));
        }

        List<PrescriptionItem> lineItems = prescriptionItemRepository.findByPrescriptionId(id);
        if (lineItems.isEmpty()) {
            // Legacy single-item fallback
            Drug matchedDrug = findDrugForPrescription(prescription);
            if (matchedDrug != null) {
                int qty = Math.max(defaultInt(prescription.getQuantity()), 1);
                if (defaultInt(matchedDrug.getStock()) < qty) {
                    throw new ResponseStatusException(HttpStatus.BAD_REQUEST, matchedDrug.getName() + " is out of stock");
                }
                matchedDrug.setStock(defaultInt(matchedDrug.getStock()) - qty);
                applyDrugStatus(matchedDrug);
                dataStore.updateDrug(matchedDrug);
            }
        } else {
            for (PrescriptionItem lineItem : lineItems) {
                Drug drug = findDrugByName(stringValue(lineItem.getDrugName()));
                if (drug != null) {
                    int qty = Math.max(lineItem.getQuantity() == null ? 1 : lineItem.getQuantity(), 1);
                    if (defaultInt(drug.getStock()) < qty) {
                        throw new ResponseStatusException(HttpStatus.BAD_REQUEST, lineItem.getDrugName() + " is out of stock");
                    }
                    int newStock = defaultInt(drug.getStock()) - qty;
                    drug.setStock(newStock);
                    applyDrugStatus(drug);
                    dataStore.updateDrug(drug);
                    if (newStock <= 10 && newStock >= 0) {
                        NotificationItem lowStockNotif = new NotificationItem();
                        lowStockNotif.setType("warning");
                        lowStockNotif.setTitle("Low Stock Alert");
                        lowStockNotif.setMessage(drug.getName() + " is running low — only " + newStock + " units remaining. Please reorder.");
                        lowStockNotif.setTime(LocalDateTime.now().toString());
                        lowStockNotif.setRead(false);
                        dataStore.addNotification(lowStockNotif);
                    }
                }
            }
        }

        prescription.setStatus("dispensed");
        prescription = dataStore.updatePrescription(prescription);
        writeAuditLog(actor.getName(), actor.getRole(), "update", "Dispensed prescription " + prescription.getRxId() + " (" + lineItems.size() + " item(s)).", "127.0.0.1");
        return Map.of("success", true, "entry", toPrescriptionResponse(prescription));
    }

    @GetMapping("/admissions")
    public List<Map<String, Object>> getAdmissions(HttpServletRequest httpRequest) {
        requirePermission(httpRequest, "admissions.view");
        return dataStore.getAdmissions().stream().map(this::toAdmissionResponse).toList();
    }

    @PostMapping("/admissions")
    public Map<String, Object> createAdmissions(HttpServletRequest httpRequest, @Valid @RequestBody AdmissionCreateRequest request) {
        requirePermission(httpRequest, "admissions.view");
        Patient linkedPatient = resolvePatient(request.patientId());
        Admission admission = new Admission();
        admission.setAdmissionId("ADM-" + LocalDateTime.now().getYear() + "-" + String.format("%03d", dataStore.getAdmissions().size() + 1));
        admission.setPatientId(resolveCanonicalPatientId(request.patientId()));
        admission.setPatientName(resolveCanonicalPatientName(linkedPatient, request.patientName()));
        admission.setWard(request.ward());
        admission.setBed(request.bed());
        admission.setDoctor(request.doctor());
        admission.setAdmittedOn(request.admittedOn());
        admission.setDiagnosis(request.diagnosis());
        admission.setStatus(request.ward().toLowerCase(Locale.ROOT).contains("icu") ? "critical" : "active");
        admission = dataStore.addAdmission(admission);
        wsService.broadcastWardStatus(buildWardSummary());
        return Map.of("success", true, "admission_id", admission.getAdmissionId(), "entry", toAdmissionResponse(admission));
    }

    @PutMapping("/admissions/{id}/discharge")
    public Map<String, Object> dischargeAdmission(HttpServletRequest httpRequest, @PathVariable Long id, @Valid @RequestBody AdmissionDischargeRequest request) {
        AppUser actor = requirePermission(httpRequest, "admissions.view");
        Admission admission = dataStore.getAdmission(id);
        if (admission == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Admission not found");
        }
        if (isEqualIgnoreCase(admission.getStatus(), "discharged")) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Admission is already discharged");
        }
        admission.setStatus("discharged");
        admission.setDischargeType(request.dischargeType());
        admission.setDischargeSummary(request.dischargeSummary());
        admission.setDischargedOn(hasText(request.dischargedOn()) ? request.dischargedOn() : LocalDate.now().toString());
        admission.setDischargedBy(request.dischargedBy());
        admission = dataStore.updateAdmission(admission);
        wsService.broadcastWardStatus(buildWardSummary());
        writeAuditLog(actor.getName(), "discharge", "Discharged admission " + admission.getAdmissionId() + " for " + admission.getPatientName(), "127.0.0.1");
        return Map.of("success", true, "entry", toAdmissionResponse(admission));
    }

    @PutMapping("/admissions/{id}/transfer")
    public Map<String, Object> transferAdmission(
            HttpServletRequest httpRequest,
            @PathVariable Long id,
            @Valid @RequestBody AdmissionTransferRequest request
    ) {
        AppUser actor = requirePermission(httpRequest, "admissions.view");
        Admission admission = dataStore.getAdmission(id);
        if (admission == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Admission not found");
        }
        if (isEqualIgnoreCase(admission.getStatus(), "discharged")) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Discharged admissions cannot be transferred");
        }
        Long admissionId = admission.getId();
        boolean bedTaken = dataStore.getAdmissions().stream()
                .filter(Objects::nonNull)
                .filter(entry -> !Objects.equals(entry.getId(), admissionId))
                .filter(entry -> !isEqualIgnoreCase(entry.getStatus(), "discharged"))
                .anyMatch(entry -> isEqualIgnoreCase(entry.getWard(), request.ward()) && isEqualIgnoreCase(entry.getBed(), request.bed()));
        if (bedTaken) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Selected bed is already occupied");
        }

        admission.setWard(request.ward().trim());
        admission.setBed(request.bed().trim());
        admission.setStatus(request.ward().toLowerCase(Locale.ROOT).contains("icu") ? "critical" : "active");
        admission = dataStore.updateAdmission(admission);
        writeAuditLog(actor.getName(), actor.getRole(), "update", "Transferred admission " + admission.getAdmissionId() + " to " + admission.getWard() + " " + admission.getBed(), "127.0.0.1");
        return Map.of("success", true, "entry", toAdmissionResponse(admission));
    }

    @GetMapping("/lab-tests")
    public Object getLabTests(HttpServletRequest httpRequest,
                              @RequestParam(required = false) Integer page,
                              @RequestParam(required = false) Integer size) {
        requirePermission(httpRequest, "laboratory.view");
        List<Map<String, Object>> all = dataStore.getLabTests().stream().map(this::toLabTestResponse).toList();
        return paginate(all, page, size);
    }

    @PostMapping("/lab-tests")
    public Map<String, Object> createLabTests(HttpServletRequest httpRequest, @Valid @RequestBody LabTestCreateRequest request) {
        AppUser actor = requirePermission(httpRequest, "laboratory.view");
        Patient linkedPatient = resolvePatient(request.patientId());
        LabTest labTest = new LabTest();
        labTest.setTestId("LAB-" + String.format("%03d", dataStore.getLabTests().size() + 1));
        labTest.setPatientId(resolveCanonicalPatientId(request.patientId()));
        labTest.setPatientName(resolveCanonicalPatientName(linkedPatient, request.patientName()));
        labTest.setTest(request.test());
        labTest.setCategory(stringValue(request.category()));
        labTest.setSection(stringValue(request.section()));
        labTest.setSampleType(stringValue(request.sampleType()));
        labTest.setClinicalNotes(stringValue(request.clinicalNotes()));
        labTest.setRequestedBy(resolveActorName(actor, request.requestedBy(), "Current Doctor"));
        labTest.setDate(LocalDate.now().toString());
        labTest.setStatus("pending");
        labTest.setResults("");
        labTest.setInterpretation("");
        labTest.setReportedBy("");
        labTest.setCompletedAt("");
        labTest.setReferenceRange("");
        labTest.setAbnormalFlag("pending");
        labTest.setSpecimenCollectedAt("");
        labTest.setApprovedBy("");
        labTest.setApprovedAt("");
        labTest = dataStore.addLabTest(labTest);
        return Map.of("success", true, "test_id", labTest.getTestId(), "entry", toLabTestResponse(labTest));
    }

    @PutMapping("/lab-tests/{id}/results")
    public Map<String, Object> saveLabResults(
            HttpServletRequest httpRequest,
            @PathVariable Long id,
            @Valid @RequestBody LabResultUpdateRequest request
    ) {
        AppUser actor = requirePermission(httpRequest, "laboratory.view");
        LabTest labTest = dataStore.getLabTest(id);
        if (labTest == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Lab test not found");
        }

        labTest.setResults(stringValue(request.results()));
        labTest.setInterpretation(stringValue(request.interpretation()));
        labTest.setReportedBy(resolveActorName(actor, request.reportedBy(), "Laboratory User"));
        labTest.setCompletedAt(LocalDateTime.now().toString());
        labTest.setReferenceRange(stringValue(request.referenceRange()));
        labTest.setAbnormalFlag(hasText(request.abnormalFlag()) ? request.abnormalFlag().trim() : "normal");
        labTest.setSpecimenCollectedAt(stringValue(firstNonBlank(request.specimenCollectedAt(), labTest.getSpecimenCollectedAt(), LocalDateTime.now().toString())));
        labTest.setApprovedBy(resolveActorName(actor, request.approvedBy(), stringValue(labTest.getApprovedBy())));
        labTest.setApprovedAt(LocalDateTime.now().toString());
        labTest.setStatus("completed");
        labTest = dataStore.updateLabTest(labTest);

        NotificationItem notif = new NotificationItem();
        notif.setType("lab");
        notif.setTitle("Lab Results Ready");
        notif.setMessage("Results for " + stringValue(labTest.getTest()) + " — Patient: " + stringValue(labTest.getPatientName()) + ". Reported by " + stringValue(labTest.getReportedBy()) + ".");
        notif.setTime(LocalDateTime.now().toString());
        notif.setRead(false);
        dataStore.addNotification(notif);

        return Map.of("success", true, "entry", toLabTestResponse(labTest));
    }

    @GetMapping("/billing")
    public Object getBilling(HttpServletRequest httpRequest,
                             @RequestParam(required = false) Integer page,
                             @RequestParam(required = false) Integer size) {
        requirePermission(httpRequest, "billing.view");
        List<Map<String, Object>> all = dataStore.getBillingInvoices().stream().map(this::toBillingResponse).toList();
        return paginate(all, page, size);
    }

    @GetMapping("/tariffs")
    public List<Map<String, Object>> getTariffs(HttpServletRequest httpRequest,
            @RequestParam(required = false) String department,
            @RequestParam(required = false) String category,
            @RequestParam(required = false) String search) {
        requireAnyPermission(httpRequest, List.of("billing.view", "billing.create", "tariffs.manage"));
        return dataStore.getServiceTariffs().stream()
                .filter(Objects::nonNull)
                .filter(tariff -> isBlank(department) || isEqualIgnoreCase(tariff.getDepartment(), department))
                .filter(tariff -> isBlank(category) || isEqualIgnoreCase(tariff.getCategory(), category))
                .filter(tariff -> isBlank(search)
                        || containsIgnoreCase(tariff.getServiceName(), search)
                        || containsIgnoreCase(tariff.getTariffCode(), search)
                        || containsIgnoreCase(tariff.getDepartment(), search))
                .map(this::toTariffResponse)
                .toList();
    }

    @PostMapping("/tariffs")
    public Map<String, Object> createTariff(HttpServletRequest httpRequest, @Valid @RequestBody ServiceTariffRequest request) {
        requirePermission(httpRequest, "tariffs.manage");
        if (dataStore.getServiceTariffByCode(request.tariffCode()) != null) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Tariff code already exists");
        }

        ServiceTariff tariff = new ServiceTariff();
        tariff.setTariffCode(request.tariffCode().trim());
        tariff.setDepartment(request.department().trim());
        tariff.setCategory(request.category().trim());
        tariff.setServiceName(request.serviceName().trim());
        tariff.setUnitLabel(request.unitLabel().trim());
        tariff.setPrice(value(request.price()));
        tariff.setStatus(isBlank(request.status()) ? "active" : request.status().trim().toLowerCase(Locale.ROOT));
        tariff = dataStore.addServiceTariff(tariff);
        return Map.of("success", true, "entry", toTariffResponse(tariff));
    }

    @PutMapping("/tariffs/{tariffCode}")
    public Map<String, Object> updateTariff(HttpServletRequest httpRequest, @PathVariable String tariffCode, @Valid @RequestBody ServiceTariffRequest request) {
        requirePermission(httpRequest, "tariffs.manage");
        ServiceTariff tariff = dataStore.getServiceTariffByCode(tariffCode);
        if (tariff == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Tariff not found");
        }

        String requestedCode = request.tariffCode().trim();
        if (!requestedCode.equalsIgnoreCase(tariffCode)) {
            ServiceTariff existing = dataStore.getServiceTariffByCode(requestedCode);
            if (existing != null && !Objects.equals(existing.getId(), tariff.getId())) {
                throw new ResponseStatusException(HttpStatus.CONFLICT, "Tariff code already exists");
            }
            tariff.setTariffCode(requestedCode);
        }

        tariff.setDepartment(request.department().trim());
        tariff.setCategory(request.category().trim());
        tariff.setServiceName(request.serviceName().trim());
        tariff.setUnitLabel(request.unitLabel().trim());
        tariff.setPrice(value(request.price()));
        tariff.setStatus(isBlank(request.status()) ? "active" : request.status().trim().toLowerCase(Locale.ROOT));
        tariff = dataStore.addServiceTariff(tariff);
        return Map.of("success", true, "entry", toTariffResponse(tariff));
    }

    @GetMapping("/billing/{invoiceId}")
    public Map<String, Object> getBillingInvoice(HttpServletRequest httpRequest, @PathVariable String invoiceId) {
        requirePermission(httpRequest, "billing.view");
        BillingInvoice invoice = dataStore.getBillingInvoiceByInvoiceId(invoiceId);
        if (invoice == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Invoice not found");
        }
        return toBillingResponse(invoice);
    }

    @PostMapping("/billing")
    public Map<String, Object> createBilling(HttpServletRequest httpRequest, @Valid @RequestBody BillingCreateRequest request) {
        requirePermission(httpRequest, "billing.create");
        Patient linkedPatient = resolvePatient(request.patientId());
        List<Map<String, Object>> lineItems = normalizeBillingLineItems(request.lineItems());
        if (lineItems.isEmpty() && isBlank(request.items())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "At least one approved service is required");
        }

        double subtotal = !lineItems.isEmpty()
                ? lineItems.stream().mapToDouble(item -> value(item.get("line_total"))).sum()
                : value(request.subtotal());
        double tax = value(request.tax());
        double total = subtotal + tax;

        BillingInvoice invoice = new BillingInvoice();
        invoice.setInvoiceId("INV-" + LocalDateTime.now().getYear() + "-" + String.format("%03d", dataStore.getBillingInvoices().size() + 1));
        invoice.setPatientId(resolveCanonicalPatientId(request.patientId()));
        invoice.setPatientName(resolveCanonicalPatientName(linkedPatient, request.patientName()));
        invoice.setItems(!lineItems.isEmpty()
                ? lineItems.stream().map(item -> stringValue(item.get("service_name"))).collect(Collectors.joining(", "))
                : stringValue(request.items()));
        invoice.setLineItemsJson(writeJson(lineItems));
        invoice.setSubtotal(subtotal);
        invoice.setTax(tax);
        invoice.setTotal(total);
        invoice.setStatus("pending");
        invoice.setDueDate(stringValue(request.dueDate()));
        invoice.setPaidDate("");
        invoice.setPaymentMethod(stringValue(request.paymentMethod()));
        invoice = dataStore.addBillingInvoice(invoice);
        return Map.of("success", true, "invoice_id", invoice.getInvoiceId(), "entry", toBillingResponse(invoice));
    }

    @PutMapping("/billing/{invoiceId}/status")
    public Map<String, Object> updateBillingStatus(HttpServletRequest httpRequest, @PathVariable String invoiceId, @RequestBody BillingStatusUpdateRequest request) {
        requirePermission(httpRequest, "billing.payments");
        BillingInvoice invoice = dataStore.getBillingInvoiceByInvoiceId(invoiceId);
        if (invoice == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Invoice not found");
        }

        String nextStatus = isBlank(request.status()) ? invoice.getStatus() : request.status().trim().toLowerCase(Locale.ROOT);
        invoice.setStatus(nextStatus);
        if (!isBlank(request.paymentMethod())) {
            invoice.setPaymentMethod(request.paymentMethod().trim());
        }
        if (isEqualIgnoreCase(nextStatus, "completed")) {
            invoice.setPaidDate(isBlank(request.paidDate()) ? LocalDate.now().toString() : request.paidDate().trim());
        } else if (isEqualIgnoreCase(nextStatus, "pending")) {
            invoice.setPaidDate("");
        }

        invoice = dataStore.addBillingInvoice(invoice);
        return Map.of("success", true, "entry", toBillingResponse(invoice));
    }

    @GetMapping("/inventory")
    public List<Map<String, Object>> getInventory(HttpServletRequest httpRequest) {
        requirePermission(httpRequest, "inventory.view");
        return dataStore.getInventoryRecords().stream().map(this::toInventoryResponse).toList();
    }

    @PostMapping("/inventory")
    public Map<String, Object> createInventory(HttpServletRequest httpRequest, @Valid @RequestBody InventoryCreateRequest request) {
        requirePermission(httpRequest, "inventory.view");
        int quantity = defaultInt(request.quantity());
        int minStock = defaultInt(request.minStock());
        String status = quantity == 0 ? "out-of-stock" : quantity <= minStock ? "low-stock" : "in-stock";
        InventoryRecord record = new InventoryRecord();
        record.setItemCode("INV-" + String.format("%03d", dataStore.getInventoryRecords().size() + 1));
        record.setName(request.name());
        record.setCategory(request.category());
        record.setQuantity(quantity);
        record.setUnit(request.unit());
        record.setMinStock(minStock);
        record.setLocation(request.location());
        record.setLastRestocked(LocalDate.now().toString());
        record.setStatus(status);
        record = dataStore.addInventoryRecord(record);
        return Map.of("success", true, "item_code", record.getItemCode(), "entry", toInventoryResponse(record));
    }

    @GetMapping("/suppliers")
    public List<Map<String, Object>> getSuppliers(HttpServletRequest httpRequest) {
        requirePermission(httpRequest, "suppliers.view");
        return dataStore.getSuppliers().stream().map(this::toSupplierResponse).toList();
    }

    @PostMapping("/suppliers")
    public Map<String, Object> createSuppliers(HttpServletRequest httpRequest, @Valid @RequestBody SupplierCreateRequest request) {
        requirePermission(httpRequest, "suppliers.view");
        Supplier supplier = new Supplier();
        supplier.setSupplierId("SUP-" + String.format("%03d", dataStore.getSuppliers().size() + 1));
        supplier.setName(request.name());
        supplier.setContact(request.contact());
        supplier.setPhone(request.phone());
        supplier.setEmail(request.email());
        supplier.setItems(0);
        supplier.setLastOrder("Never");
        supplier.setStatus("active");
        supplier = dataStore.addSupplier(supplier);
        return Map.of("success", true, "supplier_id", supplier.getSupplierId(), "entry", toSupplierResponse(supplier));
    }

    @GetMapping("/users")
    public List<Map<String, Object>> getUsers(HttpServletRequest httpRequest) {
        requirePermission(httpRequest, "users.manage");
        return dataStore.getUsers().stream().map(this::toUserResponse).toList();
    }

    @GetMapping("/users/{id}")
    public Map<String, Object> getUser(HttpServletRequest httpRequest, @PathVariable Long id) {
        AppUser currentUser = requireAuthenticatedUser(httpRequest);
        if (!Objects.equals(currentUser.getId(), id) && !userHasPermission(currentUser, "users.manage")) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "You do not have permission to view this user");
        }
        AppUser user = dataStore.getUser(id);
        if (user == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found");
        }
        return toUserResponse(user);
    }

    @PostMapping("/users")
    public Map<String, Object> createUsers(HttpServletRequest httpRequest, @Valid @RequestBody UserCreateRequest request) {
        AppUser actor = requirePermission(httpRequest, "users.manage");
        boolean actorIsAdmin = isEqualIgnoreCase(actor.getRole(), "Admin");
        if (!actorIsAdmin && hasText(request.role()) && isEqualIgnoreCase(request.role(), "Admin")) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Only an Admin can assign the Admin role");
        }
        if (!actorIsAdmin && request.permissions() != null) {
            List<String> actorPermissions = parseUserPermissions(actor);
            List<String> requestedPermissions = request.permissions().stream().filter(this::hasText).map(String::trim).toList();
            if (!actorPermissions.containsAll(requestedPermissions)) {
                throw new ResponseStatusException(HttpStatus.FORBIDDEN, "You cannot grant permissions you do not hold yourself");
            }
        }
        StaffMember linkedStaff = null;
        if (hasText(request.staffId())) {
            linkedStaff = dataStore.getStaffMemberByStaffId(request.staffId().trim());
        } else if (hasText(request.manNumber())) {
            linkedStaff = dataStore.getStaffMemberByManNumber(request.manNumber().trim());
        }

        AppUser user = new AppUser();
        user.setUserId("USR-" + String.format("%03d", dataStore.getUsers().size() + 1));
        user.setName(linkedStaff != null ? linkedStaff.getName() : request.name());
        user.setEmail(request.email());
        user.setRole(request.role());
        user.setDepartment(linkedStaff != null && hasText(linkedStaff.getDepartment()) ? linkedStaff.getDepartment() : request.department());
        user.setStaffId(linkedStaff != null ? linkedStaff.getStaffId() : stringValue(request.staffId()));
        user.setManNumber(linkedStaff != null ? stringValue(linkedStaff.getManNumber()) : stringValue(request.manNumber()));
        user.setPassword(request.password());
        user.setPermissionsJson(writeJson(resolvePermissions(request.role(), request.permissions())));
        user.setForcePasswordChange(true);
        user.setStatus(hasText(request.status()) ? request.status().trim().toLowerCase(Locale.ROOT) : "active");
        user.setLastLogin("");
        user.setPasswordChangedAt(LocalDateTime.now().toString());
        user.setPasswordVersion(1);
        user = dataStore.addUser(user);
        writeAuditLog("System", "System", "create", "Provisioned user account for " + user.getName() + " as " + user.getRole() + " in " + user.getDepartment(), "127.0.0.1");
        return Map.of("success", true, "user_id", user.getUserId(), "entry", toUserResponse(user));
    }

    @PutMapping("/users/{id}")
    public Map<String, Object> updateUser(HttpServletRequest httpRequest, @PathVariable Long id, @Valid @RequestBody UserUpdateRequest request) {
        AppUser currentUser = requireAuthenticatedUser(httpRequest);
        boolean managingUsers = userHasPermission(currentUser, "users.manage");
        boolean updatingSelf = Objects.equals(currentUser.getId(), id);
        if (!managingUsers && !updatingSelf) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "You do not have permission to update this user");
        }
        if (!managingUsers && (hasText(request.role()) || hasText(request.status()) || request.permissions() != null)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "You do not have permission to change roles, status, or permissions");
        }
        if (!managingUsers && request.forcePasswordChange() != null && Boolean.TRUE.equals(request.forcePasswordChange())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "You do not have permission to trigger a forced password reset");
        }
        boolean actorIsAdmin = isEqualIgnoreCase(currentUser.getRole(), "Admin");
        if (!actorIsAdmin && hasText(request.role()) && isEqualIgnoreCase(request.role(), "Admin")) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Only an Admin can assign the Admin role");
        }
        if (!actorIsAdmin && request.permissions() != null) {
            List<String> actorPermissions = parseUserPermissions(currentUser);
            List<String> requestedPermissions = request.permissions().stream().filter(this::hasText).map(String::trim).toList();
            if (!actorPermissions.containsAll(requestedPermissions)) {
                throw new ResponseStatusException(HttpStatus.FORBIDDEN, "You cannot grant permissions you do not hold yourself");
            }
        }
        AppUser user = dataStore.getUser(id);
        if (user == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found");
        }

        String beforeRole = stringValue(user.getRole());
        String beforeDepartment = stringValue(user.getDepartment());
        String beforeStatus = stringValue(user.getStatus());
        boolean beforeForcePasswordChange = Boolean.TRUE.equals(user.getForcePasswordChange());
        List<String> beforePermissions = parseUserPermissions(user);

        if (hasText(request.name())) {
            user.setName(request.name().trim());
        }
        if (hasText(request.email())) {
            user.setEmail(request.email().trim());
        }
        if (hasText(request.role())) {
            user.setRole(request.role().trim());
        }
        if (hasText(request.department())) {
            user.setDepartment(request.department().trim());
        }
        if (hasText(request.password())) {
            user.setPassword(request.password().trim());
            user.setForcePasswordChange(managingUsers);
            user.setPasswordChangedAt(LocalDateTime.now().toString());
            user.setPasswordVersion((user.getPasswordVersion() == null ? 1 : user.getPasswordVersion()) + 1);
        }
        if (hasText(request.status())) {
            user.setStatus(request.status().trim().toLowerCase(Locale.ROOT));
        }
        if (request.forcePasswordChange() != null) {
            user.setForcePasswordChange(request.forcePasswordChange());
        }
        if (request.permissions() != null) {
            user.setPermissionsJson(writeJson(resolvePermissions(user.getRole(), request.permissions())));
        }

        user = dataStore.updateUser(user);
        List<String> changes = new ArrayList<>();
        if (!isEqualIgnoreCase(beforeRole, user.getRole())) changes.add("role " + beforeRole + " -> " + user.getRole());
        if (!isEqualIgnoreCase(beforeDepartment, user.getDepartment())) changes.add("department " + beforeDepartment + " -> " + user.getDepartment());
        if (!isEqualIgnoreCase(beforeStatus, user.getStatus())) changes.add("status " + beforeStatus + " -> " + user.getStatus());
        if (beforeForcePasswordChange != Boolean.TRUE.equals(user.getForcePasswordChange())) changes.add("force password change flag updated");
        if (!beforePermissions.equals(parseUserPermissions(user))) changes.add("permissions updated");
        if (hasText(request.password())) changes.add("password changed");
        String changeSummary = changes.isEmpty() ? "Updated user profile details." : "Updated user: " + String.join(", ", changes) + ".";
        writeAuditLog(user.getName(), user.getRole(), "update", changeSummary, "127.0.0.1");
        return Map.of("success", true, "entry", toUserResponse(user));
    }

    @DeleteMapping("/users/{id}")
    public Map<String, Object> deleteUser(HttpServletRequest httpRequest, @PathVariable Long id) {
        AppUser currentUser = requirePermission(httpRequest, "users.manage");
        if (Objects.equals(currentUser.getId(), id)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "You cannot delete the account you are currently using");
        }
        AppUser user = dataStore.getUser(id);
        if (user == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found");
        }
        dataStore.deleteUser(user);
        writeAuditLog(currentUser.getName(), currentUser.getRole(), "delete", "Deleted user account for " + user.getName() + ".", "127.0.0.1");
        return Map.of("success", true);
    }

    @GetMapping("/drugs")
    public List<Map<String, Object>> getDrugs(HttpServletRequest httpRequest) {
        requirePermission(httpRequest, "pharmacy.view");
        return dataStore.getDrugs().stream().map(this::toDrugResponse).toList();
    }

    @PostMapping("/drugs")
    public Map<String, Object> createDrug(HttpServletRequest httpRequest, @Valid @RequestBody DrugCreateRequest request) {
        requirePermission(httpRequest, "pharmacy.view");
        int stock = defaultInt(request.stock());
        Drug drug = new Drug();
        drug.setDrugId("DRG-" + String.format("%03d", dataStore.getDrugs().size() + 1));
        drug.setName(request.name());
        drug.setCategory(request.category());
        drug.setDrugType(request.drugType());
        drug.setBatchNumber(hasText(request.batchNumber()) ? request.batchNumber().trim() : "BATCH-" + String.format("%03d", dataStore.getDrugs().size() + 1));
        drug.setStock(stock);
        int reorderLevel = defaultInt(request.reorderLevel());
        drug.setReorderLevel(reorderLevel == 0 ? 50 : reorderLevel);
        drug.setUnit(request.unit());
        drug.setExpiry(request.expiry());
        drug.setStorageLocation(hasText(request.storageLocation()) ? request.storageLocation().trim() : "Main Store");
        applyDrugStatus(drug);
        drug = dataStore.addDrug(drug);
        return Map.of("success", true, "drug_id", drug.getDrugId(), "entry", toDrugResponse(drug));
    }

    @PostMapping("/drugs/{id}/restock")
    public Map<String, Object> restockDrug(HttpServletRequest httpRequest, @PathVariable Long id, @Valid @RequestBody DrugStockAdjustmentRequest request) {
        AppUser actor = requirePermission(httpRequest, "pharmacy.view");
        Drug drug = requireDrug(id);
        int quantity = defaultInt(request.quantity());
        if (quantity <= 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Restock quantity must be greater than zero");
        }
        drug.setStock(defaultInt(drug.getStock()) + quantity);
        applyDrugStatus(drug);
        drug = dataStore.updateDrug(drug);
        writeAuditLog(actor.getName(), actor.getRole(), "update", "Restocked " + drug.getName() + " by " + quantity + " " + stringValue(drug.getUnit()) + ".", "127.0.0.1");
        return Map.of("success", true, "entry", toDrugResponse(drug));
    }

    @GetMapping("/imaging")
    public List<Map<String, Object>> getImaging(HttpServletRequest httpRequest) {
        requirePermission(httpRequest, "radiology.view");
        return dataStore.getImagingRequests().stream().map(this::toImagingResponse).toList();
    }

    @PostMapping("/imaging")
    public Map<String, Object> createImaging(HttpServletRequest httpRequest, @Valid @RequestBody ImagingCreateRequest request) {
        AppUser actor = requirePermission(httpRequest, "radiology.view");
        Patient linkedPatient = resolvePatient(request.patientId());
        ImagingRequest imagingRequest = new ImagingRequest();
        imagingRequest.setRequestId("RAD-" + String.format("%03d", dataStore.getImagingRequests().size() + 1));
        imagingRequest.setPatientId(resolveCanonicalPatientId(request.patientId()));
        imagingRequest.setPatientName(resolveCanonicalPatientName(linkedPatient, request.patientName()));
        imagingRequest.setType(request.type());
        imagingRequest.setBodyPart(request.bodyPart());
        imagingRequest.setRequestedBy(resolveActorName(actor, request.requestedBy(), "Radiology User"));
        imagingRequest.setRadiologist("");
        imagingRequest.setRequestDate(LocalDate.now().toString());
        imagingRequest.setFindings("");
        imagingRequest.setStatus("pending");
        imagingRequest = dataStore.addImagingRequest(imagingRequest);
        return Map.of("success", true, "request_id", imagingRequest.getRequestId(), "entry", toImagingResponse(imagingRequest));
    }

    @GetMapping("/insurance-claims")
    public List<Map<String, Object>> getInsuranceClaims(HttpServletRequest httpRequest) {
        requirePermission(httpRequest, "insurance.view");
        return dataStore.getInsuranceClaims().stream().map(this::toInsuranceClaimResponse).toList();
    }

    @PostMapping("/insurance-claims")
    public Map<String, Object> createInsuranceClaim(HttpServletRequest httpRequest, @Valid @RequestBody InsuranceClaimCreateRequest request) {
        requirePermission(httpRequest, "insurance.view");
        InsuranceClaim claim = new InsuranceClaim();
        claim.setClaimId("CLM-" + LocalDateTime.now().getYear() + "-" + String.format("%03d", dataStore.getInsuranceClaims().size() + 1));
        claim.setPatient(request.patient());
        claim.setInsurer(request.insurer());
        claim.setService(request.service());
        claim.setAmount(value(request.amount()));
        claim.setSubmitted(LocalDate.now().toString());
        claim.setStatus("pending");
        claim = dataStore.addInsuranceClaim(claim);
        return Map.of("success", true, "claim_id", claim.getClaimId(), "entry", toInsuranceClaimResponse(claim));
    }

    @GetMapping("/referrals")
    public List<Map<String, Object>> getReferrals(HttpServletRequest httpRequest) {
        requirePermission(httpRequest, "referrals.view");
        return dataStore.getReferralRecords().stream().map(this::toReferralResponse).toList();
    }

    @PostMapping("/referrals")
    public Map<String, Object> createReferral(HttpServletRequest httpRequest, @Valid @RequestBody ReferralCreateRequest request) {
        AppUser actor = requirePermission(httpRequest, "referrals.view");
        Patient linkedPatient = resolvePatient(request.patientId());
        ReferralRecord record = new ReferralRecord();
        record.setReferralId("REF-" + String.format("%03d", dataStore.getReferralRecords().size() + 1));
        record.setPatientId(resolveCanonicalPatientId(request.patientId()));
        record.setPatientName(resolveCanonicalPatientName(linkedPatient, request.patientName()));
        record.setFromDept(request.fromDept());
        record.setToDept(request.toDept());
        record.setReferredBy(resolveActorName(actor, request.referredBy(), "Clinic Staff"));
        record.setReason(request.reason());
        record.setUrgency(request.urgency());
        record.setDate(LocalDate.now().toString());
        record.setStatus("pending");
        record.setNotes("");
        record = dataStore.addReferralRecord(record);
        return Map.of("success", true, "referral_id", record.getReferralId(), "entry", toReferralResponse(record));
    }

    @GetMapping("/triage")
    public Object getTriage(HttpServletRequest httpRequest,
                            @RequestParam(required = false) Integer page,
                            @RequestParam(required = false) Integer size) {
        requireAnyPermission(httpRequest, List.of("triage.view", "walkin.view", "patients.view"));
        List<Map<String, Object>> all = dataStore.getTriageRecords().stream().map(this::toTriageResponse).toList();
        return paginate(all, page, size);
    }

    @PostMapping("/triage")
    public Map<String, Object> createTriage(HttpServletRequest httpRequest, @Valid @RequestBody TriageCreateRequest request) {
        AppUser actor = requirePermission(httpRequest, "triage.view");
        Patient linkedPatient = resolvePatient(request.patientId());
        TriageRecord record = new TriageRecord();
        record.setPatientId(resolveCanonicalPatientId(request.patientId()));
        record.setPatientName(resolveCanonicalPatientName(linkedPatient, request.patientName()));
        record.setLevel(request.level());
        record.setChiefComplaint(request.chiefComplaint());
        record.setBloodPressure(request.bloodPressure());
        record.setTemperature(request.temperature());
        record.setPulseRate(request.pulseRate());
        record.setRespiratoryRate(request.respiratoryRate());
        record.setOxygenSaturation(request.oxygenSaturation());
        record.setWeightKg(request.weightKg());
        record.setHeightCm(request.heightCm());
        record.setBmi(request.bmi() != null ? request.bmi() : calculateBmi(request.weightKg(), request.heightCm()));
        record.setRandomBloodSugar(request.randomBloodSugar());
        record.setPainScore(request.painScore());
        record.setConsciousnessLevel(stringValue(request.consciousnessLevel()));
        record.setNotes(stringValue(request.notes()));
        record.setVitalSigns(buildTriageVitalsSummary(record));
        record.setNurseName(resolveActorName(actor, request.nurseName(), "Triage Nurse"));
        record.setArrivalTime(LocalDateTime.now().toLocalTime().withSecond(0).withNano(0).toString());
        record.setStatus("waiting");
        record = dataStore.addTriageRecord(record);
        return Map.of("success", true, "entry", toTriageResponse(record));
    }

    @GetMapping("/queue")
    public Object getQueue(HttpServletRequest httpRequest,
                           @RequestParam(required = false) Integer page,
                           @RequestParam(required = false) Integer size) {
        requirePermission(httpRequest, "walkin.view");
        List<Map<String, Object>> all = dataStore.getQueueTickets().stream().map(this::toQueueResponse).toList();
        return paginate(all, page, size);
    }

    @PostMapping("/queue")
    public Map<String, Object> createQueueTicket(HttpServletRequest httpRequest, @Valid @RequestBody QueueTicketCreateRequest request) {
        requirePermission(httpRequest, "walkin.view");
        String time = LocalDateTime.now().toLocalTime().withSecond(0).withNano(0).toString();
        QueueTicket ticket = new QueueTicket();
        ticket.setTicketNo("Q-" + String.format("%03d", dataStore.getQueueTickets().size() + 1));
        ticket.setPatientId(stringValue(request.patientId()).isBlank() ? "UNZA-" + LocalDate.now().getYear() + "-" + String.format("%03d", dataStore.getQueueTickets().size() + 1) : request.patientId());
        ticket.setPatientName(request.patientName());
        ticket.setDepartment(request.department());
        ticket.setPriority(request.priority());
        ticket.setCheckInTime(time);
        ticket.setWaitTime("0 min");
        ticket.setStatus("waiting");
        ticket = dataStore.addQueueTicket(ticket);
        return Map.of("success", true, "entry", toQueueResponse(ticket));
    }

    @PutMapping("/queue/{id}/status")
    public Map<String, Object> updateQueueStatus(HttpServletRequest httpRequest, @PathVariable Long id, @Valid @RequestBody QueueStatusUpdateRequest request) {
        requirePermission(httpRequest, "walkin.view");
        QueueTicket existing = dataStore.getQueueTicket(id);
        if (existing == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Queue ticket not found");
        }
        existing.setDepartment(stringValue(request.department()).isBlank() ? existing.getDepartment() : request.department());
        existing.setStatus(request.status());
        dataStore.updateQueueTicket(existing);
        return Map.of("success", true, "entry", toQueueResponse(existing));
    }

    @DeleteMapping("/queue/{id}")
    public Map<String, Object> deleteQueueTicket(HttpServletRequest httpRequest, @PathVariable Long id) {
        requirePermission(httpRequest, "walkin.view");
        dataStore.deleteQueueTicket(id);
        return Map.of("success", true);
    }

    @GetMapping("/emergency")
    public List<Map<String, Object>> getEmergencyCases(HttpServletRequest httpRequest) {
        requirePermission(httpRequest, "emergency.view");
        return dataStore.getEmergencyCases().stream().map(this::toEmergencyResponse).toList();
    }

    @PostMapping("/emergency")
    public Map<String, Object> createEmergencyCase(HttpServletRequest httpRequest, @Valid @RequestBody EmergencyCaseCreateRequest request) {
        requirePermission(httpRequest, "emergency.view");
        EmergencyCase record = new EmergencyCase();
        record.setCaseId("ER-" + String.format("%03d", dataStore.getEmergencyCases().size() + 1));
        record.setPatientName(request.patientName());
        record.setAge(request.age());
        record.setGender(request.gender());
        record.setSeverity(request.severity());
        record.setChiefComplaint(request.chiefComplaint());
        record.setArrivalMode(request.arrivalMode());
        record.setArrivalTime(LocalDateTime.now().toLocalTime().withSecond(0).withNano(0).toString());
        record.setAttendingDoctor(request.attendingDoctor());
        record.setNurseOnDuty(request.nurseOnDuty());
        record.setVitals(request.vitals());
        record.setStatus("active");
        record = dataStore.addEmergencyCase(record);
        return Map.of("success", true, "entry", toEmergencyResponse(record));
    }

    @PutMapping("/emergency/{id}/status")
    public Map<String, Object> updateEmergencyCase(HttpServletRequest httpRequest, @PathVariable Long id, @Valid @RequestBody EmergencyStatusUpdateRequest request) {
        requirePermission(httpRequest, "emergency.view");
        EmergencyCase existing = dataStore.getEmergencyCase(id);
        if (existing == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Emergency case not found");
        }
        existing.setStatus(request.status());
        dataStore.updateEmergencyCase(existing);
        return Map.of("success", true, "entry", toEmergencyResponse(existing));
    }

    @GetMapping("/blood-bank/stock")
    public List<Map<String, Object>> getBloodBankStock(HttpServletRequest httpRequest) {
        requirePermission(httpRequest, "bloodbank.view");
        return dataStore.getBloodUnits().stream().map(this::toBloodUnitResponse).toList();
    }

    @GetMapping("/blood-bank/donations")
    public List<Map<String, Object>> getDonations(HttpServletRequest httpRequest) {
        requirePermission(httpRequest, "bloodbank.view");
        return dataStore.getDonations().stream().map(this::toDonationResponse).toList();
    }

    @PostMapping("/blood-bank/donations")
    public Map<String, Object> createDonation(HttpServletRequest httpRequest, @Valid @RequestBody DonationCreateRequest request) {
        requirePermission(httpRequest, "bloodbank.view");
        int units = Math.max(defaultInt(request.units()), 1);
        Donation donation = new Donation();
        donation.setDonorName(request.donorName());
        donation.setBloodType(request.bloodType());
        donation.setUnits(units);
        donation.setDate(LocalDate.now().toString());
        donation.setStatus("completed");
        donation = dataStore.addDonation(donation);

        BloodUnit unit = dataStore.getBloodUnits().stream()
                .filter(item -> item.getBloodType().equalsIgnoreCase(request.bloodType()))
                .findFirst()
                .orElse(null);
        if (unit != null) {
            unit.setQuantity(defaultInt(unit.getQuantity()) + units);
            unit.setDonorName(request.donorName());
            unit.setCollectionDate(LocalDate.now().toString());
            unit.setStatus(resolveBloodUnitStatus(defaultInt(unit.getQuantity())));
            dataStore.updateBloodUnit(unit);
        } else {
            BloodUnit newUnit = new BloodUnit();
            newUnit.setUnitId("BLD-" + String.format("%03d", dataStore.getBloodUnits().size() + 1));
            newUnit.setBloodType(request.bloodType());
            newUnit.setQuantity(units);
            newUnit.setStatus(resolveBloodUnitStatus(units));
            newUnit.setExpiryDate(LocalDate.now().plusDays(45).toString());
            newUnit.setDonorName(request.donorName());
            newUnit.setCollectionDate(LocalDate.now().toString());
            dataStore.addBloodUnit(newUnit);
        }

        return Map.of("success", true, "entry", toDonationResponse(donation));
    }

    @PostMapping("/blood-bank/stock/{id}/issue")
    public Map<String, Object> issueBloodUnit(HttpServletRequest httpRequest, @PathVariable Long id, @Valid @RequestBody BloodIssueRequest request) {
        AppUser actor = requirePermission(httpRequest, "bloodbank.view");
        BloodUnit unit = requireBloodUnit(id);
        int units = defaultInt(request.units());
        if (units <= 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Units issued must be greater than zero");
        }
        if (defaultInt(unit.getQuantity()) < units) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Not enough blood units available");
        }
        unit.setQuantity(defaultInt(unit.getQuantity()) - units);
        unit.setStatus(resolveBloodUnitStatus(defaultInt(unit.getQuantity())));
        unit = dataStore.updateBloodUnit(unit);
        writeAuditLog(actor.getName(), actor.getRole(), "update", "Issued " + units + " unit(s) of " + unit.getBloodType() + " for " + request.patientName() + ".", "127.0.0.1");
        return Map.of("success", true, "entry", toBloodUnitResponse(unit));
    }

    @GetMapping("/notifications")
    public List<Map<String, Object>> getNotifications(HttpServletRequest httpRequest) {
        requirePermission(httpRequest, "notifications.view");
        return dataStore.getNotifications().stream().map(this::toNotificationResponse).toList();
    }

    @PutMapping("/notifications/{id}")
    public Map<String, Object> updateNotification(HttpServletRequest httpRequest, @PathVariable Long id, @RequestBody NotificationStatusRequest request) {
        requirePermission(httpRequest, "notifications.view");
        NotificationItem existing = dataStore.getNotification(id);
        if (existing == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Notification not found");
        }
        if (request.read() != null) {
            existing.setRead(request.read());
        }
        dataStore.updateNotification(existing);
        return Map.of("success", true, "entry", toNotificationResponse(existing));
    }

    @PutMapping("/notifications/read-all")
    public Map<String, Object> markAllNotificationsRead(HttpServletRequest httpRequest) {
        requirePermission(httpRequest, "notifications.view");
        dataStore.getNotifications().forEach(item -> {
            item.setRead(true);
            dataStore.updateNotification(item);
        });
        return Map.of("success", true);
    }

    @DeleteMapping("/notifications/{id}")
    public Map<String, Object> deleteNotification(HttpServletRequest httpRequest, @PathVariable Long id) {
        requirePermission(httpRequest, "notifications.view");
        dataStore.deleteNotification(id);
        return Map.of("success", true);
    }

    @GetMapping("/audit-logs")
    public Object getAuditLogs(HttpServletRequest httpRequest,
                               @RequestParam(required = false) Integer page,
                               @RequestParam(required = false) Integer size) {
        requirePermission(httpRequest, "audit.view");
        List<Map<String, Object>> all = dataStore.getAuditLogs().stream().map(this::toAuditLogResponse).toList();
        return paginate(all, page, size);
    }

    @GetMapping("/audit/login")
    public Object getLoginAuditLogs(HttpServletRequest httpRequest,
                                    @RequestParam(required = false) Integer page,
                                    @RequestParam(required = false) Integer size,
                                    @RequestParam(required = false) String email) {
        requirePermission(httpRequest, "audit.view");
        List<LoginAuditLog> logs = email != null && !email.isBlank()
                ? dataStore.getLoginAuditLogsByEmail(email)
                : dataStore.getLoginAuditLogs();
        List<Map<String, Object>> mapped = logs.stream().map(l -> {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("id", l.getId());
            m.put("auditId", l.getAuditId());
            m.put("email", l.getEmail());
            m.put("ipAddress", l.getIpAddress());
            m.put("userAgent", l.getUserAgent());
            m.put("success", l.isSuccess());
            m.put("failureReason", l.getFailureReason());
            m.put("userId", l.getUserId());
            m.put("role", l.getRole());
            m.put("loggedAt", l.getLoggedAt() != null ? l.getLoggedAt().toString() : "");
            return m;
        }).toList();
        return paginate(mapped, page, size);
    }

    @GetMapping("/attendance")
    public List<Map<String, Object>> getAttendance(HttpServletRequest httpRequest) {
        requirePermission(httpRequest, "attendance.view");
        return dataStore.getAttendanceRecords().stream().map(this::toAttendanceResponse).toList();
    }

    @GetMapping("/staff-schedules")
    public List<Map<String, Object>> getStaffSchedules(HttpServletRequest httpRequest,
            @RequestParam(required = false) String weekOf) {
        requirePermission(httpRequest, "schedules.view");
        List<StaffSchedule> list = hasText(weekOf)
                ? dataStore.getStaffSchedulesByWeek(weekOf)
                : dataStore.getStaffSchedules();
        return list.stream().map(this::toStaffScheduleResponse).toList();
    }

    @PostMapping("/staff-schedules")
    public Map<String, Object> createStaffSchedule(HttpServletRequest httpRequest, @Valid @RequestBody StaffScheduleCreateRequest request) {
        requirePermission(httpRequest, "schedules.view");
        StaffSchedule schedule = new StaffSchedule();
        schedule.setScheduleId("SCH-" + String.format("%03d", dataStore.getStaffSchedules().size() + 1));
        schedule.setStaffId(request.staffId());
        schedule.setName(request.name());
        schedule.setRole(request.role());
        schedule.setDepartment(request.department());
        schedule.setDayOfWeek(request.dayOfWeek());
        schedule.setWeekOf(request.weekOf());
        schedule.setShiftName(request.shiftName());
        schedule.setStartTime(request.startTime());
        schedule.setEndTime(request.endTime());
        schedule.setLocation(hasText(request.location()) ? request.location().trim() : request.department());
        schedule.setStatus("Scheduled");
        schedule = dataStore.addStaffSchedule(schedule);
        writeAuditLog("System", "create", "Added staff schedule for " + schedule.getName(), "127.0.0.1");
        return Map.of("success", true, "entry", toStaffScheduleResponse(schedule));
    }

    @PostMapping("/staff-schedules/bulk")
    public Map<String, Object> bulkCreateStaffSchedules(HttpServletRequest httpRequest, @Valid @RequestBody StaffScheduleBulkCreateRequest request) {
        requirePermission(httpRequest, "schedules.view");
        List<Map<String, Object>> created = new ArrayList<>();
        for (String day : request.days()) {
            StaffSchedule schedule = new StaffSchedule();
            schedule.setScheduleId("SCH-" + String.format("%03d", dataStore.getStaffSchedules().size() + 1));
            schedule.setStaffId(request.staffId());
            schedule.setName(request.name());
            schedule.setRole(request.role());
            schedule.setDepartment(request.department());
            schedule.setDayOfWeek(day);
            schedule.setWeekOf(request.weekOf());
            schedule.setShiftName(request.shiftName());
            schedule.setStartTime(request.startTime());
            schedule.setEndTime(request.endTime());
            schedule.setLocation(hasText(request.location()) ? request.location().trim() : request.department());
            schedule.setStatus("Scheduled");
            schedule = dataStore.addStaffSchedule(schedule);
            created.add(toStaffScheduleResponse(schedule));
        }
        writeAuditLog("System", "create", "Bulk added " + created.size() + " schedule entries for " + request.name(), "127.0.0.1");
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("success", true);
        result.put("count", created.size());
        result.put("entries", created);
        return result;
    }

    @DeleteMapping("/staff-schedules/{id}")
    public Map<String, Object> deleteStaffSchedule(HttpServletRequest httpRequest, @PathVariable Long id) {
        requirePermission(httpRequest, "schedules.view");
        StaffSchedule existing = dataStore.getStaffSchedule(id);
        if (existing == null) throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Schedule entry not found");
        dataStore.deleteStaffSchedule(id);
        writeAuditLog("System", "delete", "Removed schedule entry for " + existing.getName() + " on " + existing.getDayOfWeek(), "127.0.0.1");
        return Map.of("success", true);
    }

    @PutMapping("/attendance/{id}/checkout")
    public Map<String, Object> checkoutAttendance(HttpServletRequest httpRequest, @PathVariable Long id) {
        requirePermission(httpRequest, "attendance.view");
        AttendanceRecord existing = dataStore.getAttendanceRecord(id);
        if (existing == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Attendance record not found");
        }
        existing.setCheckOut(LocalDateTime.now().toLocalTime().withSecond(0).withNano(0).toString());
        existing.setStatus("off-duty");
        dataStore.updateAttendanceRecord(existing);
        return Map.of("success", true, "entry", toAttendanceResponse(existing));
    }

    @GetMapping("/wards")
    public List<Map<String, Object>> getWards(HttpServletRequest httpRequest) {
        requirePermission(httpRequest, "wards.view");
        return buildWardResponses();
    }

    @PostMapping("/wards")
    public Map<String, Object> createWard(HttpServletRequest httpRequest, @Valid @RequestBody WardCreateRequest request) {
        AppUser actor = requirePermission(httpRequest, "wards.view");
        String wardName = stringValue(request.name()).trim();
        if (isBlank(wardName)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Ward name is required");
        }
        if (dataStore.getWards().stream().anyMatch(ward -> ward != null && isEqualIgnoreCase(ward.getName(), wardName))) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Ward already exists");
        }
        int totalBeds = defaultInt(request.totalBeds());
        if (totalBeds < 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Total beds cannot be negative");
        }
        WardStatus ward = new WardStatus();
        ward.setName(wardName);
        ward.setTotalBeds(totalBeds);
        ward.setOccupied(0);
        ward.setAvailable(totalBeds);
        ward = dataStore.addWard(ward);
        writeAuditLog(actor.getName(), actor.getRole(), "create", "Created ward " + ward.getName() + " with " + totalBeds + " beds.", "127.0.0.1");
        return Map.of("success", true, "entry", toWardResponse(ward, 0));
    }

    @PutMapping("/wards/{id}/beds")
    public Map<String, Object> updateWardBeds(HttpServletRequest httpRequest, @PathVariable Long id, @Valid @RequestBody WardBedsUpdateRequest request) {
        requirePermission(httpRequest, "departments.manage");
        WardStatus ward = updateWardBedCapacity(id, defaultInt(request.totalBeds()), "Updated ward bed capacity");
        return Map.of("success", true, "entry", toWardResponse(ward, countOccupiedBedsForWard(ward.getName())));
    }

    @PostMapping("/wards/{id}/beds/add")
    public Map<String, Object> addWardBed(HttpServletRequest httpRequest, @PathVariable Long id) {
        requirePermission(httpRequest, "departments.manage");
        WardStatus existingWard = requireWard(id);
        WardStatus ward = updateWardBedCapacity(id, defaultInt(existingWard.getTotalBeds()) + 1, "Added one bed to ward");
        return Map.of("success", true, "entry", toWardResponse(ward, countOccupiedBedsForWard(ward.getName())));
    }

    @PostMapping("/wards/{id}/beds/remove")
    public Map<String, Object> removeWardBed(HttpServletRequest httpRequest, @PathVariable Long id) {
        requirePermission(httpRequest, "departments.manage");
        WardStatus existingWard = requireWard(id);
        WardStatus ward = updateWardBedCapacity(id, Math.max(defaultInt(existingWard.getTotalBeds()) - 1, 0), "Removed one bed from ward");
        return Map.of("success", true, "entry", toWardResponse(ward, countOccupiedBedsForWard(ward.getName())));
    }

    @GetMapping("/dashboard")
    public Map<String, Object> getDashboardSummary(HttpServletRequest httpRequest) {
        requirePermission(httpRequest, "dashboard.view");
        List<Patient> patients = dataStore.getPatients().stream().filter(Objects::nonNull).toList();
        List<EncounterRecord> encounters = dataStore.getEncounterRecords().stream().filter(Objects::nonNull).toList();
        List<Admission> admissions = dataStore.getAdmissions().stream().filter(Objects::nonNull).toList();
        List<BillingInvoice> billing = dataStore.getBillingInvoices().stream().filter(Objects::nonNull).toList();
        List<TriageRecord> triageRecords = dataStore.getTriageRecords().stream().filter(Objects::nonNull).toList();
        List<LabTest> labTests = dataStore.getLabTests().stream().filter(Objects::nonNull).toList();
        List<ClinicalFormRecord> clinicalForms = dataStore.getClinicalForms().stream().filter(Objects::nonNull).toList();
        List<Drug> drugs = dataStore.getDrugs().stream().filter(Objects::nonNull).toList();
        List<EmergencyCase> emergencyCases = dataStore.getEmergencyCases().stream().filter(Objects::nonNull).toList();
        List<WardStatus> wards = dataStore.getWards().stream().filter(Objects::nonNull).toList();

        long occupiedBeds = admissions.stream().filter(admission -> !isEqualIgnoreCase(admission.getStatus(), "discharged")).count();
        long totalBeds = wards.stream().map(WardStatus::getTotalBeds).filter(Objects::nonNull).mapToLong(Integer::longValue).sum();
        long availableBeds = Math.max(totalBeds - occupiedBeds, 0);
        long walkInsToday = encounters.stream().filter(encounter -> isSameDay(encounter.getCreatedAt(), LocalDate.now())).count();
        long activeEncounters = encounters.stream().filter(encounter -> !encounter.isCheckedOut()).count();
        long awaitingTriage = encounters.stream()
                .filter(encounter -> !encounter.isCheckedOut())
                .filter(encounter -> isEqualIgnoreCase(encounter.getCurrentStage(), "RECEPTION") || isEqualIgnoreCase(encounter.getCurrentStage(), "TRIAGE"))
                .count();
        long labPending = labTests.stream().filter(test -> !isEqualIgnoreCase(test.getStatus(), "completed")).count();
        long pendingBilling = billing.stream().filter(invoice -> !isEqualIgnoreCase(invoice.getStatus(), "completed")).count();
        long criticalStock = drugs.stream().filter(drug -> isEqualIgnoreCase(drug.getStatus(), "critical")).count();
        long formsToday = clinicalForms.stream().filter(form -> isSameDay(form.getCreatedAt(), LocalDate.now())).count();
        Map<String, Object> totals = new LinkedHashMap<>();
        totals.put("patients", patients.size());
        totals.put("walkInsToday", walkInsToday);
        totals.put("activeEncounters", activeEncounters);
        totals.put("awaitingTriage", awaitingTriage);
        totals.put("labPending", labPending);
        totals.put("pendingBilling", pendingBilling);
        totals.put("criticalStock", criticalStock);
        totals.put("formsToday", formsToday);
        totals.put("bedsOccupied", occupiedBeds);
        totals.put("totalBeds", totalBeds);
        totals.put("availableBeds", availableBeds);

        Map<String, Object> response = new LinkedHashMap<>();
        response.put("totals", totals);
        response.put("recentPatients", patients.stream().limit(5).map(this::toDashboardPatientResponse).toList());
        response.put("weeklyPatientFlow", buildWeeklyPatientFlow(encounters));
        response.put("departmentDistribution", buildSectionDistribution(encounters, labTests, billing, admissions, emergencyCases));
        response.put("sectionSummaries", buildSectionSummaries(encounters, triageRecords, labTests, billing, admissions, clinicalForms, emergencyCases, drugs));
        return response;
    }

    @GetMapping("/reports")
    public Map<String, Object> getReports(HttpServletRequest httpRequest) {
        requirePermission(httpRequest, "reports.view");
        List<Patient> patients = dataStore.getPatients();
        List<BillingInvoice> invoices = dataStore.getBillingInvoices();
        List<AppUser> users = dataStore.getUsers();
        List<NotificationItem> notifications = dataStore.getNotifications();
        List<EncounterRecord> encounters = dataStore.getEncounterRecords();
        List<ClinicalFormRecord> clinicalForms = dataStore.getClinicalForms();
        List<LabTest> labTests = dataStore.getLabTests();
        List<Admission> admissions = dataStore.getAdmissions();
        List<Prescription> prescriptions = dataStore.getPrescriptions();
        List<ReferralRecord> referrals = dataStore.getReferralRecords();
        List<TriageRecord> triageRecords = dataStore.getTriageRecords();
        List<EmergencyCase> emergencyCases = dataStore.getEmergencyCases();
        List<BloodUnit> bloodUnits = dataStore.getBloodUnits();

        Map<String, Object> summary = new LinkedHashMap<>();
        summary.put("patients", patients.size());
        summary.put("revenue", invoices.stream().mapToDouble(BillingInvoice::getTotal).sum());
        summary.put("activeUsers", users.stream().filter(user -> "active".equalsIgnoreCase(user.getStatus())).count());
        summary.put("unreadNotifications", notifications.stream().filter(item -> !item.isRead()).count());
        summary.put("activeEncounters", encounters.stream().filter(item -> !item.isCheckedOut()).count());
        summary.put("checkedOutToday", encounters.stream().filter(EncounterRecord::isCheckedOut).count());
        summary.put("savedForms", clinicalForms.size());
        summary.put("totalLabTests", labTests.size());
        summary.put("pendingLabTests", labTests.stream().filter(t -> "pending".equalsIgnoreCase(t.getStatus())).count());
        summary.put("completedLabTests", labTests.stream().filter(t -> "completed".equalsIgnoreCase(t.getStatus())).count());
        summary.put("totalAdmissions", admissions.size());
        summary.put("activeAdmissions", admissions.stream().filter(a -> !"discharged".equalsIgnoreCase(a.getStatus())).count());
        summary.put("totalPrescriptions", prescriptions.size());
        summary.put("dispensedPrescriptions", prescriptions.stream().filter(p -> "dispensed".equalsIgnoreCase(p.getStatus())).count());
        summary.put("pendingPrescriptions", prescriptions.stream().filter(p -> !"dispensed".equalsIgnoreCase(p.getStatus())).count());
        summary.put("totalReferrals", referrals.size());
        summary.put("pendingReferrals", referrals.stream().filter(r -> "pending".equalsIgnoreCase(r.getStatus())).count());
        summary.put("totalTriageRecords", triageRecords.size());
        summary.put("emergencyCases", emergencyCases.size());
        summary.put("totalDrugs", dataStore.getDrugs().size());
        summary.put("lowStockDrugs", dataStore.getDrugs().stream().filter(d -> defaultInt(d.getStock()) <= 10).count());

        Map<String, Object> response = new LinkedHashMap<>();
        response.put("summary", summary);
        response.put("monthlyPatients", buildMonthlyPatientsReport());
        response.put("revenueData", buildRevenueReport());
        response.put("drugUsage", buildDrugUsageReport());
        response.put("workflowByStage", buildWorkflowStageReport(encounters));
        response.put("payerMix", buildPayerMixReport(patients));
        response.put("clinicalFormsByType", buildClinicalFormTypeReport(clinicalForms));
        response.put("bmiDistribution", buildBmiDistributionReport(clinicalForms));
        response.put("diagnosisDistribution", buildDiagnosisDistributionReport(clinicalForms));
        response.put("labTestsByCategory", buildLabTestCategoryReport(labTests));
        response.put("labTestStatus", buildLabTestStatusReport(labTests));
        response.put("admissionsByWard", buildAdmissionsByWardReport(admissions));
        response.put("prescriptionsByClass", buildPrescriptionClassReport(prescriptions));
        response.put("referralsByDept", buildReferralsByDeptReport(referrals));
        response.put("referralsByUrgency", buildReferralsByUrgencyReport(referrals));
        response.put("triageLevels", buildTriageLevelReport(triageRecords));
        response.put("emergencyBySeverity", buildEmergencyBySeverityReport(emergencyCases));
        response.put("patientsByGender", buildPatientsByGenderReport(patients));
        response.put("patientsByType", buildPatientsByTypeReport(patients));
        response.put("patientAgeGroups", buildPatientAgeGroupReport(patients));
        response.put("bloodBankStock", buildBloodBankStockReport(bloodUnits));
        return response;
    }

    @GetMapping("/settings")
    public Map<String, Object> getSettings(HttpServletRequest httpRequest) {
        requirePermission(httpRequest, "settings.view");
        SystemSettings settings = dataStore.getSettings();
        if (settings == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Settings not found");
        }
        return toSettingsResponse(settings);
    }

    @PutMapping("/settings")
    public Map<String, Object> updateSettings(HttpServletRequest httpRequest, @RequestBody SettingsUpdateRequest request) {
        requirePermission(httpRequest, "settings.manage");
        SystemSettings settings = dataStore.getSettings();
        if (settings == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Settings not found");
        }

        if (hasText(request.hospitalName())) settings.setHospitalName(request.hospitalName().trim());
        if (hasText(request.contactPhone())) settings.setContactPhone(request.contactPhone().trim());
        if (hasText(request.address())) settings.setAddress(request.address().trim());
        if (request.emailNotifications() != null) settings.setEmailNotifications(request.emailNotifications());
        if (request.smsNotifications() != null) settings.setSmsNotifications(request.smsNotifications());
        if (request.lowStockAlerts() != null) settings.setLowStockAlerts(request.lowStockAlerts());
        if (request.twoFactorAuth() != null) settings.setTwoFactorAuth(request.twoFactorAuth());
        if (request.auditLogging() != null) settings.setAuditLogging(request.auditLogging());
        if (request.autoLogout() != null) settings.setAutoLogout(request.autoLogout());
        if (request.backupEnabled() != null) settings.setBackupEnabled(request.backupEnabled());
        if (hasText(request.backupFrequency())) settings.setBackupFrequency(request.backupFrequency().trim());
        if (hasText(request.backupLocation())) settings.setBackupLocation(request.backupLocation().trim());
        if (hasText(request.lastBackupAt())) settings.setLastBackupAt(request.lastBackupAt().trim());

        settings = dataStore.saveSettings(settings);
        writeAuditLog("System", "Admin", "settings", "Updated clinic settings.", "127.0.0.1");
        return Map.of("success", true, "entry", toSettingsResponse(settings));
    }

    @PostMapping("/admin/clear-seeded-data")
    public Map<String, Object> clearSeededData(HttpServletRequest httpRequest, @Valid @RequestBody AdminDataCleanupRequest request) {
        AppUser currentUser = requirePermission(httpRequest, "settings.manage");
        if (!"CLEAR SEEDED DATA".equalsIgnoreCase(stringValue(request.confirmation()).trim())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Confirmation text does not match");
        }

        AppUser adminUser = dataStore.getUser(request.userId());
        if (adminUser == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Admin user not found");
        }
        if (!isEqualIgnoreCase(adminUser.getRole(), "Admin")) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Only admin users can clear seeded data");
        }
        if (!Objects.equals(currentUser.getId(), adminUser.getId())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Authenticated admin does not match the requested account");
        }

        Map<String, Integer> summary = dataStore.clearSeededData(adminUser.getId());
        writeAuditLog(adminUser.getName(), "deployment_cleanup", "Cleared seeded data before deployment.", "127.0.0.1");

        Map<String, Object> response = new LinkedHashMap<>();
        response.put("success", true);
        response.put("preserved_user_id", adminUser.getId());
        response.put("summary", summary);
        return response;
    }

    @GetMapping("/encounters")
    public Object getEncounters(HttpServletRequest httpRequest,
                                @RequestParam(required = false) Integer page,
                                @RequestParam(required = false) Integer size) {
        requirePermission(httpRequest, "walkin.view");
        List<Map<String, Object>> all = dataStore.getEncounterRecords().stream().map(this::toEncounterResponse).toList();
        return paginate(all, page, size);
    }

    @PostMapping("/encounters")
    public Map<String, Object> createEncounter(HttpServletRequest httpRequest, @Valid @RequestBody EncounterCreateRequest request) {
        requirePermission(httpRequest, "walkin.view");
        Patient patient = resolvePatient(request.patientId());
        if (patient == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Patient must be registered before opening an encounter");
        }
        String canonicalPatientId = resolveCanonicalPatientId(request.patientId());
        boolean hasActiveEncounter = dataStore.getEncounterRecords().stream()
                .anyMatch(entry -> canonicalPatientId.equalsIgnoreCase(stringValue(entry.getPatientId())) && !entry.isCheckedOut());
        if (hasActiveEncounter) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "This patient already has an active encounter");
        }
        EncounterRecord record = new EncounterRecord();
        record.setEncounterId("ENC-" + LocalDateTime.now().getYear() + "-" + String.format("%03d", dataStore.getEncounterRecords().size() + 1));
        record.setPatientId(canonicalPatientId);
        record.setPatientName(resolveCanonicalPatientName(patient, request.patientName()));
        record.setPatientType(hasText(request.patientType()) ? request.patientType().trim() : "GENERAL");
        record.setCurrentStage(hasText(request.currentStage()) ? normalizeStage(request.currentStage()) : "RECEPTION");
        record.setPaymentStatus("NOT_REQUIRED");
        record.setCheckoutEligible(false);
        record.setCheckedOut(false);
        record.setCreatedAt(LocalDateTime.now().toString());
        record.setUpdatedAt(LocalDateTime.now().toString());
        record.setCreatedBy(hasText(request.createdBy()) ? request.createdBy().trim() : "Reception");
        record.setCheckoutTime("");
        record.setStageHistory(LocalDateTime.now() + "|" + record.getCurrentStage() + "|" + record.getCreatedBy() + "|Encounter opened");
        record.setPendingActions(stringValue(request.pendingActions()));
        record.setCompletedActions("Registration");
        record.setNotes(stringValue(request.notes()));
        record = dataStore.addEncounterRecord(record);
        writeAuditLog(record.getCreatedBy(), "create", "Opened encounter " + record.getEncounterId() + " for " + record.getPatientName(), "127.0.0.1");
        return Map.of("success", true, "encounter_id", record.getEncounterId(), "entry", toEncounterResponse(record));
    }

    @PutMapping("/encounters/{id}/stage")
    public Map<String, Object> updateEncounterStage(HttpServletRequest httpRequest, @PathVariable Long id, @RequestBody EncounterStageUpdateRequest request) {
        requirePermission(httpRequest, "walkin.view");
        EncounterRecord record = dataStore.getEncounterRecord(id);
        if (record == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Encounter not found");
        }

        String stage = hasText(request.stage()) ? normalizeStage(request.stage()) : record.getCurrentStage();
        String performedBy = hasText(request.performedBy()) ? request.performedBy().trim() : "Clinic User";
        String note = hasText(request.note()) ? request.note().trim() : "Stage updated";

        record.setCurrentStage(stage);
        record.setUpdatedAt(LocalDateTime.now().toString());
        record.setStageHistory(appendHistory(record.getStageHistory(), LocalDateTime.now() + "|" + stage + "|" + performedBy + "|" + note));
        if (hasText(request.pendingActions())) record.setPendingActions(request.pendingActions().trim());
        if (hasText(request.completedActions())) record.setCompletedActions(request.completedActions().trim());
        if (hasText(request.paymentStatus())) record.setPaymentStatus(request.paymentStatus().trim().toUpperCase(Locale.ROOT));
        if (request.checkoutEligible() != null) record.setCheckoutEligible(request.checkoutEligible());
        if (hasText(request.note())) record.setNotes(appendHistory(record.getNotes(), note));

        dataStore.updateEncounterRecord(record);
        writeAuditLog(performedBy, "update", "Moved encounter " + record.getEncounterId() + " to " + stage, "127.0.0.1");
        return Map.of("success", true, "entry", toEncounterResponse(record));
    }

    @PutMapping("/encounters/{id}/checkout")
    public Map<String, Object> checkoutEncounter(HttpServletRequest httpRequest, @PathVariable Long id, @RequestBody(required = false) EncounterCheckoutRequest request) {
        requirePermission(httpRequest, "walkin.view");
        EncounterRecord record = dataStore.getEncounterRecord(id);
        if (record == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Encounter not found");
        }
        if (!record.isCheckoutEligible()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Encounter cannot be checked out before clinical instructions are completed");
        }
        if ("PENDING".equalsIgnoreCase(record.getPaymentStatus())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Encounter still has pending payment clearance");
        }
        if (hasOutstandingActions(record.getPendingActions())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Encounter still has pending actions");
        }

        String performedBy = request != null && hasText(request.performedBy()) ? request.performedBy().trim() : "Checkout Desk";
        String note = request != null && hasText(request.note()) ? request.note().trim() : "Patient checked out";

        record.setCheckedOut(true);
        record.setCurrentStage("CHECKOUT");
        record.setCheckoutTime(LocalDateTime.now().toString());
        record.setUpdatedAt(LocalDateTime.now().toString());
        record.setStageHistory(appendHistory(record.getStageHistory(), LocalDateTime.now() + "|CHECKOUT|" + performedBy + "|" + note));
        dataStore.updateEncounterRecord(record);
        writeAuditLog(performedBy, "checkout", "Checked out encounter " + record.getEncounterId(), "127.0.0.1");
        return Map.of("success", true, "entry", toEncounterResponse(record));
    }

     @GetMapping("/system-backup")
     public Map<String, Object> getSystemBackup(HttpServletRequest httpRequest) {
         requirePermission(httpRequest, "backup.export");
         return Map.of(
                 "exportedAt", LocalDateTime.now().toString(),
                 "summary", Map.of(
                         "patients", dataStore.getPatients().size(),
                         "encounters", dataStore.getEncounterRecords().size(),
                         "billingInvoices", dataStore.getBillingInvoices().size(),
                         "drugs", dataStore.getDrugs().size(),
                         "users", dataStore.getUsers().size()
                 ),
                 "settings", toSettingsResponse(dataStore.getSettings()),
                 "patients", dataStore.getPatients().stream().map(this::toPatientResponse).toList(),
                 "encounters", dataStore.getEncounterRecords().stream().map(this::toEncounterResponse).toList(),
                 "billing", dataStore.getBillingInvoices().stream().map(this::toBillingResponse).toList(),
                 "drugs", dataStore.getDrugs().stream().map(this::toDrugResponse).toList(),
                 "auditLogs", dataStore.getAuditLogs().stream().map(this::toAuditLogResponse).toList()
         );
     }

     /**
      * Comprehensive backup of ALL clinic data - includes every entity.
      * Use this for complete system export/backup.
      */
     @GetMapping("/backup/full")
     public Map<String, Object> getFullBackup(HttpServletRequest httpRequest) {
         requirePermission(httpRequest, "backup.export");
         Map<String, Object> backup = backupService.createFullBackup();
         writeAuditLog(getCurrentUser(httpRequest).getName(), "backup", "Created full system backup", "127.0.0.1");
         return backup;
     }

     /**
      * Quick statistics backup - lightweight export for reporting purposes.
      */
     @GetMapping("/backup/stats")
     public Map<String, Object> getBackupStatistics(HttpServletRequest httpRequest) {
         requirePermission(httpRequest, "backup.export");
         Map<String, Object> result = new LinkedHashMap<>();
         result.put("exportedAt", LocalDateTime.now().toString());
         Map<String, Object> database = new LinkedHashMap<>();
         database.put("patients", dataStore.getPatients().size());
         database.put("staff", dataStore.getStaffMembers().size());
         database.put("users", dataStore.getUsers().size());
         database.put("encounters", dataStore.getEncounterRecords().size());
         database.put("triages", dataStore.getTriageRecords().size());
         database.put("emergencyCases", dataStore.getEmergencyCases().size());
         database.put("appointments", dataStore.getAppointments().size());
         database.put("prescriptions", dataStore.getPrescriptions().size());
         database.put("admissions", dataStore.getAdmissions().size());
         database.put("labTests", dataStore.getLabTests().size());
         database.put("imagingRequests", dataStore.getImagingRequests().size());
         database.put("clinicalForms", dataStore.getClinicalForms().size());
         database.put("billingInvoices", dataStore.getBillingInvoices().size());
         database.put("inventoryRecords", dataStore.getInventoryRecords().size());
         database.put("drugs", dataStore.getDrugs().size());
         database.put("suppliers", dataStore.getSuppliers().size());
         database.put("referrals", dataStore.getReferralRecords().size());
         database.put("insuranceClaims", dataStore.getInsuranceClaims().size());
         database.put("bloodUnits", dataStore.getBloodUnits().size());
         database.put("donations", dataStore.getDonations().size());
         database.put("queueTickets", dataStore.getQueueTickets().size());
         database.put("notifications", dataStore.getNotifications().size());
         database.put("attendanceRecords", dataStore.getAttendanceRecords().size());
         database.put("staffSchedules", dataStore.getStaffSchedules().size());
         database.put("wards", dataStore.getWards().size());
         database.put("serviceTariffs", dataStore.getServiceTariffs().size());
         database.put("auditLogs", dataStore.getAuditLogs().size());
          result.put("database", database);
          result.put("settings", toSettingsResponse(dataStore.getSettings()));
          return result;
     }

     /**
      * Restore data from backup.
      * WARNING: This will replace ALL existing data!
      */
     @PostMapping("/backup/restore")
     public Map<String, Object> restoreBackup(HttpServletRequest httpRequest, @Valid @RequestBody Map<String, Object> backupData) {
         requirePermission(httpRequest, "settings.manage");
         AppUser user = getCurrentUser(httpRequest);
         
         try {
             Map<String, Object> result = backupService.restoreFromBackup(backupData);
             writeAuditLog(user.getName(), "restore", "Restored system from backup", "127.0.0.1");
             return Map.of(
                     "success", true,
                     "message", "Backup restored successfully",
                     "restored", result
             );
         } catch (Exception e) {
             writeAuditLog(user.getName(), "restore", "Backup restore failed: " + e.getMessage(), "127.0.0.1");
             throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Restore failed: " + e.getMessage());
         }
     }

     /**
      * Trigger manual backup (immediate)
      */
     @PostMapping("/backup/manual")
     public Map<String, Object> triggerManualBackup(HttpServletRequest httpRequest) {
         requirePermission(httpRequest, "settings.manage");
         AppUser user = getCurrentUser(httpRequest);
         
         Map<String, Object> backup = backupService.createFullBackup();
         writeAuditLog(user.getName(), "backup", "Triggered manual backup", "127.0.0.1");
         
         return Map.of(
                 "success", true,
                 "message", "Manual backup created",
                 "backupId", ((Map<String, Object>) backup.get("backup_metadata")).get("backup_id"),
                 "exportedAt", ((Map<String, Object>) backup.get("backup_metadata")).get("created_at"),
                 "recordCount", ((Map<String, Object>) ((Map<String, Object>) backup.get("backup_metadata")).get("record_counts")).get("total")
         );
     }

    private AppUser getCurrentUser(HttpServletRequest httpRequest) {
         return requireAuthenticatedUser(httpRequest);
     }

     @GetMapping("/clinical-forms")
    public List<Map<String, Object>> getClinicalForms(HttpServletRequest httpRequest) {
        requirePermission(httpRequest, "forms.view");
        return dataStore.getClinicalForms().stream().map(this::toClinicalFormResponse).toList();
    }

    @PostMapping("/clinical-forms")
    public Map<String, Object> createClinicalForm(HttpServletRequest httpRequest, @Valid @RequestBody ClinicalFormCreateRequest request) {
        AppUser currentUser = requirePermission(httpRequest, "forms.view");
        Patient linkedPatient = resolvePatient(request.patientId());
        ClinicalFormRecord record = new ClinicalFormRecord();
        record.setFormId("FORM-" + String.format("%03d", dataStore.getClinicalForms().size() + 1));
        record.setFormType(request.formType().trim());
        record.setTitle(request.title().trim());
        record.setPatientId(resolveCanonicalPatientId(request.patientId()));
        record.setPatientName(resolveCanonicalPatientName(linkedPatient, request.patientName()));
        record.setDepartment(request.department().trim());
        record.setEncounterId(stringValue(request.encounterId()));
        record.setStatus(hasText(request.status()) ? request.status().trim() : "draft");
        record.setCreatedBy(hasText(request.createdBy()) ? request.createdBy().trim() : currentUser.getName());
        record.setCreatedAt(LocalDateTime.now().toString());
        record.setUpdatedAt(LocalDateTime.now().toString());
        record.setPayloadJson(normalizeClinicalPayloadJson(request.payloadJson()));
        record = dataStore.addClinicalForm(record);
        Patient patient = resolvePatient(record.getPatientId());
        if (patient != null && "medical_fitness_certificate".equalsIgnoreCase(record.getFormType())) {
            syncMedicalExamStatus(patient, parsePayloadJson(record));
        } else if (patient != null && "student_medical_exam".equalsIgnoreCase(record.getFormType())) {
            syncMedicalExamCompletion(patient, parsePayloadJson(record), record.getCreatedAt());
        }
        writeAuditLog(record.getCreatedBy(), "create", "Saved " + record.getTitle() + " for " + record.getPatientName(), "127.0.0.1");
        return Map.of("success", true, "entry", toClinicalFormResponse(record));
    }

    @PostMapping("/clinical-forms/generate-medical-fitness-certificate")
    public Map<String, Object> generateMedicalFitnessCertificate(
            HttpServletRequest httpRequest,
            @Valid @RequestBody MedicalCertificateGenerateRequest request
    ) {
        AppUser currentUser = requirePermission(httpRequest, "forms.view");
        Patient patient = resolvePatient(request.patientId());
        if (patient == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Patient not found");
        }

        ClinicalFormRecord sourceExam = resolveClinicalForm(
                request.sourceExamFormId(),
                patient.getPatientId(),
                "student_medical_exam",
                request.encounterId()
        );
        Map<String, Object> examPayload = sourceExam != null
                ? parsePayloadJson(sourceExam)
                : sanitizePayloadMap(request.examPayload());
        if (examPayload.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "A saved student medical exam is required before generating a certificate");
        }

        Map<String, Object> certificatePayload = buildMedicalFitnessCertificatePayload(
                patient,
                examPayload,
                hasText(request.createdBy()) ? request.createdBy().trim() : currentUser.getName()
        );

        ClinicalFormRecord record = new ClinicalFormRecord();
        record.setFormId("FORM-" + String.format("%03d", dataStore.getClinicalForms().size() + 1));
        record.setFormType("medical_fitness_certificate");
        record.setTitle("Certificate of Medical Fitness");
        record.setPatientId(patient.getPatientId());
        record.setPatientName(patient.getName());
        record.setDepartment("Clinical");
        record.setEncounterId(hasText(request.encounterId())
                ? request.encounterId().trim()
                : sourceExam != null ? stringValue(sourceExam.getEncounterId()) : "");
        record.setStatus("completed");
        record.setCreatedBy(hasText(request.createdBy()) ? request.createdBy().trim() : currentUser.getName());
        record.setCreatedAt(LocalDateTime.now().toString());
        record.setUpdatedAt(LocalDateTime.now().toString());
        try {
            record.setPayloadJson(objectMapper.writeValueAsString(certificatePayload));
        } catch (Exception ex) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Failed to prepare certificate payload");
        }

        record = dataStore.addClinicalForm(record);
        syncMedicalExamStatus(patient, certificatePayload);
        writeAuditLog(record.getCreatedBy(), "create", "Generated medical fitness certificate for " + record.getPatientName(), "127.0.0.1");
        return Map.of("success", true, "entry", toClinicalFormResponse(record));
    }

    @GetMapping("/billing/summary")
    public List<Map<String, Object>> getBillingSummary(HttpServletRequest httpRequest) {
        requirePermission(httpRequest, "billing.view");
        return dataStore.getBillingInvoices().stream()
                .collect(java.util.stream.Collectors.groupingBy(BillingInvoice::getPatientId, LinkedHashMap::new, java.util.stream.Collectors.toList()))
                .entrySet()
                .stream()
                .map(entry -> {
                    List<BillingInvoice> invoices = entry.getValue();
                    Map<String, Object> response = new LinkedHashMap<>();
                    response.put("patient_id", entry.getKey());
                    response.put("patient_name", invoices.get(0).getPatientName());
                    response.put("invoice_count", invoices.size());
                    response.put("total_amount", invoices.stream().mapToDouble(BillingInvoice::getTotal).sum());
                    response.put("pending_amount", invoices.stream().filter(item -> "pending".equalsIgnoreCase(item.getStatus())).mapToDouble(BillingInvoice::getTotal).sum());
                    response.put("last_invoice", invoices.stream().map(BillingInvoice::getInvoiceId).reduce((first, second) -> second).orElse(""));
                    response.put("payment_status", invoices.stream().anyMatch(item -> "pending".equalsIgnoreCase(item.getStatus())) ? "pending" : "cleared");
                    response.put("services", invoices.stream().map(BillingInvoice::getItems).toList());
                    return response;
                })
                .toList();
    }

     @PostMapping("/login")
     public ResponseEntity<?> login(@Valid @RequestBody LoginRequest request, HttpServletRequest httpRequest) {
         String ip = extractClientIp(httpRequest);
         String userAgent = stringValue(httpRequest.getHeader("User-Agent"));

         // Rate-limit check
         if (rateLimiter.isBlocked(ip)) {
             long retryAfter = rateLimiter.retryAfterSeconds(ip);
             return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS)
                     .header("Retry-After", String.valueOf(retryAfter))
                     .body(Map.of("error", "Too many failed login attempts. Try again in " + retryAfter + " second(s)."));
         }

         AppUser user = dataStore.findUserByEmail(request.email());
         if (user == null || !dataStore.verifyPassword(user, request.password())) {
             rateLimiter.recordFailure(ip);
             writeAuditLog(stringValue(request.email()), "Unknown", "login_failed", "Failed sign-in attempt for clinic account.", ip);
             saveLoginAudit(request.email(), ip, userAgent, false, "Invalid credentials", null, null);
             return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("error", "Invalid credentials"));
         }
         if (!isEqualIgnoreCase(user.getStatus(), "active")) {
             rateLimiter.recordFailure(ip);
             writeAuditLog(user.getName(), user.getRole(), "login_blocked", "Blocked sign-in because account is inactive.", ip);
             saveLoginAudit(request.email(), ip, userAgent, false, "Account inactive", user.getUserId(), user.getRole());
             return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of("error", "Account is inactive"));
         }

        rateLimiter.clearFailures(ip);
        user.setLastLogin(LocalDateTime.now().toString());
        dataStore.updateUser(user);
        writeAuditLog(user.getName(), user.getRole(), "login", "User signed into the clinic system.", ip);
        saveLoginAudit(request.email(), ip, userAgent, true, null, user.getUserId(), user.getRole());
        String accessToken = jwtUtil.generateToken(
                user.getUserId(),
                user.getRole(),
                user.getDepartment(),
                user.getName(),
                user.getEmail()
        );
        String rawRefreshToken = refreshTokenService.createToken(user.getUserId(), user.getEmail());
        LocalDateTime expiresAt = LocalDateTime.now().plusSeconds(jwtUtil.getExpirationTime() / 1000);
        return ResponseEntity.ok(new LoginResponse(
                true,
                accessToken,
                "Bearer",
                jwtUtil.getExpirationTime() / 1000,
                expiresAt.toString(),
                rawRefreshToken,
                new LoginResponse.UserSummary(
                        user.getId(),
                        user.getUserId(),
                        user.getName(),
                        user.getEmail(),
                        user.getRole(),
                        user.getDepartment(),
                        stringValue(user.getStaffId()),
                        stringValue(user.getManNumber()),
                        stringValue(user.getStatus()),
                        Boolean.TRUE.equals(user.getForcePasswordChange()),
                        parseUserPermissions(user)
                )
        ));
    }

    private Map<String, Object> toPatientResponse(Patient patient) {
        Map<String, Object> response = new LinkedHashMap<>();
        response.put("id", patient.getId());
        response.put("patient_id", patient.getPatientId());
        response.put("clinic_number", stringValue(patient.getClinicNumber()));
        response.put("patient_type", stringValue(patient.getPatientType()));
        response.put("name", patient.getName());
        response.put("age", patient.getAge());
        response.put("gender", patient.getGender());
        response.put("dob", patient.getDob());
        response.put("phone", patient.getPhone());
        response.put("email", patient.getEmail());
        response.put("address", patient.getAddress());
        response.put("blood_group", patient.getBloodGroup());
        response.put("student_id", patient.getStudentId());
        response.put("man_number", stringValue(patient.getManNumber()));
        response.put("program", patient.getProgram());
        response.put("school", patient.getSchool());
        response.put("year", patient.getYear());
        response.put("hostel", patient.getHostel());
        response.put("emergency_contact", patient.getEmergencyContact());
        response.put("emergency_phone", patient.getEmergencyPhone());
        response.put("emergency_relation", patient.getEmergencyRelation());
        response.put("allergies", patient.getAllergies());
        response.put("conditions", patient.getConditions());
        response.put("insurance", patient.getInsurance());
        response.put("status", patient.getStatus());
        Map<String, Object> medicalStatus = buildMedicalStatusResponse(
                patient,
                firstNonBlank(patient.getStudentId(), patient.getManNumber(), patient.getPatientId()).toString(),
                isStudentPatientType(patient.getPatientType()) ? "student" : "patient",
                isStudentPatientType(patient.getPatientType()),
                isStudentPatientType(patient.getPatientType()) ? "pending" : "exempted"
        );
        response.put("requires_medical_exam", medicalStatus.get("requires_medical_exam"));
        response.put("medical_exam_status", medicalStatus.get("medical_exam_status"));
        response.put("medical_certificate_status", medicalStatus.get("certificate_status"));
        response.put("fitness_status", medicalStatus.get("fitness_status"));
        response.put("last_exam_date", medicalStatus.get("last_exam_date"));
        response.put("last_certificate_date", medicalStatus.get("last_certificate_date"));
        return response;
    }

    private Map<String, Object> toStaffResponse(StaffMember staffMember) {
        return Map.of("id", staffMember.getId(), "staff_id", staffMember.getStaffId(), "man_number", stringValue(staffMember.getManNumber()), "name", staffMember.getName(), "role", staffMember.getRole(), "department", staffMember.getDepartment(), "phone", staffMember.getPhone(), "email", stringValue(staffMember.getEmail()), "specialization", staffMember.getSpecialization(), "status", staffMember.getStatus());
    }

    private Map<String, Object> toDepartmentResponse(Department department) {
        return Map.of("id", department.getId(), "code", department.getCode(), "name", department.getName(), "head", department.getHead(), "doctors", department.getDoctors(), "nurses", department.getNurses(), "beds", department.getBeds(), "location", department.getLocation(), "phone", department.getPhone(), "status", department.getStatus());
    }

    private Map<String, Object> toAppointmentResponse(Appointment appointment) {
        Map<String, Object> response = new LinkedHashMap<>();
        response.put("id", appointment.getId());
        response.put("appointment_id", appointment.getAppointmentId());
        response.put("patient_id", appointment.getPatientId());
        response.put("patient_name", appointment.getPatientName());
        response.put("doctor_id", stringValue(appointment.getDoctorId()));
        response.put("doctor_name", appointment.getDoctorName());
        response.put("department", appointment.getDepartment());
        response.put("date", appointment.getDate());
        response.put("time", appointment.getTime());
        response.put("type", stringValue(appointment.getType()));
        response.put("status", appointment.getStatus());
        response.put("notes", stringValue(appointment.getNotes()));
        return response;
    }

    private Map<String, Object> toPrescriptionResponse(Prescription prescription) {
        Map<String, Object> response = new LinkedHashMap<>();
        response.put("id", prescription.getId());
        response.put("rx_id", prescription.getRxId());
        response.put("patient_id", prescription.getPatientId());
        response.put("patient_name", prescription.getPatientName());
        response.put("patient_id_num", stringValue(prescription.getPatientIdNum()));
        response.put("doctor", prescription.getDoctor());
        response.put("date", prescription.getDate());
        response.put("items", prescription.getItems());
        response.put("drug_name", stringValue(prescription.getDrugName()));
        response.put("quantity", defaultInt(prescription.getQuantity()));
        response.put("dosage", stringValue(prescription.getDosage()));
        response.put("duration", stringValue(prescription.getDuration()));
        response.put("instructions", stringValue(prescription.getInstructions()));
        response.put("medication_class", stringValue(prescription.getMedicationClass()));
        response.put("program", stringValue(prescription.getProgram()));
        response.put("status", prescription.getStatus());
        response.put("dispensed_by", stringValue(prescription.getDispensedBy()));
        response.put("dispensed_at", prescription.getDispensedAt() != null ? prescription.getDispensedAt().toString() : null);
        response.put("pharmacist_notes", stringValue(prescription.getPharmacistNotes()));

        List<Map<String, Object>> drugItems = prescriptionItemRepository
                .findByPrescriptionId(prescription.getId())
                .stream()
                .map(pi -> {
                    Map<String, Object> m = new LinkedHashMap<>();
                    m.put("id", pi.getId());
                    m.put("drug_name", stringValue(pi.getDrugName()));
                    m.put("quantity", pi.getQuantity() == null ? 1 : pi.getQuantity());
                    m.put("dosage", stringValue(pi.getDosage()));
                    m.put("duration", stringValue(pi.getDuration()));
                    m.put("instructions", stringValue(pi.getInstructions()));
                    m.put("medication_class", stringValue(pi.getMedicationClass()));
                    return m;
                })
                .toList();
        response.put("drug_items", drugItems);

        return response;
    }

    private Map<String, Object> toAdmissionResponse(Admission admission) {
        Map<String, Object> response = new LinkedHashMap<>();
        response.put("id", admission.getId());
        response.put("admission_id", admission.getAdmissionId());
        response.put("patient_id", admission.getPatientId());
        response.put("patient_name", admission.getPatientName());
        response.put("ward", admission.getWard());
        response.put("bed", admission.getBed());
        response.put("doctor", admission.getDoctor());
        response.put("admitted_on", admission.getAdmittedOn());
        response.put("diagnosis", admission.getDiagnosis());
        response.put("status", admission.getStatus());
        response.put("discharge_type", stringValue(admission.getDischargeType()));
        response.put("discharge_summary", stringValue(admission.getDischargeSummary()));
        response.put("discharged_on", stringValue(admission.getDischargedOn()));
        response.put("discharged_by", stringValue(admission.getDischargedBy()));
        return response;
    }

    private Map<String, Object> toLabTestResponse(LabTest test) {
        Map<String, Object> response = new LinkedHashMap<>();
        response.put("id", test.getId());
        response.put("test_id", test.getTestId());
        response.put("patient_id", test.getPatientId());
        response.put("patient_name", test.getPatientName());
        response.put("test", test.getTest());
        response.put("category", stringValue(test.getCategory()));
        response.put("section", stringValue(test.getSection()));
        response.put("sample_type", stringValue(test.getSampleType()));
        response.put("clinical_notes", stringValue(test.getClinicalNotes()));
        response.put("requested_by", test.getRequestedBy());
        response.put("date", test.getDate());
        response.put("status", test.getStatus());
        response.put("results", stringValue(test.getResults()));
        response.put("interpretation", stringValue(test.getInterpretation()));
        response.put("reported_by", stringValue(test.getReportedBy()));
        response.put("completed_at", stringValue(test.getCompletedAt()));
        response.put("reference_range", stringValue(test.getReferenceRange()));
        response.put("abnormal_flag", stringValue(test.getAbnormalFlag()));
        response.put("specimen_collected_at", stringValue(test.getSpecimenCollectedAt()));
        response.put("approved_by", stringValue(test.getApprovedBy()));
        response.put("approved_at", stringValue(test.getApprovedAt()));
        return response;
    }

    private Map<String, Object> toTariffResponse(ServiceTariff tariff) {
        Map<String, Object> response = new LinkedHashMap<>();
        response.put("id", tariff.getId());
        response.put("tariff_code", tariff.getTariffCode());
        response.put("department", tariff.getDepartment());
        response.put("category", tariff.getCategory());
        response.put("service_name", tariff.getServiceName());
        response.put("unit_label", tariff.getUnitLabel());
        response.put("price", tariff.getPrice());
        response.put("status", tariff.getStatus());
        return response;
    }

    private Map<String, Object> toBillingResponse(BillingInvoice invoice) {
        Map<String, Object> response = new LinkedHashMap<>();
        response.put("id", invoice.getId());
        response.put("invoice_id", invoice.getInvoiceId());
        response.put("patient_id", invoice.getPatientId());
        response.put("patient_name", invoice.getPatientName());
        response.put("items", invoice.getItems());
        response.put("line_items", parseBillingLineItems(invoice.getLineItemsJson()));
        response.put("subtotal", invoice.getSubtotal());
        response.put("tax", invoice.getTax());
        response.put("total", invoice.getTotal());
        response.put("status", invoice.getStatus());
        response.put("due_date", stringValue(invoice.getDueDate()));
        response.put("paid_date", stringValue(invoice.getPaidDate()));
        response.put("payment_method", stringValue(invoice.getPaymentMethod()));
        return response;
    }

    private List<Map<String, Object>> normalizeBillingLineItems(List<BillingLineItemRequest> requestItems) {
        if (requestItems == null || requestItems.isEmpty()) {
            return List.of();
        }

        List<Map<String, Object>> normalized = new ArrayList<>();
        for (BillingLineItemRequest item : requestItems) {
            if (item == null) {
                continue;
            }

            ServiceTariff tariff = dataStore.getServiceTariffByCode(item.tariffCode());
            String serviceName = tariff != null ? tariff.getServiceName() : stringValue(item.serviceName());
            if (isBlank(serviceName)) {
                continue;
            }

            int quantity = Math.max(1, defaultInt(item.quantity()));
            String department = tariff != null ? tariff.getDepartment() : stringValue(item.department());
            String tariffCode = tariff != null ? tariff.getTariffCode() : stringValue(item.tariffCode());
            double unitPrice = tariff != null ? value(tariff.getPrice()) : value(item.unitPrice());
            double lineTotal = quantity * unitPrice;

            Map<String, Object> response = new LinkedHashMap<>();
            response.put("tariff_code", tariffCode);
            response.put("service_name", serviceName);
            response.put("department", department);
            response.put("quantity", quantity);
            response.put("unit_price", unitPrice);
            response.put("line_total", lineTotal);
            normalized.add(response);
        }
        return normalized;
    }

    private List<Map<String, Object>> parseBillingLineItems(String lineItemsJson) {
        if (isBlank(lineItemsJson)) {
            return List.of();
        }
        try {
            return objectMapper.readValue(lineItemsJson, new TypeReference<List<Map<String, Object>>>() {});
        } catch (Exception ex) {
            return List.of();
        }
    }

    private String writeJson(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (Exception ex) {
            return "[]";
        }
    }

    private Map<String, Object> toInventoryResponse(InventoryRecord record) {
        return Map.of("id", record.getId(), "item_code", record.getItemCode(), "name", record.getName(), "category", record.getCategory(), "quantity", record.getQuantity(), "unit", record.getUnit(), "min_stock", record.getMinStock(), "location", record.getLocation(), "last_restocked", record.getLastRestocked(), "status", record.getStatus());
    }

    private Map<String, Object> toSupplierResponse(Supplier supplier) {
        return Map.of("id", supplier.getId(), "supplier_id", supplier.getSupplierId(), "name", supplier.getName(), "contact", supplier.getContact(), "phone", supplier.getPhone(), "email", supplier.getEmail(), "items", supplier.getItems(), "last_order", supplier.getLastOrder(), "status", supplier.getStatus());
    }

    private Map<String, Object> toUserResponse(AppUser user) {
        Map<String, Object> response = new LinkedHashMap<>();
        response.put("id", user.getId());
        response.put("user_id", user.getUserId());
        response.put("name", user.getName());
        response.put("email", user.getEmail());
        response.put("role", user.getRole());
        response.put("department", user.getDepartment());
        response.put("staff_id", stringValue(user.getStaffId()));
        response.put("man_number", stringValue(user.getManNumber()));
        response.put("status", user.getStatus());
        response.put("last_login", stringValue(user.getLastLogin()));
        response.put("force_password_change", Boolean.TRUE.equals(user.getForcePasswordChange()));
        response.put("permissions", parseUserPermissions(user));
        response.put("password_changed_at", stringValue(user.getPasswordChangedAt()));
        response.put("password_version", user.getPasswordVersion() == null ? 1 : user.getPasswordVersion());
        return response;
    }

    private Map<String, Object> toSettingsResponse(SystemSettings settings) {
        Map<String, Object> response = new LinkedHashMap<>();
        response.put("hospital_name", settings.getHospitalName());
        response.put("contact_phone", settings.getContactPhone());
        response.put("address", settings.getAddress());
        response.put("email_notifications", settings.isEmailNotifications());
        response.put("sms_notifications", settings.isSmsNotifications());
        response.put("low_stock_alerts", settings.isLowStockAlerts());
        response.put("two_factor_auth", settings.isTwoFactorAuth());
        response.put("audit_logging", settings.isAuditLogging());
        response.put("auto_logout", settings.isAutoLogout());
        response.put("backup_enabled", Boolean.TRUE.equals(settings.getBackupEnabled()));
        response.put("backup_frequency", stringValue(settings.getBackupFrequency()));
        response.put("backup_location", stringValue(settings.getBackupLocation()));
        response.put("last_backup_at", stringValue(settings.getLastBackupAt()));
        response.put("demo_data_cleared", Boolean.TRUE.equals(settings.getDemoDataCleared()));
        return response;
    }

    private Map<String, Object> toEncounterResponse(EncounterRecord record) {
        Map<String, Object> response = new LinkedHashMap<>();
        response.put("id", record.getId());
        response.put("encounter_id", record.getEncounterId());
        response.put("patient_id", record.getPatientId());
        response.put("patient_name", record.getPatientName());
        response.put("patient_type", stringValue(record.getPatientType()));
        response.put("current_stage", stringValue(record.getCurrentStage()));
        response.put("payment_status", stringValue(record.getPaymentStatus()));
        response.put("checkout_eligible", record.isCheckoutEligible());
        response.put("checked_out", record.isCheckedOut());
        response.put("created_at", stringValue(record.getCreatedAt()));
        response.put("updated_at", stringValue(record.getUpdatedAt()));
        response.put("created_by", stringValue(record.getCreatedBy()));
        response.put("checkout_time", stringValue(record.getCheckoutTime()));
        response.put("pending_actions", csvToList(record.getPendingActions()));
        response.put("completed_actions", csvToList(record.getCompletedActions()));
        response.put("notes", stringValue(record.getNotes()));
        response.put("history", historyToList(record.getStageHistory()));
        return response;
    }

    private Map<String, Object> toDrugResponse(Drug drug) {
        Map<String, Object> response = new LinkedHashMap<>();
        response.put("id", drug.getId());
        response.put("drug_id", drug.getDrugId());
        response.put("name", drug.getName());
        response.put("category", drug.getCategory());
        response.put("drug_type", stringValue(drug.getDrugType()));
        response.put("batch_number", stringValue(drug.getBatchNumber()));
        response.put("stock", drug.getStock());
        response.put("reorder_level", drug.getReorderLevel());
        response.put("unit", drug.getUnit());
        response.put("expiry", drug.getExpiry());
        response.put("storage_location", stringValue(drug.getStorageLocation()));
        response.put("status", drug.getStatus());
        return response;
    }

    private Map<String, Object> toImagingResponse(ImagingRequest request) {
        Map<String, Object> response = new LinkedHashMap<>();
        response.put("id", request.getId());
        response.put("request_id", request.getRequestId());
        response.put("patient_id", request.getPatientId());
        response.put("patient_name", request.getPatientName());
        response.put("type", request.getType());
        response.put("body_part", request.getBodyPart());
        response.put("requested_by", request.getRequestedBy());
        response.put("radiologist", stringValue(request.getRadiologist()));
        response.put("request_date", request.getRequestDate());
        response.put("findings", stringValue(request.getFindings()));
        response.put("status", request.getStatus());
        return response;
    }

    private Map<String, Object> toInsuranceClaimResponse(InsuranceClaim claim) {
        return Map.of("id", claim.getId(), "claim_id", claim.getClaimId(), "patient", claim.getPatient(), "insurer", claim.getInsurer(), "service", claim.getService(), "amount", claim.getAmount(), "submitted", claim.getSubmitted(), "status", claim.getStatus());
    }

    private Map<String, Object> toReferralResponse(ReferralRecord record) {
        Map<String, Object> response = new LinkedHashMap<>();
        response.put("id", record.getId());
        response.put("referral_id", record.getReferralId());
        response.put("patient_id", record.getPatientId());
        response.put("patient_name", record.getPatientName());
        response.put("from_dept", record.getFromDept());
        response.put("to_dept", record.getToDept());
        response.put("referred_by", record.getReferredBy());
        response.put("reason", record.getReason());
        response.put("urgency", record.getUrgency());
        response.put("date", record.getDate());
        response.put("status", record.getStatus());
        response.put("notes", stringValue(record.getNotes()));
        return response;
    }

    private Map<String, Object> toTriageResponse(TriageRecord record) {
        Map<String, Object> response = new LinkedHashMap<>();
        response.put("id", record.getId());
        response.put("patient_id", record.getPatientId());
        response.put("patient_name", record.getPatientName());
        response.put("level", record.getLevel());
        response.put("chief_complaint", record.getChiefComplaint());
        response.put("vital_signs", stringValue(record.getVitalSigns()));
        response.put("blood_pressure", stringValue(record.getBloodPressure()));
        response.put("temperature", record.getTemperature());
        response.put("pulse_rate", record.getPulseRate());
        response.put("respiratory_rate", record.getRespiratoryRate());
        response.put("oxygen_saturation", record.getOxygenSaturation());
        response.put("weight_kg", record.getWeightKg());
        response.put("height_cm", record.getHeightCm());
        response.put("bmi", record.getBmi());
        response.put("random_blood_sugar", record.getRandomBloodSugar());
        response.put("pain_score", record.getPainScore());
        response.put("consciousness_level", stringValue(record.getConsciousnessLevel()));
        response.put("notes", stringValue(record.getNotes()));
        response.put("nurse_name", record.getNurseName());
        response.put("arrival_time", record.getArrivalTime());
        response.put("status", record.getStatus());
        return response;
    }

    private Map<String, Object> toQueueResponse(QueueTicket record) {
        return Map.of("id", record.getId(), "ticket_no", record.getTicketNo(), "patient_id", record.getPatientId(), "patient_name", record.getPatientName(), "department", record.getDepartment(), "priority", record.getPriority(), "check_in_time", record.getCheckInTime(), "wait_time", record.getWaitTime(), "status", record.getStatus());
    }

    private Map<String, Object> toEmergencyResponse(EmergencyCase record) {
        Map<String, Object> response = new LinkedHashMap<>();
        response.put("id", record.getId());
        response.put("case_id", record.getCaseId());
        response.put("patient_name", record.getPatientName());
        response.put("age", record.getAge());
        response.put("gender", record.getGender());
        response.put("severity", record.getSeverity());
        response.put("chief_complaint", record.getChiefComplaint());
        response.put("arrival_mode", record.getArrivalMode());
        response.put("arrival_time", record.getArrivalTime());
        response.put("attending_doctor", record.getAttendingDoctor());
        response.put("nurse_on_duty", record.getNurseOnDuty());
        response.put("vitals", record.getVitals());
        response.put("status", record.getStatus());
        return response;
    }

    private Map<String, Object> toBloodUnitResponse(BloodUnit record) {
        return Map.of("id", record.getId(), "unit_id", record.getUnitId(), "blood_type", record.getBloodType(), "quantity", record.getQuantity(), "status", record.getStatus(), "expiry_date", record.getExpiryDate(), "donor_name", record.getDonorName(), "collection_date", record.getCollectionDate());
    }

    private Map<String, Object> toDonationResponse(Donation record) {
        return Map.of("id", record.getId(), "donor_name", record.getDonorName(), "blood_type", record.getBloodType(), "units", record.getUnits(), "date", record.getDate(), "status", record.getStatus());
    }

    private Map<String, Object> toNotificationResponse(NotificationItem record) {
        return Map.of("id", record.getId(), "type", record.getType(), "title", record.getTitle(), "message", record.getMessage(), "time", record.getTime(), "read", record.isRead());
    }

    private Map<String, Object> toAuditLogResponse(AuditLogEntry record) {
        return Map.of("id", record.getId(), "timestamp", record.getTimestamp(), "user", record.getUser(), "role", record.getRole(), "action", record.getAction(), "description", record.getDescription(), "ip_address", record.getIpAddress());
    }

    private Map<String, Object> toAttendanceResponse(AttendanceRecord record) {
        return Map.of("id", record.getId(), "staff_id", record.getStaffId(), "name", record.getName(), "role", record.getRole(), "department", record.getDepartment(), "shift", record.getShift(), "check_in", stringValue(record.getCheckIn()), "check_out", stringValue(record.getCheckOut()), "status", record.getStatus(), "date", record.getDate());
    }

    private Map<String, Object> toStaffScheduleResponse(StaffSchedule record) {
        Map<String, Object> response = new LinkedHashMap<>();
        response.put("id", record.getId());
        response.put("schedule_id", record.getScheduleId());
        response.put("staff_id", record.getStaffId());
        response.put("name", record.getName());
        response.put("role", record.getRole());
        response.put("department", record.getDepartment());
        response.put("day_of_week", record.getDayOfWeek());
        response.put("week_of", record.getWeekOf());
        response.put("shift_name", record.getShiftName());
        response.put("start_time", record.getStartTime());
        response.put("end_time", record.getEndTime());
        response.put("location", record.getLocation());
        response.put("status", record.getStatus());
        return response;
    }

    private Map<String, Object> toClinicalFormResponse(ClinicalFormRecord record) {
        Map<String, Object> response = new LinkedHashMap<>();
        response.put("id", record.getId());
        response.put("form_id", record.getFormId());
        response.put("form_type", record.getFormType());
        response.put("title", record.getTitle());
        response.put("patient_id", record.getPatientId());
        response.put("patient_name", record.getPatientName());
        response.put("department", record.getDepartment());
        response.put("encounter_id", stringValue(record.getEncounterId()));
        response.put("status", record.getStatus());
        response.put("created_by", record.getCreatedBy());
        response.put("created_at", record.getCreatedAt());
        response.put("updated_at", record.getUpdatedAt());
        response.put("payload_json", stringValue(record.getPayloadJson()));
        return response;
    }

    private List<Map<String, Object>> buildWardResponses() {
        return dataStore.getWards().stream()
                .filter(Objects::nonNull)
                .map(ward -> toWardResponse(ward, countOccupiedBedsForWard(ward.getName())))
                .toList();
    }

    private int countOccupiedBedsForWard(String wardName) {
        return (int) dataStore.getAdmissions().stream()
                .filter(Objects::nonNull)
                .filter(admission -> !isEqualIgnoreCase(admission.getStatus(), "discharged"))
                .filter(admission -> isEqualIgnoreCase(admission.getWard(), wardName))
                .count();
    }

    private List<Admission> getActiveAdmissionsForWard(String wardName) {
        return dataStore.getAdmissions().stream()
                .filter(Objects::nonNull)
                .filter(admission -> !isEqualIgnoreCase(admission.getStatus(), "discharged"))
                .filter(admission -> isEqualIgnoreCase(admission.getWard(), wardName))
                .toList();
    }

    private Integer extractBedNumber(String bedLabel) {
        String digits = stringValue(bedLabel).replaceAll("[^0-9]", "");
        if (isBlank(digits)) {
            return null;
        }
        try {
            return Integer.parseInt(digits);
        } catch (NumberFormatException ex) {
            return null;
        }
    }

    private WardStatus requireWard(Long id) {
        WardStatus ward = dataStore.getWard(id);
        if (ward == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Ward not found");
        }
        return ward;
    }

    private Drug requireDrug(Long id) {
        Drug drug = dataStore.getDrug(id);
        if (drug == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Drug not found");
        }
        return drug;
    }

    private BloodUnit requireBloodUnit(Long id) {
        BloodUnit unit = dataStore.getBloodUnits().stream()
                .filter(Objects::nonNull)
                .filter(entry -> Objects.equals(entry.getId(), id))
                .findFirst()
                .orElse(null);
        if (unit == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Blood stock record not found");
        }
        return unit;
    }

    private WardStatus updateWardBedCapacity(Long id, int requestedTotalBeds, String auditActionPrefix) {
        WardStatus ward = requireWard(id);
        int occupiedBeds = countOccupiedBedsForWard(ward.getName());
        if (requestedTotalBeds < occupiedBeds) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Total beds cannot be less than the number of occupied beds");
        }

        ward.setTotalBeds(requestedTotalBeds);
        ward.setOccupied(occupiedBeds);
        ward.setAvailable(Math.max(requestedTotalBeds - occupiedBeds, 0));
        ward = dataStore.updateWard(ward);
        writeAuditLog("System", "Admin", "update", auditActionPrefix + " " + ward.getName() + ". Total beds: " + requestedTotalBeds + ".", "127.0.0.1");
        return ward;
    }

    private void applyDrugStatus(Drug drug) {
        int stock = defaultInt(drug.getStock());
        int reorderLevel = defaultInt(drug.getReorderLevel());
        drug.setStatus(stock <= reorderLevel ? "critical" : "available");
    }

    private String resolveBloodUnitStatus(int quantity) {
        return quantity <= 3 ? "critical" : "available";
    }

    private Drug findDrugForPrescription(Prescription prescription) {
        String itemText = stringValue(firstNonBlank(prescription.getDrugName(), prescription.getItems())).trim();
        if (isBlank(itemText)) {
            return null;
        }
        String candidateName = itemText.contains(" - ") ? itemText.substring(0, itemText.indexOf(" - ")).trim() : itemText;
        return dataStore.getDrugs().stream()
                .filter(Objects::nonNull)
                .filter(drug -> isEqualIgnoreCase(drug.getName(), candidateName))
                .findFirst()
                .orElse(null);
    }

    private List<Map<String, Object>> buildDrugUsageReport() {
        Map<String, Long> counts = new java.util.LinkedHashMap<>();
        for (Prescription rx : dataStore.getPrescriptions()) {
            if (!isEqualIgnoreCase(rx.getStatus(), "dispensed")) continue;
            List<PrescriptionItem> lineItems = prescriptionItemRepository.findByPrescriptionId(rx.getId());
            if (!lineItems.isEmpty()) {
                for (PrescriptionItem item : lineItems) {
                    String name = stringValue(item.getDrugName());
                    if (!isBlank(name)) {
                        String key = name.split(" ")[0];
                        counts.merge(key, 1L, Long::sum);
                    }
                }
            } else {
                String name = stringValue(firstNonBlank(rx.getDrugName(), rx.getItems()));
                if (!isBlank(name)) {
                    String key = name.split(" ")[0];
                    counts.merge(key, 1L, Long::sum);
                }
            }
        }
        return counts.entrySet().stream()
                .sorted(Map.Entry.<String, Long>comparingByValue().reversed())
                .limit(8)
                .map(e -> {
                    Map<String, Object> m = new LinkedHashMap<>();
                    m.put("drug", e.getKey());
                    m.put("dispensed", e.getValue());
                    return m;
                })
                .collect(Collectors.toList());
    }

    private Drug findDrugByName(String drugName) {
        if (isBlank(drugName)) return null;
        String base = drugName.contains(" ") ? drugName.split(" ")[0].trim() : drugName.trim();
        return dataStore.getDrugs().stream()
                .filter(Objects::nonNull)
                .filter(drug -> containsIgnoreCase(drug.getName(), base) || isEqualIgnoreCase(drug.getName(), drugName))
                .findFirst()
                .orElse(null);
    }

    private Map<String, Object> toWardResponse(WardStatus record, int occupiedBeds) {
        int totalBeds = defaultInt(record.getTotalBeds());
        int availableBeds = Math.max(totalBeds - occupiedBeds, 0);
        List<Admission> activeAdmissions = getActiveAdmissionsForWard(record.getName());
        Map<Integer, Admission> bedAssignments = activeAdmissions.stream()
                .map(admission -> Map.entry(extractBedNumber(admission.getBed()), admission))
                .filter(entry -> entry.getKey() != null)
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue, (first, second) -> first));

        List<Map<String, Object>> bedBoard = IntStream.rangeClosed(1, Math.max(totalBeds, 0))
                .mapToObj(bedNumber -> {
                    Admission assignedAdmission = bedAssignments.get(bedNumber);
                    Map<String, Object> bedEntry = new LinkedHashMap<>();
                    bedEntry.put("bed_number", bedNumber);
                    bedEntry.put("bed_label", "Bed " + bedNumber);
                    bedEntry.put("status", assignedAdmission == null ? "available" : "occupied");
                    bedEntry.put("patient_name", assignedAdmission != null ? stringValue(assignedAdmission.getPatientName()) : "");
                    bedEntry.put("patient_id", assignedAdmission != null ? stringValue(assignedAdmission.getPatientId()) : "");
                    bedEntry.put("admission_id", assignedAdmission != null ? stringValue(assignedAdmission.getAdmissionId()) : "");
                    bedEntry.put("doctor", assignedAdmission != null ? stringValue(assignedAdmission.getDoctor()) : "");
                    bedEntry.put("diagnosis", assignedAdmission != null ? stringValue(assignedAdmission.getDiagnosis()) : "");
                    return bedEntry;
                })
                .toList();

        return Map.of(
                "id", record.getId(),
                "name", record.getName(),
                "total_beds", totalBeds,
                "occupied", occupiedBeds,
                "available", availableBeds,
                "bed_board", bedBoard
        );
    }

    private Map<String, Object> toDashboardPatientResponse(Patient patient) {
        Map<String, Object> response = new LinkedHashMap<>();
        response.put("id", patient.getId());
        response.put("name", String.valueOf(firstNonBlank(patient.getName(), "Unknown Patient")));
        response.put("patientId", String.valueOf(firstNonBlank(patient.getPatientId(), "N/A")));
        response.put("department", inferDepartment(patient));
        response.put("status", String.valueOf(firstNonBlank(patient.getStatus(), "registered")));
        response.put("date", LocalDate.now().toString());
        return response;
    }

    private String inferDepartment(Patient patient) {
        String patientId = stringValue(patient.getPatientId());
        if (patientId.isBlank()) {
            return "General Medicine";
        }
        return dataStore.getEncounterRecords().stream()
                .filter(encounter -> isEqualIgnoreCase(encounter.getPatientId(), patientId))
                .findFirst()
                .map(encounter -> humanizeStage(encounter.getCurrentStage()))
                .orElseGet(() -> dataStore.getClinicalForms().stream()
                        .filter(form -> isEqualIgnoreCase(form.getPatientId(), patientId))
                        .findFirst()
                        .map(form -> String.valueOf(firstNonBlank(form.getDepartment(), "General Medicine")))
                        .orElse("General Medicine"));
    }

    private List<Map<String, Object>> buildWeeklyPatientFlow(List<EncounterRecord> encounters) {
        LocalDate today = LocalDate.now();
        return IntStream.rangeClosed(0, 6)
                .mapToObj(offset -> today.minusDays(6L - offset))
                .map(date -> {
                    Map<String, Object> entry = new LinkedHashMap<>();
                    entry.put("day", date.getDayOfWeek().getDisplayName(TextStyle.SHORT, Locale.ENGLISH));
                    entry.put("date", date.toString());
                    entry.put("patients", encounters.stream().filter(encounter -> isSameDay(encounter.getCreatedAt(), date)).count());
                    entry.put("checkedOut", encounters.stream().filter(encounter -> isSameDay(encounter.getCheckoutTime(), date)).count());
                    return entry;
                })
                .toList();
    }

    private List<Map<String, Object>> buildSectionDistribution(
            List<EncounterRecord> encounters,
            List<LabTest> labTests,
            List<BillingInvoice> billing,
            List<Admission> admissions,
            List<EmergencyCase> emergencyCases) {
        List<Map<String, Object>> sections = new ArrayList<>();
        sections.add(sectionEntry("Reception", encounters.stream().filter(encounter -> isEqualIgnoreCase(encounter.getCurrentStage(), "RECEPTION")).count()));
        sections.add(sectionEntry("Triage", encounters.stream().filter(encounter -> isEqualIgnoreCase(encounter.getCurrentStage(), "TRIAGE")).count()));
        sections.add(sectionEntry("Consultation", encounters.stream().filter(encounter -> isEqualIgnoreCase(encounter.getCurrentStage(), "CONSULTATION")).count()));
        sections.add(sectionEntry("Laboratory", labTests.stream().filter(test -> !isEqualIgnoreCase(test.getStatus(), "completed")).count()));
        sections.add(sectionEntry("Pharmacy", encounters.stream().filter(encounter -> isEqualIgnoreCase(encounter.getCurrentStage(), "PHARMACY")).count()));
        sections.add(sectionEntry("Billing", billing.stream().filter(invoice -> !isEqualIgnoreCase(invoice.getStatus(), "completed")).count()));
        sections.add(sectionEntry("Wards", admissions.stream().filter(admission -> !isEqualIgnoreCase(admission.getStatus(), "discharged")).count()));
        sections.add(sectionEntry("Emergency", emergencyCases.stream().filter(caseItem -> !isEqualIgnoreCase(caseItem.getStatus(), "resolved")).count()));
        return sections;
    }

    private Map<String, Object> sectionEntry(String name, long value) {
        Map<String, Object> entry = new LinkedHashMap<>();
        entry.put("name", name);
        entry.put("value", value);
        return entry;
    }

    private List<Map<String, Object>> buildSectionSummaries(
            List<EncounterRecord> encounters,
            List<TriageRecord> triageRecords,
            List<LabTest> labTests,
            List<BillingInvoice> billing,
            List<Admission> admissions,
            List<ClinicalFormRecord> clinicalForms,
            List<EmergencyCase> emergencyCases,
            List<Drug> drugs) {
        List<Map<String, Object>> summaries = new ArrayList<>();
        summaries.add(buildSectionSummary("reception", "Reception & Walk-In", encounters.stream().filter(encounter -> isEqualIgnoreCase(encounter.getCurrentStage(), "RECEPTION")).count(), encounters.stream().filter(encounter -> isSameDay(encounter.getCreatedAt(), LocalDate.now())).count(), clinicalForms.stream().filter(form -> isEqualIgnoreCase(form.getDepartment(), "Reception")).count(), "Patient registration, opening encounters, and routing walk-ins"));
        summaries.add(buildSectionSummary("triage", "Triage", triageRecords.stream().filter(record -> !isEqualIgnoreCase(record.getStatus(), "transferred")).count(), triageRecords.stream().filter(record -> isEqualIgnoreCase(record.getLevel(), "red") || isEqualIgnoreCase(record.getLevel(), "orange")).count(), clinicalForms.stream().filter(form -> isEqualIgnoreCase(form.getDepartment(), "Triage")).count(), "Detailed vital signs, risk classification, and immediate prioritization"));
        summaries.add(buildSectionSummary("consultation", "Consultation / OPD", encounters.stream().filter(encounter -> isEqualIgnoreCase(encounter.getCurrentStage(), "CONSULTATION")).count(), encounters.stream().filter(encounter -> !isCheckedOut(encounter) && !hasOutstandingActions(encounter.getPendingActions())).count(), clinicalForms.stream().filter(form -> isEqualIgnoreCase(form.getDepartment(), "Clinical")).count(), "Doctor assessment, diagnosis, management plan, and referrals"));
        summaries.add(buildSectionSummary("laboratory", "Laboratory", labTests.stream().filter(test -> !isEqualIgnoreCase(test.getStatus(), "completed")).count(), labTests.stream().filter(test -> isEqualIgnoreCase(test.getStatus(), "pending")).count(), clinicalForms.stream().filter(form -> containsIgnoreCase(form.getDepartment(), "Laboratory")).count(), "Test requests, specimen logging, results entry, and lab registers"));
        summaries.add(buildSectionSummary("pharmacy", "Pharmacy", encounters.stream().filter(encounter -> isEqualIgnoreCase(encounter.getCurrentStage(), "PHARMACY")).count(), drugs.stream().filter(drug -> isEqualIgnoreCase(drug.getStatus(), "critical")).count(), clinicalForms.stream().filter(form -> containsIgnoreCase(form.getDepartment(), "Pharmacy")).count(), "Dispensing, medicine availability, and issue tracking"));
        summaries.add(buildSectionSummary("billing", "Billing & Accounts", billing.stream().filter(invoice -> !isEqualIgnoreCase(invoice.getStatus(), "completed")).count(), billing.stream().filter(invoice -> isEqualIgnoreCase(invoice.getStatus(), "pending")).count(), clinicalForms.stream().filter(form -> containsIgnoreCase(form.getDepartment(), "Accounts") || containsIgnoreCase(form.getDepartment(), "Billing")).count(), "Fee lookup, invoices, payment clearance, and service totals"));
        summaries.add(buildSectionSummary("medical-records", "Medical Records", clinicalForms.size(), encounters.stream().filter(encounter -> encounter.isCheckedOut()).count(), clinicalForms.stream().filter(form -> containsIgnoreCase(form.getDepartment(), "Records")).count(), "Paper-to-digital forms, patient file continuity, and archive support"));
        summaries.add(buildSectionSummary("mch", "Maternal & Child Health", encounters.stream().filter(encounter -> isEqualIgnoreCase(encounter.getCurrentStage(), "MCH")).count(), clinicalForms.stream().filter(form -> containsIgnoreCase(form.getTitle(), "maternal") || containsIgnoreCase(form.getTitle(), "child")).count(), clinicalForms.stream().filter(form -> containsIgnoreCase(form.getDepartment(), "MCH")).count(), "Antenatal, family planning, under-five, and mother-child services"));
        summaries.add(buildSectionSummary("eye-clinic", "Eye Clinic", clinicalForms.stream().filter(form -> containsIgnoreCase(form.getDepartment(), "Eye Clinic")).count(), clinicalForms.stream().filter(form -> containsIgnoreCase(form.getTitle(), "spectacles")).count(), clinicalForms.stream().filter(form -> containsIgnoreCase(form.getDepartment(), "Eye Clinic")).count(), "Eye assessments, outpatient records, and spectacles prescriptions"));
        summaries.add(buildSectionSummary("inpatient", "Inpatient / Wards", admissions.stream().filter(admission -> !isEqualIgnoreCase(admission.getStatus(), "discharged")).count(), admissions.stream().filter(admission -> isEqualIgnoreCase(admission.getStatus(), "critical")).count(), clinicalForms.stream().filter(form -> containsIgnoreCase(form.getDepartment(), "Inpatient")).count(), "Admissions, sick list certificates, in-patient drug sheets, and bed visibility"));
        summaries.add(buildSectionSummary("emergency", "Emergency", emergencyCases.stream().filter(caseItem -> !isEqualIgnoreCase(caseItem.getStatus(), "resolved")).count(), emergencyCases.stream().filter(caseItem -> isEqualIgnoreCase(caseItem.getSeverity(), "critical")).count(), clinicalForms.stream().filter(form -> containsIgnoreCase(form.getDepartment(), "Emergency")).count(), "Immediate stabilization, urgent escalation, and emergency coordination"));
        return summaries;
    }

    private Map<String, Object> buildSectionSummary(String id, String name, long activeCount, long pendingCount, long formCount, String description) {
        Map<String, Object> entry = new LinkedHashMap<>();
        entry.put("id", id);
        entry.put("name", name);
        entry.put("activeCount", activeCount);
        entry.put("pendingCount", pendingCount);
        entry.put("formCount", formCount);
        entry.put("description", description);
        return entry;
    }

    private boolean isSameDay(String timestamp, LocalDate date) {
        if (!hasText(timestamp)) {
            return false;
        }
        try {
            return LocalDate.parse(timestamp.substring(0, 10)).equals(date);
        } catch (Exception ignored) {
            return false;
        }
    }

    private boolean containsIgnoreCase(String value, String pattern) {
        return stringValue(value).toLowerCase(Locale.ROOT).contains(stringValue(pattern).toLowerCase(Locale.ROOT));
    }

    private boolean isCheckedOut(EncounterRecord encounter) {
        return encounter != null && encounter.isCheckedOut();
    }

    private String humanizeStage(String stage) {
        String raw = stringValue(stage).trim();
        if (raw.isBlank()) {
            return "General Medicine";
        }
        return Arrays.stream(raw.split("_"))
                .filter(part -> !part.isBlank())
                .map(part -> part.substring(0, 1).toUpperCase(Locale.ROOT) + part.substring(1).toLowerCase(Locale.ROOT))
                .collect(Collectors.joining(" "));
    }

    private List<Map<String, Object>> buildMonthlyPatientsReport() {
        return List.of("Jan", "Feb", "Mar", "Apr", "May", "Jun", "Jul", "Aug", "Sep", "Oct", "Nov", "Dec").stream()
                .map(month -> {
                    Map<String, Object> entry = new LinkedHashMap<>();
                    entry.put("month", month);
                    entry.put("patients", 250 + month.length() * 20 + dataStore.getPatients().size() * 5);
                    return entry;
                })
                .toList();
    }

    private List<Map<String, Object>> buildRevenueReport() {
        double base = dataStore.getBillingInvoices().stream().mapToDouble(BillingInvoice::getTotal).sum();
        return List.of("Jan", "Feb", "Mar", "Apr", "May", "Jun", "Jul", "Aug", "Sep", "Oct", "Nov", "Dec").stream()
                .map(month -> {
                    Map<String, Object> entry = new LinkedHashMap<>();
                    entry.put("month", month);
                    entry.put("revenue", base + month.length() * 1500);
                    return entry;
                })
                .toList();
    }

    private List<Map<String, Object>> buildWorkflowStageReport(List<EncounterRecord> encounters) {
        List<String> stages = List.of("RECEPTION", "TRIAGE", "CONSULTATION", "LABORATORY", "RADIOLOGY", "PHARMACY", "ACCOUNTS", "MCH", "INPATIENT", "CHECKOUT");
        return stages.stream()
                .map(stage -> {
                    Map<String, Object> entry = new LinkedHashMap<>();
                    entry.put("stage", stage);
                    entry.put("count", encounters.stream().filter(encounter -> stage.equalsIgnoreCase(encounter.getCurrentStage())).count());
                    return entry;
                })
                .toList();
    }

    private List<Map<String, Object>> buildPayerMixReport(List<Patient> patients) {
        List<String> schemes = List.of("NHIMA", "UNZALARU", "UNZA Medical", "Private", "Cash");
        return schemes.stream()
                .map(scheme -> {
                    Map<String, Object> entry = new LinkedHashMap<>();
                    entry.put("scheme", scheme);
                    entry.put("count", patients.stream().filter(patient -> scheme.equalsIgnoreCase(stringValue(patient.getInsurance()))).count());
                    return entry;
                })
                .toList();
    }

    private List<Map<String, Object>> buildClinicalFormTypeReport(List<ClinicalFormRecord> forms) {
        return forms.stream()
                .collect(java.util.stream.Collectors.groupingBy(ClinicalFormRecord::getFormType, LinkedHashMap::new, java.util.stream.Collectors.counting()))
                .entrySet()
                .stream()
                .map(entry -> {
                    Map<String, Object> item = new LinkedHashMap<>();
                    item.put("form_type", entry.getKey());
                    item.put("count", entry.getValue());
                    return item;
                })
                .toList();
    }

    private List<Map<String, Object>> buildBmiDistributionReport(List<ClinicalFormRecord> forms) {
        Map<String, Long> counts = new LinkedHashMap<>();
        counts.put("Underweight", 0L);
        counts.put("Normal", 0L);
        counts.put("Overweight", 0L);
        counts.put("Obese", 0L);

        forms.stream()
                .filter(form -> "student_medical_exam".equalsIgnoreCase(form.getFormType()))
                .map(this::parsePayloadJson)
                .forEach(payload -> {
                    double bmi = parseDoubleValue(payload.get("bmi"));
                    if (bmi <= 0) {
                        return;
                    }
                    String category = bmi < 18.5 ? "Underweight" : bmi < 25 ? "Normal" : bmi < 30 ? "Overweight" : "Obese";
                    counts.put(category, counts.get(category) + 1);
                });

        return counts.entrySet().stream()
                .map(entry -> {
                    Map<String, Object> item = new LinkedHashMap<>();
                    item.put("category", entry.getKey());
                    item.put("count", entry.getValue());
                    return item;
                })
                .toList();
    }

    private List<Map<String, Object>> buildDiagnosisDistributionReport(List<ClinicalFormRecord> forms) {
        return forms.stream()
                .map(this::parsePayloadJson)
                .map(payload -> String.valueOf(firstNonBlank(
                        payload.get("diagnosis"),
                        payload.get("general_comments"),
                        payload.get("clinic_summary")
                )))
                .map(value -> value.contains("\n") ? value.substring(0, value.indexOf('\n')) : value)
                .map(value -> value.contains(",") ? value.substring(0, value.indexOf(',')) : value)
                .map(String::trim)
                .filter(value -> !value.isEmpty())
                .collect(java.util.stream.Collectors.groupingBy(value -> value, LinkedHashMap::new, java.util.stream.Collectors.counting()))
                .entrySet()
                .stream()
                .limit(8)
                .map(entry -> {
                    Map<String, Object> item = new LinkedHashMap<>();
                    item.put("diagnosis", entry.getKey());
                    item.put("count", entry.getValue());
                    return item;
                })
                .toList();
    }

    private List<Map<String, Object>> buildLabTestCategoryReport(List<LabTest> tests) {
        return tests.stream()
                .filter(Objects::nonNull)
                .collect(Collectors.groupingBy(t -> hasText(t.getCategory()) ? t.getCategory().trim() : "Other", LinkedHashMap::new, Collectors.counting()))
                .entrySet().stream()
                .sorted((a, b) -> Long.compare(b.getValue(), a.getValue()))
                .map(e -> { Map<String, Object> m = new LinkedHashMap<>(); m.put("category", e.getKey()); m.put("count", e.getValue()); return m; })
                .toList();
    }

    private List<Map<String, Object>> buildLabTestStatusReport(List<LabTest> tests) {
        long pending = tests.stream().filter(Objects::nonNull).filter(t -> "pending".equalsIgnoreCase(t.getStatus())).count();
        long completed = tests.stream().filter(Objects::nonNull).filter(t -> "completed".equalsIgnoreCase(t.getStatus())).count();
        long other = tests.size() - pending - completed;
        return List.of(
            Map.of("status", "Pending", "count", pending),
            Map.of("status", "Completed", "count", completed),
            Map.of("status", "Other", "count", Math.max(0, other))
        );
    }

    private List<Map<String, Object>> buildAdmissionsByWardReport(List<Admission> admissions) {
        return admissions.stream()
                .filter(Objects::nonNull)
                .collect(Collectors.groupingBy(a -> hasText(a.getWard()) ? a.getWard().trim() : "Unassigned", LinkedHashMap::new, Collectors.counting()))
                .entrySet().stream()
                .sorted((a, b) -> Long.compare(b.getValue(), a.getValue()))
                .map(e -> { Map<String, Object> m = new LinkedHashMap<>(); m.put("ward", e.getKey()); m.put("count", e.getValue()); return m; })
                .toList();
    }

    private List<Map<String, Object>> buildPrescriptionClassReport(List<Prescription> prescriptions) {
        Map<String, Long> byClass = new LinkedHashMap<>();
        prescriptions.stream().filter(Objects::nonNull).forEach(rx -> {
            List<PrescriptionItem> items = prescriptionItemRepository.findByPrescriptionId(rx.getId());
            if (!items.isEmpty()) {
                items.forEach(item -> {
                    String cls = hasText(item.getMedicationClass()) ? item.getMedicationClass().trim() : "Unclassified";
                    byClass.put(cls, byClass.getOrDefault(cls, 0L) + 1L);
                });
            } else {
                String cls = hasText(rx.getMedicationClass()) ? rx.getMedicationClass().trim() : "Unclassified";
                byClass.put(cls, byClass.getOrDefault(cls, 0L) + 1L);
            }
        });
        return byClass.entrySet().stream()
                .sorted((a, b) -> Long.compare(b.getValue(), a.getValue()))
                .map(e -> { Map<String, Object> m = new LinkedHashMap<>(); m.put("class", e.getKey()); m.put("count", e.getValue()); return m; })
                .toList();
    }

    private List<Map<String, Object>> buildReferralsByDeptReport(List<ReferralRecord> referrals) {
        return referrals.stream()
                .filter(Objects::nonNull)
                .collect(Collectors.groupingBy(r -> hasText(r.getToDept()) ? r.getToDept().trim() : "Unspecified", LinkedHashMap::new, Collectors.counting()))
                .entrySet().stream()
                .sorted((a, b) -> Long.compare(b.getValue(), a.getValue()))
                .limit(10)
                .map(e -> { Map<String, Object> m = new LinkedHashMap<>(); m.put("dept", e.getKey()); m.put("count", e.getValue()); return m; })
                .toList();
    }

    private List<Map<String, Object>> buildReferralsByUrgencyReport(List<ReferralRecord> referrals) {
        return List.of("Emergency", "Urgent", "Routine").stream()
                .map(level -> { Map<String, Object> m = new LinkedHashMap<>(); m.put("urgency", level); m.put("count", referrals.stream().filter(r -> level.equalsIgnoreCase(r.getUrgency())).count()); return m; })
                .toList();
    }

    private List<Map<String, Object>> buildTriageLevelReport(List<TriageRecord> records) {
        return List.of("Level 1", "Level 2", "Level 3", "Level 4").stream()
                .map(level -> { Map<String, Object> m = new LinkedHashMap<>(); m.put("level", level); m.put("count", records.stream().filter(r -> level.equalsIgnoreCase(r.getLevel())).count()); return m; })
                .toList();
    }

    private List<Map<String, Object>> buildEmergencyBySeverityReport(List<EmergencyCase> cases) {
        return List.of("Critical", "Serious", "Moderate", "Minor").stream()
                .map(sev -> { Map<String, Object> m = new LinkedHashMap<>(); m.put("severity", sev); m.put("count", cases.stream().filter(c -> sev.equalsIgnoreCase(c.getSeverity())).count()); return m; })
                .toList();
    }

    private List<Map<String, Object>> buildPatientsByGenderReport(List<Patient> patients) {
        return List.of("Male", "Female", "Other").stream()
                .map(g -> { Map<String, Object> m = new LinkedHashMap<>(); m.put("gender", g); m.put("count", patients.stream().filter(p -> g.equalsIgnoreCase(p.getGender())).count()); return m; })
                .toList();
    }

    private List<Map<String, Object>> buildPatientsByTypeReport(List<Patient> patients) {
        return patients.stream()
                .filter(Objects::nonNull)
                .collect(Collectors.groupingBy(p -> hasText(p.getPatientType()) ? p.getPatientType().trim() : "Other", LinkedHashMap::new, Collectors.counting()))
                .entrySet().stream()
                .map(e -> { Map<String, Object> m = new LinkedHashMap<>(); m.put("type", e.getKey()); m.put("count", e.getValue()); return m; })
                .toList();
    }

    private List<Map<String, Object>> buildPatientAgeGroupReport(List<Patient> patients) {
        Map<String, Long> groups = new LinkedHashMap<>();
        groups.put("0-17", 0L); groups.put("18-25", 0L); groups.put("26-35", 0L); groups.put("36-50", 0L); groups.put("51+", 0L);
        patients.stream().filter(Objects::nonNull).forEach(p -> {
            int age = defaultInt(p.getAge());
            String g = age < 18 ? "0-17" : age <= 25 ? "18-25" : age <= 35 ? "26-35" : age <= 50 ? "36-50" : "51+";
            groups.put(g, groups.get(g) + 1L);
        });
        return groups.entrySet().stream()
                .map(e -> { Map<String, Object> m = new LinkedHashMap<>(); m.put("group", e.getKey()); m.put("count", e.getValue()); return m; })
                .toList();
    }

    private List<Map<String, Object>> buildBloodBankStockReport(List<BloodUnit> units) {
        return units.stream()
                .filter(Objects::nonNull)
                .map(u -> { Map<String, Object> m = new LinkedHashMap<>(); m.put("bloodType", stringValue(u.getBloodType())); m.put("quantity", defaultInt(u.getQuantity())); m.put("status", stringValue(u.getStatus())); return m; })
                .toList();
    }

    private List<String> csvToList(String input) {
        if (!hasText(input)) {
            return List.of();
        }
        return Arrays.stream(input.split(","))
                .map(String::trim)
                .filter(value -> !value.isEmpty())
                .toList();
    }

    private List<Map<String, String>> historyToList(String history) {
        if (!hasText(history)) {
            return List.of();
        }
        return Arrays.stream(history.split("\\|\\|"))
                .map(String::trim)
                .filter(value -> !value.isEmpty())
                .map(entry -> {
                    String[] parts = entry.split("\\|", 4);
                    Map<String, String> item = new LinkedHashMap<>();
                    item.put("timestamp", parts.length > 0 ? parts[0] : "");
                    item.put("stage", parts.length > 1 ? parts[1] : "");
                    item.put("performed_by", parts.length > 2 ? parts[2] : "");
                    item.put("note", parts.length > 3 ? parts[3] : "");
                    return item;
                })
                .toList();
    }

    private String appendHistory(String current, String item) {
        if (!hasText(current)) {
            return item;
        }
        return current + "||" + item;
    }

    private boolean hasOutstandingActions(String actions) {
        return csvToList(actions).stream().anyMatch(Objects::nonNull);
    }

    private String normalizeStage(String input) {
        return input.trim().replace(' ', '_').toUpperCase(Locale.ROOT);
    }

    private Map<String, Object> parsePayloadJson(ClinicalFormRecord form) {
        try {
            return objectMapper.readValue(stringValue(form.getPayloadJson()), new TypeReference<Map<String, Object>>() {});
        } catch (Exception ignored) {
            return Map.of();
        }
    }

    private ClinicalFormRecord resolveClinicalForm(String identifier, String patientId, String expectedFormType, String encounterId) {
        String normalizedIdentifier = stringValue(identifier).trim();
        if (hasText(normalizedIdentifier)) {
            for (ClinicalFormRecord form : dataStore.getClinicalForms()) {
                if (!matchesExpectedForm(form, patientId, expectedFormType, encounterId)) {
                    continue;
                }
                if (normalizedIdentifier.equalsIgnoreCase(stringValue(form.getFormId()).trim())
                        || normalizedIdentifier.equalsIgnoreCase(stringValue(form.getId()).trim())) {
                    return form;
                }
            }
        }

        return dataStore.getClinicalForms().stream()
                .filter(form -> matchesExpectedForm(form, patientId, expectedFormType, encounterId))
                .sorted((left, right) -> stringValue(right.getCreatedAt()).compareToIgnoreCase(stringValue(left.getCreatedAt())))
                .findFirst()
                .orElse(null);
    }

    private boolean matchesExpectedForm(ClinicalFormRecord form, String patientId, String expectedFormType, String encounterId) {
        if (!isBlank(patientId) && !isEqualIgnoreCase(form.getPatientId(), patientId)) {
            return false;
        }
        if (!isBlank(expectedFormType) && !isEqualIgnoreCase(form.getFormType(), expectedFormType)) {
            return false;
        }
        return isBlank(encounterId) || isEqualIgnoreCase(form.getEncounterId(), encounterId);
    }

    private String normalizeClinicalPayloadJson(String payloadJson) {
        Map<String, Object> payload = parsePayloadMap(payloadJson);
        if (payload.isEmpty()) {
            return stringValue(payloadJson);
        }
        Map<String, Object> sanitized = sanitizePayloadMap(payload);

        String diagnosisCode = stringValue(sanitized.get("diagnosis_code")).trim();
        if (hasText(diagnosisCode)) {
            String diagnosisLabel = diagnosisLabelForCode(diagnosisCode);
            if (hasText(diagnosisLabel) && isBlank(stringValue(sanitized.get("diagnosis")))) {
                sanitized.put("diagnosis", diagnosisLabel);
            }
            if (isBlank(stringValue(sanitized.get("diagnosis_label")))) {
                sanitized.put("diagnosis_label", diagnosisLabel);
            }
            if (isBlank(stringValue(sanitized.get("diagnosis_category")))) {
                sanitized.put("diagnosis_category", diagnosisCategoryForCode(diagnosisCode));
            }
            if (isBlank(stringValue(sanitized.get("treatment_program")))) {
                String defaultProgram = defaultProgramForDiagnosisCode(diagnosisCode);
                if (hasText(defaultProgram)) {
                    sanitized.put("treatment_program", defaultProgram);
                }
            }
        }

        if (hasText(stringValue(sanitized.get("medications_given"))) && isBlank(stringValue(sanitized.get("medication_class")))) {
            sanitized.put("medication_class", inferMedicationClass(stringValue(sanitized.get("medications_given"))));
        }

        return writeJson(sanitized);
    }

    private Map<String, Object> parsePayloadMap(String payloadJson) {
        try {
            return objectMapper.readValue(stringValue(payloadJson), new TypeReference<Map<String, Object>>() {});
        } catch (Exception ex) {
            return Map.of();
        }
    }

    private String diagnosisLabelForCode(String code) {
        return switch (stringValue(code).trim().toUpperCase(Locale.ROOT)) {
            case "MAL-001" -> "Malaria";
            case "TB-001" -> "Tuberculosis";
            case "HIV-001" -> "HIV Disease / ART Follow-up";
            case "URTI-001" -> "Upper Respiratory Tract Infection";
            case "LRTI-001" -> "Lower Respiratory Tract Infection";
            case "AST-001" -> "Asthma";
            case "HTN-001" -> "Hypertension";
            case "DM-001" -> "Diabetes Mellitus";
            case "ANC-001" -> "Antenatal Care Visit";
            case "PMTCT-001" -> "PMTCT Follow-up";
            case "STI-001" -> "Sexually Transmitted Infection";
            case "GI-001" -> "Gastroenteritis";
            case "PUD-001" -> "Peptic Ulcer Disease / Gastritis";
            case "UTI-001" -> "Urinary Tract Infection";
            case "EYE-001" -> "Conjunctivitis / Eye Infection";
            case "ENT-001" -> "ENT Infection";
            case "MH-001" -> "Depression / Anxiety";
            case "NEU-001" -> "Epilepsy / Seizure Disorder";
            case "ONC-001" -> "Oncology / Suspected Malignancy";
            case "TRA-001" -> "Trauma / Injury";
            case "DERM-001" -> "Skin Condition / Dermatitis";
            case "OTH-001" -> "Other Clinical Diagnosis";
            default -> "";
        };
    }

    private String diagnosisCategoryForCode(String code) {
        return switch (stringValue(code).trim().toUpperCase(Locale.ROOT)) {
            case "MAL-001", "TB-001", "HIV-001" -> "Infectious Diseases";
            case "URTI-001", "LRTI-001", "AST-001" -> "Respiratory";
            case "HTN-001" -> "Cardiovascular";
            case "DM-001" -> "Endocrine";
            case "ANC-001", "PMTCT-001" -> "MCH";
            case "STI-001" -> "Sexual Health";
            case "GI-001", "PUD-001" -> "Gastrointestinal";
            case "UTI-001" -> "Genitourinary";
            case "EYE-001" -> "Eye Clinic";
            case "ENT-001" -> "ENT";
            case "MH-001" -> "Mental Health";
            case "NEU-001" -> "Neurology";
            case "ONC-001" -> "Oncology";
            case "TRA-001" -> "Emergency";
            case "DERM-001" -> "Dermatology";
            default -> "Other";
        };
    }

    private String defaultProgramForDiagnosisCode(String code) {
        return switch (stringValue(code).trim().toUpperCase(Locale.ROOT)) {
            case "MAL-001" -> "Malaria";
            case "TB-001" -> "TB";
            case "HIV-001" -> "ARV / HIV";
            case "AST-001" -> "Asthma / Respiratory";
            case "HTN-001" -> "Hypertension";
            case "DM-001" -> "Diabetes";
            case "ANC-001", "PMTCT-001" -> "ANC / PMTCT";
            case "MH-001" -> "Mental Health";
            case "NEU-001" -> "Epilepsy / Neurology";
            case "ONC-001" -> "Oncology";
            default -> "";
        };
    }

    private String inferMedicationClass(String medications) {
        String text = stringValue(medications).toLowerCase(Locale.ROOT);
        if (text.contains("amoxicillin") || text.contains("ciprofloxacin") || text.contains("ceftriaxone") || text.contains("azithromycin") || text.contains("doxycycline") || text.contains("metronidazole")) {
            return "Antibiotic";
        }
        if (text.contains("coartem") || text.contains("artemether") || text.contains("quinine")) {
            return "Antimalarial";
        }
        if (text.contains("tenofovir") || text.contains("lamivudine") || text.contains("dolutegravir") || text.contains("efavirenz")) {
            return "ARV";
        }
        if (text.contains("rifampicin") || text.contains("isoniazid") || text.contains("pyrazinamide") || text.contains("ethambutol")) {
            return "Anti-TB";
        }
        if (text.contains("amlodipine") || text.contains("atenolol") || text.contains("enalapril") || text.contains("losartan")) {
            return "Antihypertensive";
        }
        if (text.contains("metformin") || text.contains("insulin")) {
            return "Antidiabetic";
        }
        if (text.contains("salbutamol") || text.contains("inhaler")) {
            return "Bronchodilator / Respiratory";
        }
        if (text.contains("paracetamol") || text.contains("ibuprofen") || text.contains("diclofenac")) {
            return "Analgesic / Anti-inflammatory";
        }
        if (text.contains("omeprazole") || text.contains("antacid")) {
            return "Gastrointestinal";
        }
        return "Other";
    }

    private String resolveCanonicalPatientId(String submittedPatientId) {
        Patient linkedPatient = resolvePatient(submittedPatientId);
        return linkedPatient != null
                ? stringValue(firstNonBlank(linkedPatient.getPatientId(), linkedPatient.getClinicNumber())).trim()
                : stringValue(submittedPatientId).trim();
    }

    private String resolveCanonicalPatientName(Patient linkedPatient, String submittedPatientName) {
        if (linkedPatient != null && hasText(linkedPatient.getName())) {
            return linkedPatient.getName().trim();
        }
        return stringValue(submittedPatientName).trim();
    }

    private void syncPatientIdentifierReferences(String oldPatientIdentifier, String oldClinicNumber, String newPatientIdentifier) {
        if (isBlank(newPatientIdentifier)) {
            return;
        }
        List<String> legacyIdentifiers = java.util.stream.Stream.of(oldPatientIdentifier, oldClinicNumber)
                .filter(this::hasText)
                .map(String::trim)
                .distinct()
                .filter(identifier -> !identifier.equalsIgnoreCase(newPatientIdentifier))
                .toList();
        if (legacyIdentifiers.isEmpty()) {
            return;
        }

        dataStore.getAppointments().stream()
                .filter(entry -> matchesAnyIdentifier(entry.getPatientId(), legacyIdentifiers))
                .forEach(entry -> {
                    entry.setPatientId(newPatientIdentifier);
                    dataStore.updateAppointment(entry);
                });
        dataStore.getPrescriptions().stream()
                .filter(entry -> matchesAnyIdentifier(entry.getPatientId(), legacyIdentifiers) || matchesAnyIdentifier(entry.getPatientIdNum(), legacyIdentifiers))
                .forEach(entry -> {
                    entry.setPatientId(newPatientIdentifier);
                    entry.setPatientIdNum(newPatientIdentifier);
                    dataStore.updatePrescription(entry);
                });
        dataStore.getAdmissions().stream()
                .filter(entry -> matchesAnyIdentifier(entry.getPatientId(), legacyIdentifiers))
                .forEach(entry -> {
                    entry.setPatientId(newPatientIdentifier);
                    dataStore.updateAdmission(entry);
                });
        dataStore.getLabTests().stream()
                .filter(entry -> matchesAnyIdentifier(entry.getPatientId(), legacyIdentifiers))
                .forEach(entry -> {
                    entry.setPatientId(newPatientIdentifier);
                    dataStore.updateLabTest(entry);
                });
        dataStore.getBillingInvoices().stream()
                .filter(entry -> matchesAnyIdentifier(entry.getPatientId(), legacyIdentifiers))
                .forEach(entry -> {
                    entry.setPatientId(newPatientIdentifier);
                    dataStore.updateBillingInvoice(entry);
                });
        dataStore.getImagingRequests().stream()
                .filter(entry -> matchesAnyIdentifier(entry.getPatientId(), legacyIdentifiers))
                .forEach(entry -> {
                    entry.setPatientId(newPatientIdentifier);
                    dataStore.updateImagingRequest(entry);
                });
        dataStore.getReferralRecords().stream()
                .filter(entry -> matchesAnyIdentifier(entry.getPatientId(), legacyIdentifiers))
                .forEach(entry -> {
                    entry.setPatientId(newPatientIdentifier);
                    dataStore.updateReferralRecord(entry);
                });
        dataStore.getTriageRecords().stream()
                .filter(entry -> matchesAnyIdentifier(entry.getPatientId(), legacyIdentifiers))
                .forEach(entry -> {
                    entry.setPatientId(newPatientIdentifier);
                    dataStore.updateTriageRecord(entry);
                });
        dataStore.getQueueTickets().stream()
                .filter(entry -> matchesAnyIdentifier(entry.getPatientId(), legacyIdentifiers))
                .forEach(entry -> {
                    entry.setPatientId(newPatientIdentifier);
                    dataStore.updateQueueTicket(entry);
                });
        dataStore.getEncounterRecords().stream()
                .filter(entry -> matchesAnyIdentifier(entry.getPatientId(), legacyIdentifiers))
                .forEach(entry -> {
                    entry.setPatientId(newPatientIdentifier);
                    dataStore.updateEncounterRecord(entry);
                });
        dataStore.getClinicalForms().stream()
                .filter(entry -> matchesAnyIdentifier(entry.getPatientId(), legacyIdentifiers))
                .forEach(entry -> {
                    entry.setPatientId(newPatientIdentifier);
                    dataStore.updateClinicalForm(entry);
                });
    }

    private boolean matchesAnyIdentifier(String candidate, List<String> identifiers) {
        String normalizedCandidate = stringValue(candidate).trim();
        return identifiers.stream().anyMatch(identifier -> identifier.equalsIgnoreCase(normalizedCandidate));
    }

    private Map<String, Object> sanitizePayloadMap(Map<String, Object> payload) {
        if (payload == null || payload.isEmpty()) {
            return Map.of();
        }
        Map<String, Object> sanitized = new LinkedHashMap<>();
        payload.forEach((key, value) -> sanitized.put(stringValue(key), value == null ? "" : value));
        return sanitized;
    }

    private Map<String, Object> buildMedicalFitnessCertificatePayload(Patient patient, Map<String, Object> examPayload, String examinerName) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("name", firstNonBlank(patient.getName(), examPayload.get("name"), examPayload.get("patient_name")));
        payload.put("school", firstNonBlank(examPayload.get("school"), patient.getSchool(), patient.getProgram()));
        payload.put("comp_no", firstNonBlank(
                examPayload.get("comp_no"),
                examPayload.get("computer_number"),
                patient.getStudentId(),
                patient.getManNumber(),
                patient.getPatientId()
        ));
        payload.put("fitness_status", resolveFitnessStatusFromPayload(examPayload));
        payload.put("comments", firstNonBlank(
                examPayload.get("comments"),
                examPayload.get("diagnosis"),
                examPayload.get("general_comments")
        ));
        payload.put("examiner_name", firstNonBlank(examinerName, examPayload.get("examiner_name")));
        payload.put("official_date", firstNonBlank(
                examPayload.get("official_date"),
                examPayload.get("date"),
                LocalDate.now().toString()
        ));
        return payload;
    }

    private String resolveFitnessStatusFromPayload(Map<String, Object> payload) {
        String raw = stringValue(firstNonBlank(payload.get("fitness_status"), payload.get("fitness_recommendation"))).trim();
        if (raw.isEmpty()) {
            return "PENDING";
        }
        String normalized = raw.toUpperCase(Locale.ROOT);
        if (normalized.startsWith("FIT")) {
            return "FIT";
        }
        if (normalized.contains("UNFIT")) {
            return "UNFIT";
        }
        return normalized;
    }

    private Map<String, Object> buildMedicalStatusResponse(
            Patient patient,
            String externalIdentifier,
            String source,
            boolean requiresMedicalExam,
            String defaultStatus
    ) {
        ClinicalFormRecord latestExam = patient == null ? null : resolveClinicalForm(null, patient.getPatientId(), "student_medical_exam", null);
        ClinicalFormRecord latestCertificate = patient == null ? null : resolveClinicalForm(null, patient.getPatientId(), "medical_fitness_certificate", null);
        Map<String, Object> certificatePayload = latestCertificate == null ? Map.of() : parsePayloadJson(latestCertificate);

        Map<String, Object> response = new LinkedHashMap<>();
        response.put("identifier", externalIdentifier);
        response.put("source", source);
        response.put("patient_id", patient == null ? "" : patient.getPatientId());
        response.put("patient_name", patient == null ? "" : patient.getName());
        response.put("requires_medical_exam", requiresMedicalExam);
        response.put("medical_exam_status", latestExam != null ? "completed" : defaultStatus);
        response.put("certificate_status", latestCertificate != null ? "issued" : "pending");
        response.put("examined", latestExam != null);
        response.put("fitness_status", latestCertificate != null
                ? resolveFitnessStatusFromPayload(certificatePayload)
                : latestExam != null ? resolveFitnessStatusFromPayload(parsePayloadJson(latestExam)) : "PENDING");
        response.put("last_exam_date", latestExam == null ? "" : stringValue(latestExam.getCreatedAt()));
        response.put("last_certificate_date", latestCertificate == null ? "" : stringValue(latestCertificate.getCreatedAt()));
        return response;
    }

    private void syncMedicalExamStatus(Patient patient, Map<String, Object> certificatePayload) {
        String notes = stringValue(firstNonBlank(certificatePayload.get("comments"), certificatePayload.get("fitness_status"))).trim();
        String examDate = stringValue(firstNonBlank(certificatePayload.get("official_date"), LocalDate.now().toString()));

        if (isStudentPatientType(patient.getPatientType()) && hasText(patient.getStudentId())) {
            updateStudentMedicalRecord(patient.getStudentId(), Map.of(
                    "examined", true,
                    "examDate", examDate,
                    "notes", notes
            ));
            return;
        }

        if (isStaffLinkedPatientType(patient.getPatientType()) && hasText(patient.getManNumber())) {
            updateStaffMedicalRecord(patient.getManNumber(), Map.of(
                    "examined", true,
                    "examDate", examDate,
                    "notes", notes
            ));
        }
    }

    private void syncMedicalExamCompletion(Patient patient, Map<String, Object> examPayload, String examDateTime) {
        String notes = stringValue(firstNonBlank(
                examPayload.get("diagnosis"),
                examPayload.get("general_comments"),
                examPayload.get("fitness_recommendation")
        )).trim();
        String examDate = hasText(examDateTime) ? examDateTime : LocalDate.now().toString();

        if (isStudentPatientType(patient.getPatientType()) && hasText(patient.getStudentId())) {
            updateStudentMedicalRecord(patient.getStudentId(), Map.of(
                    "examined", true,
                    "examDate", examDate,
                    "notes", notes
            ));
            return;
        }

        if (isStaffLinkedPatientType(patient.getPatientType()) && hasText(patient.getManNumber())) {
            updateStaffMedicalRecord(patient.getManNumber(), Map.of(
                    "examined", true,
                    "examDate", examDate,
                    "notes", notes
            ));
        }
    }

    private double parseDoubleValue(Object value) {
        if (value == null) {
            return 0.0;
        }
        try {
            String cleaned = String.valueOf(value).replaceAll("[^\\d.]", "");
            return cleaned.isBlank() ? 0.0 : Double.parseDouble(cleaned);
        } catch (Exception ignored) {
            return 0.0;
        }
    }

    private Double calculateBmi(Double weightKg, Double heightCm) {
        if (weightKg == null || heightCm == null || weightKg <= 0 || heightCm <= 0) {
            return null;
        }
        double heightMeters = heightCm / 100.0;
        double bmi = weightKg / (heightMeters * heightMeters);
        return Math.round(bmi * 10.0) / 10.0;
    }

    private String buildTriageVitalsSummary(TriageRecord record) {
        List<String> parts = new java.util.ArrayList<>();
        if (record.getTemperature() != null) parts.add("Temp " + record.getTemperature() + "C");
        if (hasText(record.getBloodPressure())) parts.add("BP " + record.getBloodPressure());
        if (record.getPulseRate() != null) parts.add("PR " + record.getPulseRate() + " bpm");
        if (record.getRespiratoryRate() != null) parts.add("RR " + record.getRespiratoryRate() + "/min");
        if (record.getOxygenSaturation() != null) parts.add("SpO2 " + record.getOxygenSaturation() + "%");
        if (record.getWeightKg() != null) parts.add("Wt " + record.getWeightKg() + " kg");
        if (record.getHeightCm() != null) parts.add("Ht " + record.getHeightCm() + " cm");
        if (record.getBmi() != null) parts.add("BMI " + record.getBmi());
        if (record.getRandomBloodSugar() != null) parts.add("RBS " + record.getRandomBloodSugar() + " mmol/L");
        if (record.getPainScore() != null) parts.add("Pain " + record.getPainScore() + "/10");
        if (hasText(record.getConsciousnessLevel())) parts.add("AVPU " + record.getConsciousnessLevel());
        return String.join(", ", parts);
    }

    private Object firstNonBlank(Object... values) {
        for (Object value : values) {
            if (value != null && !String.valueOf(value).trim().isEmpty()) {
                return value;
            }
        }
        return "";
    }

    private int defaultInt(Integer value) { return value == null ? 0 : value; }
    private double value(Double input) { return input == null ? 0.0 : input; }
    private double value(Object input) {
        if (input instanceof Number number) {
            return number.doubleValue();
        }
        try {
            return Double.parseDouble(stringValue(input));
        } catch (Exception ex) {
            return 0.0;
        }
    }
    private String stringValue(String input) { return input == null ? "" : input; }
    private String stringValue(Object input) { return input == null ? "" : String.valueOf(input); }
    private boolean isEqualIgnoreCase(String left, String right) { return stringValue(left).equalsIgnoreCase(stringValue(right)); }
    private boolean hasText(String input) { return input != null && !input.trim().isEmpty(); }
    private boolean isBlank(String input) { return stringValue(input).trim().isEmpty(); }

    private List<String> resolvePermissions(String role, List<String> requestedPermissions) {
        if (requestedPermissions != null && !requestedPermissions.isEmpty()) {
            return requestedPermissions.stream()
                    .filter(this::hasText)
                    .map(String::trim)
                    .distinct()
                    .toList();
        }
        return defaultPermissionsForRole(role);
    }

    private AppUser requireAuthenticatedUser(HttpServletRequest request) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated() || !(auth.getPrincipal() instanceof AuthUserDetails)) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Missing signed-in user context");
        }
        AuthUserDetails userDetails = (AuthUserDetails) auth.getPrincipal();
        AppUser user = dataStore.getUser(userDetails.getId());
        if (user == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "User not found");
        }
        if (!isEqualIgnoreCase(user.getStatus(), "active")) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Account is inactive");
        }
        return user;
    }

    private AppUser requirePermission(HttpServletRequest request, String permission) {
        AppUser user = requireAuthenticatedUser(request);
        if (!userHasPermission(user, permission)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "You do not have permission to perform this action");
        }
        return user;
    }

    private AppUser requireAnyPermission(HttpServletRequest request, List<String> permissions) {
        AppUser user = requireAuthenticatedUser(request);
        boolean allowed = permissions.stream().anyMatch(permission -> userHasPermission(user, permission));
        if (!allowed) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "You do not have permission to perform this action");
        }
        return user;
    }

    private boolean userHasPermission(AppUser user, String permission) {
        return parseUserPermissions(user).contains(permission);
    }

    private String resolveActorName(AppUser actor, String submittedName, String fallback) {
        if (actor != null && hasText(actor.getName())) {
            return actor.getName().trim();
        }
        if (hasText(submittedName)) {
            return submittedName.trim();
        }
        return fallback;
    }

    private List<String> parseUserPermissions(AppUser user) {
        String raw = stringValue(user.getPermissionsJson());
        if (isBlank(raw)) {
            return defaultPermissionsForRole(user.getRole());
        }
        try {
            return objectMapper.readValue(raw, new TypeReference<List<String>>() {});
        } catch (Exception ex) {
            return defaultPermissionsForRole(user.getRole());
        }
    }

    private List<String> defaultPermissionsForRole(String role) {
        return com.unza.clinic.security.RolePermissions.forRole(role);
    }

    private void writeAuditLog(String user, String action, String description, String ipAddress) {
        writeAuditLog(user, "System", action, description, ipAddress);
    }

    private void writeAuditLog(String user, String role, String action, String description, String ipAddress) {
        AuditLogEntry entry = new AuditLogEntry();
        entry.setTimestamp(LocalDateTime.now().toString());
        entry.setUser(user);
        entry.setRole(hasText(role) ? role : "System");
        entry.setAction(action);
        entry.setDescription(description);
        entry.setIpAddress(ipAddress);
        dataStore.addAuditLog(entry);
    }

    private void saveLoginAudit(String email, String ip, String userAgent,
                                boolean success, String failureReason,
                                String userId, String role) {
        LoginAuditLog log = new LoginAuditLog();
        log.setAuditId("LAL-" + UUID.randomUUID().toString().replace("-", "").substring(0, 16).toUpperCase());
        log.setEmail(email);
        log.setIpAddress(ip);
        log.setUserAgent(userAgent);
        log.setSuccess(success);
        log.setFailureReason(failureReason);
        log.setUserId(userId);
        log.setRole(role);
        log.setLoggedAt(LocalDateTime.now());
        dataStore.saveLoginAuditLog(log);
    }

    private String extractClientIp(HttpServletRequest request) {
        String forwarded = request.getHeader("X-Forwarded-For");
        if (forwarded != null && !forwarded.isBlank()) {
            return forwarded.split(",")[0].trim();
        }
        String realIp = request.getHeader("X-Real-IP");
        if (realIp != null && !realIp.isBlank()) {
            return realIp.trim();
        }
        return request.getRemoteAddr();
    }

    private Object paginate(List<Map<String, Object>> all, Integer page, Integer size) {
        if (page == null) return all;
        int pageNum = Math.max(0, page);
        int pageSize = (size != null && size > 0) ? size : 20;
        int total = all.size();
        int totalPages = (int) Math.ceil((double) total / pageSize);
        int fromIndex = Math.min(pageNum * pageSize, total);
        int toIndex = Math.min(fromIndex + pageSize, total);
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("data", all.subList(fromIndex, toIndex));
        result.put("page", pageNum);
        result.put("size", pageSize);
        result.put("totalElements", total);
        result.put("totalPages", totalPages);
        return result;
    }

    private Map<String, Object> buildWardSummary() {
        Map<String, Long> byWard = dataStore.getAdmissions().stream()
                .filter(a -> a.getStatus() != null && !"discharged".equalsIgnoreCase(a.getStatus()))
                .collect(Collectors.groupingBy(
                        a -> a.getWard() != null ? a.getWard() : "Unknown",
                        Collectors.counting()
                ));
        long totalActive = byWard.values().stream().mapToLong(Long::longValue).sum();
        Map<String, Object> summary = new LinkedHashMap<>();
        summary.put("timestamp", LocalDateTime.now().toString());
        summary.put("totalActiveAdmissions", totalActive);
        summary.put("byWard", byWard);
        return summary;
    }

    private AuthUserDetails getCurrentUserDetails() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.isAuthenticated() && auth.getPrincipal() instanceof AuthUserDetails userDetails) {
            return userDetails;
        }
        return null;
    }

    /**
     * Change password endpoint for authenticated users.
     * Requires current password verification and allows password updates.
     */
    @PostMapping("/change-password")
    public Map<String, Object> changePassword(@RequestBody Map<String, String> request) {
        String currentPassword = stringValue(request.get("currentPassword"));
        String newPassword = stringValue(request.get("newPassword"));
        AuthUserDetails currentUser = getCurrentUserDetails();
        if (currentUser == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Missing signed-in user context");
        }
        String email = currentUser.getEmail();

        if (isBlank(currentPassword) || isBlank(newPassword)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Current and new password are required");
        }
        
        AppUser user = dataStore.findUserByEmail(email);
        if (user == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "User not found");
        }
        
        if (!dataStore.verifyPassword(user, currentPassword)) {
            writeAuditLog(user.getName(), user.getRole(), "password_change_failed", "Invalid current password provided", "127.0.0.1");
            return Map.of("success", false, "message", "Current password is incorrect");
        }
        
        user.setPassword(newPassword);
        user.setForcePasswordChange(false);
        user.setPasswordChangedAt(LocalDateTime.now().toString());
        user.setPasswordVersion((user.getPasswordVersion() == null ? 1 : user.getPasswordVersion()) + 1);
        dataStore.updateUser(user);
        
        writeAuditLog(user.getName(), user.getRole(), "password_change", "User changed their password successfully", "127.0.0.1");
        
        Map<String, Object> userPayload = new LinkedHashMap<>();
        userPayload.put("id", user.getId());
        userPayload.put("user_id", user.getUserId());
        userPayload.put("name", user.getName());
        userPayload.put("email", user.getEmail());
        userPayload.put("role", user.getRole());
        userPayload.put("department", user.getDepartment());
        userPayload.put("staff_id", stringValue(user.getStaffId()));
        userPayload.put("man_number", stringValue(user.getManNumber()));
        userPayload.put("status", user.getStatus());
        userPayload.put("force_password_change", false);
        userPayload.put("permissions", parseUserPermissions(user));
        userPayload.put("password_changed_at", user.getPasswordChangedAt());
        userPayload.put("password_version", user.getPasswordVersion());

        Map<String, Object> response = new LinkedHashMap<>();
        response.put("success", true);
        response.put("message", "Password updated successfully");
        response.put("user", userPayload);
        return response;
    }

    /**
     * Admin-only: reset another user's password without requiring the current password.
     */
    @PostMapping("/users/{id}/reset-password")
    public Map<String, Object> adminResetPassword(HttpServletRequest httpRequest, @PathVariable Long id, @RequestBody Map<String, String> body) {
        AppUser actor = requireAuthenticatedUser(httpRequest);
        requirePermission(httpRequest, "users.reset_password");
        String newPassword = stringValue(body.get("newPassword"));
        if (isBlank(newPassword) || newPassword.length() < 6) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "New password must be at least 6 characters");
        }
        AppUser target = dataStore.getUser(id);
        if (target == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found");
        }
        target.setPassword(newPassword);
        target.setForcePasswordChange(true);
        target.setPasswordChangedAt(LocalDateTime.now().toString());
        target.setPasswordVersion((target.getPasswordVersion() == null ? 1 : target.getPasswordVersion()) + 1);
        dataStore.updateUser(target);
        writeAuditLog(actor.getName(), actor.getRole(), "admin_password_reset", "Admin reset password for user: " + target.getName() + " (" + target.getEmail() + ")", "127.0.0.1");
        return Map.of("success", true, "message", "Password reset. User will be required to change it on next login.");
    }

    // ===================================================================
    // MCH Clinic — Antenatal, Immunization, Family Planning
    // ===================================================================

    @GetMapping("/mch")
    public Map<String, Object> getMchAll(HttpServletRequest req) {
        requirePermission(req, "mch.view");
        List<Map<String, Object>> antenatal = List.of();
        List<Map<String, Object>> immunizations = List.of();
        List<Map<String, Object>> familyPlanning = List.of();
        try { antenatal = jdbc.queryForList("SELECT * FROM mch_antenatal_visits ORDER BY created_at DESC"); } catch (Exception ignored) {}
        try { immunizations = jdbc.queryForList("SELECT * FROM mch_immunization_visits ORDER BY created_at DESC"); } catch (Exception ignored) {}
        try { familyPlanning = jdbc.queryForList("SELECT * FROM mch_family_planning ORDER BY created_at DESC"); } catch (Exception ignored) {}
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("antenatal", antenatal);
        result.put("immunizations", immunizations);
        result.put("familyPlanning", familyPlanning);
        return result;
    }

    @GetMapping("/mch/antenatal")
    public List<Map<String, Object>> getMchAntenatal(HttpServletRequest req) {
        requirePermission(req, "mch.view");
        try {
            return jdbc.queryForList("SELECT * FROM mch_antenatal_visits ORDER BY created_at DESC");
        } catch (Exception e) { return List.of(); }
    }

    @PostMapping("/mch/antenatal")
    public Map<String, Object> createMchAntenatal(HttpServletRequest req, @RequestBody Map<String, Object> body) {
        requirePermission(req, "mch.view");
        String visitId = "ANC-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
        try {
            jdbc.update("""
                INSERT INTO mch_antenatal_visits
                (visit_id,patient_id,patient_name,visit_date,gestational_age,gravida,para,lmp,edd,
                 blood_pressure,weight,fetal_heart_rate,presentation,urinalysis,hiv_status,syphilis_status,
                 ferrous,folic_acid,itn,tetanus_vaccine,next_visit,nurse_notes,risk_factors,nurse_name,status)
                VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,'active')
                """,
                visitId,
                strOf(body, "patient_id"), strOf(body, "patient_name"), strOf(body, "visit_date"),
                strOf(body, "gestational_age"), intOf(body, "gravida"), intOf(body, "para"),
                strOf(body, "lmp"), strOf(body, "edd"), strOf(body, "blood_pressure"),
                strOf(body, "weight"), strOf(body, "fetal_heart_rate"), strOf(body, "presentation"),
                strOf(body, "urinalysis"), strOf(body, "hiv_status"), strOf(body, "syphilis_status"),
                boolOf(body, "ferrous"), boolOf(body, "folic_acid"), boolOf(body, "itn"),
                strOf(body, "tetanus_vaccine"), strOf(body, "next_visit"),
                strOf(body, "nurse_notes"), strOf(body, "risk_factors"), strOf(body, "nurse_name")
            );
            Map<String, Object> entry = jdbc.queryForMap("SELECT * FROM mch_antenatal_visits WHERE visit_id = ?", visitId);
            return Map.of("success", true, "entry", entry);
        } catch (Exception e) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Failed to save antenatal visit: " + e.getMessage());
        }
    }

    @GetMapping("/mch/immunization")
    public List<Map<String, Object>> getMchImmunization(HttpServletRequest req) {
        requirePermission(req, "mch.view");
        try {
            return jdbc.queryForList("SELECT * FROM mch_immunization_visits ORDER BY created_at DESC");
        } catch (Exception e) { return List.of(); }
    }

    @PostMapping("/mch/immunization")
    public Map<String, Object> createMchImmunization(HttpServletRequest req, @RequestBody Map<String, Object> body) {
        requirePermission(req, "mch.view");
        String childId = "IMMU-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
        try {
            jdbc.update("""
                INSERT INTO mch_immunization_visits
                (child_id,child_name,mother_name,dob,vaccines_given,visit_date,next_due,
                 weight,height,muac,nutrition_status,nurse_notes,nurse_name)
                VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?)
                """,
                childId, strOf(body, "child_name"), strOf(body, "mother_name"), strOf(body, "dob"),
                strOf(body, "vaccines_given"), strOf(body, "visit_date"), strOf(body, "next_due"),
                strOf(body, "weight"), strOf(body, "height"), strOf(body, "muac"),
                strOf(body, "nutrition_status"), strOf(body, "nurse_notes"), strOf(body, "nurse_name")
            );
            Map<String, Object> entry = jdbc.queryForMap("SELECT * FROM mch_immunization_visits WHERE child_id = ?", childId);
            return Map.of("success", true, "entry", entry);
        } catch (Exception e) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Failed to save immunization: " + e.getMessage());
        }
    }

    @GetMapping("/mch/family-planning")
    public List<Map<String, Object>> getMchFamilyPlanning(HttpServletRequest req) {
        requirePermission(req, "mch.view");
        try {
            return jdbc.queryForList("SELECT * FROM mch_family_planning ORDER BY created_at DESC");
        } catch (Exception e) { return List.of(); }
    }

    @PostMapping("/mch/family-planning")
    public Map<String, Object> createMchFamilyPlanning(HttpServletRequest req, @RequestBody Map<String, Object> body) {
        requirePermission(req, "mch.view");
        String visitId = "FP-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
        try {
            jdbc.update("""
                INSERT INTO mch_family_planning
                (visit_id,patient_id,patient_name,method,start_date,next_visit,
                 side_effects,counseling_given,notes,nurse_name,status)
                VALUES (?,?,?,?,?,?,?,?,?,?,'active')
                """,
                visitId, strOf(body, "patient_id"), strOf(body, "patient_name"),
                strOf(body, "method"), strOf(body, "start_date"), strOf(body, "next_visit"),
                strOf(body, "side_effects"), boolOf(body, "counseling_given"),
                strOf(body, "notes"), strOf(body, "nurse_name")
            );
            Map<String, Object> entry = jdbc.queryForMap("SELECT * FROM mch_family_planning WHERE visit_id = ?", visitId);
            return Map.of("success", true, "entry", entry);
        } catch (Exception e) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Failed to save family planning: " + e.getMessage());
        }
    }

    // ===================================================================
    // ART / HIV Clinic
    // ===================================================================

    @GetMapping("/art/patients")
    public List<Map<String, Object>> getArtPatients(HttpServletRequest req) {
        requirePermission(req, "art.view");
        try {
            return jdbc.queryForList("SELECT * FROM art_patients ORDER BY created_at DESC");
        } catch (Exception e) { return List.of(); }
    }

    @PostMapping("/art/patients")
    public Map<String, Object> createArtPatient(HttpServletRequest req, @RequestBody Map<String, Object> body) {
        requirePermission(req, "art.view");
        try {
            jdbc.update("""
                INSERT INTO art_patients
                (art_number,patient_id,patient_name,enrollment_date,current_regimen,regimen_line,
                 cd4_baseline,cd4_latest,vl_latest,vl_date,disclosure_status,pregnancy_status,
                 tb_status,tb_treatment,adherence_score,next_pickup,next_clinic_date,
                 transfer_in,transfer_from,status)
                VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,'active')
                ON CONFLICT (art_number) DO NOTHING
                """,
                strOf(body, "art_number"), strOf(body, "patient_id"), strOf(body, "patient_name"),
                strOf(body, "enrollment_date"), strOf(body, "current_regimen"), strOf(body, "regimen_line"),
                intOf(body, "cd4_baseline"), intOf(body, "cd4_latest"),
                longOf(body, "vl_latest"), strOf(body, "vl_date"),
                strOf(body, "disclosure_status"), strOf(body, "pregnancy_status"),
                strOf(body, "tb_status"), boolOf(body, "tb_treatment"),
                strOf(body, "adherence_score"), strOf(body, "next_pickup"), strOf(body, "next_clinic_date"),
                boolOf(body, "transfer_in"), strOf(body, "transfer_from")
            );
            Map<String, Object> entry = jdbc.queryForMap("SELECT * FROM art_patients WHERE art_number = ?", strOf(body, "art_number"));
            return Map.of("success", true, "entry", entry);
        } catch (Exception e) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Failed to save ART patient: " + e.getMessage());
        }
    }

    @GetMapping("/art/visits")
    public List<Map<String, Object>> getArtVisits(HttpServletRequest req) {
        requirePermission(req, "art.view");
        try {
            return jdbc.queryForList("SELECT * FROM art_clinic_visits ORDER BY created_at DESC");
        } catch (Exception e) { return List.of(); }
    }

    @PostMapping("/art/visits")
    public Map<String, Object> createArtVisit(HttpServletRequest req, @RequestBody Map<String, Object> body) {
        requirePermission(req, "art.view");
        String visitId = "ART-V-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
        try {
            jdbc.update("""
                INSERT INTO art_clinic_visits
                (visit_id,art_number,patient_id,patient_name,visit_date,clinician_name,
                 weight,blood_pressure,cd4_count,viral_load,vl_date,viral_load_status,
                 regimen,regimen_changed,change_reason,ois_present,cotrimoxazole,inh,
                 days_dispensed,pills_returned,adherence_score,side_effects,
                 counseling_given,next_visit,notes)
                VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)
                """,
                visitId, strOf(body, "art_number"), strOf(body, "patient_id"), strOf(body, "patient_name"),
                strOf(body, "visit_date"), strOf(body, "clinician_name"),
                strOf(body, "weight"), strOf(body, "blood_pressure"),
                intOf(body, "cd4_count"), longOf(body, "viral_load"),
                strOf(body, "vl_date"), strOf(body, "viral_load_status"),
                strOf(body, "regimen"), boolOf(body, "regimen_changed"), strOf(body, "change_reason"),
                strOf(body, "ois_present"), boolOf(body, "cotrimoxazole"), boolOf(body, "inh"),
                intOf(body, "days_dispensed"), intOf(body, "pills_returned"),
                strOf(body, "adherence_score"), strOf(body, "side_effects"),
                boolOf(body, "counseling_given"), strOf(body, "next_visit"), strOf(body, "notes")
            );
            Map<String, Object> entry = jdbc.queryForMap("SELECT * FROM art_clinic_visits WHERE visit_id = ?", visitId);
            return Map.of("success", true, "entry", entry);
        } catch (Exception e) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Failed to save ART visit: " + e.getMessage());
        }
    }

    // ===================================================================
    // Dental Clinic
    // ===================================================================

    @GetMapping("/dental")
    public List<Map<String, Object>> getDentalRecords(HttpServletRequest req) {
        requirePermission(req, "dental.view");
        try {
            return jdbc.queryForList("SELECT * FROM dental_records ORDER BY created_at DESC");
        } catch (Exception e) { return List.of(); }
    }

    @PostMapping("/dental")
    public Map<String, Object> createDentalRecord(HttpServletRequest req, @RequestBody Map<String, Object> body) {
        requirePermission(req, "dental.view");
        String visitId = "DENT-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
        try {
            jdbc.update("""
                INSERT INTO dental_records
                (visit_id,patient_id,patient_name,visit_date,dentist_name,chief_complaint,
                 teeth_affected,diagnosis,treatment_performed,local_anesthetic,anesthetic_type,
                 extractions_done,fillings_placed,scaling_done,xray_taken,xray_findings,
                 medications,referral_needed,referral_reason,next_appointment,notes,status)
                VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,'completed')
                """,
                visitId, strOf(body, "patient_id"), strOf(body, "patient_name"),
                strOf(body, "visit_date"), strOf(body, "dentist_name"), strOf(body, "chief_complaint"),
                strOf(body, "teeth_affected"), strOf(body, "diagnosis"), strOf(body, "treatment_performed"),
                boolOf(body, "local_anesthetic"), strOf(body, "anesthetic_type"),
                strOf(body, "extractions_done"), strOf(body, "fillings_placed"),
                boolOf(body, "scaling_done"), boolOf(body, "xray_taken"), strOf(body, "xray_findings"),
                strOf(body, "medications"), boolOf(body, "referral_needed"), strOf(body, "referral_reason"),
                strOf(body, "next_appointment"), strOf(body, "notes")
            );
            Map<String, Object> entry = jdbc.queryForMap("SELECT * FROM dental_records WHERE visit_id = ?", visitId);
            return Map.of("success", true, "entry", entry);
        } catch (Exception e) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Failed to save dental record: " + e.getMessage());
        }
    }

    // ===================================================================
    // Eye Clinic
    // ===================================================================

    @GetMapping("/eye-clinic")
    public List<Map<String, Object>> getEyeRecords(HttpServletRequest req) {
        requirePermission(req, "eye.view");
        try {
            return jdbc.queryForList("SELECT * FROM eye_clinic_records ORDER BY created_at DESC");
        } catch (Exception e) { return List.of(); }
    }

    @PostMapping("/eye-clinic")
    public Map<String, Object> createEyeRecord(HttpServletRequest req, @RequestBody Map<String, Object> body) {
        requirePermission(req, "eye.view");
        String visitId = "EYE-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
        try {
            jdbc.update("""
                INSERT INTO eye_clinic_records
                (visit_id,patient_id,patient_name,visit_date,optometrist_name,chief_complaint,
                 va_right_unaided,va_left_unaided,va_right_corrected,va_left_corrected,
                 refraction_right_sph,refraction_right_cyl,refraction_right_axis,
                 refraction_left_sph,refraction_left_cyl,refraction_left_axis,
                 iop_right,iop_left,colour_vision,slit_lamp_findings,fundoscopy,
                 diagnosis,spectacles_prescribed,spectacles_type,treatment,
                 referral_needed,referral_reason,next_review,notes,status)
                VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,'completed')
                """,
                visitId, strOf(body, "patient_id"), strOf(body, "patient_name"),
                strOf(body, "visit_date"), strOf(body, "optometrist_name"), strOf(body, "chief_complaint"),
                strOf(body, "va_right_unaided"), strOf(body, "va_left_unaided"),
                strOf(body, "va_right_corrected"), strOf(body, "va_left_corrected"),
                strOf(body, "refraction_right_sph"), strOf(body, "refraction_right_cyl"), strOf(body, "refraction_right_axis"),
                strOf(body, "refraction_left_sph"), strOf(body, "refraction_left_cyl"), strOf(body, "refraction_left_axis"),
                strOf(body, "iop_right"), strOf(body, "iop_left"), strOf(body, "colour_vision"),
                strOf(body, "slit_lamp_findings"), strOf(body, "fundoscopy"),
                strOf(body, "diagnosis"), boolOf(body, "spectacles_prescribed"), strOf(body, "spectacles_type"),
                strOf(body, "treatment"), boolOf(body, "referral_needed"), strOf(body, "referral_reason"),
                strOf(body, "next_review"), strOf(body, "notes")
            );
            Map<String, Object> entry = jdbc.queryForMap("SELECT * FROM eye_clinic_records WHERE visit_id = ?", visitId);
            return Map.of("success", true, "entry", entry);
        } catch (Exception e) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Failed to save eye clinic record: " + e.getMessage());
        }
    }

    // ===================================================================
    // STI Clinic
    // ===================================================================

    @GetMapping("/sti")
    public List<Map<String, Object>> getStiRecords(HttpServletRequest req) {
        requirePermission(req, "sti.view");
        try {
            return jdbc.queryForList("SELECT * FROM sti_records ORDER BY created_at DESC");
        } catch (Exception e) { return List.of(); }
    }

    @PostMapping("/sti")
    public Map<String, Object> createStiRecord(HttpServletRequest req, @RequestBody Map<String, Object> body) {
        requirePermission(req, "sti.view");
        String visitId = "STI-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
        try {
            jdbc.update("""
                INSERT INTO sti_records
                (visit_id,patient_id,patient_name,visit_date,clinician_name,chief_complaint,
                 syndrome_classification,lab_tests_done,hiv_test,syphilis_test,hepatitis_b,
                 diagnosis,treatment_protocol,medications_given,partner_notification,partner_treated,
                 condoms_provided,hiv_counseling_given,adherence_counseling_given,follow_up_date,notes,status)
                VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,'completed')
                """,
                visitId, strOf(body, "patient_id"), strOf(body, "patient_name"),
                strOf(body, "visit_date"), strOf(body, "clinician_name"), strOf(body, "chief_complaint"),
                strOf(body, "syndrome_classification"), strOf(body, "lab_tests_done"),
                strOf(body, "hiv_test"), strOf(body, "syphilis_test"), strOf(body, "hepatitis_b"),
                strOf(body, "diagnosis"), strOf(body, "treatment_protocol"), strOf(body, "medications_given"),
                boolOf(body, "partner_notification"), boolOf(body, "partner_treated"),
                intOf(body, "condoms_provided"),
                boolOf(body, "hiv_counseling_given"), boolOf(body, "adherence_counseling_given"),
                strOf(body, "follow_up_date"), strOf(body, "notes")
            );
            Map<String, Object> entry = jdbc.queryForMap("SELECT * FROM sti_records WHERE visit_id = ?", visitId);
            return Map.of("success", true, "entry", entry);
        } catch (Exception e) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Failed to save STI record: " + e.getMessage());
        }
    }

    // ===================================================================
    // Physiotherapy
    // ===================================================================

    @GetMapping("/physio/referrals")
    public List<Map<String, Object>> getPhysioReferrals(HttpServletRequest req) {
        requirePermission(req, "physio.view");
        try {
            return jdbc.queryForList("SELECT * FROM physio_referrals ORDER BY created_at DESC");
        } catch (Exception e) { return List.of(); }
    }

    @PostMapping("/physio/referrals")
    public Map<String, Object> createPhysioReferral(HttpServletRequest req, @RequestBody Map<String, Object> body) {
        requirePermission(req, "physio.view");
        String referralId = "PHYS-R-" + UUID.randomUUID().toString().substring(0, 7).toUpperCase();
        try {
            jdbc.update("""
                INSERT INTO physio_referrals
                (referral_id,patient_id,patient_name,referred_by,referral_date,diagnosis,
                 referral_reason,urgency,assigned_therapist,first_appointment,status)
                VALUES (?,?,?,?,?,?,?,?,?,?,'pending')
                """,
                referralId, strOf(body, "patient_id"), strOf(body, "patient_name"),
                strOf(body, "referred_by"), strOf(body, "referral_date"), strOf(body, "diagnosis"),
                strOf(body, "referral_reason"), strOf(body, "urgency"),
                strOf(body, "assigned_therapist"), strOf(body, "first_appointment")
            );
            Map<String, Object> entry = jdbc.queryForMap("SELECT * FROM physio_referrals WHERE referral_id = ?", referralId);
            return Map.of("success", true, "entry", entry);
        } catch (Exception e) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Failed to save physio referral: " + e.getMessage());
        }
    }

    @GetMapping("/physio/sessions")
    public List<Map<String, Object>> getPhysioSessions(HttpServletRequest req) {
        requirePermission(req, "physio.view");
        try {
            return jdbc.queryForList("SELECT * FROM physio_sessions ORDER BY created_at DESC");
        } catch (Exception e) { return List.of(); }
    }

    @PostMapping("/physio/sessions")
    public Map<String, Object> createPhysioSession(HttpServletRequest req, @RequestBody Map<String, Object> body) {
        requirePermission(req, "physio.view");
        String sessionId = "PHYS-S-" + UUID.randomUUID().toString().substring(0, 7).toUpperCase();
        try {
            jdbc.update("""
                INSERT INTO physio_sessions
                (session_id,patient_id,patient_name,session_date,therapist_name,session_number,
                 diagnosis,treatment_given,pain_score_before,pain_score_after,
                 mobility_before,mobility_after,exercises_done,home_program,progress_notes,next_session,status)
                VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,'completed')
                """,
                sessionId, strOf(body, "patient_id"), strOf(body, "patient_name"),
                strOf(body, "session_date"), strOf(body, "therapist_name"), intOf(body, "session_number"),
                strOf(body, "diagnosis"), strOf(body, "treatment_given"),
                intOf(body, "pain_score_before"), intOf(body, "pain_score_after"),
                strOf(body, "mobility_before"), strOf(body, "mobility_after"),
                strOf(body, "exercises_done"), strOf(body, "home_program"),
                strOf(body, "progress_notes"), strOf(body, "next_session")
            );
            Map<String, Object> entry = jdbc.queryForMap("SELECT * FROM physio_sessions WHERE session_id = ?", sessionId);
            return Map.of("success", true, "entry", entry);
        } catch (Exception e) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Failed to save physio session: " + e.getMessage());
        }
    }

    // ===================================================================
    // Pharmacy Dispensing Queue & Log
    // ===================================================================

    @GetMapping("/pharmacy/dispensing-queue")
    public List<Map<String, Object>> getDispensingQueue(HttpServletRequest req) {
        requireAnyPermission(req, List.of("pharmacy.view", "pharmacy.dispense"));
        return dataStore.getPrescriptions().stream()
            .filter(p -> "pending".equalsIgnoreCase(stringValue(p.getStatus())))
            .map(this::toPrescriptionResponse)
            .collect(Collectors.toList());
    }

    @GetMapping("/pharmacy/dispensing-log")
    public List<Map<String, Object>> getDispensingLog(HttpServletRequest req) {
        requireAnyPermission(req, List.of("pharmacy.view", "pharmacy.dispense"));
        try {
            return jdbc.queryForList("SELECT * FROM pharmacy_dispensing_log ORDER BY created_at DESC LIMIT 500");
        } catch (Exception e) { return List.of(); }
    }

    @PostMapping("/pharmacy/dispensing-log")
    public Map<String, Object> createDispensingLog(HttpServletRequest req, @RequestBody Map<String, Object> body) {
        requireAnyPermission(req, List.of("pharmacy.view", "pharmacy.dispense"));
        String logId = "LOG-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
        try {
            jdbc.update("""
                INSERT INTO pharmacy_dispensing_log
                (log_id,rx_id,patient_id,patient_name,patient_type,drug_name,
                 quantity_requested,quantity_dispensed,unit,dispensing_instructions,
                 pharmacist_name,pharmacist_notes,is_controlled,substituted,substitute_reason,dispensed_at)
                VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,CURRENT_TIMESTAMP)
                """,
                logId, strOf(body, "rx_id"), strOf(body, "patient_id"), strOf(body, "patient_name"),
                strOf(body, "patient_type"), strOf(body, "drug_name"),
                intOf(body, "quantity_requested"), intOf(body, "quantity_dispensed"),
                strOf(body, "unit"), strOf(body, "dispensing_instructions"),
                strOf(body, "pharmacist_name"), strOf(body, "pharmacist_notes"),
                boolOf(body, "is_controlled"), boolOf(body, "substituted"), strOf(body, "substitute_reason")
            );
            Map<String, Object> entry = jdbc.queryForMap("SELECT * FROM pharmacy_dispensing_log WHERE log_id = ?", logId);
            return Map.of("success", true, "entry", entry);
        } catch (Exception e) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Failed to save dispensing log: " + e.getMessage());
        }
    }

    @GetMapping("/pharmacy/controlled-register")
    public List<Map<String, Object>> getControlledRegister(HttpServletRequest req) {
        requireAnyPermission(req, List.of("pharmacy.view", "pharmacy.dispense"));
        try {
            return jdbc.queryForList("SELECT * FROM controlled_drugs_register ORDER BY created_at DESC LIMIT 500");
        } catch (Exception e) { return List.of(); }
    }

    @PostMapping("/pharmacy/dispense/{rxId}")
    public Map<String, Object> dispenseFromQueue(HttpServletRequest req,
                                                  @PathVariable String rxId,
                                                  @RequestBody Map<String, Object> body) {
        requireAnyPermission(req, List.of("pharmacy.view", "pharmacy.dispense"));
        com.unza.clinic.model.Prescription prescription = dataStore.getPrescriptions().stream()
            .filter(p -> rxId.equalsIgnoreCase(stringValue(p.getRxId())))
            .findFirst()
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Prescription not found"));

        prescription.setStatus("dispensed");
        prescription.setDispensedBy(strOf(body, "pharmacist_name"));
        prescription.setDispensedAt(LocalDateTime.now());
        prescription.setPharmacistNotes(strOf(body, "pharmacist_notes"));
        dataStore.updatePrescription(prescription);

        boolean isControlled = boolOf(body, "is_controlled");
        if (isControlled) {
            String entryId = "CDR-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
            try {
                jdbc.update("""
                    INSERT INTO controlled_drugs_register
                    (entry_id,drug_name,schedule,rx_id,patient_id,patient_name,
                     quantity_dispensed,unit,dispensed_by,authorized_by,date_dispensed,notes)
                    VALUES (?,?,?,?,?,?,?,?,?,?,?,?)
                    """,
                    entryId, strOf(body, "drug_name"), strOf(body, "schedule"),
                    rxId, stringValue(prescription.getPatientId()), stringValue(prescription.getPatientName()),
                    intOf(body, "quantity_dispensed"), strOf(body, "unit"),
                    strOf(body, "pharmacist_name"), strOf(body, "authorized_by"),
                    LocalDate.now().toString(), strOf(body, "notes")
                );
            } catch (Exception ignored) {}
        }

        writeAuditLog("System", "Pharmacist", "dispense", "Dispensed Rx " + rxId, "127.0.0.1");
        return Map.of("success", true, "entry", toPrescriptionResponse(prescription));
    }

    // ===================================================================
    // Private helpers for specialized clinic endpoints
    // ===================================================================

    private String strOf(Map<String, Object> body, String key) {
        Object val = body.get(key);
        return val == null ? null : val.toString();
    }

    private Integer intOf(Map<String, Object> body, String key) {
        Object val = body.get(key);
        if (val == null) return null;
        if (val instanceof Number n) return n.intValue();
        try { return Integer.parseInt(val.toString()); } catch (Exception e) { return null; }
    }

    private Long longOf(Map<String, Object> body, String key) {
        Object val = body.get(key);
        if (val == null) return null;
        if (val instanceof Number n) return n.longValue();
        try { return Long.parseLong(val.toString()); } catch (Exception e) { return null; }
    }

    private Boolean boolOf(Map<String, Object> body, String key) {
        Object val = body.get(key);
        if (val == null) return false;
        if (val instanceof Boolean b) return b;
        return Boolean.parseBoolean(val.toString());
    }
}
