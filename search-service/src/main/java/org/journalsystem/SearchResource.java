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
     * Search patients by name or condition
     *
     * Examples:
     * GET /api/search/patients?name=Anna
     * GET /api/search/patients?condition=Diabetes
     */
    @Blocking
    @GET
    @Path("/patients")
    public Uni<Response> searchPatients(
            @QueryParam("name") String name,
            @QueryParam("condition") String condition
    ) {
        LOG.infof("Search patients - name: %s, condition: %s", name, condition);

        if (name != null && !name.trim().isEmpty()) {
            return searchService.searchPatientsByName(name.trim())
                    .map(results -> Response.ok(results).build());
        } else if (condition != null && !condition.trim().isEmpty()) {
            return searchService.searchPatientsByCondition(condition.trim())
                    .map(results -> Response.ok(results).build());
        }

        return Uni.createFrom().item(
                Response.status(Response.Status.BAD_REQUEST)
                        .entity("{\"error\": \"Please provide 'name' or 'condition' query parameter\"}")
                        .build()
        );
    }

    /**
     * Get all patients for a specific doctor
     *
     * Example:
     * GET /api/search/doctors/123/patients
     */
    @GET
    @Path("/doctors/{doctorId}/patients")
    public Uni<Response> getDoctorPatients(@PathParam("doctorId") String doctorId) {
        LOG.infof("Get patients for doctor: %s", doctorId);

        if (doctorId == null || doctorId.trim().isEmpty()) {
            return Uni.createFrom().item(
                    Response.status(Response.Status.BAD_REQUEST)
                            .entity("{\"error\": \"Doctor ID is required\"}")
                            .build()
            );
        }

        return searchService.getDoctorPatients(doctorId)
                .map(result -> Response.ok(result).build());
    }

    /**
     * Get encounters for a doctor on a specific date
     *
     * Example:
     * GET /api/search/doctors/123/encounters?date=2025-01-15
     */
    @GET
    @Path("/doctors/{doctorId}/encounters")
    public Uni<Response> getDoctorEncounters(
            @PathParam("doctorId") String doctorId,
            @QueryParam("date") String dateStr
    ) {
        LOG.infof("Get encounters for doctor %s on date %s", doctorId, dateStr);

        if (doctorId == null || doctorId.trim().isEmpty()) {
            return Uni.createFrom().item(
                    Response.status(Response.Status.BAD_REQUEST)
                            .entity("{\"error\": \"Doctor ID is required\"}")
                            .build()
            );
        }

        if (dateStr == null || dateStr.trim().isEmpty()) {
            return Uni.createFrom().item(
                    Response.status(Response.Status.BAD_REQUEST)
                            .entity("{\"error\": \"Date is required (format: YYYY-MM-DD)\"}")
                            .build()
            );
        }

        try {
            LocalDate date = LocalDate.parse(dateStr.trim());
            return searchService.getDoctorEncountersByDate(doctorId, date)
                    .map(results -> Response.ok(results).build());
        } catch (DateTimeParseException e) {
            return Uni.createFrom().item(
                    Response.status(Response.Status.BAD_REQUEST)
                            .entity("{\"error\": \"Invalid date format. Use YYYY-MM-DD\"}")
                            .build()
            );
        }
    }
}