package com.unza.clinic.service;

import com.unza.clinic.model.AppUser;
import com.unza.clinic.model.Department;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/** Resolves section-head authority without granting system-wide administrator access. */
@Service
public class DepartmentAccessService {

    private static final Set<String> HEAD_BASE_PERMISSIONS = Set.of(
            "dashboard.view", "sections.view", "notifications.view", "patients.view",
            "staff.view", "staff.manage", "schedules.view", "schedules.manage",
            "records.view", "forms.view", "prescriptions.view", "referrals.view",
            "reports.view", "security.report"
    );

    private final ClinicDataStore dataStore;

    public DepartmentAccessService(ClinicDataStore dataStore) {
        this.dataStore = dataStore;
    }

    public boolean isAdmin(AppUser user) {
        return user != null && equalsValue(user.getRole(), "Admin");
    }

    public Department headedDepartment(AppUser user) {
        if (user == null) return null;
        return dataStore.getDepartments().stream()
                .filter(department -> matchesHead(department, user))
                .findFirst()
                .orElseGet(() -> equalsValue(user.getRole(), "Department Head")
                        ? dataStore.getDepartments().stream()
                            .filter(department -> departmentMatches(department, user.getDepartment()))
                            .findFirst().orElse(null)
                        : null);
    }

    public boolean isDepartmentHead(AppUser user) {
        return headedDepartment(user) != null;
    }

    public boolean canManageDepartment(AppUser user, String departmentValue) {
        if (isAdmin(user)) return true;
        Department headed = headedDepartment(user);
        return headed != null && departmentMatches(headed, departmentValue);
    }

    public boolean canManageStage(AppUser user, String stageValue) {
        if (isAdmin(user)) return true;
        Department headed = headedDepartment(user);
        if (headed == null) return false;
        return aliasesForDepartment(headed).contains(normalize(stageValue));
    }

    public List<String> expandPermissions(AppUser user, List<String> basePermissions) {
        LinkedHashSet<String> permissions = new LinkedHashSet<>(basePermissions == null ? List.of() : basePermissions);
        Department headed = headedDepartment(user);
        if (headed == null) return new ArrayList<>(permissions);

        permissions.addAll(HEAD_BASE_PERMISSIONS);
        Set<String> aliases = aliasesForDepartment(headed);
        if (aliases.contains("RECEPTION") || aliases.contains("ACCOUNTS")) {
            permissions.addAll(List.of("walkin.view", "billing.view", "billing.create", "billing.payments", "insurance.view"));
        }
        if (aliases.contains("TRIAGE")) permissions.addAll(List.of("triage.view", "emergency.view"));
        if (aliases.contains("CONSULTATION")) permissions.addAll(List.of("walkin.view", "triage.view", "emergency.view", "admissions.view", "wards.view"));
        if (aliases.contains("LABORATORY")) permissions.addAll(List.of("laboratory.view", "bloodbank.view"));
        if (aliases.contains("RADIOLOGY")) permissions.add("radiology.view");
        if (aliases.contains("PHARMACY")) permissions.addAll(List.of("pharmacy.view", "pharmacy.dispense", "inventory.view", "suppliers.view"));
        if (aliases.contains("MCH")) permissions.addAll(List.of("mch.view", "art.view", "vct.view"));
        if (aliases.contains("ART")) permissions.addAll(List.of("art.view", "vct.view"));
        if (aliases.contains("VCT")) permissions.addAll(List.of("vct.view", "art.view", "counseling.view"));
        if (aliases.contains("DENTAL")) permissions.add("dental.view");
        if (aliases.contains("EYE")) permissions.add("eye.view");
        if (aliases.contains("STI")) permissions.addAll(List.of("sti.view", "vct.view"));
        if (aliases.contains("PHYSIOTHERAPY")) permissions.add("physio.view");
        if (aliases.contains("COUNSELING")) permissions.add("counseling.view");
        if (aliases.contains("INPATIENT")) permissions.addAll(List.of("admissions.view", "wards.view"));
        return new ArrayList<>(permissions);
    }

    private boolean matchesHead(Department department, AppUser user) {
        if (department == null) return false;
        String linkedId = normalize(department.getHeadUserId());
        boolean linked = !linkedId.isBlank() && (linkedId.equals(normalize(user.getUserId()))
                || linkedId.equals(normalize(user.getStaffId()))
                || linkedId.equals(normalize(user.getEmail()))
                || linkedId.equals(normalize(user.getId())));
        boolean legacyNameMatch = !normalize(department.getHead()).isBlank()
                && normalize(department.getHead()).equals(normalize(user.getName()));
        return linked || legacyNameMatch;
    }

    private boolean departmentMatches(Department department, String value) {
        String normalized = normalize(value);
        return !normalized.isBlank() && (normalize(department.getCode()).equals(normalized)
                || normalize(department.getName()).equals(normalized)
                || aliasesForDepartment(department).contains(normalized));
    }

    private Set<String> aliasesForDepartment(Department department) {
        String combined = normalize(stringValue(department.getCode()) + " " + stringValue(department.getName()));
        LinkedHashSet<String> aliases = new LinkedHashSet<>();
        aliases.add(normalize(department.getCode()));
        aliases.add(normalize(department.getName()));
        if (containsAny(combined, "CONSULT", "CLINICAL", "OPD")) aliases.add("CONSULTATION");
        if (containsAny(combined, "RECEPTION", "ACCOUNTS", "BILLING", "CASH")) aliases.addAll(List.of("RECEPTION", "ACCOUNTS"));
        if (combined.contains("TRIAGE")) aliases.add("TRIAGE");
        if (containsAny(combined, "LAB", "PATHOLOGY")) aliases.add("LABORATORY");
        if (containsAny(combined, "RADIOLOGY", "IMAGING", "X_RAY")) aliases.add("RADIOLOGY");
        if (combined.contains("PHARM")) aliases.add("PHARMACY");
        if (containsAny(combined, "MCH", "MATERN", "CHILD")) aliases.add("MCH");
        if (containsAny(combined, "ART", "HIV_CLINIC")) aliases.add("ART");
        if (containsAny(combined, "VCT", "HIV_TEST")) aliases.add("VCT");
        if (combined.contains("DENT")) aliases.add("DENTAL");
        if (containsAny(combined, "EYE", "OPTOM")) aliases.add("EYE");
        if (combined.contains("STI")) aliases.add("STI");
        if (combined.contains("PHYSIO")) aliases.add("PHYSIOTHERAPY");
        if (containsAny(combined, "COUNSEL", "COUNSELL")) aliases.add("COUNSELING");
        if (containsAny(combined, "WARD", "INPATIENT", "ADMISSION")) aliases.add("INPATIENT");
        return aliases;
    }

    private boolean containsAny(String value, String... fragments) {
        for (String fragment : fragments) if (value.contains(fragment)) return true;
        return false;
    }

    private boolean equalsValue(String left, String right) { return normalize(left).equals(normalize(right)); }
    private String stringValue(Object value) { return value == null ? "" : String.valueOf(value); }
    private String normalize(Object value) {
        return stringValue(value).trim().replace('-', '_').replace(' ', '_').toUpperCase(Locale.ROOT);
    }
}
