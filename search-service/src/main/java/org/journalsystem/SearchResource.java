package org.journalsystem;

import io.smallrye.mutiny.Uni;
import jakarta.inject.Inject;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import org.journalsystem.dto.PatientSearchResult;
import org.journalsystem.service.SearchService;

import java.util.List;

@Path("/api/search")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class SearchResource {

    @Inject
    SearchService searchService;

    @GET
    @Path("/patients")
    public Uni<List<PatientSearchResult>> searchPatients(
            @QueryParam("name") String name,
            @QueryParam("condition") String condition
    ) {
        if (name != null && !name.isEmpty()) {
            return searchService.searchPatientsByName(name);
        } else if (condition != null && !condition.isEmpty()) {
            return searchService.searchPatientsByCondition(condition);
        }
        return Uni.createFrom().item(List.of());
    }

    @GET
    @Path("/hello")
    public String hello() {
        return "Search Service is running on Quarkus!";
    }
}