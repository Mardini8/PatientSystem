package org.journalsystem.client;

import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.QueryParam;
import org.eclipse.microprofile.rest.client.inject.RegisterRestClient;

@RegisterRestClient(configKey = "fhir-api")
public interface FhirClient {

    @GET
    @Path("/Patient")
    String searchPatients(@QueryParam("name") String name);

    @GET
    @Path("/Patient/{id}")
    String getPatient(@PathParam("id") String id);

    @GET
    @Path("/Condition")
    String searchConditions(@QueryParam("code:text") String conditionText);

    @GET
    @Path("/Practitioner/{id}")
    String getPractitioner(@PathParam("id") String id);

    @GET
    @Path("/Encounter")
    String searchEncounters(
            @QueryParam("practitioner") String practitionerId,
            @QueryParam("date") String date
    );
}