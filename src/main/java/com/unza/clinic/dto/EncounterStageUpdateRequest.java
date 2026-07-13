package com.unza.clinic.dto;

public record EncounterStageUpdateRequest(
        String stage,
        String department,
        String performedBy,
        String note,
        String pendingActions,
        String completedActions,
        String paymentStatus,
        Boolean checkoutEligible
) {
}
