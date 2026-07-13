package com.unza.clinic.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record BloodIssueRequest(
        @NotBlank String patientName,
        String patientId,
        @NotNull Integer units
) {
}
