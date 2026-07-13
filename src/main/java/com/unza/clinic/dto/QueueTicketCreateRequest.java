package com.unza.clinic.dto;

import jakarta.validation.constraints.NotBlank;

public record QueueTicketCreateRequest(
        String patientId,
        @NotBlank String patientName,
        @NotBlank String department,
        @NotBlank String priority
) {
}
