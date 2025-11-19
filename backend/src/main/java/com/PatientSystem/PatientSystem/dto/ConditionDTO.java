package com.PatientSystem.PatientSystem.dto;

import java.time.LocalDate;

public record ConditionDTO(
        Long id,
        Long patientId,
        Long practitionerId,
        String description,
        LocalDate assertedDate
) {}
