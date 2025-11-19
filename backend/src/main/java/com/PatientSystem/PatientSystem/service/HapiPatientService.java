package com.PatientSystem.PatientSystem.service;

import ca.uhn.fhir.rest.client.api.IGenericClient;
import ca.uhn.fhir.util.BundleUtil;
import lombok.RequiredArgsConstructor;
import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.Patient;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class HapiPatientService {

    private final HapiClientService hapiClient;

    public List<Patient> getAllPatients() {
        IGenericClient client = hapiClient.getClient();

        Bundle bundle = client
                .search()
                .forResource(Patient.class)
                .returnBundle(Bundle.class)
                .execute();

        return BundleUtil.toListOfEntries(hapiClient.getContext(), bundle)
                .stream()
                .map(entry -> (Patient) entry.getResource())
                .toList();
    }

    public Optional<Patient> getPatientById(String id) {
        try {
            IGenericClient client = hapiClient.getClient();

            Patient patient = client
                    .read()
                    .resource(Patient.class)
                    .withId(id)
                    .execute();

            return Optional.of(patient);
        } catch (Exception e) {
            System.err.println("Kunde inte hitta patient med ID: " + id);
            return Optional.empty();
        }
    }

    public Optional<Patient> getPatientByPersonnummer(String personnummer) {
        try {
            IGenericClient client = hapiClient.getClient();

            Bundle bundle = client
                    .search()
                    .forResource(Patient.class)
                    .where(Patient.IDENTIFIER.exactly().code(personnummer))
                    .returnBundle(Bundle.class)
                    .execute();

            List<Patient> patients = BundleUtil.toListOfEntries(hapiClient.getContext(), bundle)
                    .stream()
                    .map(entry -> (Patient) entry.getResource())
                    .toList();

            if (!patients.isEmpty()) {
                return Optional.of(patients.get(0));
            }

            return Optional.empty();
        } catch (Exception e) {
            System.err.println("Kunde inte hitta patient med personnummer: " + personnummer);
            return Optional.empty();
        }
    }
}