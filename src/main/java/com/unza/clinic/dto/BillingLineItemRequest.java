package com.unza.clinic.dto;

public record BillingLineItemRequest(
        String tariffCode,
        String serviceName,
        String department,
        Integer quantity,
        Double unitPrice,
        Double lineTotal
) {
}
