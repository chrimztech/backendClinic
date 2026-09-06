package com.unza.clinic.dto;

import jakarta.validation.constraints.NotBlank;

public record DepartmentCreateRequest(
        @NotBlank String name,
        @NotBlank String head,
        String headUserId,
        Integer clinicians,
        Integer doctors,
        Integer nurses,
        Integer beds,
        @NotBlank String location,
        @NotBlank String phone
) {
}
