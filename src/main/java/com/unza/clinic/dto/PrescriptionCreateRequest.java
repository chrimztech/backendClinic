package com.unza.clinic.dto;

import com.fasterxml.jackson.annotation.JsonAlias;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import java.util.List;

public record PrescriptionCreateRequest(
        @NotBlank String patientId,
        String patientName,
        String patientIdNum,
        @JsonAlias("doctor") String clinician,
        String program,
        @NotEmpty List<PrescriptionItemDto> drugItems
) {}
