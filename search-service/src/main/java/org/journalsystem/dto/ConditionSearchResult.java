package org.journalsystem.dto;

public record ConditionSearchResult(
        String id,
        String patientId,
        String patientName,
        String description,
        String recordedDate
) {}