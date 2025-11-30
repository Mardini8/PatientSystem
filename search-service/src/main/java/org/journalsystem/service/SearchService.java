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

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

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
                LOG.error("Error searching patients", e);  // skriv ut hela stack trace
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
     * Get all patients for a specific doctor
     */
    public Uni<DoctorPatientsResult> getDoctorPatients(String doctorId) {
        LOG.infof("Getting patients for doctor: %s", doctorId);

        return Uni.createFrom().item(() -> {
            try {
                // Get doctor info
                FhirBundle.FhirResource doctor = fhirClient.getPractitioner(doctorId);
                String doctorName = extractPractitionerName(doctor);

                // Get encounters for this doctor
                FhirBundle encounterBundle = fhirClient.getEncountersForPractitioner("Practitioner/" + doctorId);

                // Extract unique patient IDs
                List<String> patientIds = new ArrayList<>();
                if (encounterBundle != null && encounterBundle.entry != null) {
                    for (FhirBundle.BundleEntry entry : encounterBundle.entry) {
                        // Extract patient reference from encounter
                        // Note: This is simplified, you may need to parse the subject field
                        if (entry.resource.subject != null && entry.resource.subject.reference != null) {
                            String patientId = entry.resource.subject.reference.replace("Patient/", "");
                            if (!patientIds.contains(patientId)) {
                                patientIds.add(patientId);
                            }
                        }
                    }
                }

                // Fetch patient details
                List<PatientSearchResult> patients = new ArrayList<>();
                for (String patientId : patientIds) {
                    try {
                        FhirBundle.FhirResource patient = fhirClient.getPatient(patientId);
                        PatientSearchResult result = FhirMapper.toPatientSearchResult(patient);
                        if (result != null) {
                            patients.add(result);
                        }
                    } catch (Exception e) {
                        LOG.warnf("Could not fetch patient %s: %s", patientId, e.getMessage());
                    }
                }

                LOG.infof("Found %d patients for doctor", patients.size());
                return new DoctorPatientsResult(doctorId, doctorName, patients);
            } catch (Exception e) {
                LOG.errorf("Error getting doctor patients: %s", e.getMessage());
                return new DoctorPatientsResult(doctorId, "Unknown", new ArrayList<>());
            }
        });
    }

    /**
     * Get encounters for a doctor on a specific date
     */
    public Uni<List<EncounterSearchResult>> getDoctorEncountersByDate(String doctorId, LocalDate date) {
        LOG.infof("Getting encounters for doctor %s on date %s", doctorId, date);

        return Uni.createFrom().item(() -> {
            try {
                // Format date as YYYY-MM-DD
                String dateStr = date.format(DateTimeFormatter.ISO_LOCAL_DATE);

                // Search encounters by practitioner and date
                FhirBundle bundle = fhirClient.searchEncounters("Practitioner/" + doctorId, dateStr);

                List<EncounterSearchResult> results = FhirMapper.bundleToEncounterList(bundle);

                LOG.infof("Found %d encounters", results.size());
                return results;
            } catch (Exception e) {
                LOG.errorf("Error getting doctor encounters: %s", e.getMessage());
                return new ArrayList<>();
            }
        });
    }

    /**
     * Helper method to extract practitioner name
     */
    private String extractPractitionerName(FhirBundle.FhirResource practitioner) {
        if (practitioner == null || practitioner.name == null || practitioner.name.isEmpty()) {
            return "Unknown Doctor";
        }

        FhirBundle.HumanName name = practitioner.name.get(0);
        String firstName = (name.given != null && !name.given.isEmpty()) ? name.given.get(0) : "";
        String lastName = name.family != null ? name.family : "";

        return (firstName + " " + lastName).trim();
    }
}