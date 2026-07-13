package com.unza.clinic.dto;

public record BillingStatusUpdateRequest(
        String status,
        String paymentMethod,
        String paidDate
) {
}
