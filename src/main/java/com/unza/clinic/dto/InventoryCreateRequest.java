package com.unza.clinic.dto;

import jakarta.validation.constraints.NotBlank;

public record InventoryCreateRequest(
        @NotBlank String name,
        @NotBlank String category,
        Integer quantity,
        @NotBlank String unit,
        Integer minStock,
        @NotBlank String location
) {
}
