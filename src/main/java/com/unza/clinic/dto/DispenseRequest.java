package com.unza.clinic.dto;

import java.util.List;

public record DispenseRequest(
        List<DispenseChargeItem> items
) {
}
