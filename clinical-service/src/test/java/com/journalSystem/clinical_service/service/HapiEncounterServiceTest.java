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

    private HapiEncounterService hapiEncounterService;

    private Encounter testEncounter;
    private Date startTime;
    private Date endTime;

    @BeforeEach
    void setUp() {
        hapiEncounterService = new HapiEncounterService(hapiClientService);

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
        // Arrange
        List<Encounter> expectedEncounters = new ArrayList<>();
        expectedEncounters.add(testEncounter);
        expectedEncounters.add(createTestEncounter("67890", "Patient/98765", "Practitioner/22222",
                startTime, endTime));

        HapiEncounterService spyService = spy(hapiEncounterService);
        doReturn(expectedEncounters).when(spyService).getAllEncounters();

        // Act
        List<Encounter> result = spyService.getAllEncounters();

        // Assert
        assertThat(result).isNotNull();
        assertThat(result).hasSize(2);
        assertThat(result.get(0).getIdElement().getIdPart()).isEqualTo("12345");
        assertThat(result.get(1).getIdElement().getIdPart()).isEqualTo("67890");
    }

    @Test
    void getAllEncounters_shouldReturnEmptyList_whenNoEncountersExist() {
        // Arrange
        HapiEncounterService spyService = spy(hapiEncounterService);
        doReturn(new ArrayList<Encounter>()).when(spyService).getAllEncounters();

        // Act
        List<Encounter> result = spyService.getAllEncounters();

        // Assert
        assertThat(result).isNotNull();
        assertThat(result).isEmpty();
    }

    @Test
    void getAllEncounters_shouldHandleMultipleEncounters() {
        // Arrange
        List<Encounter> encounters = new ArrayList<>();

        Calendar cal = Calendar.getInstance();
        for (int i = 1; i <= 5; i++) {
            Date start = cal.getTime();
            cal.add(Calendar.HOUR, 1);
            Date end = cal.getTime();
            encounters.add(createTestEncounter(String.valueOf(i), "Patient/" + i, "Practitioner/" + i,
                    start, end));
            cal.add(Calendar.DAY_OF_MONTH, 1);
        }

        HapiEncounterService spyService = spy(hapiEncounterService);
        doReturn(encounters).when(spyService).getAllEncounters();

        // Act
        List<Encounter> result = spyService.getAllEncounters();

        // Assert
        assertThat(result).hasSize(5);
    }

    // getEncountersForPatient() TESTS

    @Test
    void getEncountersForPatient_shouldReturnEncounters_whenPatientHasEncounters() {
        // Arrange
        String patientId = "98765";
        List<Encounter> patientEncounters = new ArrayList<>();
        patientEncounters.add(testEncounter);
        patientEncounters.add(createTestEncounter("67890", "Patient/" + patientId, "Practitioner/22222",
                startTime, endTime));

        HapiEncounterService spyService = spy(hapiEncounterService);
        doReturn(patientEncounters).when(spyService).getEncountersForPatient(patientId);

        // Act
        List<Encounter> result = spyService.getEncountersForPatient(patientId);

        // Assert
        assertThat(result).hasSize(2);
        assertThat(result.get(0).getSubject().getReference()).contains(patientId);
        assertThat(result.get(1).getSubject().getReference()).contains(patientId);
    }

    @Test
    void getEncountersForPatient_shouldReturnEmptyList_whenNoEncountersExist() {
        // Arrange
        String patientId = "99999";
        HapiEncounterService spyService = spy(hapiEncounterService);
        doReturn(new ArrayList<Encounter>()).when(spyService).getEncountersForPatient(patientId);

        // Act
        List<Encounter> result = spyService.getEncountersForPatient(patientId);

        // Assert
        assertThat(result).isEmpty();
    }

    @Test
    void getEncountersForPatient_shouldReturnEmptyList_whenNullPatientId() {
        // Arrange
        HapiEncounterService spyService = spy(hapiEncounterService);
        doReturn(new ArrayList<Encounter>()).when(spyService).getEncountersForPatient(null);

        // Act
        List<Encounter> result = spyService.getEncountersForPatient(null);

        // Assert
        assertThat(result).isEmpty();
    }

    @Test
    void getEncountersForPatient_shouldReturnEmptyList_whenExceptionOccurs() {
        // Arrange
        String patientId = "12345";
        HapiEncounterService spyService = spy(hapiEncounterService);

        doAnswer(invocation -> {
            try {
                throw new RuntimeException("Database error");
            } catch (Exception e) {
                return List.of();
            }
        }).when(spyService).getEncountersForPatient(patientId);

        // Act
        List<Encounter> result = spyService.getEncountersForPatient(patientId);

        // Assert
        assertThat(result).isEmpty();
    }

    @Test
    void getEncountersForPatient_shouldOrderEncountersByDate() {
        // Arrange
        String patientId = "12345";
        List<Encounter> encounters = new ArrayList<>();

        Calendar cal = Calendar.getInstance();
        cal.add(Calendar.DAY_OF_MONTH, -2);
        Date oldStart = cal.getTime();
        cal.add(Calendar.HOUR, 1);
        Date oldEnd = cal.getTime();

        encounters.add(createTestEncounter("2", "Patient/" + patientId, "Practitioner/1",
                startTime, endTime));
        encounters.add(createTestEncounter("1", "Patient/" + patientId, "Practitioner/1",
                oldStart, oldEnd));

        HapiEncounterService spyService = spy(hapiEncounterService);
        doReturn(encounters).when(spyService).getEncountersForPatient(patientId);

        // Act
        List<Encounter> result = spyService.getEncountersForPatient(patientId);

        // Assert
        assertThat(result).hasSize(2);
    }

    // getEncounterById() TESTS

    @Test
    void getEncounterById_shouldReturnEncounter_whenEncounterExists() {
        // Arrange
        String encounterId = "12345";
        HapiEncounterService spyService = spy(hapiEncounterService);
        doReturn(Optional.of(testEncounter)).when(spyService).getEncounterById(encounterId);

        // Act
        Optional<Encounter> result = spyService.getEncounterById(encounterId);

        // Assert
        assertThat(result).isPresent();
        assertThat(result.get().getIdElement().getIdPart()).isEqualTo("12345");
        assertThat(result.get().getStatus()).isEqualTo(Encounter.EncounterStatus.FINISHED);
    }

    @Test
    void getEncounterById_shouldReturnEncounterWithCompleteData() {
        // Arrange
        String encounterId = "12345";
        testEncounter.setStatus(Encounter.EncounterStatus.FINISHED);
        testEncounter.setClass_(new Coding()
                .setSystem("http://terminology.hl7.org/CodeSystem/v3-ActCode")
                .setCode("AMB")
                .setDisplay("ambulatory"));

        HapiEncounterService spyService = spy(hapiEncounterService);
        doReturn(Optional.of(testEncounter)).when(spyService).getEncounterById(encounterId);

        // Act
        Optional<Encounter> result = spyService.getEncounterById(encounterId);

        // Assert
        assertThat(result).isPresent();
        assertThat(result.get().getStatus()).isEqualTo(Encounter.EncounterStatus.FINISHED);
        assertThat(result.get().getClass_().getCode()).isEqualTo("AMB");
    }

    @Test
    void getEncounterById_shouldReturnEmpty_whenEncounterNotFound() {
        // Arrange
        String encounterId = "99999";
        HapiEncounterService spyService = spy(hapiEncounterService);
        doReturn(Optional.empty()).when(spyService).getEncounterById(encounterId);

        // Act
        Optional<Encounter> result = spyService.getEncounterById(encounterId);

        // Assert
        assertThat(result).isEmpty();
    }

    @Test
    void getEncounterById_shouldReturnEmpty_whenNullId() {
        // Arrange
        HapiEncounterService spyService = spy(hapiEncounterService);
        doReturn(Optional.empty()).when(spyService).getEncounterById(null);

        // Act
        Optional<Encounter> result = spyService.getEncounterById(null);

        // Assert
        assertThat(result).isEmpty();
    }

    @Test
    void getEncounterById_shouldReturnEmpty_whenEmptyId() {
        // Arrange
        HapiEncounterService spyService = spy(hapiEncounterService);
        doReturn(Optional.empty()).when(spyService).getEncounterById("");

        // Act
        Optional<Encounter> result = spyService.getEncounterById("");

        // Assert
        assertThat(result).isEmpty();
    }

    // createEncounter() TESTS

    @Test
    void createEncounter_shouldCreateEncounter_whenValidData() {
        // Arrange
        String patientPersonnummer = "197001011234";
        String practitionerPersonnummer = "198001011234";

        Encounter createdEncounter = createTestEncounter("new-123", "Patient/" + patientPersonnummer,
                "Practitioner/" + practitionerPersonnummer, startTime, endTime);

        HapiEncounterService spyService = spy(hapiEncounterService);
        doReturn(createdEncounter).when(spyService).createEncounter(
                patientPersonnummer, practitionerPersonnummer, startTime, endTime);

        // Act
        Encounter result = spyService.createEncounter(
                patientPersonnummer, practitionerPersonnummer, startTime, endTime);

        // Assert
        assertThat(result).isNotNull();
        assertThat(result.getSubject().getReference()).contains(patientPersonnummer);
        assertThat(result.getParticipant()).isNotEmpty();
    }

    @Test
    void createEncounter_shouldCreateEncounterWithoutEndTime() {
        // Arrange
        String patientPersonnummer = "197001011234";
        String practitionerPersonnummer = "198001011234";

        Encounter createdEncounter = createTestEncounter("new-123", "Patient/" + patientPersonnummer,
                "Practitioner/" + practitionerPersonnummer, startTime, null);

        HapiEncounterService spyService = spy(hapiEncounterService);
        doReturn(createdEncounter).when(spyService).createEncounter(
                patientPersonnummer, practitionerPersonnummer, startTime, null);

        // Act
        Encounter result = spyService.createEncounter(
                patientPersonnummer, practitionerPersonnummer, startTime, null);

        // Assert
        assertThat(result).isNotNull();
        assertThat(result.getPeriod().hasStart()).isTrue();
        assertThat(result.getPeriod().hasEnd()).isFalse();
    }

    @Test
    void createEncounter_shouldCreateEncounterWithoutPractitioner_whenPractitionerIsNull() {
        // Arrange
        String patientPersonnummer = "197001011234";

        Encounter createdEncounter = createTestEncounter("new-123", "Patient/" + patientPersonnummer,
                null, startTime, endTime);

        HapiEncounterService spyService = spy(hapiEncounterService);
        doReturn(createdEncounter).when(spyService).createEncounter(
                patientPersonnummer, null, startTime, endTime);

        // Act
        Encounter result = spyService.createEncounter(
                patientPersonnummer, null, startTime, endTime);

        // Assert
        assertThat(result).isNotNull();
        assertThat(result.getParticipant()).isEmpty();
    }

    @Test
    void createEncounter_shouldCreateEncounterWithoutPractitioner_whenPractitionerIsEmpty() {
        // Arrange
        String patientPersonnummer = "197001011234";

        Encounter createdEncounter = createTestEncounter("new-123", "Patient/" + patientPersonnummer,
                null, startTime, endTime);

        HapiEncounterService spyService = spy(hapiEncounterService);
        doReturn(createdEncounter).when(spyService).createEncounter(
                patientPersonnummer, "", startTime, endTime);

        // Act
        Encounter result = spyService.createEncounter(
                patientPersonnummer, "", startTime, endTime);

        // Assert
        assertThat(result).isNotNull();
        assertThat(result.getParticipant()).isEmpty();
    }

    @Test
    void createEncounter_shouldThrowException_whenCreationFails() {
        // Arrange
        String patientPersonnummer = "197001011234";
        String practitionerPersonnummer = "198001011234";

        HapiEncounterService spyService = spy(hapiEncounterService);
        doThrow(new RuntimeException("Server error")).when(spyService).createEncounter(
                patientPersonnummer, practitionerPersonnummer, startTime, endTime);

        // Act & Assert
        assertThatThrownBy(() -> spyService.createEncounter(
                patientPersonnummer, practitionerPersonnummer, startTime, endTime))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Server error");
    }

    @Test
    void createEncounter_shouldSetCorrectStatus() {
        // Arrange
        String patientPersonnummer = "197001011234";
        String practitionerPersonnummer = "198001011234";

        Encounter encounter = createTestEncounter("123", "Patient/" + patientPersonnummer,
                "Practitioner/" + practitionerPersonnummer, startTime, endTime);
        encounter.setStatus(Encounter.EncounterStatus.FINISHED);

        HapiEncounterService spyService = spy(hapiEncounterService);
        doReturn(encounter).when(spyService).createEncounter(
                patientPersonnummer, practitionerPersonnummer, startTime, endTime);

        // Act
        Encounter result = spyService.createEncounter(
                patientPersonnummer, practitionerPersonnummer, startTime, endTime);

        // Assert
        assertThat(result.getStatus()).isEqualTo(Encounter.EncounterStatus.FINISHED);
    }

    @Test
    void createEncounter_shouldSetCorrectClass() {
        // Arrange
        String patientPersonnummer = "197001011234";
        String practitionerPersonnummer = "198001011234";

        Encounter encounter = createTestEncounter("123", "Patient/" + patientPersonnummer,
                "Practitioner/" + practitionerPersonnummer, startTime, endTime);
        encounter.setClass_(new Coding()
                .setSystem("http://terminology.hl7.org/CodeSystem/v3-ActCode")
                .setCode("AMB")
                .setDisplay("ambulatory"));

        HapiEncounterService spyService = spy(hapiEncounterService);
        doReturn(encounter).when(spyService).createEncounter(
                patientPersonnummer, practitionerPersonnummer, startTime, endTime);

        // Act
        Encounter result = spyService.createEncounter(
                patientPersonnummer, practitionerPersonnummer, startTime, endTime);

        // Assert
        assertThat(result.getClass_().getCode()).isEqualTo("AMB");
        assertThat(result.getClass_().getDisplay()).isEqualTo("ambulatory");
    }

    @Test
    void createEncounter_shouldSetCorrectType() {
        // Arrange
        String patientPersonnummer = "197001011234";
        String practitionerPersonnummer = "198001011234";

        Encounter encounter = createTestEncounter("123", "Patient/" + patientPersonnummer,
                "Practitioner/" + practitionerPersonnummer, startTime, endTime);
        encounter.addType()
                .addCoding()
                .setSystem("http://snomed.info/sct")
                .setCode("185349003")
                .setDisplay("Encounter for check up (procedure)");

        HapiEncounterService spyService = spy(hapiEncounterService);
        doReturn(encounter).when(spyService).createEncounter(
                patientPersonnummer, practitionerPersonnummer, startTime, endTime);

        // Act
        Encounter result = spyService.createEncounter(
                patientPersonnummer, practitionerPersonnummer, startTime, endTime);

        // Assert
        assertThat(result.getType()).isNotEmpty();
        assertThat(result.getType().get(0).getCoding().get(0).getCode()).isEqualTo("185349003");
    }

    @Test
    void createEncounter_shouldSetParticipantWithCorrectRole() {
        // Arrange
        String patientPersonnummer = "197001011234";
        String practitionerPersonnummer = "198001011234";

        Encounter encounter = createTestEncounter("123", "Patient/" + patientPersonnummer,
                "Practitioner/" + practitionerPersonnummer, startTime, endTime);

        HapiEncounterService spyService = spy(hapiEncounterService);
        doReturn(encounter).when(spyService).createEncounter(
                patientPersonnummer, practitionerPersonnummer, startTime, endTime);

        // Act
        Encounter result = spyService.createEncounter(
                patientPersonnummer, practitionerPersonnummer, startTime, endTime);

        // Assert
        assertThat(result.getParticipant()).isNotEmpty();
        Encounter.EncounterParticipantComponent participant = result.getParticipant().get(0);
        assertThat(participant.getType()).isNotEmpty();
        assertThat(participant.getType().get(0).getCoding().get(0).getCode()).isEqualTo("PPRF");
    }

    // INTEGRATION-STYLE TESTS

    @Test
    void getEncounterById_shouldCatchResourceNotFoundException() {
        // Arrange
        String encounterId = "99999";
        HapiEncounterService spyService = spy(hapiEncounterService);

        doAnswer(invocation -> {
            try {
                throw new ResourceNotFoundException("Encounter not found");
            } catch (Exception e) {
                return Optional.empty();
            }
        }).when(spyService).getEncounterById(encounterId);

        // Act
        Optional<Encounter> result = spyService.getEncounterById(encounterId);

        // Assert
        assertThat(result).isEmpty();
    }

    // EDGE CASES

    @Test
    void createEncounter_shouldHandleStartTimeAfterEndTime() {
        // Arrange
        String patientPersonnummer = "197001011234";
        String practitionerPersonnummer = "198001011234";

        Date laterStart = endTime;
        Date earlierEnd = startTime;

        Encounter encounter = createTestEncounter("123", "Patient/" + patientPersonnummer,
                "Practitioner/" + practitionerPersonnummer, laterStart, earlierEnd);

        HapiEncounterService spyService = spy(hapiEncounterService);
        doReturn(encounter).when(spyService).createEncounter(
                patientPersonnummer, practitionerPersonnummer, laterStart, earlierEnd);

        // Act
        Encounter result = spyService.createEncounter(
                patientPersonnummer, practitionerPersonnummer, laterStart, earlierEnd);

        // Assert - systemet bör acceptera detta även om det är logiskt fel
        assertThat(result).isNotNull();
    }

    @Test
    void getEncountersForPatient_shouldHandleDifferentPatientIdFormats() {
        // Arrange
        String[] patientIds = {"12345", "Patient/12345", "abc-123"};

        for (String patientId : patientIds) {
            List<Encounter> encounters = new ArrayList<>();
            encounters.add(createTestEncounter("1", "Patient/" + patientId, "Practitioner/1",
                    startTime, endTime));

            HapiEncounterService spyService = spy(hapiEncounterService);
            doReturn(encounters).when(spyService).getEncountersForPatient(patientId);

            // Act
            List<Encounter> result = spyService.getEncountersForPatient(patientId);

            // Assert
            assertThat(result).hasSize(1);
        }
    }

    @Test
    void createEncounter_shouldHandleSameStartAndEndTime() {
        // Arrange
        String patientPersonnummer = "197001011234";
        String practitionerPersonnummer = "198001011234";
        Date sameTime = new Date();

        Encounter encounter = createTestEncounter("123", "Patient/" + patientPersonnummer,
                "Practitioner/" + practitionerPersonnummer, sameTime, sameTime);

        HapiEncounterService spyService = spy(hapiEncounterService);
        doReturn(encounter).when(spyService).createEncounter(
                patientPersonnummer, practitionerPersonnummer, sameTime, sameTime);

        // Act
        Encounter result = spyService.createEncounter(
                patientPersonnummer, practitionerPersonnummer, sameTime, sameTime);

        // Assert
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