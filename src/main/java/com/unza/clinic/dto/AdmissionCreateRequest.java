package com.unza.clinic.dto;

import com.fasterxml.jackson.annotation.JsonAlias;
import jakarta.validation.constraints.NotBlank;

public record AdmissionCreateRequest(
        @NotBlank String patientId,
        @NotBlank String patientName,
        @NotBlank String ward,
        @NotBlank String bed,
        @JsonAlias("doctor") @NotBlank String clinician,
        @NotBlank String admittedOn,
        @NotBlank String diagnosis
) {
}
