package com.unza.clinic.dto;

import jakarta.validation.constraints.NotBlank;

public record TriageCreateRequest(
        @NotBlank String patientId,
        @NotBlank String patientName,
        @NotBlank String level,
        @NotBlank String chiefComplaint,
        String vitalSigns,
        @NotBlank String bloodPressure,
        String nurseName,
        Double temperature,
        Integer pulseRate,
        Integer respiratoryRate,
        Integer oxygenSaturation,
        Double weightKg,
        Double heightCm,
        Double bmi,
        Double randomBloodSugar,
        Integer painScore,
        String consciousnessLevel,
        String notes
) {
}
