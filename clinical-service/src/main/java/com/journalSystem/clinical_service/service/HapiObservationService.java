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
public class HapiObservationService {

    private final HapiClientService hapiClient;
    private final FhirLookupService fhirLookupService;

    public List<Observation> getAllObservations() {
        IGenericClient client = hapiClient.getClient();

        Bundle bundle = client
                .search()
                .forResource(Observation.class)
                .returnBundle(Bundle.class)
                .execute();

        return BundleUtil.toListOfEntries(hapiClient.getContext(), bundle)
                .stream()
                .map(entry -> (Observation) entry.getResource())
                .toList();
    }

    public List<Observation> getObservationsForPatient(String patientPersonnummer) {
        try {
            IGenericClient client = hapiClient.getClient();

            // Look up the actual FHIR ID from personnummer
            String patientFhirId = fhirLookupService.findPatientIdByPersonnummer(patientPersonnummer);

            Bundle bundle = client
                    .search()
                    .forResource(Observation.class)
                    .where(Observation.PATIENT.hasId(patientFhirId))
                    .returnBundle(Bundle.class)
                    .execute();

            return BundleUtil.toListOfEntries(hapiClient.getContext(), bundle)
                    .stream()
                    .map(entry -> (Observation) entry.getResource())
                    .toList();
        } catch (Exception e) {
            System.err.println("Could not fetch observations for patient: " + patientPersonnummer);
            e.printStackTrace();
            return List.of();
        }
    }

    public Optional<Observation> getObservationById(String id) {
        try {
            IGenericClient client = hapiClient.getClient();

            Observation observation = client
                    .read()
                    .resource(Observation.class)
                    .withId(id)
                    .execute();

            return Optional.of(observation);
        } catch (Exception e) {
            System.err.println("Could not find observation with ID: " + id);
            return Optional.empty();
        }
    }

    public Observation createObservation(
            String patientPersonnummer,
            String performerPersonnummer,
            String description,
            String value,
            String unit,
            Date effectiveDateTime
    ) {
        IGenericClient client = hapiClient.getClient();

        // Look up actual FHIR IDs from personnummer
        String patientFhirId = fhirLookupService.findPatientIdByPersonnummer(patientPersonnummer);
        System.out.println("Creating observation - Patient personnummer: " + patientPersonnummer + " -> FHIR ID: " + patientFhirId);

        Observation observation = new Observation();
        observation.setStatus(Observation.ObservationStatus.FINAL);

        observation.addCategory()
                .addCoding()
                .setSystem("http://terminology.hl7.org/CodeSystem/observation-category")
                .setCode("vital-signs")
                .setDisplay("Vital signs");

        observation.getCode()
                .addCoding()
                .setSystem("http://loinc.org")
                .setCode("8310-5")
                .setDisplay(description);
        observation.getCode().setText(description);

        // Use the FHIR ID, not the personnummer
        observation.setSubject(new Reference("Patient/" + patientFhirId));

        if (performerPersonnummer != null && !performerPersonnummer.isEmpty()) {
            String practitionerFhirId = fhirLookupService.findPractitionerIdByPersonnummer(performerPersonnummer);
            System.out.println("Creating observation - Practitioner personnummer: " + performerPersonnummer + " -> FHIR ID: " + practitionerFhirId);
            observation.addPerformer(new Reference("Practitioner/" + practitionerFhirId));
        }

        if (value != null && !value.isEmpty()) {
            try {
                double numericValue = Double.parseDouble(value);
                Quantity quantity = new Quantity()
                        .setValue(numericValue)
                        .setUnit(unit != null && !unit.isEmpty() ? unit : "{score}")
                        .setSystem("http://unitsofmeasure.org")
                        .setCode(unit != null && !unit.isEmpty() ? unit : "{score}");
                observation.setValue(quantity);
            } catch (NumberFormatException e) {
                observation.setValue(new StringType(value));
            }
        }

        observation.setEffective(new DateTimeType(effectiveDateTime));
        observation.setIssued(effectiveDateTime);

        try {
            MethodOutcome outcome = client
                    .create()
                    .resource(observation)
                    .execute();

            String newId = outcome.getId().getIdPart();
            System.out.println("✓ Observation created with ID: " + newId);
            return getObservationById(newId).orElse(observation);
        } catch (Exception e) {
            System.err.println("Error creating observation: " + e.getMessage());
            e.printStackTrace();
            throw e;
        }
    }
}