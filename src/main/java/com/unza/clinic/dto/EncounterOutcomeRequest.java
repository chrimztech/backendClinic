package com.unza.clinic.dto;

public record EncounterOutcomeRequest(
        String outcome,
        String performedBy,
        String note
) {
}
