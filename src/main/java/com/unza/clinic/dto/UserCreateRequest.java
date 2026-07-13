package com.unza.clinic.dto;

import jakarta.validation.constraints.NotBlank;
import java.util.List;

public record UserCreateRequest(
        @NotBlank String name,
        @NotBlank String email,
        @NotBlank String role,
        @NotBlank String department,
        String staffId,
        String manNumber,
        @NotBlank String password,
        String status,
        Boolean forcePasswordChange,
        List<String> permissions
) {
}
