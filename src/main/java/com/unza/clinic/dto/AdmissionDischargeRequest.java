package com.unza.clinic.dto;

import jakarta.validation.constraints.NotBlank;

public record AdmissionDischargeRequest(
        @NotBlank String dischargeType,
        @NotBlank String dischargeSummary,
        String dischargedOn,
        @NotBlank String dischargedBy
) {
}
