package com.journalSystem.clinical_service.dto;

import java.time.LocalDate;

public record ConditionDTO(
        Long id,
        Long patientId,
        Long practitionerId,
        String description,
        LocalDate assertedDate
) {}