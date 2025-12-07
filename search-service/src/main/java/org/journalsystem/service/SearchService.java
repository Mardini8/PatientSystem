package org.journalsystem.service;

import org.journalsystem.client.FhirClient;
import org.journalsystem.dto.*;
import org.journalsystem.dto.fhir.FhirBundle;
import org.journalsystem.mapper.FhirMapper;

import io.smallrye.mutiny.Uni;
import io.smallrye.mutiny.Multi;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.eclipse.microprofile.rest.client.inject.RestClient;
import org.jboss.logging.Logger;

import java.util.ArrayList;
import java.util.List;
import java.util.HashSet;
import java.util.Set;

@ApplicationScoped
public class SearchService {

    private static final Logger LOG = Logger.getLogger(SearchService.class);

    @Inject
    @RestClient
    FhirClient fhirClient;

    /**
     * Search patients by name
     */
    public Uni<List<PatientSearchResult>> searchPatientsByName(String name) {
        LOG.infof("Searching patients by name: %s", name);

        return fhirClient.searchPatients(name)
                .onItem().transform(bundle -> {
                    LOG.infof("Bundle total field: %d", bundle != null ? bundle.total : 0);
                    return FhirMapper.bundleToPatientList(bundle);
                })
                .onFailure().recoverWithItem(e -> {
                    LOG.error("Error searching patients", e);
                    return new ArrayList<PatientSearchResult>();
                });
    }

    /**
     * Search patients by condition
     */
    public Uni<List<PatientSearchResult>> searchPatientsByCondition(String condition) {
        LOG.infof("Searching patients by condition: %s", condition);

        return fhirClient.searchConditions(condition)
                .onItem().transformToUni(conditionBundle -> {
                    if (conditionBundle == null || conditionBundle.entry == null) {
                        return Uni.createFrom().item(new ArrayList<PatientSearchResult>());
                    }

                    // Extract unique patient IDs
                    Set<String> patientIds = new HashSet<>();
                    for (FhirBundle.BundleEntry entry : conditionBundle.entry) {
                        if (entry.resource.subject != null && entry.resource.subject.reference != null) {
                            String patientId = entry.resource.subject.reference.replace("Patient/", "");
                            patientIds.add(patientId);
                        }
                    }

                    // Fetch all patients in parallel using Multi
                    Multi<PatientSearchResult> patientsMulti = Multi.createFrom().iterable(patientIds)
                            .onItem().transformToUniAndMerge(patientId ->
                                    fhirClient.getPatient(patientId)
                                            .onItem().transform(FhirMapper::toPatientSearchResult)
                                            .onFailure().recoverWithNull()
                            )
                            .filter(result -> result != null);

                    return patientsMulti.collect().asList();
                })
                .onFailure().recoverWithItem(e -> {
                    LOG.errorf("Error searching patients by condition: %s", e.getMessage());
                    return new ArrayList<PatientSearchResult>();
                });
    }

    /**
     * Search patients by practitioner ID
     */
    public Uni<List<PatientSearchResult>> searchPatientsByPractitionerId(String practitionerIdOrIdentifier) {
        LOG.infof("Searching patients by practitioner ID/identifier: %s", practitionerIdOrIdentifier);

        return resolvePractitionerIdReactive(practitionerIdOrIdentifier)
                .onItem().transformToUni(actualPractitionerId -> {
                    if (actualPractitionerId == null) {
                        LOG.warnf("Could not resolve practitioner ID for: %s", practitionerIdOrIdentifier);
                        return Uni.createFrom().item(new ArrayList<PatientSearchResult>());
                    }

                    String practitionerReference = actualPractitionerId.startsWith("Practitioner/")
                            ? actualPractitionerId
                            : "Practitioner/" + actualPractitionerId;

                    LOG.infof("Searching with practitioner reference: %s", practitionerReference);

                    // Fetch encounters
                    return fhirClient.searchEncountersByPractitioner(practitionerReference)
                            .onFailure().recoverWithItem(new FhirBundle())
                            .onItem().transformToUni(encounterBundle -> {
                                Set<String> uniquePatientIds = new HashSet<>();

                                // Extract patient IDs from encounters
                                if (encounterBundle != null && encounterBundle.entry != null) {
                                    for (FhirBundle.BundleEntry entry : encounterBundle.entry) {
                                        if (entry.resource.subject != null && entry.resource.subject.reference != null) {
                                            String patientId = entry.resource.subject.reference.replace("Patient/", "");
                                            uniquePatientIds.add(patientId);
                                        }
                                    }
                                }

                                LOG.infof("Found %d unique patient IDs", uniquePatientIds.size());

                                // Fetch all patients in parallel
                                Multi<PatientSearchResult> patientsMulti = Multi.createFrom().iterable(uniquePatientIds)
                                        .onItem().transformToUniAndMerge(patientId ->
                                                fhirClient.getPatient(patientId)
                                                        .onItem().transform(FhirMapper::toPatientSearchResult)
                                                        .onFailure().recoverWithNull()
                                        )
                                        .filter(result -> result != null);

                                return patientsMulti.collect().asList();
                            });
                })
                .onFailure().recoverWithItem(e -> {
                    LOG.errorf("Error searching patients by practitioner: %s", e.getMessage());
                    return new ArrayList<PatientSearchResult>();
                });
    }

