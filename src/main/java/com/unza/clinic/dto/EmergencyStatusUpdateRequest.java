package com.unza.clinic.dto;

import jakarta.validation.constraints.NotBlank;

public record EmergencyStatusUpdateRequest(
        @NotBlank String status
) {
}
