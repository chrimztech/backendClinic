package com.unza.clinic.dto;

import jakarta.validation.constraints.NotBlank;

public record ImagingResultUpdateRequest(
        @NotBlank String findings,
        String radiologist,
        String status
) {
}
