package com.unza.clinic.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public record WardBedsUpdateRequest(
        @NotNull @Min(0) Integer totalBeds
) {
}
