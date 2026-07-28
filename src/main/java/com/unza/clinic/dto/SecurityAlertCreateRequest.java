package com.unza.clinic.dto;

import com.unza.clinic.model.SecurityAlertCategory;
import com.unza.clinic.model.SecurityAlertSeverity;
import jakarta.validation.constraints.NotNull;

/** Body for POST /api/security-alerts — a locally-filed sensitive-case report. */
public record SecurityAlertCreateRequest(
        @NotNull SecurityAlertCategory category,
        @NotNull SecurityAlertSeverity severity,
        String subjectStudentId,
        String subjectName,
        String description,
        Double latitude,
        Double longitude
) {
}
