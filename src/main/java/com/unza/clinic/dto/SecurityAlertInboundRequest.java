package com.unza.clinic.dto;

import com.unza.clinic.model.SecurityAlertCategory;
import com.unza.clinic.model.SecurityAlertSeverity;
import com.unza.clinic.model.SecurityAlertSourceType;

/**
 * Shared cross-system wire shape for a security alert, used both:
 *  - inbound: POST /api/external/counseling/security-alerts/inbound (counselling -> clinic)
 *  - outbound: POST {counseling.baseUrl}/api/clinic/security-alerts/inbound (clinic -> counselling)
 */
public record SecurityAlertInboundRequest(
        String externalAlertId,
        SecurityAlertCategory category,
        SecurityAlertSeverity severity,
        SecurityAlertSourceType sourceType,
        String subjectStudentId,
        String subjectName,
        String reportedByName,
        String description,
        Double latitude,
        Double longitude,
        String occurredAt
) {
}
