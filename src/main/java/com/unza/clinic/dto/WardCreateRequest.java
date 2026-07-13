package com.unza.clinic.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record WardCreateRequest(
        @NotBlank String name,
        @NotNull Integer totalBeds
) {
}
