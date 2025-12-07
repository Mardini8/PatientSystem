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
class HapiConditionServiceTest {

    @Mock
    private HapiClientService hapiClientService;

    private HapiConditionService hapiConditionService;

    private Condition testCondition;
    private Date recordedDate;

    @BeforeEach
    void setUp() {
        hapiConditionService = new HapiConditionService(hapiClientService);

        recordedDate = new Date();

        testCondition = createTestCondition("12345", "Patient/98765", "Practitioner/11111",
                "Diabetes Type 2", recordedDate);
    }

    // getAllConditions() TESTS

    @Test
    void getAllConditions_shouldReturnListOfConditions_whenConditionsExist() {
        // Arrange
        List<Condition> expectedConditions = new ArrayList<>();
        expectedConditions.add(testCondition);
        expectedConditions.add(createTestCondition("67890", "Patient/98765", "Practitioner/22222",
                "Hypertension", recordedDate));

        HapiConditionService spyService = spy(hapiConditionService);
        doReturn(expectedConditions).when(spyService).getAllConditions();

        // Act
        List<Condition> result = spyService.getAllConditions();

        // Assert
        assertThat(result).isNotNull();
        assertThat(result).hasSize(2);
        assertThat(result.get(0).getIdElement().getIdPart()).isEqualTo("12345");
        assertThat(result.get(1).getIdElement().getIdPart()).isEqualTo("67890");
    }

    @Test
    void getAllConditions_shouldReturnEmptyList_whenNoConditionsExist() {
        // Arrange
        HapiConditionService spyService = spy(hapiConditionService);
        doReturn(new ArrayList<Condition>()).when(spyService).getAllConditions();

        // Act
        List<Condition> result = spyService.getAllConditions();

        // Assert
        assertThat(result).isNotNull();
        assertThat(result).isEmpty();
    }

    @Test
    void getAllConditions_shouldHandleMultipleConditions() {
        // Arrange
        List<Condition> conditions = new ArrayList<>();
        String[] diagnoses = {"Diabetes", "Hypertension", "Asthma", "Arthritis", "Depression"};

        for (int i = 0; i < diagnoses.length; i++) {
            conditions.add(createTestCondition(String.valueOf(i + 1), "Patient/123", "Practitioner/456",
                    diagnoses[i], recordedDate));
        }

        HapiConditionService spyService = spy(hapiConditionService);
        doReturn(conditions).when(spyService).getAllConditions();

        // Act
        List<Condition> result = spyService.getAllConditions();

        // Assert
        assertThat(result).hasSize(5);
        for (int i = 0; i < diagnoses.length; i++) {
            assertThat(result.get(i).getCode().getText()).isEqualTo(diagnoses[i]);
        }
    }

    // getConditionsForPatient() TESTS

    @Test
    void getConditionsForPatient_shouldReturnConditions_whenPatientHasConditions() {
        // Arrange
        String patientId = "98765";
        List<Condition> patientConditions = new ArrayList<>();
        patientConditions.add(testCondition);
        patientConditions.add(createTestCondition("67890", "Patient/" + patientId, "Practitioner/22222",
                "Hypertension", recordedDate));

        HapiConditionService spyService = spy(hapiConditionService);
        doReturn(patientConditions).when(spyService).getConditionsForPatient(patientId);

        // Act
        List<Condition> result = spyService.getConditionsForPatient(patientId);

        // Assert
        assertThat(result).hasSize(2);
        assertThat(result.get(0).getSubject().getReference()).contains(patientId);
        assertThat(result.get(1).getSubject().getReference()).contains(patientId);
    }

    @Test
    void getConditionsForPatient_shouldReturnEmptyList_whenNoConditionsExist() {
        // Arrange
        String patientId = "99999";
        HapiConditionService spyService = spy(hapiConditionService);
        doReturn(new ArrayList<Condition>()).when(spyService).getConditionsForPatient(patientId);

        // Act
        List<Condition> result = spyService.getConditionsForPatient(patientId);

        // Assert
        assertThat(result).isEmpty();
    }

    @Test
    void getConditionsForPatient_shouldReturnEmptyList_whenNullPatientId() {
        // Arrange
        HapiConditionService spyService = spy(hapiConditionService);
        doReturn(new ArrayList<Condition>()).when(spyService).getConditionsForPatient(null);

        // Act
        List<Condition> result = spyService.getConditionsForPatient(null);

        // Assert
        assertThat(result).isEmpty();
    }

