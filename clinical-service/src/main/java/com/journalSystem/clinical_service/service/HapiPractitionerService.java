package com.journalSystem.clinical_service.service;

import ca.uhn.fhir.rest.client.api.IGenericClient;
import ca.uhn.fhir.util.BundleUtil;
import lombok.RequiredArgsConstructor;
import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.Practitioner;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class HapiPractitionerService {

    private final HapiClientService hapiClient;

    public List<Practitioner> getAllPractitioners() {
        IGenericClient client = hapiClient.getClient();

        Bundle bundle = client
                .search()
                .forResource(Practitioner.class)
                .returnBundle(Bundle.class)
                .execute();

        return BundleUtil.toListOfEntries(hapiClient.getContext(), bundle)
                .stream()
                .map(entry -> (Practitioner) entry.getResource())
                .toList();
    }

    public Optional<Practitioner> getPractitionerById(String id) {
        try {
            IGenericClient client = hapiClient.getClient();
            Practitioner practitioner = client
                    .read()
                    .resource(Practitioner.class)
                    .withId(id)
                    .execute();
            return Optional.of(practitioner);
        } catch (Exception e) {
            System.err.println("Could not find practitioner with ID: " + id);
            return Optional.empty();
        }
    }

    public List<Practitioner> searchPractitionerByName(String name) {
        try {
            IGenericClient client = hapiClient.getClient();

            Bundle bundle = client
                    .search()
                    .forResource(Practitioner.class)
                    .where(Practitioner.NAME.matches().value(name))
                    .returnBundle(Bundle.class)
                    .execute();

            return BundleUtil.toListOfEntries(hapiClient.getContext(), bundle)
                    .stream()
                    .map(entry -> (Practitioner) entry.getResource())
                    .toList();
        } catch (Exception e) {
            System.err.println("Could not search practitioner with name: " + name);
            e.printStackTrace();
            return List.of();
        }
    }
}