package com.unza.clinic.dto;

import jakarta.validation.constraints.NotBlank;

public record AdmissionTransferRequest(
        @NotBlank String ward,
        @NotBlank String bed
) {
}
