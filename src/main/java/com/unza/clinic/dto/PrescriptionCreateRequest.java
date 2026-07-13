package com.unza.clinic.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import java.util.List;

public record PrescriptionCreateRequest(
        @NotBlank String patientId,
        String patientName,
        String patientIdNum,
        String doctor,
        String program,
        @NotEmpty List<PrescriptionItemDto> drugItems
) {}