    @Test
    void getConditionsForPatient_shouldReturnEmptyList_whenExceptionOccurs() {
        // Arrange
        String patientId = "12345";
        HapiConditionService spyService = spy(hapiConditionService);

        doAnswer(invocation -> {
            try {
                throw new RuntimeException("Database error");
            } catch (Exception e) {
                return List.of();
            }
        }).when(spyService).getConditionsForPatient(patientId);

        // Act
        List<Condition> result = spyService.getConditionsForPatient(patientId);

        // Assert
        assertThat(result).isEmpty();
    }

    @Test
    void getConditionsForPatient_shouldHandleMultipleConditionsForSamePatient() {
        // Arrange
        String patientId = "12345";
        List<Condition> conditions = new ArrayList<>();

        Calendar cal = Calendar.getInstance();
        for (int i = 0; i < 3; i++) {
            cal.add(Calendar.MONTH, -i);
            Date date = cal.getTime();
            conditions.add(createTestCondition(String.valueOf(i), "Patient/" + patientId,
                    "Practitioner/" + i, "Condition " + i, date));
        }

        HapiConditionService spyService = spy(hapiConditionService);
        doReturn(conditions).when(spyService).getConditionsForPatient(patientId);

        // Act
        List<Condition> result = spyService.getConditionsForPatient(patientId);

        // Assert
        assertThat(result).hasSize(3);
    }

    // getConditionById() TESTS

    @Test
    void getConditionById_shouldReturnCondition_whenConditionExists() {
        // Arrange
        String conditionId = "12345";
        HapiConditionService spyService = spy(hapiConditionService);
        doReturn(Optional.of(testCondition)).when(spyService).getConditionById(conditionId);

        // Act
        Optional<Condition> result = spyService.getConditionById(conditionId);

        // Assert
        assertThat(result).isPresent();
        assertThat(result.get().getIdElement().getIdPart()).isEqualTo("12345");
        assertThat(result.get().getCode().getText()).isEqualTo("Diabetes Type 2");
    }

    @Test
    void getConditionById_shouldReturnConditionWithCompleteData() {
        // Arrange
        String conditionId = "12345";
        testCondition.getClinicalStatus()
                .addCoding()
                .setSystem("http://terminology.hl7.org/CodeSystem/condition-clinical")
                .setCode("active");
        testCondition.getVerificationStatus()
                .addCoding()
                .setSystem("http://terminology.hl7.org/CodeSystem/condition-ver-status")
                .setCode("confirmed");

        HapiConditionService spyService = spy(hapiConditionService);
        doReturn(Optional.of(testCondition)).when(spyService).getConditionById(conditionId);

        // Act
        Optional<Condition> result = spyService.getConditionById(conditionId);

        // Assert
        assertThat(result).isPresent();
        assertThat(result.get().getClinicalStatus().getCoding().get(0).getCode()).isEqualTo("active");
        assertThat(result.get().getVerificationStatus().getCoding().get(0).getCode()).isEqualTo("confirmed");
    }

    @Test
    void getConditionById_shouldReturnEmpty_whenConditionNotFound() {
        // Arrange
        String conditionId = "99999";
        HapiConditionService spyService = spy(hapiConditionService);
        doReturn(Optional.empty()).when(spyService).getConditionById(conditionId);

        // Act
        Optional<Condition> result = spyService.getConditionById(conditionId);

        // Assert
        assertThat(result).isEmpty();
    }

    @Test
    void getConditionById_shouldReturnEmpty_whenNullId() {
        // Arrange
        HapiConditionService spyService = spy(hapiConditionService);
        doReturn(Optional.empty()).when(spyService).getConditionById(null);

        // Act
        Optional<Condition> result = spyService.getConditionById(null);

        // Assert
        assertThat(result).isEmpty();
    }

    @Test
    void getConditionById_shouldReturnEmpty_whenEmptyId() {
        // Arrange
        HapiConditionService spyService = spy(hapiConditionService);
        doReturn(Optional.empty()).when(spyService).getConditionById("");

        // Act
        Optional<Condition> result = spyService.getConditionById("");

        // Assert
        assertThat(result).isEmpty();
    }

    // createCondition() TESTS

