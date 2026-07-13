package com.unza.clinic.dto;

import jakarta.validation.constraints.NotBlank;

public record ServiceTariffRequest(
        @NotBlank String tariffCode,
        @NotBlank String department,
        @NotBlank String category,
        @NotBlank String serviceName,
        @NotBlank String unitLabel,
        Double price,
        String status
) {
}
