package com.unza.clinic.dto;

import jakarta.validation.constraints.NotBlank;

public record ClinicalFormCreateRequest(
        @NotBlank String formType,
        @NotBlank String title,
        @NotBlank String patientId,
        @NotBlank String patientName,
        @NotBlank String department,
        String encounterId,
        String status,
        String createdBy,
        @NotBlank String payloadJson
) {
}
