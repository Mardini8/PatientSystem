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
public class HapiConditionService {

    private final HapiClientService hapiClient;

    public List<Condition> getAllConditions() {
        IGenericClient client = hapiClient.getClient();

        Bundle bundle = client
                .search()
                .forResource(Condition.class)
                .returnBundle(Bundle.class)
                .execute();

        return BundleUtil.toListOfEntries(hapiClient.getContext(), bundle)
                .stream()
                .map(entry -> (Condition) entry.getResource())
                .toList();
    }

    public List<Condition> getConditionsForPatient(String patientId) {
        try {
            IGenericClient client = hapiClient.getClient();

            Bundle bundle = client
                    .search()
                    .forResource(Condition.class)
                    .where(Condition.PATIENT.hasId(patientId))
                    .returnBundle(Bundle.class)
                    .execute();

            return BundleUtil.toListOfEntries(hapiClient.getContext(), bundle)
                    .stream()
                    .map(entry -> (Condition) entry.getResource())
                    .toList();
        } catch (Exception e) {
            System.err.println("Could not fetch conditions for patient: " + patientId);
            e.printStackTrace();
            return List.of();
        }
    }

    public Optional<Condition> getConditionById(String id) {
        try {
            IGenericClient client = hapiClient.getClient();

            Condition condition = client
                    .read()
                    .resource(Condition.class)
                    .withId(id)
                    .execute();

            return Optional.of(condition);
        } catch (Exception e) {
            System.err.println("Could not find condition with ID: " + id);
            return Optional.empty();
        }
    }

    public Condition createCondition(
            String patientPersonnummer,
            String practitionerPersonnummer,
            String description,
            Date recordedDate
    ) {
        IGenericClient client = hapiClient.getClient();

        Condition condition = new Condition();

        condition.getClinicalStatus()
                .addCoding()
                .setSystem("http://terminology.hl7.org/CodeSystem/condition-clinical")
                .setCode("active");

        condition.getVerificationStatus()
                .addCoding()
                .setSystem("http://terminology.hl7.org/CodeSystem/condition-ver-status")
                .setCode("confirmed");

        condition.setSubject(new Reference("Patient/" + patientPersonnummer));

        if (practitionerPersonnummer != null && !practitionerPersonnummer.isEmpty()) {
            condition.setRecorder(new Reference("Practitioner/" + practitionerPersonnummer));
        }

        condition.getCode()
                .addCoding()
                .setSystem("http://snomed.info/sct")
                .setCode("404684003")
                .setDisplay(description);
        condition.getCode().setText(description);

        condition.setRecordedDate(recordedDate);
        condition.setOnset(new DateTimeType(recordedDate));

        try {
            MethodOutcome outcome = client
                    .create()
                    .resource(condition)
                    .execute();

            String newId = outcome.getId().getIdPart();
            System.out.println("✓ Condition created with ID: " + newId);
            return getConditionById(newId).orElse(condition);
        } catch (Exception e) {
            System.err.println("Error creating condition: " + e.getMessage());
            e.printStackTrace();
            throw e;
        }
    }
}