package com.unza.clinic.dto;

import jakarta.validation.constraints.NotBlank;

public record AppointmentStatusUpdateRequest(
        @NotBlank String status
) {
}
