package com.unza.clinic.dto;

import jakarta.validation.constraints.NotBlank;
import java.util.List;

public record BillingCreateRequest(
        @NotBlank String patientId,
        @NotBlank String patientName,
        String items,
        List<BillingLineItemRequest> lineItems,
        Double subtotal,
        Double tax,
        Double total,
        String dueDate,
        String paymentMethod
) {
}
