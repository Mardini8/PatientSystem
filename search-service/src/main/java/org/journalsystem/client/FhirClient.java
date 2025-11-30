package org.journalsystem.client;

import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import org.eclipse.microprofile.rest.client.inject.RegisterRestClient;
import org.journalsystem.dto.fhir.FhirBundle;

@RegisterRestClient(configKey = "fhir-api")
@Produces(MediaType.APPLICATION_JSON)
public interface FhirClient {

    @GET
    @Path("/Patient")
    FhirBundle searchPatients(@QueryParam("name") String name);

    @GET
    @Path("/Patient/{id}")
    FhirBundle.FhirResource getPatient(@PathParam("id") String id);

    @GET
    @Path("/Condition")
    FhirBundle searchConditions(@QueryParam("code:text") String conditionText);

    @GET
    @Path("/Condition")
    FhirBundle getConditionsForPatient(@QueryParam("subject") String patientId);

    @GET
    @Path("/Practitioner/{id}")
    FhirBundle.FhirResource getPractitioner(@PathParam("id") String id);

    @GET
    @Path("/Encounter")
    FhirBundle searchEncounters(
            @QueryParam("practitioner") String practitionerId,
            @QueryParam("date") String date
    );

    @GET
    @Path("/Encounter")
    FhirBundle getEncountersForPractitioner(@QueryParam("participant") String practitionerId);
}