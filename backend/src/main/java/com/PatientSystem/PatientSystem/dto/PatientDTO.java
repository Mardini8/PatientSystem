package com.PatientSystem.PatientSystem.dto;

public record PatientDTO(
        Long id,
        String firstName,
        String lastName,
        String socialSecurityNumber,
        String dateOfBirth
) {}