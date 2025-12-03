package org.journalsystem.service;

import org.journalsystem.client.FhirClient;
import org.journalsystem.dto.*;
import org.journalsystem.dto.fhir.FhirBundle;
import org.journalsystem.mapper.FhirMapper;

import io.smallrye.mutiny.Uni;
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

        return Uni.createFrom().item(() -> {
            try {
                FhirBundle bundle = fhirClient.searchPatients(name);
                LOG.infof("FHIR bundle from server: %s", bundle);

                if (bundle != null) {
                    LOG.infof("Bundle total field: %d", bundle.total);
                    LOG.infof("Bundle entry size: %d",
                            bundle.entry == null ? -1 : bundle.entry.size());
                }

                List<PatientSearchResult> results =
                        FhirMapper.bundleToPatientList(bundle);
                LOG.infof("Found %d patients after mapping", results.size());
                return results;
            } catch (Exception e) {
                LOG.error("Error searching patients", e);
                return new ArrayList<>();
            }
        });
    }

    /**
     * Search patients by condition/diagnosis
     */
    public Uni<List<PatientSearchResult>> searchPatientsByCondition(String condition) {
        LOG.infof("Searching patients by condition: %s", condition);

        return Uni.createFrom().item(() -> {
            try {
                // Step 1: Search for conditions matching the text
                FhirBundle conditionBundle = fhirClient.searchConditions(condition);

                if (conditionBundle == null || conditionBundle.entry == null) {
                    return new ArrayList<>();
                }

                // Step 2: Extract unique patient IDs from conditions
                List<String> patientIds = new ArrayList<>();
                for (FhirBundle.BundleEntry entry : conditionBundle.entry) {
                    if (entry.resource.subject != null && entry.resource.subject.reference != null) {
                        String patientId = entry.resource.subject.reference.replace("Patient/", "");
                        if (!patientIds.contains(patientId)) {
                            patientIds.add(patientId);
                        }
                    }
                }

                // Step 3: Fetch patient details for each ID
                List<PatientSearchResult> results = new ArrayList<>();
                for (String patientId : patientIds) {
                    try {
                        FhirBundle.FhirResource patient = fhirClient.getPatient(patientId);
                        PatientSearchResult result = FhirMapper.toPatientSearchResult(patient);
                        if (result != null) {
                            results.add(result);
                        }
                    } catch (Exception e) {
                        LOG.warnf("Could not fetch patient %s: %s", patientId, e.getMessage());
                    }
                }

                LOG.infof("Found %d patients with condition", results.size());
                return results;
            } catch (Exception e) {
                LOG.errorf("Error searching patients by condition: %s", e.getMessage());
                return new ArrayList<>();
            }
        });
    }

    /**
     * Search patients by practitioner ID or identifier
     * If the input is a numeric identifier (like 9999994392), it will first resolve it to the actual FHIR ID
     */
    public Uni<List<PatientSearchResult>> searchPatientsByPractitionerId(String practitionerIdOrIdentifier) {
        LOG.infof("Searching patients by practitioner ID/identifier: %s", practitionerIdOrIdentifier);

        return Uni.createFrom().item(() -> {
            try {
                String actualPractitionerId = resolvePractitionerId(practitionerIdOrIdentifier);

                if (actualPractitionerId == null) {
                    LOG.warnf("Could not resolve practitioner ID for: %s", practitionerIdOrIdentifier);
                    return new ArrayList<>();
                }

                LOG.infof("Resolved practitioner ID: %s", actualPractitionerId);

                // Use the actual practitioner ID
                Set<String> uniquePatientIds = new HashSet<>();

                // Ensure the practitioner reference has the correct format
                String practitionerReference = actualPractitionerId.startsWith("Practitioner/")
                        ? actualPractitionerId
                        : "Practitioner/" + actualPractitionerId;

                LOG.infof("Searching with practitioner reference: %s", practitionerReference);

                // Search encounters where this practitioner participated
                try {
                    FhirBundle encounterBundle = fhirClient.searchEncountersByPractitioner(practitionerReference);
                    if (encounterBundle != null && encounterBundle.entry != null) {
                        LOG.infof("Found %d encounters", encounterBundle.entry.size());
                        for (FhirBundle.BundleEntry entry : encounterBundle.entry) {
                            if (entry.resource.subject != null && entry.resource.subject.reference != null) {
                                String patientId = entry.resource.subject.reference.replace("Patient/", "");
                                uniquePatientIds.add(patientId);
                            }
                        }
                    }
                } catch (Exception e) {
                    LOG.warnf("Could not search encounters for practitioner %s: %s", actualPractitionerId, e.getMessage());
                }

                // Search care teams where this practitioner is a participant
                try {
                    FhirBundle careTeamBundle = fhirClient.searchCareTeamsByPractitioner(practitionerReference);
                    if (careTeamBundle != null && careTeamBundle.entry != null) {
                        LOG.infof("Found %d care teams", careTeamBundle.entry.size());
                        for (FhirBundle.BundleEntry entry : careTeamBundle.entry) {
                            if (entry.resource.subject != null && entry.resource.subject.reference != null) {
                                String patientId = entry.resource.subject.reference.replace("Patient/", "");
                                uniquePatientIds.add(patientId);
                            }
                        }
                    }
                } catch (Exception e) {
                    LOG.warnf("Could not search care teams for practitioner %s: %s", actualPractitionerId, e.getMessage());
                }

                LOG.infof("Found %d unique patient IDs", uniquePatientIds.size());

                // Fetch patient details for each unique ID
                List<PatientSearchResult> results = new ArrayList<>();
                for (String patientId : uniquePatientIds) {
                    try {
                        FhirBundle.FhirResource patient = fhirClient.getPatient(patientId);
                        PatientSearchResult result = FhirMapper.toPatientSearchResult(patient);
                        if (result != null) {
                            results.add(result);
                        }
                    } catch (Exception e) {
                        LOG.warnf("Could not fetch patient %s: %s", patientId, e.getMessage());
                    }
                }

                LOG.infof("Found %d patients for practitioner", results.size());
                return results;
            } catch (Exception e) {
                LOG.errorf("Error searching patients by practitioner: %s", e.getMessage());
                return new ArrayList<>();
            }
        });
    }

    /**
     * Search encounters by practitioner ID/identifier and optional date
     * If date is null, returns all encounters for the practitioner
     * If date is provided, returns only encounters on that specific date
     */
    public Uni<List<EncounterSearchResult>> searchEncountersByPractitioner(
            String practitionerIdOrIdentifier,
            String date) {

        LOG.infof("Searching encounters by practitioner: %s, date: %s", practitionerIdOrIdentifier, date);

        return Uni.createFrom().item(() -> {
            try {
                // Step 1: Resolve practitioner ID
                String actualPractitionerId = resolvePractitionerId(practitionerIdOrIdentifier);

                if (actualPractitionerId == null) {
                    LOG.warnf("Could not resolve practitioner ID for: %s", practitionerIdOrIdentifier);
                    return new ArrayList<>();
                }

                LOG.infof("Resolved practitioner ID: %s", actualPractitionerId);

                // Step 2: Search encounters
                FhirBundle encounterBundle;
                if (date != null && !date.trim().isEmpty()) {
                    // Search with both practitioner and date
                    LOG.infof("Searching encounters for practitioner %s on date %s", actualPractitionerId, date);
                    encounterBundle = fhirClient.searchEncountersByPractitionerAndDate(actualPractitionerId, date);
                } else {
                    // Search with only practitioner
                    LOG.infof("Searching all encounters for practitioner %s", actualPractitionerId);
                    encounterBundle = fhirClient.searchEncountersByPractitionerOnly(actualPractitionerId);
                }

                if (encounterBundle == null || encounterBundle.entry == null) {
                    LOG.infof("No encounters found");
                    return new ArrayList<>();
                }

                LOG.infof("Found %d encounters", encounterBundle.entry.size());

                // Step 3: Convert to EncounterSearchResult with patient and practitioner names
                List<EncounterSearchResult> results = new ArrayList<>();
                for (FhirBundle.BundleEntry entry : encounterBundle.entry) {
                    try {
                        // Get patient name
                        String patientName = "";
                        String patientId = "";
                        if (entry.resource.subject != null && entry.resource.subject.reference != null) {
                            patientId = entry.resource.subject.reference.replace("Patient/", "");
                            try {
                                FhirBundle.FhirResource patient = fhirClient.getPatient(patientId);
                                patientName = getFullName(patient);
                            } catch (Exception e) {
                                LOG.warnf("Could not fetch patient name for %s: %s", patientId, e.getMessage());
                            }
                        }

                        // Get practitioner name
                        String practitionerName = "";
                        try {
                            FhirBundle.FhirResource practitioner = fhirClient.getPractitioner(actualPractitionerId);
                            practitionerName = getFullName(practitioner);
                        } catch (Exception e) {
                            LOG.warnf("Could not fetch practitioner name for %s: %s", actualPractitionerId, e.getMessage());
                        }

                        // Use FhirMapper to create the result
                        EncounterSearchResult result = FhirMapper.toEncounterSearchResult(
                                entry.resource,
                                patientName,
                                practitionerName
                        );

                        if (result != null) {
                            results.add(result);
                        }
                    } catch (Exception e) {
                        LOG.warnf("Could not map encounter: %s", e.getMessage());
                    }
                }

                LOG.infof("Returning %d encounter results", results.size());
                return results;
            } catch (Exception e) {
                LOG.errorf("Error searching encounters by practitioner: %s", e.getMessage());
                return new ArrayList<>();
            }
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

    /**
     * Resolve a practitioner identifier (like 9999994392) to the actual FHIR ID
     * If input is already a UUID format, return as-is
     * Otherwise, search by identifier to get the actual ID
     */
    private String resolvePractitionerId(String idOrIdentifier) {
        // Check if it's already a UUID format (contains hyphens)
        if (idOrIdentifier.contains("-")) {
            // Remove "Practitioner/" prefix if present
            return idOrIdentifier.replace("Practitioner/", "");
        }

        // It's likely an identifier (numeric), so we need to search for it
        try {
            LOG.infof("Searching practitioner by identifier: %s", idOrIdentifier);
            FhirBundle bundle = fhirClient.searchPractitionerByIdentifier(idOrIdentifier);

            if (bundle != null && bundle.entry != null && !bundle.entry.isEmpty()) {
                // Get the first matching practitioner's ID
                String practitionerId = bundle.entry.get(0).resource.id;
                LOG.infof("Resolved identifier %s to ID: %s", idOrIdentifier, practitionerId);
                return practitionerId;
            } else {
                LOG.warnf("No practitioner found with identifier: %s", idOrIdentifier);
                return null;
            }
        } catch (Exception e) {
            LOG.errorf("Error resolving practitioner identifier %s: %s", idOrIdentifier, e.getMessage());
            return null;
        }
    }

}