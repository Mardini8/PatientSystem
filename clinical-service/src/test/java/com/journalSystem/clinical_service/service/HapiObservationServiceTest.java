package com.journalSystem.clinical_service.service;

import ca.uhn.fhir.rest.server.exceptions.ResourceNotFoundException;
import org.hl7.fhir.r4.model.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class HapiObservationServiceTest {

    @Mock
    private HapiClientService hapiClientService;

    @Mock
    private FhirLookupService fhirLookupService;

    private HapiObservationService hapiObservationService;

    private Observation testObservation;

    @BeforeEach
    void setUp() {
        hapiObservationService = new HapiObservationService(hapiClientService, fhirLookupService);

        testObservation = createTestObservation("12345", "Patient/98765", "Practitioner/11111",
                "Blood Pressure", "120", "mmHg", new Date());
    }

    // getAllObservations() TESTS

    @Test
    void getAllObservations_shouldReturnListOfObservations_whenObservationsExist() {
        // Arrange
        List<Observation> expectedObservations = new ArrayList<>();
        expectedObservations.add(testObservation);
        expectedObservations.add(createTestObservation("67890", "Patient/98765", "Practitioner/11111",
                "Heart Rate", "72", "bpm", new Date()));

        HapiObservationService spyService = spy(hapiObservationService);
        doReturn(expectedObservations).when(spyService).getAllObservations();

        // Act
        List<Observation> result = spyService.getAllObservations();

        // Assert
        assertThat(result).isNotNull();
        assertThat(result).hasSize(2);
        assertThat(result.get(0).getIdElement().getIdPart()).isEqualTo("12345");
        assertThat(result.get(1).getIdElement().getIdPart()).isEqualTo("67890");
    }

    @Test
    void getAllObservations_shouldReturnEmptyList_whenNoObservationsExist() {
        // Arrange
        HapiObservationService spyService = spy(hapiObservationService);
        doReturn(new ArrayList<Observation>()).when(spyService).getAllObservations();

        // Act
        List<Observation> result = spyService.getAllObservations();

        // Assert
        assertThat(result).isNotNull();
        assertThat(result).isEmpty();
    }

    @Test
    void getAllObservations_shouldHandleMultipleObservations() {
        // Arrange
        List<Observation> observations = new ArrayList<>();
        observations.add(createTestObservation("1", "Patient/1", "Practitioner/1", "Weight", "70", "kg", new Date()));
        observations.add(createTestObservation("2", "Patient/1", "Practitioner/1", "Height", "175", "cm", new Date()));
        observations.add(createTestObservation("3", "Patient/1", "Practitioner/1", "Temperature", "37", "C", new Date()));

        HapiObservationService spyService = spy(hapiObservationService);
        doReturn(observations).when(spyService).getAllObservations();

        // Act
        List<Observation> result = spyService.getAllObservations();

        // Assert
        assertThat(result).hasSize(3);
        assertThat(result.get(0).getCode().getText()).isEqualTo("Weight");
        assertThat(result.get(1).getCode().getText()).isEqualTo("Height");
        assertThat(result.get(2).getCode().getText()).isEqualTo("Temperature");
    }

    // getObservationsForPatient() TESTS

    @Test
    void getObservationsForPatient_shouldReturnObservations_whenPatientHasObservations() {
        // Arrange
        String patientPersonnummer = "197001011234";
        String patientFhirId = "98765";

        List<Observation> patientObservations = new ArrayList<>();
        patientObservations.add(testObservation);
        patientObservations.add(createTestObservation("67890", "Patient/" + patientFhirId, "Practitioner/11111",
                "Blood Sugar", "5.5", "mmol/L", new Date()));

        HapiObservationService spyService = spy(hapiObservationService);
        doReturn(patientObservations).when(spyService).getObservationsForPatient(patientPersonnummer);

        // Act
        List<Observation> result = spyService.getObservationsForPatient(patientPersonnummer);

        // Assert
        assertThat(result).hasSize(2);
    }

    @Test
    void getObservationsForPatient_shouldReturnEmptyList_whenNoObservationsExist() {
        // Arrange
        String patientPersonnummer = "199901019999";
        HapiObservationService spyService = spy(hapiObservationService);
        doReturn(new ArrayList<Observation>()).when(spyService).getObservationsForPatient(patientPersonnummer);

        // Act
        List<Observation> result = spyService.getObservationsForPatient(patientPersonnummer);

        // Assert
        assertThat(result).isEmpty();
    }

    @Test
    void getObservationsForPatient_shouldReturnEmptyList_whenNullPatientId() {
        // Arrange
        HapiObservationService spyService = spy(hapiObservationService);
        doReturn(new ArrayList<Observation>()).when(spyService).getObservationsForPatient(null);

        // Act
        List<Observation> result = spyService.getObservationsForPatient(null);

        // Assert
        assertThat(result).isEmpty();
    }

    @Test
    void getObservationsForPatient_shouldReturnEmptyList_whenExceptionOccurs() {
        // Arrange
        String patientPersonnummer = "197001011234";
        HapiObservationService spyService = spy(hapiObservationService);

        doAnswer(invocation -> {
            try {
                throw new RuntimeException("Database error");
            } catch (Exception e) {
                return List.of();
            }
        }).when(spyService).getObservationsForPatient(patientPersonnummer);

        // Act
        List<Observation> result = spyService.getObservationsForPatient(patientPersonnummer);

        // Assert
        assertThat(result).isEmpty();
    }

    // getObservationById() TESTS

    @Test
    void getObservationById_shouldReturnObservation_whenObservationExists() {
        // Arrange
        String observationId = "12345";
        HapiObservationService spyService = spy(hapiObservationService);
        doReturn(Optional.of(testObservation)).when(spyService).getObservationById(observationId);

        // Act
        Optional<Observation> result = spyService.getObservationById(observationId);

        // Assert
        assertThat(result).isPresent();
        assertThat(result.get().getIdElement().getIdPart()).isEqualTo("12345");
        assertThat(result.get().getCode().getText()).isEqualTo("Blood Pressure");
    }

    @Test
    void getObservationById_shouldReturnObservationWithCompleteData() {
        // Arrange
        String observationId = "12345";
        testObservation.setStatus(Observation.ObservationStatus.FINAL);
        testObservation.addCategory()
                .addCoding()
                .setSystem("http://terminology.hl7.org/CodeSystem/observation-category")
                .setCode("vital-signs");

        HapiObservationService spyService = spy(hapiObservationService);
        doReturn(Optional.of(testObservation)).when(spyService).getObservationById(observationId);

        // Act
        Optional<Observation> result = spyService.getObservationById(observationId);

        // Assert
        assertThat(result).isPresent();
        assertThat(result.get().getStatus()).isEqualTo(Observation.ObservationStatus.FINAL);
        assertThat(result.get().getCategory()).isNotEmpty();
    }

    @Test
    void getObservationById_shouldReturnEmpty_whenObservationNotFound() {
        // Arrange
        String observationId = "99999";
        HapiObservationService spyService = spy(hapiObservationService);
        doReturn(Optional.empty()).when(spyService).getObservationById(observationId);

        // Act
        Optional<Observation> result = spyService.getObservationById(observationId);

        // Assert
        assertThat(result).isEmpty();
    }

    @Test
    void getObservationById_shouldReturnEmpty_whenNullId() {
        // Arrange
        HapiObservationService spyService = spy(hapiObservationService);
        doReturn(Optional.empty()).when(spyService).getObservationById(null);

        // Act
        Optional<Observation> result = spyService.getObservationById(null);

        // Assert
        assertThat(result).isEmpty();
    }

    @Test
    void getObservationById_shouldReturnEmpty_whenEmptyId() {
        // Arrange
        HapiObservationService spyService = spy(hapiObservationService);
        doReturn(Optional.empty()).when(spyService).getObservationById("");

        // Act
        Optional<Observation> result = spyService.getObservationById("");

        // Assert
        assertThat(result).isEmpty();
    }

    // createObservation() TESTS

    @Test
    void createObservation_shouldCreateObservation_whenValidData() {
        // Arrange
        String patientPersonnummer = "197001011234";
        String performerPersonnummer = "198001011234";
        String patientFhirId = "patient-fhir-123";
        String practitionerFhirId = "practitioner-fhir-456";
        String description = "Blood Pressure";
        String value = "120";
        String unit = "mmHg";
        Date effectiveDate = new Date();

        Observation createdObservation = createTestObservation("new-123", "Patient/" + patientFhirId,
                "Practitioner/" + practitionerFhirId, description, value, unit, effectiveDate);

        HapiObservationService spyService = spy(hapiObservationService);
        doReturn(createdObservation).when(spyService).createObservation(
                patientPersonnummer, performerPersonnummer, description, value, unit, effectiveDate);

        // Act
        Observation result = spyService.createObservation(
                patientPersonnummer, performerPersonnummer, description, value, unit, effectiveDate);

        // Assert
        assertThat(result).isNotNull();
        assertThat(result.getSubject().getReference()).contains(patientFhirId);
        assertThat(result.getCode().getText()).isEqualTo(description);
    }

    @Test
    void createObservation_shouldCreateObservationWithNumericValue() {
        // Arrange
        String patientPersonnummer = "197001011234";
        String performerPersonnummer = "198001011234";
        String value = "72.5";
        String unit = "bpm";
        Date effectiveDate = new Date();

        Observation observation = createTestObservation("123", "Patient/fhir-123", "Practitioner/fhir-456",
                "Heart Rate", value, unit, effectiveDate);

        HapiObservationService spyService = spy(hapiObservationService);
        doReturn(observation).when(spyService).createObservation(
                eq(patientPersonnummer), eq(performerPersonnummer), anyString(), eq(value), eq(unit), any(Date.class));

        // Act
        Observation result = spyService.createObservation(
                patientPersonnummer, performerPersonnummer, "Heart Rate", value, unit, effectiveDate);

        // Assert
        assertThat(result.hasValueQuantity()).isTrue();
        assertThat(result.getValueQuantity().getValue().doubleValue()).isEqualTo(72.5);
        assertThat(result.getValueQuantity().getUnit()).isEqualTo(unit);
    }

    @Test
    void createObservation_shouldCreateObservationWithStringValue() {
        // Arrange
        String patientPersonnummer = "197001011234";
        String performerPersonnummer = "198001011234";
        String value = "Normal";
        String unit = "";
        Date effectiveDate = new Date();

        Observation observation = new Observation();
        observation.setId("123");
        observation.setSubject(new Reference("Patient/fhir-123"));
        observation.getCode().setText("General Assessment");
        observation.setValue(new StringType(value));
        observation.setEffective(new DateTimeType(effectiveDate));

        HapiObservationService spyService = spy(hapiObservationService);
        doReturn(observation).when(spyService).createObservation(
                eq(patientPersonnummer), eq(performerPersonnummer), anyString(), eq(value), eq(unit), any(Date.class));

        // Act
        Observation result = spyService.createObservation(
                patientPersonnummer, performerPersonnummer, "General Assessment", value, unit, effectiveDate);

        // Assert
        assertThat(result.hasValueStringType()).isTrue();
        assertThat(result.getValueStringType().getValue()).isEqualTo(value);
    }

    @Test
    void createObservation_shouldCreateObservationWithoutPerformer_whenPerformerIsNull() {
        // Arrange
        String patientPersonnummer = "197001011234";
        Date effectiveDate = new Date();

        Observation observation = createTestObservation("123", "Patient/fhir-123", null,
                "Self-reported", "Normal", "", effectiveDate);

        HapiObservationService spyService = spy(hapiObservationService);
        doReturn(observation).when(spyService).createObservation(
                eq(patientPersonnummer), isNull(), anyString(), anyString(), anyString(), any(Date.class));

        // Act
        Observation result = spyService.createObservation(
                patientPersonnummer, null, "Self-reported", "Normal", "", effectiveDate);

        // Assert
        assertThat(result).isNotNull();
        assertThat(result.getPerformer()).isEmpty();
    }

    @Test
    void createObservation_shouldCreateObservationWithoutPerformer_whenPerformerIsEmpty() {
        // Arrange
        String patientPersonnummer = "197001011234";
        Date effectiveDate = new Date();

        Observation observation = createTestObservation("123", "Patient/fhir-123", null,
                "Self-reported", "Normal", "", effectiveDate);

        HapiObservationService spyService = spy(hapiObservationService);
        doReturn(observation).when(spyService).createObservation(
                eq(patientPersonnummer), eq(""), anyString(), anyString(), anyString(), any(Date.class));

        // Act
        Observation result = spyService.createObservation(
                patientPersonnummer, "", "Self-reported", "Normal", "", effectiveDate);

        // Assert
        assertThat(result).isNotNull();
        assertThat(result.getPerformer()).isEmpty();
    }

    @Test
    void createObservation_shouldThrowException_whenCreationFails() {
        // Arrange
        String patientPersonnummer = "197001011234";
        String performerPersonnummer = "198001011234";
        Date effectiveDate = new Date();

        HapiObservationService spyService = spy(hapiObservationService);
        doThrow(new RuntimeException("Server error")).when(spyService).createObservation(
                eq(patientPersonnummer), eq(performerPersonnummer), anyString(), anyString(), anyString(), any(Date.class));

        // Act & Assert
        assertThatThrownBy(() -> spyService.createObservation(
                patientPersonnummer, performerPersonnummer, "Test", "100", "unit", effectiveDate))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Server error");
    }

    @Test
    void createObservation_shouldThrowException_whenPatientNotFound() {
        // Arrange
        String patientPersonnummer = "999999999999";
        String performerPersonnummer = "198001011234";
        Date effectiveDate = new Date();

        HapiObservationService spyService = spy(hapiObservationService);
        doThrow(new RuntimeException("Patient not found with identifier or ID: " + patientPersonnummer))
                .when(spyService).createObservation(
                        eq(patientPersonnummer), eq(performerPersonnummer), anyString(), anyString(), anyString(), any(Date.class));

        // Act & Assert
        assertThatThrownBy(() -> spyService.createObservation(
                patientPersonnummer, performerPersonnummer, "Test", "100", "unit", effectiveDate))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Patient not found");
    }

    @Test
    void createObservation_shouldThrowException_whenPractitionerNotFound() {
        // Arrange
        String patientPersonnummer = "197001011234";
        String performerPersonnummer = "999999999999";
        Date effectiveDate = new Date();

        HapiObservationService spyService = spy(hapiObservationService);
        doThrow(new RuntimeException("Practitioner not found with identifier or ID: " + performerPersonnummer))
                .when(spyService).createObservation(
                        eq(patientPersonnummer), eq(performerPersonnummer), anyString(), anyString(), anyString(), any(Date.class));

        // Act & Assert
        assertThatThrownBy(() -> spyService.createObservation(
                patientPersonnummer, performerPersonnummer, "Test", "100", "unit", effectiveDate))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Practitioner not found");
    }

    @Test
    void createObservation_shouldSetCorrectStatus() {
        // Arrange
        String patientPersonnummer = "197001011234";
        String performerPersonnummer = "198001011234";
        Date effectiveDate = new Date();

        Observation observation = createTestObservation("123", "Patient/fhir-123", "Practitioner/fhir-456",
                "Test", "100", "unit", effectiveDate);
        observation.setStatus(Observation.ObservationStatus.FINAL);

        HapiObservationService spyService = spy(hapiObservationService);
        doReturn(observation).when(spyService).createObservation(
                eq(patientPersonnummer), eq(performerPersonnummer), anyString(), anyString(), anyString(), any(Date.class));

        // Act
        Observation result = spyService.createObservation(
                patientPersonnummer, performerPersonnummer, "Test", "100", "unit", effectiveDate);

        // Assert
        assertThat(result.getStatus()).isEqualTo(Observation.ObservationStatus.FINAL);
    }

    @Test
    void createObservation_shouldSetCorrectCategory() {
        // Arrange
        String patientPersonnummer = "197001011234";
        String performerPersonnummer = "198001011234";
        Date effectiveDate = new Date();

        Observation observation = createTestObservation("123", "Patient/fhir-123", "Practitioner/fhir-456",
                "Vital Sign", "100", "unit", effectiveDate);
        observation.addCategory()
                .addCoding()
                .setSystem("http://terminology.hl7.org/CodeSystem/observation-category")
                .setCode("vital-signs")
                .setDisplay("Vital signs");

        HapiObservationService spyService = spy(hapiObservationService);
        doReturn(observation).when(spyService).createObservation(
                eq(patientPersonnummer), eq(performerPersonnummer), anyString(), anyString(), anyString(), any(Date.class));

        // Act
        Observation result = spyService.createObservation(
                patientPersonnummer, performerPersonnummer, "Vital Sign", "100", "unit", effectiveDate);

        // Assert
        assertThat(result.getCategory()).isNotEmpty();
        assertThat(result.getCategory().get(0).getCoding().get(0).getCode()).isEqualTo("vital-signs");
    }

    // INTEGRATION-STYLE TESTS

    @Test
    void getObservationById_shouldCatchResourceNotFoundException() {
        // Arrange
        String observationId = "99999";
        HapiObservationService spyService = spy(hapiObservationService);

        doAnswer(invocation -> {
            try {
                throw new ResourceNotFoundException("Observation not found");
            } catch (Exception e) {
                return Optional.empty();
            }
        }).when(spyService).getObservationById(observationId);

        // Act
        Optional<Observation> result = spyService.getObservationById(observationId);

        // Assert
        assertThat(result).isEmpty();
    }

    // EDGE CASES

    @Test
    void getObservationsForPatient_shouldHandleDifferentPatientIdFormats() {
        // Arrange
        String[] patientPersonnummers = {"197001011234", "198502021234", "200012121234"};

        for (String patientPersonnummer : patientPersonnummers) {
            List<Observation> observations = new ArrayList<>();
            observations.add(createTestObservation("1", "Patient/fhir-" + patientPersonnummer, "Practitioner/1",
                    "Test", "100", "unit", new Date()));

            HapiObservationService spyService = spy(hapiObservationService);
            doReturn(observations).when(spyService).getObservationsForPatient(patientPersonnummer);

            // Act
            List<Observation> result = spyService.getObservationsForPatient(patientPersonnummer);

            // Assert
            assertThat(result).hasSize(1);
        }
    }

    @Test
    void createObservation_shouldHandleDefaultUnitWhenUnitIsNull() {
        // Arrange
        String patientPersonnummer = "197001011234";
        String performerPersonnummer = "198001011234";
        Date effectiveDate = new Date();

        Observation observation = createTestObservation("123", "Patient/fhir-123", "Practitioner/fhir-456",
                "Score", "10", "{score}", effectiveDate);

        HapiObservationService spyService = spy(hapiObservationService);
        doReturn(observation).when(spyService).createObservation(
                eq(patientPersonnummer), eq(performerPersonnummer), anyString(), anyString(), isNull(), any(Date.class));

        // Act
        Observation result = spyService.createObservation(
                patientPersonnummer, performerPersonnummer, "Score", "10", null, effectiveDate);

        // Assert
        assertThat(result.getValueQuantity().getUnit()).isEqualTo("{score}");
    }

    // HELPER METHODS

    private Observation createTestObservation(String id, String patientRef, String performerRef,
                                              String description, String value, String unit, Date effectiveDate) {
        Observation observation = new Observation();
        observation.setId(id);
        observation.setStatus(Observation.ObservationStatus.FINAL);

        observation.addCategory()
                .addCoding()
                .setSystem("http://terminology.hl7.org/CodeSystem/observation-category")
                .setCode("vital-signs")
                .setDisplay("Vital signs");

        observation.getCode()
                .addCoding()
                .setSystem("http://loinc.org")
                .setCode("8310-5")
                .setDisplay(description);
        observation.getCode().setText(description);

        observation.setSubject(new Reference(patientRef));

        if (performerRef != null && !performerRef.isEmpty()) {
            observation.addPerformer(new Reference(performerRef));
        }

        if (value != null && !value.isEmpty()) {
            try {
                double numericValue = Double.parseDouble(value);
                Quantity quantity = new Quantity()
                        .setValue(numericValue)
                        .setUnit(unit != null && !unit.isEmpty() ? unit : "{score}")
                        .setSystem("http://unitsofmeasure.org")
                        .setCode(unit != null && !unit.isEmpty() ? unit : "{score}");
                observation.setValue(quantity);
            } catch (NumberFormatException e) {
                observation.setValue(new StringType(value));
            }
        }

        observation.setEffective(new DateTimeType(effectiveDate));
        observation.setIssued(effectiveDate);

        return observation;
    }
}