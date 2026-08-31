package com.unza.clinic.dto;

import jakarta.validation.constraints.NotBlank;

public record StaffUpdateRequest(
        @NotBlank String name,
        String manNumber,
        @NotBlank String role,
        @NotBlank String department,
        @NotBlank String phone,
        String email,
        @NotBlank String specialization,
        String status
) {
}
