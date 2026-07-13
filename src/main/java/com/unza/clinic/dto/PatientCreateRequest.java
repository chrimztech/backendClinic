package com.unza.clinic.dto;

import jakarta.validation.constraints.NotBlank;

public record PatientCreateRequest(
        String clinicNumber,
        String patientType,
        @NotBlank String name,
        Integer age,
        String gender,
        String dob,
        @NotBlank String phone,
        String email,
        @NotBlank String address,
        String bloodGroup,
        String studentId,
        String manNumber,
        String program,
        String school,
        Integer year,
        String hostel,
        @NotBlank String emergencyContact,
        @NotBlank String emergencyPhone,
        String emergencyRelation,
        String allergies,
        String conditions,
        String insurance
) {
}
