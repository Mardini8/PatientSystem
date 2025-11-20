package com.journalSystem.clinical_service.dto;

import java.time.LocalDateTime;

public record EncounterDTO(
        Long id,
        Long patientId,
        Long practitionerId,
        Long locationId,
        LocalDateTime startTime,
        LocalDateTime endTime
) {}