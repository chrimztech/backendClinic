package com.unza.clinic.dto;

import jakarta.validation.constraints.NotBlank;

public record SupplierUpdateRequest(
        @NotBlank String name,
        @NotBlank String contact,
        @NotBlank String phone,
        @NotBlank String email,
        String status
) {
}