    /**
     * Resolve practitioner identifier to FHIR ID
     */
    private Uni<String> resolvePractitionerIdReactive(String idOrIdentifier) {
        // Check if it's already a UUID format (contains hyphens)
        if (idOrIdentifier.contains("-")) {
            return Uni.createFrom().item(idOrIdentifier.replace("Practitioner/", ""));
        }

        // Search by identifier
        LOG.infof("Searching practitioner by identifier: %s", idOrIdentifier);
        return fhirClient.searchPractitionerByIdentifier(idOrIdentifier)
                .onItem().transform(bundle -> {
                    if (bundle != null && bundle.entry != null && !bundle.entry.isEmpty()) {
                        String practitionerId = bundle.entry.get(0).resource.id;
                        LOG.infof("Resolved identifier %s to ID: %s", idOrIdentifier, practitionerId);
                        return practitionerId;
                    } else {
                        LOG.warnf("No practitioner found with identifier: %s", idOrIdentifier);
                        return null;
                    }
                })
                .onFailure().recoverWithItem(e -> {
                    LOG.errorf("Error resolving practitioner identifier %s: %s", idOrIdentifier, e.getMessage());
                    return null;
                });
    }

    /**
     * Search encounters by practitioner
     */
    public Uni<List<EncounterSearchResult>> searchEncountersByPractitioner(
            String practitionerIdOrIdentifier,
            String date) {

        LOG.infof("Searching encounters by practitioner: %s, date: %s", practitionerIdOrIdentifier, date);

        return resolvePractitionerIdReactive(practitionerIdOrIdentifier)
                .onItem().transformToUni(actualPractitionerId -> {
                    if (actualPractitionerId == null) {
                        LOG.warnf("Could not resolve practitioner ID for: %s", practitionerIdOrIdentifier);
                        return Uni.createFrom().item(new ArrayList<EncounterSearchResult>());
                    }

                    LOG.infof("Resolved practitioner ID: %s", actualPractitionerId);

                    // Search encounters based on whether date is provided
                    Uni<FhirBundle> encounterBundleUni;
                    if (date != null && !date.trim().isEmpty()) {
                        LOG.infof("Searching encounters for practitioner %s on date %s", actualPractitionerId, date);
                        encounterBundleUni = fhirClient.searchEncountersByPractitionerAndDate(actualPractitionerId, date);
                    } else {
                        LOG.infof("Searching all encounters for practitioner %s", actualPractitionerId);
                        encounterBundleUni = fhirClient.searchEncountersByPractitionerOnly(actualPractitionerId);
                    }

                    return encounterBundleUni
                            .onItem().transformToUni(encounterBundle -> {
                                if (encounterBundle == null || encounterBundle.entry == null) {
                                    LOG.infof("No encounters found");
                                    return Uni.createFrom().item(new ArrayList<EncounterSearchResult>());
                                }

                                LOG.infof("Found %d encounters", encounterBundle.entry.size());

                                // Process each encounter reactively
                                Multi<EncounterSearchResult> encountersMulti = Multi.createFrom().iterable(encounterBundle.entry)
                                        .onItem().transformToUniAndMerge(entry ->
                                                mapToEncounterSearchResultReactive(entry.resource, actualPractitionerId)
                                        )
                                        .filter(result -> result != null);

                                return encountersMulti.collect().asList();
                            });
                })
                .onFailure().recoverWithItem(e -> {
                    LOG.errorf("Error searching encounters by practitioner: %s", e.getMessage());
                    return new ArrayList<EncounterSearchResult>();
                });
    }

    /**
     *  Map FHIR resource to EncounterSearchResult
     */
    private Uni<EncounterSearchResult> mapToEncounterSearchResultReactive(
            FhirBundle.FhirResource resource,
            String practitionerId) {

        String encounterId = resource.id;
        String patientId = null;

        // Extract patient ID
        if (resource.subject != null && resource.subject.reference != null) {
            patientId = resource.subject.reference.replace("Patient/", "");
        }

        if (patientId == null) {
            return Uni.createFrom().nullItem();
        }

        String finalPatientId = patientId;

        // Fetch patient and practitioner names in parallel
        Uni<String> patientNameUni = fhirClient.getPatient(patientId)
                .onItem().transform(this::getFullName)
                .onFailure().recoverWithItem("");

        Uni<String> practitionerNameUni = fhirClient.getPractitioner(practitionerId)
                .onItem().transform(this::getFullName)
                .onFailure().recoverWithItem("");

        return Uni.combine().all().unis(patientNameUni, practitionerNameUni)
                .asTuple()
                .onItem().transform(tuple -> {
                    String patientName = tuple.getItem1();
                    String practitionerName = tuple.getItem2();

                    return FhirMapper.toEncounterSearchResult(
                            resource,
                            patientName,
                            practitionerName
                    );
                });
    }

    /**
     * Get full name from FHIR resource
     */
    private String getFullName(FhirBundle.FhirResource resource) {
        if (resource == null || resource.name == null || resource.name.isEmpty()) {
            return "";
        }

        FhirBundle.HumanName name = resource.name.get(0);
        StringBuilder fullName = new StringBuilder();

        if (name.given != null && !name.given.isEmpty()) {
            fullName.append(String.join(" ", name.given));
        }

        if (name.family != null && !name.family.isEmpty()) {
            if (fullName.length() > 0) {
                fullName.append(" ");
            }
            fullName.append(name.family);
        }

        return fullName.toString();
    }
}