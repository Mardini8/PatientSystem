package com.PatientSystem.PatientSystem.service;

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

    public List<Observation> getObservationsForPatient(String patientId) {
        try {
            IGenericClient client = hapiClient.getClient();

            Bundle bundle = client
                    .search()
                    .forResource(Observation.class)
                    .where(Observation.PATIENT.hasId(patientId))
                    .returnBundle(Bundle.class)
                    .execute();

            return BundleUtil.toListOfEntries(hapiClient.getContext(), bundle)
                    .stream()
                    .map(entry -> (Observation) entry.getResource())
                    .toList();
        } catch (Exception e) {
            System.err.println("Kunde inte hämta observations för patient: " + patientId);
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
            System.err.println("Kunde inte hitta observation med ID: " + id);
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

        observation.setSubject(new Reference("Patient/" + patientPersonnummer));

        if (performerPersonnummer != null && !performerPersonnummer.isEmpty()) {
            observation.addPerformer(new Reference("Practitioner/" + performerPersonnummer));
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
            System.out.println("Observation skapad med ID: " + newId);
            return getObservationById(newId).orElse(observation);
        } catch (Exception e) {
            System.err.println("Fel vid skapande av observation: " + e.getMessage());
            e.printStackTrace();
            throw e;
        }
    }
}