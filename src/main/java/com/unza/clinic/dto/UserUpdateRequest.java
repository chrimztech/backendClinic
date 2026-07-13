package com.unza.clinic.dto;

import java.util.List;

public record UserUpdateRequest(
        String name,
        String email,
        String role,
        String department,
        String password,
        String status,
        Boolean forcePasswordChange,
        List<String> permissions
) {
}
