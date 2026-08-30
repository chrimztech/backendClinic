package com.unza.clinic.dto;

public record EncounterQueueUpdateRequest(
        String status,
        String priority,
        String performedBy,
        String note
) {
}
