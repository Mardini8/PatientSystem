package com.journalSystem.clinical_service.dto;

public record PatientDTO(
        Long id,
        String firstName,
        String lastName,
        String socialSecurityNumber,
        String dateOfBirth
) {}