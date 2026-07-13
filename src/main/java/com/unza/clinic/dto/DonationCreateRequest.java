package com.unza.clinic.dto;

import jakarta.validation.constraints.NotBlank;

public record DonationCreateRequest(
        @NotBlank String donorName,
        @NotBlank String bloodType,
        Integer units
) {
}
