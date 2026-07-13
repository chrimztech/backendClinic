package com.unza.clinic.dto;

import jakarta.validation.constraints.NotBlank;

import java.util.Map;

public record MedicalCertificateGenerateRequest(
        @NotBlank String patientId,
        String encounterId,
        String sourceExamFormId,
        String createdBy,
        Map<String, Object> examPayload
) {
}
