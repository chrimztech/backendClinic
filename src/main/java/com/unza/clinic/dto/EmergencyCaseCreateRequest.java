package com.unza.clinic.dto;

import com.fasterxml.jackson.annotation.JsonAlias;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record EmergencyCaseCreateRequest(
        @NotBlank String patientName,
        @NotNull Integer age,
        @NotBlank String gender,
        @NotBlank String severity,
        @JsonAlias("chiefComplaint") @NotBlank String purpose,
        @NotBlank String arrivalMode,
        @JsonAlias("attendingDoctor") @NotBlank String attendingClinician,
        @NotBlank String nurseOnDuty,
        @NotBlank String vitals
) {
}
