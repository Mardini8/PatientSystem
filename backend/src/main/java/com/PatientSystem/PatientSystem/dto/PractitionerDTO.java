package com.PatientSystem.PatientSystem.dto;

public record PractitionerDTO(
        Long id,
        String firstName,
        String lastName,
        String socialSecurityNumber,
        String dateOfBirth,
        String title,
        Long organizationId
) {}