package com.unza.clinic.dto;

import jakarta.validation.constraints.NotBlank;

public record AppointmentUpdateRequest(
        String doctorId,
        @NotBlank String doctorName,
        @NotBlank String department,
        @NotBlank String date,
        @NotBlank String time,
        String type,
        String notes
) {
}
