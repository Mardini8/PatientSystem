package com.journalSystem.clinical_service.dto;

import java.time.LocalDateTime;

public record ObservationDTO(
        Long id,
        Long patientId,
        Long performerId,
        Long encounterId,
        String description,
        LocalDateTime effectiveDateTime
) {}