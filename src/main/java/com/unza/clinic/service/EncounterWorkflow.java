package com.unza.clinic.service;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.Collections;

/**
 * Canonical encounter stages, queue states, and allowed routing paths.
 * Keeping this outside the controller makes the workflow independently testable.
 */
public final class EncounterWorkflow {

    private EncounterWorkflow() {}

    public static final String RECEPTION = "RECEPTION";
    public static final String CHECKOUT = "CHECKOUT";

    private static final Map<String, List<String>> TRANSITIONS;
    private static final Map<String, List<String>> STAGE_TASKS;
    private static final Map<String, Integer> STAGE_SLA_MINUTES;
    private static final Set<String> QUEUE_STATUSES = Set.of("WAITING", "IN_PROGRESS", "ON_HOLD");
    private static final Set<String> PRIORITIES = Set.of("ROUTINE", "PRIORITY", "URGENT", "EMERGENCY");

    static {
        Map<String, List<String>> transitions = new LinkedHashMap<>();
        transitions.put("RECEPTION", List.of("TRIAGE", "CONSULTATION", "EMERGENCY", "MCH", "VCT", "ART", "DENTAL", "EYE", "STI", "PHYSIOTHERAPY", "COUNSELING"));
        transitions.put("TRIAGE", List.of("CONSULTATION", "EMERGENCY", "MCH", "VCT", "ART", "DENTAL", "EYE", "STI", "PHYSIOTHERAPY", "COUNSELING", "INPATIENT"));
        transitions.put("CONSULTATION", List.of("TRIAGE", "LABORATORY", "RADIOLOGY", "PHARMACY", "ACCOUNTS", "MCH", "VCT", "ART", "DENTAL", "EYE", "STI", "PHYSIOTHERAPY", "COUNSELING", "EMERGENCY", "INPATIENT", "CHECKOUT"));
        transitions.put("EMERGENCY", List.of("CONSULTATION", "LABORATORY", "RADIOLOGY", "PHARMACY", "INPATIENT", "CHECKOUT"));
        transitions.put("LABORATORY", List.of("CONSULTATION", "PHARMACY", "ACCOUNTS", "CHECKOUT"));
        transitions.put("RADIOLOGY", List.of("CONSULTATION", "PHARMACY", "ACCOUNTS", "CHECKOUT"));
        transitions.put("PHARMACY", List.of("CONSULTATION", "ACCOUNTS", "CHECKOUT"));
        transitions.put("ACCOUNTS", List.of("PHARMACY", "CHECKOUT"));
        transitions.put("MCH", specialistDestinations());
        transitions.put("VCT", List.of("ART", "CONSULTATION", "COUNSELING", "ACCOUNTS", "CHECKOUT"));
        transitions.put("ART", specialistDestinations());
        transitions.put("DENTAL", specialistDestinations());
        transitions.put("EYE", specialistDestinations());
        transitions.put("STI", specialistDestinations());
        transitions.put("PHYSIOTHERAPY", specialistDestinations());
        transitions.put("COUNSELING", specialistDestinations());
        transitions.put("INPATIENT", List.of("CONSULTATION", "PHARMACY", "ACCOUNTS", "CHECKOUT"));
        transitions.put("CHECKOUT", List.of());
        TRANSITIONS = Collections.unmodifiableMap(new LinkedHashMap<>(transitions));

        Map<String, List<String>> tasks = new LinkedHashMap<>();
        tasks.put("RECEPTION", List.of("Identity verification", "Department routing"));
        tasks.put("TRIAGE", List.of("Triage assessment", "Vital signs recorded"));
        tasks.put("CONSULTATION", List.of("Clinician consultation", "Clinical notes completed"));
        tasks.put("EMERGENCY", List.of("Emergency assessment", "Emergency care documented"));
        tasks.put("LABORATORY", List.of("Laboratory investigation completed", "Results released"));
        tasks.put("RADIOLOGY", List.of("Imaging completed", "Imaging report released"));
        tasks.put("PHARMACY", List.of("Prescription verified", "Medication dispensed"));
        tasks.put("ACCOUNTS", List.of("Payment clearance"));
        tasks.put("MCH", List.of("MCH review", "Maternal or child care documented"));
        tasks.put("VCT", List.of("Consent and pre-test counseling", "HIV test recorded", "Post-test counseling and ART linkage"));
        tasks.put("ART", List.of("ART review", "ART care documented"));
        tasks.put("DENTAL", List.of("Dental assessment", "Dental care documented"));
        tasks.put("EYE", List.of("Eye assessment", "Eye care documented"));
        tasks.put("STI", List.of("STI assessment", "STI care documented"));
        tasks.put("PHYSIOTHERAPY", List.of("Physiotherapy assessment", "Treatment documented"));
        tasks.put("COUNSELING", List.of("Counseling session", "Counseling outcome documented"));
        tasks.put("INPATIENT", List.of("Admission documentation", "Ward handover"));
        tasks.put("CHECKOUT", List.of());
        STAGE_TASKS = Collections.unmodifiableMap(new LinkedHashMap<>(tasks));

        Map<String, Integer> slaMinutes = new LinkedHashMap<>();
        slaMinutes.put("RECEPTION", 15);
        slaMinutes.put("TRIAGE", 15);
        slaMinutes.put("CONSULTATION", 30);
        slaMinutes.put("EMERGENCY", 5);
        slaMinutes.put("LABORATORY", 45);
        slaMinutes.put("RADIOLOGY", 60);
        slaMinutes.put("PHARMACY", 20);
        slaMinutes.put("ACCOUNTS", 15);
        slaMinutes.put("MCH", 30);
        slaMinutes.put("VCT", 30);
        slaMinutes.put("ART", 30);
        slaMinutes.put("DENTAL", 30);
        slaMinutes.put("EYE", 30);
        slaMinutes.put("STI", 30);
        slaMinutes.put("PHYSIOTHERAPY", 30);
        slaMinutes.put("COUNSELING", 30);
        slaMinutes.put("INPATIENT", 20);
        slaMinutes.put("CHECKOUT", 10);
        STAGE_SLA_MINUTES = Collections.unmodifiableMap(slaMinutes);
    }