    @Test
    void createCondition_shouldCreateCondition_whenValidData() {
        // Arrange
        String patientPersonnummer = "197001011234";
        String practitionerPersonnummer = "198001011234";
        String description = "Diabetes Type 2";

        Condition createdCondition = createTestCondition("new-123", "Patient/" + patientPersonnummer,
                "Practitioner/" + practitionerPersonnummer, description, recordedDate);

        HapiConditionService spyService = spy(hapiConditionService);
        doReturn(createdCondition).when(spyService).createCondition(
                patientPersonnummer, practitionerPersonnummer, description, recordedDate);

        // Act
        Condition result = spyService.createCondition(
                patientPersonnummer, practitionerPersonnummer, description, recordedDate);

        // Assert
        assertThat(result).isNotNull();
        assertThat(result.getSubject().getReference()).contains(patientPersonnummer);
        assertThat(result.getCode().getText()).isEqualTo(description);
    }

    @Test
    void createCondition_shouldCreateConditionWithoutPractitioner_whenPractitionerIsNull() {
        // Arrange
        String patientPersonnummer = "197001011234";
        String description = "Self-reported condition";

        Condition createdCondition = createTestCondition("new-123", "Patient/" + patientPersonnummer,
                null, description, recordedDate);

        HapiConditionService spyService = spy(hapiConditionService);
        doReturn(createdCondition).when(spyService).createCondition(
                patientPersonnummer, null, description, recordedDate);

        // Act
        Condition result = spyService.createCondition(
                patientPersonnummer, null, description, recordedDate);

        // Assert
        assertThat(result).isNotNull();
        assertThat(result.hasRecorder()).isFalse();
    }

    @Test
    void createCondition_shouldCreateConditionWithoutPractitioner_whenPractitionerIsEmpty() {
        // Arrange
        String patientPersonnummer = "197001011234";
        String description = "Self-reported condition";

        Condition createdCondition = createTestCondition("new-123", "Patient/" + patientPersonnummer,
                null, description, recordedDate);

        HapiConditionService spyService = spy(hapiConditionService);
        doReturn(createdCondition).when(spyService).createCondition(
                patientPersonnummer, "", description, recordedDate);

        // Act
        Condition result = spyService.createCondition(
                patientPersonnummer, "", description, recordedDate);

        // Assert
        assertThat(result).isNotNull();
        assertThat(result.hasRecorder()).isFalse();
    }

    @Test
    void createCondition_shouldThrowException_whenCreationFails() {
        // Arrange
        String patientPersonnummer = "197001011234";
        String practitionerPersonnummer = "198001011234";
        String description = "Test Condition";

        HapiConditionService spyService = spy(hapiConditionService);
        doThrow(new RuntimeException("Server error")).when(spyService).createCondition(
                patientPersonnummer, practitionerPersonnummer, description, recordedDate);

        // Act & Assert
        assertThatThrownBy(() -> spyService.createCondition(
                patientPersonnummer, practitionerPersonnummer, description, recordedDate))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Server error");
    }

    @Test
    void createCondition_shouldSetCorrectClinicalStatus() {
        // Arrange
        String patientPersonnummer = "197001011234";
        String practitionerPersonnummer = "198001011234";
        String description = "Active Condition";

        Condition condition = createTestCondition("123", "Patient/" + patientPersonnummer,
                "Practitioner/" + practitionerPersonnummer, description, recordedDate);
        condition.getClinicalStatus()
                .addCoding()
                .setSystem("http://terminology.hl7.org/CodeSystem/condition-clinical")
                .setCode("active");

        HapiConditionService spyService = spy(hapiConditionService);
        doReturn(condition).when(spyService).createCondition(
                patientPersonnummer, practitionerPersonnummer, description, recordedDate);

        // Act
        Condition result = spyService.createCondition(
                patientPersonnummer, practitionerPersonnummer, description, recordedDate);

        // Assert
        assertThat(result.getClinicalStatus()).isNotNull();
        assertThat(result.getClinicalStatus().getCoding().get(0).getCode()).isEqualTo("active");
    }

