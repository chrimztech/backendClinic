package com.unza.clinic.dto;

/** Body for POST /api/security-alerts/{id}/resolve */
public record SecurityAlertResolveRequest(
        String resolutionNotes,
        Boolean falsePositive
) {
}