    private static List<String> specialistDestinations() {
        return List.of("CONSULTATION", "LABORATORY", "RADIOLOGY", "PHARMACY", "ACCOUNTS", "INPATIENT", "CHECKOUT");
    }

    public static List<String> stages() {
        return List.copyOf(TRANSITIONS.keySet());
    }

    public static List<String> nextStages(String currentStage) {
        return TRANSITIONS.getOrDefault(normalizeStage(currentStage), List.of());
    }

    public static List<String> tasksForStage(String stage) {
        return STAGE_TASKS.getOrDefault(normalizeStage(stage), List.of());
    }

    /** Permission required to view or operate a department queue. */
    public static String permissionForStage(String stage) {
        return permissionsForStage(stage).get(0);
    }

    /** Any one of these permissions grants access to the department queue. */
    public static List<String> permissionsForStage(String stage) {
        return switch (normalizeStage(stage)) {
            case "RECEPTION" -> List.of("walkin.view");
            case "TRIAGE" -> List.of("triage.view");
            case "CONSULTATION" -> List.of("forms.view");
            case "EMERGENCY" -> List.of("emergency.view");
            case "LABORATORY" -> List.of("laboratory.view");
            case "RADIOLOGY" -> List.of("radiology.view");
            case "PHARMACY" -> List.of("pharmacy.view", "pharmacy.dispense");
            case "ACCOUNTS" -> List.of("billing.view", "billing.payments");
            case "MCH" -> List.of("mch.view");
            case "VCT" -> List.of("vct.view");
            case "ART" -> List.of("art.view");
            case "DENTAL" -> List.of("dental.view");
            case "EYE" -> List.of("eye.view");
            case "STI" -> List.of("sti.view");
            case "PHYSIOTHERAPY" -> List.of("physio.view");
            case "COUNSELING" -> List.of("counseling.view");
            case "INPATIENT" -> List.of("admissions.view", "wards.view");
            case "CHECKOUT" -> List.of("records.view", "walkin.view", "billing.view");
            default -> throw new IllegalArgumentException("Unknown encounter stage: " + stage);
        };
    }

    public static int slaMinutesForStage(String stage) {
        return STAGE_SLA_MINUTES.getOrDefault(normalizeStage(stage), 30);
    }

    public static boolean canTransition(String currentStage, String targetStage) {
        String current = normalizeStage(currentStage);
        String target = normalizeStage(targetStage);
        return current.equals(target) || TRANSITIONS.getOrDefault(current, List.of()).contains(target);
    }

    public static String normalizeStage(String input) {
        String value = normalizeCode(input);
        if (!TRANSITIONS.containsKey(value)) {
            throw new IllegalArgumentException("Unknown encounter stage: " + value);
        }
        return value;
    }

    public static String normalizeQueueStatus(String input) {
        String value = normalizeCode(input);
        if (!QUEUE_STATUSES.contains(value)) {
            throw new IllegalArgumentException("Unknown queue status: " + value);
        }
        return value;
    }

    public static String normalizePriority(String input) {
        String value = normalizeCode(input);
        if (!PRIORITIES.contains(value)) {
            throw new IllegalArgumentException("Unknown queue priority: " + value);
        }
        return value;
    }

    private static String normalizeCode(String input) {
        return String.valueOf(input == null ? "" : input)
                .trim()
                .replace('-', '_')
                .replace(' ', '_')
                .toUpperCase(Locale.ROOT);
    }
}