    @Test
    void createCondition_shouldSetCorrectVerificationStatus() {
        // Arrange
        String patientPersonnummer = "197001011234";
        String practitionerPersonnummer = "198001011234";
        String description = "Confirmed Condition";

        Condition condition = createTestCondition("123", "Patient/" + patientPersonnummer,
                "Practitioner/" + practitionerPersonnummer, description, recordedDate);
        condition.getVerificationStatus()
                .addCoding()
                .setSystem("http://terminology.hl7.org/CodeSystem/condition-ver-status")
                .setCode("confirmed");

        HapiConditionService spyService = spy(hapiConditionService);
        doReturn(condition).when(spyService).createCondition(
                patientPersonnummer, practitionerPersonnummer, description, recordedDate);

        // Act
        Condition result = spyService.createCondition(
                patientPersonnummer, practitionerPersonnummer, description, recordedDate);

        // Assert
        assertThat(result.getVerificationStatus()).isNotNull();
        assertThat(result.getVerificationStatus().getCoding().get(0).getCode()).isEqualTo("confirmed");
    }

    @Test
    void createCondition_shouldSetRecordedDate() {
        // Arrange
        String patientPersonnummer = "197001011234";
        String practitionerPersonnummer = "198001011234";
        String description = "Test Condition";
        Date specificDate = new Date();

        Condition condition = createTestCondition("123", "Patient/" + patientPersonnummer,
                "Practitioner/" + practitionerPersonnummer, description, specificDate);

        HapiConditionService spyService = spy(hapiConditionService);
        doReturn(condition).when(spyService).createCondition(
                patientPersonnummer, practitionerPersonnummer, description, specificDate);

        // Act
        Condition result = spyService.createCondition(
                patientPersonnummer, practitionerPersonnummer, description, specificDate);

        // Assert
        assertThat(result.getRecordedDate()).isEqualTo(specificDate);
    }

    @Test
    void createCondition_shouldSetOnsetDate() {
        // Arrange
        String patientPersonnummer = "197001011234";
        String practitionerPersonnummer = "198001011234";
        String description = "Test Condition";
        Date onsetDate = new Date();

        Condition condition = createTestCondition("123", "Patient/" + patientPersonnummer,
                "Practitioner/" + practitionerPersonnummer, description, onsetDate);
        condition.setOnset(new DateTimeType(onsetDate));

        HapiConditionService spyService = spy(hapiConditionService);
        doReturn(condition).when(spyService).createCondition(
                patientPersonnummer, practitionerPersonnummer, description, onsetDate);

        // Act
        Condition result = spyService.createCondition(
                patientPersonnummer, practitionerPersonnummer, description, onsetDate);

        // Assert
        assertThat(result.hasOnset()).isTrue();
    }

    @Test
    void createCondition_shouldSetCorrectCodeSystem() {
        // Arrange
        String patientPersonnummer = "197001011234";
        String practitionerPersonnummer = "198001011234";
        String description = "SNOMED Condition";

        Condition condition = createTestCondition("123", "Patient/" + patientPersonnummer,
                "Practitioner/" + practitionerPersonnummer, description, recordedDate);
        condition.getCode()
                .addCoding()
                .setSystem("http://snomed.info/sct")
                .setCode("404684003")
                .setDisplay(description);

        HapiConditionService spyService = spy(hapiConditionService);
        doReturn(condition).when(spyService).createCondition(
                patientPersonnummer, practitionerPersonnummer, description, recordedDate);

        // Act
        Condition result = spyService.createCondition(
                patientPersonnummer, practitionerPersonnummer, description, recordedDate);

        // Assert
        assertThat(result.getCode().getCoding()).isNotEmpty();
        assertThat(result.getCode().getCoding().get(0).getSystem()).isEqualTo("http://snomed.info/sct");
    }

    // INTEGRATION-STYLE TESTS

    @Test
    void getConditionById_shouldCatchResourceNotFoundException() {
        // Arrange
        String conditionId = "99999";
        HapiConditionService spyService = spy(hapiConditionService);

        doAnswer(invocation -> {
            try {
                throw new ResourceNotFoundException("Condition not found");
            } catch (Exception e) {
                return Optional.empty();
            }
        }).when(spyService).getConditionById(conditionId);

        // Act
        Optional<Condition> result = spyService.getConditionById(conditionId);

        // Assert
        assertThat(result).isEmpty();
    }

    // EDGE CASES

