package org.journalsystem.mapper;

import org.journalsystem.dto.*;
import org.journalsystem.dto.fhir.FhirBundle;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

public class FhirMapper {

    public static PatientSearchResult toPatientSearchResult(FhirBundle.FhirResource resource) {
        if (resource == null || !"Patient".equals(resource.resourceType)) {
            return null;
        }

        String firstName = "";
        String lastName = "";
        if (resource.name != null && !resource.name.isEmpty()) {
            FhirBundle.HumanName name = resource.name.get(0);
            if (name.given != null && !name.given.isEmpty()) {
                firstName = name.given.get(0);
            }
            lastName = name.family != null ? name.family : "";
        }

        String ssn = "";
        if (resource.identifier != null && !resource.identifier.isEmpty()) {
            ssn = resource.identifier.get(0).value;
        }

        return new PatientSearchResult(
                resource.id,
                firstName,
                lastName,
                ssn,
                resource.birthDate
        );
    }

    public static ConditionSearchResult toConditionSearchResult(
            FhirBundle.FhirResource resource,
            String patientName) {
        if (resource == null || !"Condition".equals(resource.resourceType)) {
            return null;
        }

        String description = "Unknown condition";
        if (resource.code != null) {
            if (resource.code.text != null) {
                description = resource.code.text;
            } else if (resource.code.coding != null && !resource.code.coding.isEmpty()) {
                description = resource.code.coding.get(0).display;
            }
        }

        String patientId = "";
        if (resource.subject != null && resource.subject.reference != null) {
            patientId = resource.subject.reference.replace("Patient/", "");
        }

        return new ConditionSearchResult(
                resource.id,
                patientId,
                patientName,
                description,
                resource.recordedDate
        );
    }

    public static EncounterSearchResult toEncounterSearchResult(
            FhirBundle.FhirResource resource,
            String patientName,
            String practitionerName) {
        if (resource == null || !"Encounter".equals(resource.resourceType)) {
            return null;
        }

        String patientId = "";
        String practitionerId = "";

        // Extract patient ID from subject
        if (resource.subject != null && resource.subject.reference != null) {
            patientId = resource.subject.reference.replace("Patient/", "");
        }

        // Extract practitioner from participants
        if (resource.participant != null && !resource.participant.isEmpty()) {
            for (FhirBundle.Participant p : resource.participant) {
                if (p.individual != null && p.individual.reference != null) {
                    if (p.individual.reference.startsWith("Practitioner/")) {
                        practitionerId = p.individual.reference.replace("Practitioner/", "");
                        break; // Use first practitioner found
                    }
                }
            }
        }

        String startTime = "";
        String endTime = "";
        if (resource.period != null) {
            startTime = resource.period.start;
            endTime = resource.period.end;
        }

        return new EncounterSearchResult(
                resource.id,
                patientId,
                patientName,
                practitionerId,
                practitionerName,
                startTime,
                endTime
        );
    }

    public static List<PatientSearchResult> bundleToPatientList(FhirBundle bundle) {
        List<PatientSearchResult> results = new ArrayList<>();

        if (bundle == null || bundle.entry == null) {
            return results;
        }

        for (FhirBundle.BundleEntry entry : bundle.entry) {
            PatientSearchResult patient = toPatientSearchResult(entry.resource);
            if (patient != null) {
                results.add(patient);
            }
        }

        return results;
    }

    public static List<ConditionSearchResult> bundleToConditionList(FhirBundle bundle) {
        List<ConditionSearchResult> results = new ArrayList<>();

        if (bundle == null || bundle.entry == null) {
            return results;
        }

        for (FhirBundle.BundleEntry entry : bundle.entry) {
            ConditionSearchResult condition = toConditionSearchResult(entry.resource, "");
            if (condition != null) {
                results.add(condition);
            }
        }

        return results;
    }

    public static List<EncounterSearchResult> bundleToEncounterList(FhirBundle bundle) {
        List<EncounterSearchResult> results = new ArrayList<>();

        if (bundle == null || bundle.entry == null) {
            return results;
        }

        for (FhirBundle.BundleEntry entry : bundle.entry) {
            EncounterSearchResult encounter = toEncounterSearchResult(entry.resource, "", "");
            if (encounter != null) {
                results.add(encounter);
            }
        }

        return results;
    }
}