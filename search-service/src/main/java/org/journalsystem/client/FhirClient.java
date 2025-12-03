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

    // Practitioner methods
    @GET
    @Path("/Practitioner")
    FhirBundle searchPractitioners(@QueryParam("name") String name);

    @GET
    @Path("/Practitioner")
    FhirBundle searchPractitionerByIdentifier(@QueryParam("identifier") String identifier);

    @GET
    @Path("/Practitioner/{id}")
    FhirBundle.FhirResource getPractitioner(@PathParam("id") String id);

    // Encounter methods
    @GET
    @Path("/Encounter")
    FhirBundle searchEncountersByPractitioner(@QueryParam("participant") String practitionerId);

    @GET
    @Path("/Encounter")
    FhirBundle searchEncountersByPractitionerAndDate(
            @QueryParam("practitioner") String practitionerId,
            @QueryParam("date") String date
    );

    @GET
    @Path("/Encounter")
    FhirBundle searchEncountersByPractitionerOnly(@QueryParam("practitioner") String practitionerId);

    @GET
    @Path("/CareTeam")
    FhirBundle searchCareTeamsByPractitioner(@QueryParam("participant") String practitionerId);

}