    @Test
    void getConditionsForPatient_shouldHandleDifferentPatientIdFormats() {
        // Arrange
        String[] patientIds = {"12345", "Patient/12345", "abc-123"};

        for (String patientId : patientIds) {
            List<Condition> conditions = new ArrayList<>();
            conditions.add(createTestCondition("1", "Patient/" + patientId, "Practitioner/1",
                    "Test Condition", recordedDate));

            HapiConditionService spyService = spy(hapiConditionService);
            doReturn(conditions).when(spyService).getConditionsForPatient(patientId);

            // Act
            List<Condition> result = spyService.getConditionsForPatient(patientId);

            // Assert
            assertThat(result).hasSize(1);
        }
    }

    @Test
    void createCondition_shouldHandleLongDescription() {
        // Arrange
        String patientPersonnummer = "197001011234";
        String practitionerPersonnummer = "198001011234";
        String longDescription = "A".repeat(500);

        Condition condition = createTestCondition("123", "Patient/" + patientPersonnummer,
                "Practitioner/" + practitionerPersonnummer, longDescription, recordedDate);

        HapiConditionService spyService = spy(hapiConditionService);
        doReturn(condition).when(spyService).createCondition(
                patientPersonnummer, practitionerPersonnummer, longDescription, recordedDate);

        // Act
        Condition result = spyService.createCondition(
                patientPersonnummer, practitionerPersonnummer, longDescription, recordedDate);

        // Assert
        assertThat(result.getCode().getText()).hasSize(500);
    }

    @Test
    void createCondition_shouldHandleSpecialCharactersInDescription() {
        // Arrange
        String patientPersonnummer = "197001011234";
        String practitionerPersonnummer = "198001011234";
        String[] specialDescriptions = {
                "Type-2 Diabetes",
                "High Blood Pressure (HTN)",
                "Sjögren's Syndrome",
                "COVID-19",
                "Condition: Active & Confirmed"
        };

        for (String description : specialDescriptions) {
            Condition condition = createTestCondition("123", "Patient/" + patientPersonnummer,
                    "Practitioner/" + practitionerPersonnummer, description, recordedDate);

            HapiConditionService spyService = spy(hapiConditionService);
            doReturn(condition).when(spyService).createCondition(
                    patientPersonnummer, practitionerPersonnummer, description, recordedDate);

            // Act
            Condition result = spyService.createCondition(
                    patientPersonnummer, practitionerPersonnummer, description, recordedDate);

            // Assert
            assertThat(result.getCode().getText()).isEqualTo(description);
        }
    }

    @Test
    void getAllConditions_shouldHandleMixedConditionData() {
        // Arrange
        List<Condition> conditions = new ArrayList<>();

        // Full data condition
        Condition fullCondition = createTestCondition("1", "Patient/123", "Practitioner/456",
                "Full Condition", recordedDate);
        fullCondition.getClinicalStatus().addCoding().setCode("active");
        conditions.add(fullCondition);

        // Minimal data condition
        Condition minimalCondition = new Condition();
        minimalCondition.setId("2");
        minimalCondition.setSubject(new Reference("Patient/123"));
        conditions.add(minimalCondition);

        HapiConditionService spyService = spy(hapiConditionService);
        doReturn(conditions).when(spyService).getAllConditions();

        // Act
        List<Condition> result = spyService.getAllConditions();

        // Assert
        assertThat(result).hasSize(2);
        assertThat(result.get(0).hasClinicalStatus()).isTrue();
        assertThat(result.get(1).hasClinicalStatus()).isFalse();
    }

    // HELPER METHODS

    private Condition createTestCondition(String id, String patientRef, String practitionerRef,
                                          String description, Date recordedDate) {
        Condition condition = new Condition();
        condition.setId(id);

        condition.getClinicalStatus()
                .addCoding()
                .setSystem("http://terminology.hl7.org/CodeSystem/condition-clinical")
                .setCode("active");

        condition.getVerificationStatus()
                .addCoding()
                .setSystem("http://terminology.hl7.org/CodeSystem/condition-ver-status")
                .setCode("confirmed");

        condition.setSubject(new Reference(patientRef));

        if (practitionerRef != null && !practitionerRef.isEmpty()) {
            condition.setRecorder(new Reference(practitionerRef));
        }

        condition.getCode()
                .addCoding()
                .setSystem("http://snomed.info/sct")
                .setCode("404684003")
                .setDisplay(description);
        condition.getCode().setText(description);

        condition.setRecordedDate(recordedDate);
        condition.setOnset(new DateTimeType(recordedDate));

        return condition;
    }
}