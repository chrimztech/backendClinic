package com.unza.clinic.dto;

import jakarta.validation.constraints.NotBlank;

public record LabTestCreateRequest(
        @NotBlank String patientId,
        @NotBlank String patientName,
        @NotBlank String test,
        String category,
        String section,
        String sampleType,
        String clinicalNotes,
        String requestedBy
) {
}
