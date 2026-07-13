package com.unza.clinic.dto;

import jakarta.validation.constraints.NotBlank;

public record LabResultUpdateRequest(
        @NotBlank String results,
        String interpretation,
        String reportedBy,
        String referenceRange,
        String abnormalFlag,
        String specimenCollectedAt,
        String approvedBy
) {
}
