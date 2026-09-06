package com.unza.clinic.dto;

import jakarta.validation.constraints.NotBlank;

public record ConsultationRoomRequest(
        String roomCode,
        @NotBlank String name,
        @NotBlank String department,
        String location,
        String status
) {
}
