package com.unza.clinic.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record EmergencyCaseCreateRequest(
        @NotBlank String patientName,
        @NotNull Integer age,
        @NotBlank String gender,
        @NotBlank String severity,
        @NotBlank String chiefComplaint,
        @NotBlank String arrivalMode,
        @NotBlank String attendingDoctor,
        @NotBlank String nurseOnDuty,
        @NotBlank String vitals
) {
}
