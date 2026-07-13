package com.unza.clinic.dto;

import jakarta.validation.constraints.NotBlank;

public record QueueStatusUpdateRequest(
        @NotBlank String status,
        String department
) {
}
