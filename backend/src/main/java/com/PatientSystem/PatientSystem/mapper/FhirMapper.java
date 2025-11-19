package com.PatientSystem.PatientSystem.mapper;

import com.PatientSystem.PatientSystem.dto.*;
import org.hl7.fhir.r4.model.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Date;

public class FhirMapper {

    public static PatientDTO patientToDTO(org.hl7.fhir.r4.model.Patient fhirPatient) {
        if (fhirPatient == null) {
            return null;
        }

        String fhirId = fhirPatient.getIdElement().getIdPart();

        Long id = extractNumericId(fhirId);

        String firstName = "";
        String lastName = "";
        if (!fhirPatient.getName().isEmpty()) {
            HumanName name = fhirPatient.getName().get(0);
            if (!name.getGiven().isEmpty()) {
                firstName = name.getGiven().get(0).getValue();
            }
            if (name.hasFamily()) {
                lastName = name.getFamily();
            }
        }

        String socialSecurityNumber = extractIdentifier(fhirPatient.getIdentifier());

        Date birthDate = fhirPatient.getBirthDate();
        String dateOfBirth = null;
        if (birthDate != null) {
            dateOfBirth = birthDate.toInstant()
                    .atZone(ZoneId.systemDefault())
                    .toLocalDate()
                    .toString();
        }

        return new PatientDTO(
                id,
                firstName,
                lastName,
                socialSecurityNumber,
                dateOfBirth
        );
    }

    public static ConditionDTO conditionToDTO(org.hl7.fhir.r4.model.Condition fhirCondition) {
        if (fhirCondition == null) {
            return null;
        }

        Long id = extractNumericId(fhirCondition.getIdElement().getIdPart());

        Long patientId = null;
        if (fhirCondition.hasSubject()) {
            patientId = extractNumericId(fhirCondition.getSubject().getReferenceElement().getIdPart());
        }

        Long practitionerId = null;
        if (fhirCondition.hasAsserter()) {
            practitionerId = extractNumericId(fhirCondition.getAsserter().getReferenceElement().getIdPart());
        } else if (fhirCondition.hasRecorder()) {
            practitionerId = extractNumericId(fhirCondition.getRecorder().getReferenceElement().getIdPart());
        }

        String description = "Okänd diagnos";
        if (fhirCondition.hasCode() && fhirCondition.getCode().hasText()) {
            description = fhirCondition.getCode().getText();
        } else if (fhirCondition.hasCode() && !fhirCondition.getCode().getCoding().isEmpty()) {
            description = fhirCondition.getCode().getCoding().get(0).getDisplay();
        }

        java.time.LocalDate assertedDate = null;
        if (fhirCondition.hasRecordedDate()) {
            assertedDate = fhirCondition.getRecordedDate().toInstant()
                    .atZone(ZoneId.systemDefault())
                    .toLocalDate();
        }

        return new ConditionDTO(
                id,
                patientId,
                practitionerId,
                description,
                assertedDate
        );
    }

    public static ObservationDTO observationToDTO(org.hl7.fhir.r4.model.Observation fhirObservation) {
        if (fhirObservation == null) {
            return null;
        }

        Long id = extractNumericId(fhirObservation.getIdElement().getIdPart());

        Long patientId = null;
        if (fhirObservation.hasSubject()) {
            patientId = extractNumericId(fhirObservation.getSubject().getReferenceElement().getIdPart());
        }

        Long practitionerId = null;
        if (!fhirObservation.getPerformer().isEmpty()) {
            Reference performer = fhirObservation.getPerformer().get(0);
            if (performer.getReferenceElement().getResourceType().equals("Practitioner")) {
                practitionerId = extractNumericId(performer.getReferenceElement().getIdPart());
            }
        }

        Long encounterId = null;
        if (fhirObservation.hasEncounter()) {
            encounterId = extractNumericId(fhirObservation.getEncounter().getReferenceElement().getIdPart());
        }

        String description = "Okänd observation";
        if (fhirObservation.hasCode() && fhirObservation.getCode().hasText()) {
            description = fhirObservation.getCode().getText();
        } else if (fhirObservation.hasCode() && !fhirObservation.getCode().getCoding().isEmpty()) {
            description = fhirObservation.getCode().getCoding().get(0).getDisplay();
        }

        if (fhirObservation.hasValueQuantity()) {
            Quantity value = fhirObservation.getValueQuantity();
            description += ": " + value.getValue() + " " + value.getUnit();
        } else if (fhirObservation.hasValueStringType()) {
            description += ": " + fhirObservation.getValueStringType().getValue();
        }

        java.time.LocalDateTime effectiveDateTime = null;
        if (fhirObservation.hasEffectiveDateTimeType()) {
            effectiveDateTime = fhirObservation.getEffectiveDateTimeType().getValue().toInstant()
                    .atZone(ZoneId.systemDefault())
                    .toLocalDateTime();
        }

        return new ObservationDTO(
                id,
                patientId,
                practitionerId,
                encounterId,
                description,
                effectiveDateTime
        );
    }

