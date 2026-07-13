package com.unza.clinic.dto;

import jakarta.validation.constraints.NotBlank;

public record EncounterCreateRequest(
        @NotBlank String patientId,
        @NotBlank String patientName,
        String patientType,
        String createdBy,
        String currentStage,
        String pendingActions,
        String notes
) {
}
