package org.journalsystem.client;

import io.smallrye.mutiny.Uni;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import org.eclipse.microprofile.rest.client.inject.RegisterRestClient;
import org.journalsystem.dto.fhir.FhirBundle;

@RegisterRestClient(configKey = "fhir-api")
@Produces(MediaType.APPLICATION_JSON)
public interface FhirClient {

    @GET
    @Path("/Patient")
    Uni<FhirBundle> searchPatients(@QueryParam("name") String name);

    @GET
    @Path("/Patient/{id}")
    Uni<FhirBundle.FhirResource> getPatient(@PathParam("id") String id);

    @GET
    @Path("/Condition")
    Uni<FhirBundle> searchConditions(@QueryParam("code:text") String conditionText);

    // Practitioner methods
    @GET
    @Path("/Practitioner")
    Uni<FhirBundle> searchPractitioners(@QueryParam("name") String name);

    @GET
    @Path("/Practitioner")
    Uni<FhirBundle> searchPractitionerByIdentifier(@QueryParam("identifier") String identifier);

    @GET
    @Path("/Practitioner/{id}")
    Uni<FhirBundle.FhirResource> getPractitioner(@PathParam("id") String id);

    // Encounter methods
    @GET
    @Path("/Encounter")
    Uni<FhirBundle> searchEncountersByPractitioner(@QueryParam("participant") String practitionerId);

    @GET
    @Path("/Encounter")
    Uni<FhirBundle> searchEncountersByPractitionerAndDate(
            @QueryParam("practitioner") String practitionerId,
            @QueryParam("date") String date
    );

    @GET
    @Path("/Encounter")
    Uni<FhirBundle> searchEncountersByPractitionerOnly(@QueryParam("practitioner") String practitionerId);

}