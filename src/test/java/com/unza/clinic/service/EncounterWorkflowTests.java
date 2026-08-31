package com.unza.clinic.service;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class EncounterWorkflowTests {

    @Test
    void receptionCanRouteToTriageAndSpecialistDepartments() {
        assertTrue(EncounterWorkflow.canTransition("RECEPTION", "TRIAGE"));
        assertTrue(EncounterWorkflow.canTransition("reception", "dental"));
        assertTrue(EncounterWorkflow.canTransition("RECEPTION", "ART"));
        assertFalse(EncounterWorkflow.canTransition("RECEPTION", "LABORATORY"));
    }

    @Test
    void specialistDepartmentsCanReturnToConsultationOrFinishTheVisit() {
        assertTrue(EncounterWorkflow.canTransition("EYE", "CONSULTATION"));
        assertTrue(EncounterWorkflow.canTransition("PHYSIOTHERAPY", "CHECKOUT"));
        assertFalse(EncounterWorkflow.canTransition("CHECKOUT", "RECEPTION"));
    }

    @Test
    void codesAreNormalizedAndUnknownValuesAreRejected() {
        assertEquals("IN_PROGRESS", EncounterWorkflow.normalizeQueueStatus("in progress"));
        assertEquals("PHYSIOTHERAPY", EncounterWorkflow.normalizeStage("physiotherapy"));
        assertThrows(IllegalArgumentException.class, () -> EncounterWorkflow.normalizeStage("unknown-room"));
        assertThrows(IllegalArgumentException.class, () -> EncounterWorkflow.normalizePriority("whenever"));
    }

    @Test
    void queuesHaveDepartmentPermissionsAndResponseTargets() {
        assertEquals("walkin.view", EncounterWorkflow.permissionForStage("RECEPTION"));
        assertEquals("laboratory.view", EncounterWorkflow.permissionForStage("LABORATORY"));
        assertEquals(List.of("pharmacy.view", "pharmacy.dispense"),
                EncounterWorkflow.permissionsForStage("PHARMACY"));
        assertEquals(List.of("records.view", "walkin.view", "billing.view"),
                EncounterWorkflow.permissionsForStage("CHECKOUT"));
        assertEquals(5, EncounterWorkflow.slaMinutesForStage("EMERGENCY"));
        assertEquals(30, EncounterWorkflow.slaMinutesForStage("CONSULTATION"));
    }
}
