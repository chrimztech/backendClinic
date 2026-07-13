package com.unza.clinic.dto;

import jakarta.validation.constraints.NotNull;

public record DrugStockAdjustmentRequest(
        @NotNull Integer quantity
) {
}
