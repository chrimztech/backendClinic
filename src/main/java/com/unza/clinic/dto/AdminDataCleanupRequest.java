package com.unza.clinic.dto;

import jakarta.validation.constraints.NotNull;

public record AdminDataCleanupRequest(
        @NotNull Long userId,
        String confirmation
) {
}
