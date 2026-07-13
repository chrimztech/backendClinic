package com.unza.clinic.dto;

import jakarta.validation.constraints.NotBlank;

public record DrugCreateRequest(
        @NotBlank String name,
        @NotBlank String category,
        @NotBlank String drugType,
        String batchNumber,
        Integer stock,
        Integer reorderLevel,
        @NotBlank String unit,
        @NotBlank String expiry,
        String storageLocation
) {
}
