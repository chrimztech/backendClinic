package com.unza.clinic.dto;

import jakarta.validation.constraints.NotBlank;

public record AdmissionCreateRequest(
        @NotBlank String patientId,
        @NotBlank String patientName,
        @NotBlank String ward,
        @NotBlank String bed,
        @NotBlank String doctor,
        @NotBlank String admittedOn,
        @NotBlank String diagnosis
) {
}
