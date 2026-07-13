package com.unza.clinic.dto;

import jakarta.validation.constraints.NotBlank;

public record InsuranceClaimCreateRequest(
        @NotBlank String patient,
        @NotBlank String insurer,
        @NotBlank String service,
        Double amount
) {
}
