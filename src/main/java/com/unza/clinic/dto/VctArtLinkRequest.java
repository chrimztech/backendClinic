package com.unza.clinic.dto;

import jakarta.validation.constraints.NotBlank;

public record VctArtLinkRequest(
        @NotBlank String artNumber,
        String enrollmentDate,
        String currentRegimen,
        String regimenLine,
        String nextClinicDate
) {
}
