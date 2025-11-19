package com.PatientSystem.PatientSystem.service;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.rest.client.api.IGenericClient;
import org.springframework.stereotype.Service;

@Service
public class HapiClientService {

    private final IGenericClient client;
    private final FhirContext context;

    public HapiClientService() {
        this.context = FhirContext.forR4();

        String baseURL = "https://hapi-fhir.app.cloud.cbh.kth.se/fhir";

        this.client = context.newRestfulGenericClient(baseURL);

        System.out.println("HAPI FHIR Client initialiserad med URL: " + baseURL);
    }

    public IGenericClient getClient() {
        return client;
    }

    public FhirContext getContext() {
        return context;
    }
}