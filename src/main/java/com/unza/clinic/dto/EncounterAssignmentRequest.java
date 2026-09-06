package com.unza.clinic.dto;

public record EncounterAssignmentRequest(
        String assignedTo,
        String status,
        String performedBy,
        String note,
        String consultationRoomCode
) {
}
