package com.journalSystem.clinical_service.service;

import ca.uhn.fhir.rest.api.MethodOutcome;
import ca.uhn.fhir.rest.client.api.IGenericClient;
import ca.uhn.fhir.util.BundleUtil;
import lombok.RequiredArgsConstructor;
import org.hl7.fhir.r4.model.*;
import org.springframework.stereotype.Service;

import java.util.Date;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class HapiEncounterService {

    private final HapiClientService hapiClient;
    private final FhirLookupService fhirLookupService;

    public List<Encounter> getAllEncounters() {
        IGenericClient client = hapiClient.getClient();

        Bundle bundle = client
                .search()
                .forResource(Encounter.class)
                .returnBundle(Bundle.class)
                .execute();

        return BundleUtil.toListOfEntries(hapiClient.getContext(), bundle)
                .stream()
                .map(entry -> (Encounter) entry.getResource())
                .toList();
    }

    public List<Encounter> getEncountersForPatient(String patientPersonnummer) {
        try {
            IGenericClient client = hapiClient.getClient();

            // Look up the actual FHIR ID from personnummer
            String patientFhirId = fhirLookupService.findPatientIdByPersonnummer(patientPersonnummer);

            Bundle bundle = client
                    .search()
                    .forResource(Encounter.class)
                    .where(Encounter.PATIENT.hasId(patientFhirId))
                    .returnBundle(Bundle.class)
                    .execute();

            return BundleUtil.toListOfEntries(hapiClient.getContext(), bundle)
                    .stream()
                    .map(entry -> (Encounter) entry.getResource())
                    .toList();
        } catch (Exception e) {
            System.err.println("Could not fetch encounters for patient: " + patientPersonnummer);
            e.printStackTrace();
            return List.of();
        }
    }

    public Optional<Encounter> getEncounterById(String id) {
        try {
            IGenericClient client = hapiClient.getClient();

            Encounter encounter = client
                    .read()
                    .resource(Encounter.class)
                    .withId(id)
                    .execute();

            return Optional.of(encounter);
        } catch (Exception e) {
            System.err.println("Could not find encounter with ID: " + id);
            return Optional.empty();
        }
    }

    public Encounter createEncounter(
            String patientPersonnummer,
            String practitionerPersonnummer,
            Date startTime,
            Date endTime
    ) {
        IGenericClient client = hapiClient.getClient();

        // Look up actual FHIR IDs from personnummer
        String patientFhirId = fhirLookupService.findPatientIdByPersonnummer(patientPersonnummer);
        System.out.println("Creating encounter - Patient personnummer: " + patientPersonnummer + " -> FHIR ID: " + patientFhirId);

        Encounter encounter = new Encounter();
        encounter.setStatus(Encounter.EncounterStatus.FINISHED);

        encounter.setClass_(new Coding()
                .setSystem("http://terminology.hl7.org/CodeSystem/v3-ActCode")
                .setCode("AMB")
                .setDisplay("ambulatory"));

        encounter.addType()
                .addCoding()
                .setSystem("http://snomed.info/sct")
                .setCode("185349003")
                .setDisplay("Encounter for check up (procedure)");

        // Use the FHIR ID, not the personnummer
        encounter.setSubject(new Reference("Patient/" + patientFhirId));

        if (practitionerPersonnummer != null && !practitionerPersonnummer.isEmpty()) {
            String practitionerFhirId = fhirLookupService.findPractitionerIdByPersonnummer(practitionerPersonnummer);
            System.out.println("Creating encounter - Practitioner personnummer: " + practitionerPersonnummer + " -> FHIR ID: " + practitionerFhirId);

            Encounter.EncounterParticipantComponent participant = encounter.addParticipant();

            participant.addType()
                    .addCoding()
                    .setSystem("http://terminology.hl7.org/CodeSystem/v3-ParticipationType")
                    .setCode("PPRF")
                    .setDisplay("primary performer");

            participant.setIndividual(new Reference("Practitioner/" + practitionerFhirId));

            Period participantPeriod = new Period();
            participantPeriod.setStart(startTime);
            if (endTime != null) {
                participantPeriod.setEnd(endTime);
            }
            participant.setPeriod(participantPeriod);
        }

        Period period = new Period();
        period.setStart(startTime);
        if (endTime != null) {
            period.setEnd(endTime);
        }
        encounter.setPeriod(period);

        try {
            MethodOutcome outcome = client
                    .create()
                    .resource(encounter)
                    .execute();

            String newId = outcome.getId().getIdPart();
            System.out.println("✓ Encounter created with ID: " + newId);
            return getEncounterById(newId).orElse(encounter);
        } catch (Exception e) {
            System.err.println("Error creating encounter: " + e.getMessage());
            e.printStackTrace();
            throw e;
        }
    }
}