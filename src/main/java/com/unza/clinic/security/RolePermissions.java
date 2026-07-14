package com.unza.clinic.security;

import java.util.List;
import java.util.Map;

/**
 * Single source of truth for each role's default permission set.
 * Used by both JwtAuthenticationFilter (request-time authority/permission gate)
 * and ApiController (permission fallback when a user has no custom permissionsJson),
 * so the two enforcement paths can never drift out of sync with each other.
 */
public final class RolePermissions {

    private RolePermissions() {}

    private static final Map<String, List<String>> DEFAULTS = Map.ofEntries(
            Map.entry("Admin", List.of(
                    "dashboard.view", "sections.view", "notifications.view", "patients.view", "walkin.view",
                    "triage.view", "emergency.view", "staff.view", "schedules.view", "records.view",
                    "forms.view", "prescriptions.view", "referrals.view", "laboratory.view", "radiology.view",
                    "bloodbank.view", "pharmacy.view", "pharmacy.dispense", "suppliers.view", "inventory.view", "wards.view",
                    "admissions.view", "billing.view", "billing.create", "billing.payments", "insurance.view",
                    "counseling.view", "mch.view", "art.view", "dental.view", "eye.view", "sti.view", "physio.view",
                    "departments.manage", "attendance.view", "users.manage", "users.reset_password",
                    "patients.manage", "staff.manage",
                    "audit.view", "audit.export", "reports.view", "settings.view", "settings.manage",
                    "backup.export", "tariffs.manage"
            )),
            Map.entry("Doctor", List.of(
                    "dashboard.view", "sections.view", "notifications.view", "patients.view", "patients.manage", "walkin.view",
                    "triage.view", "emergency.view", "staff.view", "schedules.view", "records.view",
                    "forms.view", "prescriptions.view", "referrals.view", "counseling.view", "laboratory.view", "radiology.view",
                    "bloodbank.view", "wards.view", "admissions.view", "reports.view",
                    "mch.view", "art.view", "dental.view", "eye.view", "sti.view", "physio.view"
            )),
            Map.entry("Nurse", List.of(
                    "dashboard.view", "sections.view", "notifications.view", "patients.view", "walkin.view",
                    "triage.view", "emergency.view", "staff.view", "records.view", "forms.view", "counseling.view",
                    "wards.view", "admissions.view", "mch.view"
            )),
            Map.entry("MCH Nurse", List.of(
                    "dashboard.view", "sections.view", "notifications.view", "patients.view", "walkin.view",
                    "triage.view", "records.view", "forms.view", "mch.view", "referrals.view"
            )),
            Map.entry("Receptionist", List.of(
                    "dashboard.view", "sections.view", "notifications.view", "patients.view", "walkin.view",
                    "schedules.view", "forms.view", "billing.view", "billing.create", "billing.payments", "insurance.view"
            )),
            Map.entry("Cashier", List.of(
                    "dashboard.view", "sections.view", "notifications.view", "patients.view",
                    "billing.view", "billing.create", "billing.payments", "insurance.view", "tariffs.manage"
            )),
            Map.entry("Records Clerk", List.of(
                    "dashboard.view", "sections.view", "notifications.view", "patients.view", "walkin.view",
                    "records.view", "forms.view", "schedules.view"
            )),
            Map.entry("Pharmacist", List.of(
                    "dashboard.view", "sections.view", "notifications.view", "patients.view",
                    "forms.view", "pharmacy.view", "pharmacy.dispense", "suppliers.view", "inventory.view",
                    "prescriptions.view", "billing.view", "billing.create", "billing.payments"
            )),
            Map.entry("Lab Technician", List.of(
                    "dashboard.view", "sections.view", "notifications.view", "patients.view",
                    "forms.view", "laboratory.view", "radiology.view", "bloodbank.view", "reports.view"
            )),
            Map.entry("Radiographer", List.of(
                    "dashboard.view", "sections.view", "notifications.view", "patients.view",
                    "forms.view", "radiology.view", "reports.view"
            )),
            Map.entry("Counselor", List.of(
                    "dashboard.view", "sections.view", "notifications.view", "patients.view",
                    "records.view", "forms.view", "counseling.view", "referrals.view",
                    "art.view", "sti.view"
            )),
            Map.entry("Dentist", List.of(
                    "dashboard.view", "sections.view", "notifications.view", "patients.view", "walkin.view",
                    "records.view", "forms.view", "prescriptions.view", "referrals.view", "dental.view"
            )),
            Map.entry("Optometrist", List.of(
                    "dashboard.view", "sections.view", "notifications.view", "patients.view", "walkin.view",
                    "records.view", "forms.view", "prescriptions.view", "referrals.view", "eye.view"
            )),
            Map.entry("Physiotherapist", List.of(
                    "dashboard.view", "sections.view", "notifications.view", "patients.view", "walkin.view",
                    "records.view", "forms.view", "referrals.view", "physio.view"
            ))
    );

    public static List<String> forRole(String role) {
        if (role == null) {
            return List.of("dashboard.view");
        }
        for (Map.Entry<String, List<String>> entry : DEFAULTS.entrySet()) {
            if (entry.getKey().equalsIgnoreCase(role.trim())) {
                return entry.getValue();
            }
        }
        return List.of("dashboard.view");
    }
}
