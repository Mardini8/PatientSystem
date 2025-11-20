package com.journalSystem.clinical_service.service;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.rest.client.api.IGenericClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class HapiClientService {

    private final IGenericClient client;
    private final FhirContext context;

    public HapiClientService(@Value("${fhir.server.url}") String fhirServerUrl) {
        this.context = FhirContext.forR4();
        this.client = context.newRestfulGenericClient(fhirServerUrl);
        System.out.println("✓ HAPI FHIR Client initialized: " + fhirServerUrl);
    }

    public IGenericClient getClient() {
        return client;
    }

    public FhirContext getContext() {
        return context;
    }
}