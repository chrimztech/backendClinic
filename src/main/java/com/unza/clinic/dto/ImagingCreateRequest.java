package com.unza.clinic.dto;

import jakarta.validation.constraints.NotBlank;

public record ImagingCreateRequest(
        @NotBlank String patientId,
        @NotBlank String patientName,
        @NotBlank String type,
        @NotBlank String bodyPart,
        String requestedBy
) {
}
