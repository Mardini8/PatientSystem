package org.journalsystem;

import org.journalsystem.dto.*;
import org.journalsystem.service.SearchService;
import io.smallrye.mutiny.Uni;
import io.smallrye.common.annotation.Blocking;
import jakarta.inject.Inject;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.jboss.logging.Logger;

import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.List;

@Path("/api/search")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class SearchResource {

    private static final Logger LOG = Logger.getLogger(SearchResource.class);

    @Inject
    SearchService searchService;

    /**
     * Health check endpoint
     */
    @GET
    @Path("/hello")
    @Produces(MediaType.TEXT_PLAIN)
    public String hello() {
        return "Search Service is running on Quarkus Reactive!";
    }

    /**
     * Search patients by name, condition, or practitioner ID
     *
     * Examples:
     * GET /api/search/patients?name=Anna
     * GET /api/search/patients?condition=Diabetes
     * GET /api/search/patients?practitionerId=12345
     */
    @Blocking
    @GET
    @Path("/patients")
    public Uni<Response> searchPatients(
            @QueryParam("name") String name,
            @QueryParam("condition") String condition,
            @QueryParam("practitionerId") String practitionerId
    ) {
        LOG.infof("Search patients - name: %s, condition: %s, practitionerId: %s",
                name, condition, practitionerId);

        if (name != null && !name.trim().isEmpty()) {
            return searchService.searchPatientsByName(name.trim())
                    .map(results -> Response.ok(results).build());
        } else if (condition != null && !condition.trim().isEmpty()) {
            return searchService.searchPatientsByCondition(condition.trim())
                    .map(results -> Response.ok(results).build());
        } else if (practitionerId != null && !practitionerId.trim().isEmpty()) {
            return searchService.searchPatientsByPractitionerId(practitionerId.trim())
                    .map(results -> Response.ok(results).build());
        }

        return Uni.createFrom().item(
                Response.status(Response.Status.BAD_REQUEST)
                        .entity("{\"error\": \"Please provide 'name', 'condition', or 'practitionerId' query parameter\"}")
                        .build()
        );
    }

    /**
     * Search encounters by practitioner ID and optional date
     *
     * Examples:
     * GET /api/search/encounters?practitionerId=9999994392
     * GET /api/search/encounters?practitionerId=9999994392&date=1989-11-21
     * GET /api/search/encounters?practitionerId=aa21bb8e-dd17-3f9e-92ed-804c556a45d8&date=1989-11-21
     */
    @Blocking
    @GET
    @Path("/encounters")
    public Uni<Response> searchEncounters(
            @QueryParam("practitionerId") String practitionerId,
            @QueryParam("date") String date
    ) {
        LOG.infof("Search encounters - practitionerId: %s, date: %s", practitionerId, date);

        if (practitionerId == null || practitionerId.trim().isEmpty()) {
            return Uni.createFrom().item(
                    Response.status(Response.Status.BAD_REQUEST)
                            .entity("{\"error\": \"Please provide 'practitionerId' query parameter\"}")
                            .build()
            );
        }

        return searchService.searchEncountersByPractitioner(practitionerId.trim(), date)
                .map(results -> Response.ok(results).build());
    }
}