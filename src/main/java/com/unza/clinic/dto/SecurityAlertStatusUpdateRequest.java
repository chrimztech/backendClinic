package com.unza.clinic.dto;

/**
 * Shared cross-system wire shape for a status update, used both:
 *  - inbound: PATCH /api/external/counseling/security-alerts/inbound/{id}/status
 *  - outbound: PATCH {counseling.baseUrl}/api/clinic/security-alerts/inbound/{externalAlertId}/status
 */
public record SecurityAlertStatusUpdateRequest(
        String status,
        String actorName,
        String resolutionNotes
) {
}
