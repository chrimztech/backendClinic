package com.unza.clinic.dto;

import com.fasterxml.jackson.annotation.JsonAlias;
import jakarta.validation.constraints.NotBlank;

public record AppointmentCreateRequest(
        @NotBlank String patientId,
        @NotBlank String patientName,
        @JsonAlias({"doctorId", "doctor_id"}) String clinicianId,
        @JsonAlias({"doctorName", "doctor_name"}) @NotBlank String clinicianName,
        @NotBlank String department,
        @NotBlank String date,
        @NotBlank String time,
        String type,
        String notes
) {
}