    public static EncounterDTO encounterToDTO(org.hl7.fhir.r4.model.Encounter fhirEncounter) {
        if (fhirEncounter == null) {
            return null;
        }

        Long id = extractNumericId(fhirEncounter.getIdElement().getIdPart());

        Long patientId = null;
        if (fhirEncounter.hasSubject()) {
            patientId = extractNumericId(fhirEncounter.getSubject().getReferenceElement().getIdPart());
        }

        Long practitionerId = null;
        if (!fhirEncounter.getParticipant().isEmpty()) {
            for (Encounter.EncounterParticipantComponent participant : fhirEncounter.getParticipant()) {
                if (participant.hasIndividual() &&
                        participant.getIndividual().getReferenceElement().getResourceType().equals("Practitioner")) {
                    practitionerId = extractNumericId(participant.getIndividual().getReferenceElement().getIdPart());
                    break;
                }
            }
        }

        Long organizationId = null;
        if (fhirEncounter.hasServiceProvider()) {
            organizationId = extractNumericId(fhirEncounter.getServiceProvider().getReferenceElement().getIdPart());
        }

        java.time.LocalDateTime startTime = null;
        java.time.LocalDateTime endTime = null;
        if (fhirEncounter.hasPeriod()) {
            Period period = fhirEncounter.getPeriod();
            if (period.hasStart()) {
                startTime = period.getStart().toInstant()
                        .atZone(ZoneId.systemDefault())
                        .toLocalDateTime();
            }
            if (period.hasEnd()) {
                endTime = period.getEnd().toInstant()
                        .atZone(ZoneId.systemDefault())
                        .toLocalDateTime();
            }
        }

        return new EncounterDTO(
                id,
                patientId,
                practitionerId,
                organizationId,
                startTime,
                endTime
        );
    }

    public static PractitionerDTO practitionerToDTO(org.hl7.fhir.r4.model.Practitioner fhirPractitioner) {
        if (fhirPractitioner == null) {
            return null;
        }

        Long id = extractNumericId(fhirPractitioner.getIdElement().getIdPart());

        String firstName = "";
        String lastName = "";
        if (!fhirPractitioner.getName().isEmpty()) {
            HumanName name = fhirPractitioner.getName().get(0);
            if (!name.getGiven().isEmpty()) {
                firstName = name.getGiven().get(0).getValue();
            }
            if (name.hasFamily()) {
                lastName = name.getFamily();
            }
        }

        String socialSecurityNumber = extractIdentifier(fhirPractitioner.getIdentifier());

        Date birthDate = fhirPractitioner.getBirthDate();
        String dateOfBirth = null;
        if (birthDate != null) {
            dateOfBirth = birthDate.toInstant()
                    .atZone(ZoneId.systemDefault())
                    .toLocalDate()
                    .toString();
        }

        String title = "Vårdpersonal";
        if (!fhirPractitioner.getQualification().isEmpty()) {
            Practitioner.PractitionerQualificationComponent qual = fhirPractitioner.getQualification().get(0);
            if (qual.hasCode() && qual.getCode().hasText()) {
                title = qual.getCode().getText();
            }
        }

        return new PractitionerDTO(
                id,
                firstName,
                lastName,
                socialSecurityNumber,
                dateOfBirth,
                title,
                null
        );
    }

    private static String extractIdentifier(java.util.List<Identifier> identifiers) {
        for (Identifier identifier : identifiers) {
            if (identifier.hasValue()) {
                return identifier.getValue();
            }
        }
        return "Okänt";
    }

    private static Long extractNumericId(String fhirId) {
        try {
            if (fhirId == null) return 0L;
            if (fhirId.contains("/")) {
                fhirId = fhirId.substring(fhirId.lastIndexOf("/") + 1);
            }
            return Long.parseLong(fhirId);
        } catch (NumberFormatException e) {
            System.err.println("Kunde inte konvertera FHIR ID: " + fhirId);
            return 0L;
        }
    }
}