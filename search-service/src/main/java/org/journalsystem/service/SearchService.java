package org.journalsystem.service;

import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.eclipse.microprofile.rest.client.inject.RestClient;
import org.journalsystem.client.FhirClient;
import org.journalsystem.dto.PatientSearchResult;

import java.util.ArrayList;
import java.util.List;

@ApplicationScoped
public class SearchService {

    @Inject
    @RestClient
    FhirClient fhirClient;

    public Uni<List<PatientSearchResult>> searchPatientsByName(String name) {
        return Uni.createFrom().item(() -> {
            // TODO: Parse FHIR response and convert to DTOs
            // För nu returnerar vi en tom lista
            System.out.println("Searching patients by name: " + name);
            return new ArrayList<PatientSearchResult>();
        });
    }

    public Uni<List<PatientSearchResult>> searchPatientsByCondition(String condition) {
        return Uni.createFrom().item(() -> {
            System.out.println("Searching patients by condition: " + condition);
            return new ArrayList<PatientSearchResult>();
        });
    }
}