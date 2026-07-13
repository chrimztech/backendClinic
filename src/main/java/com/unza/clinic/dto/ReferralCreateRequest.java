package com.unza.clinic.dto;

import jakarta.validation.constraints.NotBlank;

public record ReferralCreateRequest(
        @NotBlank String patientId,
        @NotBlank String patientName,
        @NotBlank String fromDept,
        @NotBlank String toDept,
        String referredBy,
        @NotBlank String reason,
        @NotBlank String urgency
) {
}
