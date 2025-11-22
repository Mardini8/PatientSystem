package org.journalsystem.dto;

import java.util.List;

public record DoctorPatientsResult(
        String doctorId,
        String doctorName,
        List<PatientSearchResult> patients
) {}