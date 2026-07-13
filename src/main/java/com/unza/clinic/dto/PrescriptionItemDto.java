package com.unza.clinic.dto;

public record PrescriptionItemDto(
        Long id,
        String drugName,
        Integer quantity,
        String dosage,
        String duration,
        String instructions,
        String medicationClass
) {}
