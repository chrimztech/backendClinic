package com.unza.clinic.dto;

import jakarta.validation.constraints.NotBlank;

public record StaffScheduleCreateRequest(
        @NotBlank String staffId,
        @NotBlank String name,
        @NotBlank String role,
        @NotBlank String department,
        @NotBlank String dayOfWeek,
        @NotBlank String weekOf,
        @NotBlank String shiftName,
        @NotBlank String startTime,
        @NotBlank String endTime,
        String location
) {
}
