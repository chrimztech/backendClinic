package com.unza.clinic.service;

import org.junit.jupiter.api.Test;

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
}
