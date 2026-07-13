package com.unza.clinic.dto;

import jakarta.validation.constraints.NotBlank;

public record StaffCreateRequest(
        String manNumber,
        @NotBlank String name,
        @NotBlank String role,
        @NotBlank String department,
        @NotBlank String phone,
        String email,
        @NotBlank String specialization
) {
}
