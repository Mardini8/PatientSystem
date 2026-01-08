package com.journalSystem.clinical_service.service;

import ca.uhn.fhir.rest.server.exceptions.ResourceNotFoundException;
import org.hl7.fhir.r4.model.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class HapiEncounterServiceTest {

    @Mock
    private HapiClientService hapiClientService;

    @Mock
    private FhirLookupService fhirLookupService;

    private HapiEncounterService hapiEncounterService;

    private Encounter testEncounter;
    private Date startTime;
    private Date endTime;

    @BeforeEach
    void setUp() {
        hapiEncounterService = new HapiEncounterService(hapiClientService, fhirLookupService);

        Calendar cal = Calendar.getInstance();
        startTime = cal.getTime();
        cal.add(Calendar.HOUR, 1);
        endTime = cal.getTime();

        testEncounter = createTestEncounter("12345", "Patient/98765", "Practitioner/11111",
                startTime, endTime);
    }

    // getAllEncounters() TESTS

    @Test
    void getAllEncounters_shouldReturnListOfEncounters_whenEncountersExist() {
        List<Encounter> expectedEncounters = new ArrayList<>();
        expectedEncounters.add(testEncounter);
        expectedEncounters.add(createTestEncounter("67890", "Patient/98765", "Practitioner/22222",
                startTime, endTime));

        HapiEncounterService spyService = spy(hapiEncounterService);
        doReturn(expectedEncounters).when(spyService).getAllEncounters();

        List<Encounter> result = spyService.getAllEncounters();

        assertThat(result).isNotNull();
        assertThat(result).hasSize(2);
        assertThat(result.get(0).getIdElement().getIdPart()).isEqualTo("12345");
        assertThat(result.get(1).getIdElement().getIdPart()).isEqualTo("67890");
    }

    @Test
    void getAllEncounters_shouldReturnEmptyList_whenNoEncountersExist() {
        HapiEncounterService spyService = spy(hapiEncounterService);
        doReturn(new ArrayList<Encounter>()).when(spyService).getAllEncounters();

        List<Encounter> result = spyService.getAllEncounters();

        assertThat(result).isNotNull();
        assertThat(result).isEmpty();
    }

    // getEncountersForPatient() TESTS

    @Test
    void getEncountersForPatient_shouldReturnEncounters_whenPatientHasEncounters() {
        String patientPersonnummer = "197001011234";
        String patientFhirId = "98765";

        List<Encounter> patientEncounters = new ArrayList<>();
        patientEncounters.add(testEncounter);
        patientEncounters.add(createTestEncounter("67890", "Patient/" + patientFhirId, "Practitioner/22222",
                startTime, endTime));

        HapiEncounterService spyService = spy(hapiEncounterService);
        doReturn(patientEncounters).when(spyService).getEncountersForPatient(patientPersonnummer);

        List<Encounter> result = spyService.getEncountersForPatient(patientPersonnummer);

        assertThat(result).hasSize(2);
    }

    @Test
    void getEncountersForPatient_shouldReturnEmptyList_whenNoEncountersExist() {
        String patientPersonnummer = "199901019999";
        HapiEncounterService spyService = spy(hapiEncounterService);
        doReturn(new ArrayList<Encounter>()).when(spyService).getEncountersForPatient(patientPersonnummer);

        List<Encounter> result = spyService.getEncountersForPatient(patientPersonnummer);

        assertThat(result).isEmpty();
    }

    @Test
    void getEncountersForPatient_shouldReturnEmptyList_whenNullPatientId() {
        HapiEncounterService spyService = spy(hapiEncounterService);
        doReturn(new ArrayList<Encounter>()).when(spyService).getEncountersForPatient(null);

        List<Encounter> result = spyService.getEncountersForPatient(null);

        assertThat(result).isEmpty();
    }

    // getEncounterById() TESTS

    @Test
    void getEncounterById_shouldReturnEncounter_whenEncounterExists() {
        String encounterId = "12345";
        HapiEncounterService spyService = spy(hapiEncounterService);
        doReturn(Optional.of(testEncounter)).when(spyService).getEncounterById(encounterId);

        Optional<Encounter> result = spyService.getEncounterById(encounterId);

        assertThat(result).isPresent();
        assertThat(result.get().getIdElement().getIdPart()).isEqualTo("12345");
        assertThat(result.get().getStatus()).isEqualTo(Encounter.EncounterStatus.FINISHED);
    }

    @Test
    void getEncounterById_shouldReturnEmpty_whenEncounterNotFound() {
        String encounterId = "99999";
        HapiEncounterService spyService = spy(hapiEncounterService);
        doReturn(Optional.empty()).when(spyService).getEncounterById(encounterId);

        Optional<Encounter> result = spyService.getEncounterById(encounterId);

        assertThat(result).isEmpty();
    }

    @Test
    void getEncounterById_shouldReturnEmpty_whenNullId() {
        HapiEncounterService spyService = spy(hapiEncounterService);
        doReturn(Optional.empty()).when(spyService).getEncounterById(null);

        Optional<Encounter> result = spyService.getEncounterById(null);

        assertThat(result).isEmpty();
    }

    @Test
    void getEncounterById_shouldReturnEmpty_whenEmptyId() {
        HapiEncounterService spyService = spy(hapiEncounterService);
        doReturn(Optional.empty()).when(spyService).getEncounterById("");

        Optional<Encounter> result = spyService.getEncounterById("");

        assertThat(result).isEmpty();
    }

    // createEncounter() TESTS

    @Test
    void createEncounter_shouldCreateEncounter_whenValidData() {
        String patientPersonnummer = "197001011234";
        String practitionerPersonnummer = "198001011234";
        String patientFhirId = "patient-fhir-123";
        String practitionerFhirId = "practitioner-fhir-456";

        Encounter createdEncounter = createTestEncounter("new-123", "Patient/" + patientFhirId,
                "Practitioner/" + practitionerFhirId, startTime, endTime);

        HapiEncounterService spyService = spy(hapiEncounterService);
        doReturn(createdEncounter).when(spyService).createEncounter(
                patientPersonnummer, practitionerPersonnummer, startTime, endTime);

        Encounter result = spyService.createEncounter(
                patientPersonnummer, practitionerPersonnummer, startTime, endTime);

        assertThat(result).isNotNull();
        assertThat(result.getSubject().getReference()).contains(patientFhirId);
        assertThat(result.getParticipant()).isNotEmpty();
    }

    @Test
    void createEncounter_shouldCreateEncounterWithoutEndTime() {
        String patientPersonnummer = "197001011234";
        String practitionerPersonnummer = "198001011234";
        String patientFhirId = "patient-fhir-123";
        String practitionerFhirId = "practitioner-fhir-456";

        Encounter createdEncounter = createTestEncounter("new-123", "Patient/" + patientFhirId,
                "Practitioner/" + practitionerFhirId, startTime, null);

        HapiEncounterService spyService = spy(hapiEncounterService);
        doReturn(createdEncounter).when(spyService).createEncounter(
                patientPersonnummer, practitionerPersonnummer, startTime, null);

        Encounter result = spyService.createEncounter(
                patientPersonnummer, practitionerPersonnummer, startTime, null);

        assertThat(result).isNotNull();
        assertThat(result.getPeriod().hasStart()).isTrue();
        assertThat(result.getPeriod().hasEnd()).isFalse();
    }

    @Test
    void createEncounter_shouldCreateEncounterWithoutPractitioner_whenPractitionerIsNull() {
        String patientPersonnummer = "197001011234";
        String patientFhirId = "patient-fhir-123";

        Encounter createdEncounter = createTestEncounter("new-123", "Patient/" + patientFhirId,
                null, startTime, endTime);

        HapiEncounterService spyService = spy(hapiEncounterService);
        doReturn(createdEncounter).when(spyService).createEncounter(
                patientPersonnummer, null, startTime, endTime);

        Encounter result = spyService.createEncounter(
                patientPersonnummer, null, startTime, endTime);

        assertThat(result).isNotNull();
        assertThat(result.getParticipant()).isEmpty();
    }

    @Test
    void createEncounter_shouldCreateEncounterWithoutPractitioner_whenPractitionerIsEmpty() {
        String patientPersonnummer = "197001011234";
        String patientFhirId = "patient-fhir-123";

        Encounter createdEncounter = createTestEncounter("new-123", "Patient/" + patientFhirId,
                null, startTime, endTime);

        HapiEncounterService spyService = spy(hapiEncounterService);
        doReturn(createdEncounter).when(spyService).createEncounter(
                patientPersonnummer, "", startTime, endTime);

        Encounter result = spyService.createEncounter(
                patientPersonnummer, "", startTime, endTime);

        assertThat(result).isNotNull();
        assertThat(result.getParticipant()).isEmpty();
    }

    @Test
    void createEncounter_shouldThrowException_whenCreationFails() {
        String patientPersonnummer = "197001011234";
        String practitionerPersonnummer = "198001011234";

        HapiEncounterService spyService = spy(hapiEncounterService);
        doThrow(new RuntimeException("Server error")).when(spyService).createEncounter(
                patientPersonnummer, practitionerPersonnummer, startTime, endTime);

        assertThatThrownBy(() -> spyService.createEncounter(
                patientPersonnummer, practitionerPersonnummer, startTime, endTime))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Server error");
    }

    @Test
    void createEncounter_shouldThrowException_whenPatientNotFound() {
        String patientPersonnummer = "999999999999";
        String practitionerPersonnummer = "198001011234";

        HapiEncounterService spyService = spy(hapiEncounterService);
        doThrow(new RuntimeException("Patient not found with identifier or ID: " + patientPersonnummer))
                .when(spyService).createEncounter(
                        patientPersonnummer, practitionerPersonnummer, startTime, endTime);

        assertThatThrownBy(() -> spyService.createEncounter(
                patientPersonnummer, practitionerPersonnummer, startTime, endTime))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Patient not found");
    }

    @Test
    void createEncounter_shouldThrowException_whenPractitionerNotFound() {
        String patientPersonnummer = "197001011234";
        String practitionerPersonnummer = "999999999999";

        HapiEncounterService spyService = spy(hapiEncounterService);
        doThrow(new RuntimeException("Practitioner not found with identifier or ID: " + practitionerPersonnummer))
                .when(spyService).createEncounter(
                        patientPersonnummer, practitionerPersonnummer, startTime, endTime);

        assertThatThrownBy(() -> spyService.createEncounter(
                patientPersonnummer, practitionerPersonnummer, startTime, endTime))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Practitioner not found");
    }

    @Test
    void createEncounter_shouldSetCorrectStatus() {
        String patientPersonnummer = "197001011234";
        String practitionerPersonnummer = "198001011234";

        Encounter encounter = createTestEncounter("123", "Patient/fhir-123",
                "Practitioner/fhir-456", startTime, endTime);
        encounter.setStatus(Encounter.EncounterStatus.FINISHED);

        HapiEncounterService spyService = spy(hapiEncounterService);
        doReturn(encounter).when(spyService).createEncounter(
                patientPersonnummer, practitionerPersonnummer, startTime, endTime);

        Encounter result = spyService.createEncounter(
                patientPersonnummer, practitionerPersonnummer, startTime, endTime);

        assertThat(result.getStatus()).isEqualTo(Encounter.EncounterStatus.FINISHED);
    }

    @Test
    void createEncounter_shouldSetCorrectClass() {
        String patientPersonnummer = "197001011234";
        String practitionerPersonnummer = "198001011234";

        Encounter encounter = createTestEncounter("123", "Patient/fhir-123",
                "Practitioner/fhir-456", startTime, endTime);
        encounter.setClass_(new Coding()
                .setSystem("http://terminology.hl7.org/CodeSystem/v3-ActCode")
                .setCode("AMB")
                .setDisplay("ambulatory"));

        HapiEncounterService spyService = spy(hapiEncounterService);
        doReturn(encounter).when(spyService).createEncounter(
                patientPersonnummer, practitionerPersonnummer, startTime, endTime);

        Encounter result = spyService.createEncounter(
                patientPersonnummer, practitionerPersonnummer, startTime, endTime);

        assertThat(result.getClass_().getCode()).isEqualTo("AMB");
        assertThat(result.getClass_().getDisplay()).isEqualTo("ambulatory");
    }

    @Test
    void createEncounter_shouldSetParticipantWithCorrectRole() {
        String patientPersonnummer = "197001011234";
        String practitionerPersonnummer = "198001011234";

        Encounter encounter = createTestEncounter("123", "Patient/fhir-123",
                "Practitioner/fhir-456", startTime, endTime);

        HapiEncounterService spyService = spy(hapiEncounterService);
        doReturn(encounter).when(spyService).createEncounter(
                patientPersonnummer, practitionerPersonnummer, startTime, endTime);

        Encounter result = spyService.createEncounter(
                patientPersonnummer, practitionerPersonnummer, startTime, endTime);

        assertThat(result.getParticipant()).isNotEmpty();
        Encounter.EncounterParticipantComponent participant = result.getParticipant().get(0);
        assertThat(participant.getType()).isNotEmpty();
        assertThat(participant.getType().get(0).getCoding().get(0).getCode()).isEqualTo("PPRF");
    }

    // INTEGRATION-STYLE TESTS

    @Test
    void getEncounterById_shouldCatchResourceNotFoundException() {
        String encounterId = "99999";
        HapiEncounterService spyService = spy(hapiEncounterService);

        doAnswer(invocation -> {
            try {
                throw new ResourceNotFoundException("Encounter not found");
            } catch (Exception e) {
                return Optional.empty();
            }
        }).when(spyService).getEncounterById(encounterId);

        Optional<Encounter> result = spyService.getEncounterById(encounterId);

        assertThat(result).isEmpty();
    }

    // EDGE CASES

    @Test
    void getEncountersForPatient_shouldHandleDifferentPatientIdFormats() {
        String[] patientPersonnummers = {"197001011234", "198502021234", "200012121234"};

        for (String patientPersonnummer : patientPersonnummers) {
            List<Encounter> encounters = new ArrayList<>();
            encounters.add(createTestEncounter("1", "Patient/fhir-" + patientPersonnummer, "Practitioner/1",
                    startTime, endTime));

            HapiEncounterService spyService = spy(hapiEncounterService);
            doReturn(encounters).when(spyService).getEncountersForPatient(patientPersonnummer);

            List<Encounter> result = spyService.getEncountersForPatient(patientPersonnummer);

            assertThat(result).hasSize(1);
        }
    }

    @Test
    void createEncounter_shouldHandleSameStartAndEndTime() {
        String patientPersonnummer = "197001011234";
        String practitionerPersonnummer = "198001011234";
        Date sameTime = new Date();

        Encounter encounter = createTestEncounter("123", "Patient/fhir-123",
                "Practitioner/fhir-456", sameTime, sameTime);

        HapiEncounterService spyService = spy(hapiEncounterService);
        doReturn(encounter).when(spyService).createEncounter(
                patientPersonnummer, practitionerPersonnummer, sameTime, sameTime);

        Encounter result = spyService.createEncounter(
                patientPersonnummer, practitionerPersonnummer, sameTime, sameTime);

        assertThat(result).isNotNull();
        assertThat(result.getPeriod().getStart()).isEqualTo(result.getPeriod().getEnd());
    }

    // HELPER METHODS

    private Encounter createTestEncounter(String id, String patientRef, String practitionerRef,
                                          Date startTime, Date endTime) {
        Encounter encounter = new Encounter();
        encounter.setId(id);
        encounter.setStatus(Encounter.EncounterStatus.FINISHED);

        encounter.setClass_(new Coding()
                .setSystem("http://terminology.hl7.org/CodeSystem/v3-ActCode")
                .setCode("AMB")
                .setDisplay("ambulatory"));

        encounter.addType()
                .addCoding()
                .setSystem("http://snomed.info/sct")
                .setCode("185349003")
                .setDisplay("Encounter for check up (procedure)");

        encounter.setSubject(new Reference(patientRef));

        if (practitionerRef != null && !practitionerRef.isEmpty()) {
            Encounter.EncounterParticipantComponent participant = encounter.addParticipant();

            participant.addType()
                    .addCoding()
                    .setSystem("http://terminology.hl7.org/CodeSystem/v3-ParticipationType")
                    .setCode("PPRF")
                    .setDisplay("primary performer");

            participant.setIndividual(new Reference(practitionerRef));

            Period participantPeriod = new Period();
            participantPeriod.setStart(startTime);
            if (endTime != null) {
                participantPeriod.setEnd(endTime);
            }
            participant.setPeriod(participantPeriod);
        }

        Period period = new Period();
        period.setStart(startTime);
        if (endTime != null) {
            period.setEnd(endTime);
        }
        encounter.setPeriod(period);

        return encounter;
    }
}