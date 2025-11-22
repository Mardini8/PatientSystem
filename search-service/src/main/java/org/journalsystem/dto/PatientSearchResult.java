package org.journalsystem.dto;

public record PatientSearchResult(
        String id,
        String firstName,
        String lastName,
        String socialSecurityNumber,
        String dateOfBirth
) {}