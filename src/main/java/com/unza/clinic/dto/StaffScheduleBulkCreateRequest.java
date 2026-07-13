package com.unza.clinic.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import java.util.List;

public record StaffScheduleBulkCreateRequest(
        @NotBlank String staffId,
        @NotBlank String name,
        @NotBlank String role,
        @NotBlank String department,
        @NotEmpty List<String> days,
        @NotBlank String weekOf,
        @NotBlank String shiftName,
        @NotBlank String startTime,
        @NotBlank String endTime,
        String location
) {
}
