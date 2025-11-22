package org.journalsystem.dto;

public record EncounterSearchResult(
        String id,
        String patientId,
        String patientName,
        String practitionerId,
        String practitionerName,
        String startTime,
        String endTime
) {}