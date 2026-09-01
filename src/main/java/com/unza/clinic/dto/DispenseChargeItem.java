package com.unza.clinic.dto;

public record DispenseChargeItem(
        String drugName,
        String tariffCode,
        Integer quantity
) {
}
