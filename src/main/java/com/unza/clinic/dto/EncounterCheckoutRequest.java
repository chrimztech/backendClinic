package com.unza.clinic.dto;

public record EncounterCheckoutRequest(
        String performedBy,
        String note
) {
}
