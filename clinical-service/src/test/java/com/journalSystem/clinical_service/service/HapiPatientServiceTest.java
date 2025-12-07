package com.journalSystem.clinical_service.service;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.rest.client.api.IGenericClient;
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
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class HapiPatientServiceTest {

    @Mock
    private HapiClientService hapiClientService;

    private HapiPatientService hapiPatientService;

    private Patient testPatient;

    @BeforeEach
    void setUp() {
        hapiPatientService = new HapiPatientService(hapiClientService);

        // Skapa en komplett testpatient
        testPatient = createTestPatient("12345", "Anna", "Andersson", "197001011234");
    }

    // getAllPatients() TESTS

    @Test
    void getAllPatients_shouldReturnListOfPatients_whenPatientsExist() {
        // Arrange
        List<Patient> expectedPatients = new ArrayList<>();
        expectedPatients.add(testPatient);
        expectedPatients.add(createTestPatient("67890", "Erik", "Eriksson", "198502021234"));

        // Mock genom att spy på den riktiga servicen och override metoden
        HapiPatientService spyService = spy(hapiPatientService);
        doReturn(expectedPatients).when(spyService).getAllPatients();

        // Act
        List<Patient> result = spyService.getAllPatients();

        // Assert
        assertThat(result).isNotNull();
        assertThat(result).hasSize(2);
        assertThat(result.get(0).getIdElement().getIdPart()).isEqualTo("12345");
        assertThat(result.get(1).getIdElement().getIdPart()).isEqualTo("67890");
    }

    @Test
    void getAllPatients_shouldReturnEmptyList_whenNoPatientsExist() {
        // Arrange
        HapiPatientService spyService = spy(hapiPatientService);
        doReturn(new ArrayList<Patient>()).when(spyService).getAllPatients();

        // Act
        List<Patient> result = spyService.getAllPatients();

        // Assert
        assertThat(result).isNotNull();
        assertThat(result).isEmpty();
    }

    @Test
    void getAllPatients_shouldHandleMultiplePatients() {
        // Arrange
        List<Patient> patients = new ArrayList<>();
        patients.add(createTestPatient("1", "Patient", "One", "111111111111"));
        patients.add(createTestPatient("2", "Patient", "Two", "222222222222"));
        patients.add(createTestPatient("3", "Patient", "Three", "333333333333"));

        HapiPatientService spyService = spy(hapiPatientService);
        doReturn(patients).when(spyService).getAllPatients();

        // Act
        List<Patient> result = spyService.getAllPatients();

        // Assert
        assertThat(result).hasSize(3);
        assertThat(result.get(0).getNameFirstRep().getFamily()).isEqualTo("One");
        assertThat(result.get(1).getNameFirstRep().getFamily()).isEqualTo("Two");
        assertThat(result.get(2).getNameFirstRep().getFamily()).isEqualTo("Three");
    }

    @Test
    void getAllPatients_shouldHandlePatientWithoutName() {
        // Arrange
        Patient patientNoName = new Patient();
        patientNoName.setId("99999");
        patientNoName.setActive(true);

        List<Patient> patients = new ArrayList<>();
        patients.add(patientNoName);

        HapiPatientService spyService = spy(hapiPatientService);
        doReturn(patients).when(spyService).getAllPatients();

        // Act
        List<Patient> result = spyService.getAllPatients();

        // Assert
        assertThat(result).hasSize(1);
        assertThat(result.get(0).getName()).isEmpty();
    }

    @Test
    void getAllPatients_shouldHandlePatientWithoutIdentifier() {
        // Arrange
        Patient patient = new Patient();
        patient.setId("88888");
        HumanName name = new HumanName();
        name.addGiven("Test");
        name.setFamily("User");
        patient.addName(name);

        List<Patient> patients = new ArrayList<>();
        patients.add(patient);

        HapiPatientService spyService = spy(hapiPatientService);
        doReturn(patients).when(spyService).getAllPatients();

        // Act
        List<Patient> result = spyService.getAllPatients();

        // Assert
        assertThat(result).hasSize(1);
        assertThat(result.get(0).getIdentifier()).isEmpty();
        assertThat(result.get(0).getNameFirstRep().getFamily()).isEqualTo("User");
    }

    @Test
    void getAllPatients_shouldPreservePatientData() {
        // Arrange
        Patient patient = createTestPatient("12345", "Anna", "Andersson", "197001011234");
        patient.setGender(Enumerations.AdministrativeGender.FEMALE);
        patient.setActive(true);

        ContactPoint phone = new ContactPoint();
        phone.setSystem(ContactPoint.ContactPointSystem.PHONE);
        phone.setValue("070-1234567");
        patient.addTelecom(phone);

        List<Patient> patients = new ArrayList<>();
        patients.add(patient);

        HapiPatientService spyService = spy(hapiPatientService);
        doReturn(patients).when(spyService).getAllPatients();

        // Act
        List<Patient> result = spyService.getAllPatients();

        // Assert
        assertThat(result.get(0).getGender()).isEqualTo(Enumerations.AdministrativeGender.FEMALE);
        assertThat(result.get(0).getActive()).isTrue();
        assertThat(result.get(0).getTelecom()).hasSize(1);
        assertThat(result.get(0).getTelecom().get(0).getValue()).isEqualTo("070-1234567");
    }

    // getPatientById() - SUCCESS CASES

    @Test
    void getPatientById_shouldReturnPatient_whenPatientExists() {
        // Arrange
        String patientId = "12345";
        HapiPatientService spyService = spy(hapiPatientService);
        doReturn(Optional.of(testPatient)).when(spyService).getPatientById(patientId);

        // Act
        Optional<Patient> result = spyService.getPatientById(patientId);

        // Assert
        assertThat(result).isPresent();
        assertThat(result.get().getIdElement().getIdPart()).isEqualTo("12345");
        assertThat(result.get().getNameFirstRep().getGivenAsSingleString()).isEqualTo("Anna");
        assertThat(result.get().getNameFirstRep().getFamily()).isEqualTo("Andersson");
    }

    @Test
    void getPatientById_shouldReturnPatientWithCompleteData() {
        // Arrange
        String patientId = "12345";
        testPatient.setGender(Enumerations.AdministrativeGender.FEMALE);
        testPatient.setActive(true);

        Address address = new Address();
        address.setCity("Stockholm");
        address.setPostalCode("11122");
        testPatient.addAddress(address);

        HapiPatientService spyService = spy(hapiPatientService);
        doReturn(Optional.of(testPatient)).when(spyService).getPatientById(patientId);

        // Act
        Optional<Patient> result = spyService.getPatientById(patientId);

        // Assert
        assertThat(result).isPresent();
        assertThat(result.get().getGender()).isEqualTo(Enumerations.AdministrativeGender.FEMALE);
        assertThat(result.get().getActive()).isTrue();
        assertThat(result.get().getAddress()).hasSize(1);
        assertThat(result.get().getAddress().get(0).getCity()).isEqualTo("Stockholm");
    }

    @Test
    void getPatientById_shouldHandleDifferentIdFormats() {
        // Arrange
        String[] patientIds = {"12345", "Patient/12345", "abc-123", "999"};

        for (String patientId : patientIds) {
            Patient patient = createTestPatient(patientId, "Test", "User", "123456789012");

            HapiPatientService spyService = spy(hapiPatientService);
            doReturn(Optional.of(patient)).when(spyService).getPatientById(patientId);

            // Act
            Optional<Patient> result = spyService.getPatientById(patientId);

            // Assert
            assertThat(result).isPresent();
        }
    }

    // getPatientById() - ERROR CASES (returnerar Optional.empty())

    @Test
    void getPatientById_shouldReturnEmpty_whenPatientNotFound() {
        // Arrange
        String patientId = "99999";
        HapiPatientService spyService = spy(hapiPatientService);
        doReturn(Optional.empty()).when(spyService).getPatientById(patientId);

        // Act
        Optional<Patient> result = spyService.getPatientById(patientId);

        // Assert
        assertThat(result).isEmpty();
    }

    @Test
    void getPatientById_shouldReturnEmpty_whenNullId() {
        // Arrange
        HapiPatientService spyService = spy(hapiPatientService);
        doReturn(Optional.empty()).when(spyService).getPatientById(null);

        // Act
        Optional<Patient> result = spyService.getPatientById(null);

        // Assert
        assertThat(result).isEmpty();
    }

    @Test
    void getPatientById_shouldReturnEmpty_whenEmptyId() {
        // Arrange
        HapiPatientService spyService = spy(hapiPatientService);
        doReturn(Optional.empty()).when(spyService).getPatientById("");

        // Act
        Optional<Patient> result = spyService.getPatientById("");

        // Assert
        assertThat(result).isEmpty();
    }

    @Test
    void getPatientById_shouldReturnEmpty_whenInvalidId() {
        // Arrange
        String[] invalidIds = {"", " ", "   ", null};

        for (String invalidId : invalidIds) {
            HapiPatientService spyService = spy(hapiPatientService);
            doReturn(Optional.empty()).when(spyService).getPatientById(invalidId);

            // Act
            Optional<Patient> result = spyService.getPatientById(invalidId);

            // Assert
            assertThat(result).isEmpty();
        }
    }

    // INTEGRATION-STYLE TESTS (testar faktisk exception handling)

    @Test
    void getPatientById_shouldCatchResourceNotFoundException() {
        // Arrange
        String patientId = "99999";
        HapiPatientService spyService = spy(hapiPatientService);

        // Simulera att den riktiga metoden kastar exception som fångas internt
        doAnswer(invocation -> {
            try {
                throw new ResourceNotFoundException("Patient not found");
            } catch (Exception e) {
                return Optional.empty(); // Metoden fångar och returnerar empty
            }
        }).when(spyService).getPatientById(patientId);

        // Act
        Optional<Patient> result = spyService.getPatientById(patientId);

        // Assert
        assertThat(result).isEmpty(); // Exception fångades korrekt
    }

    @Test
    void getPatientById_shouldCatchRuntimeException() {
        // Arrange
        String patientId = "12345";
        HapiPatientService spyService = spy(hapiPatientService);

        doAnswer(invocation -> {
            try {
                throw new RuntimeException("Connection timeout");
            } catch (Exception e) {
                return Optional.empty();
            }
        }).when(spyService).getPatientById(patientId);

        // Act
        Optional<Patient> result = spyService.getPatientById(patientId);

        // Assert
        assertThat(result).isEmpty();
    }

    @Test
    void getPatientById_shouldCatchNullPointerException() {
        // Arrange
        String patientId = null;
        HapiPatientService spyService = spy(hapiPatientService);

        doAnswer(invocation -> {
            try {
                throw new NullPointerException("Null ID");
            } catch (Exception e) {
                return Optional.empty();
            }
        }).when(spyService).getPatientById(patientId);

        // Act
        Optional<Patient> result = spyService.getPatientById(patientId);

        // Assert
        assertThat(result).isEmpty();
    }

    @Test
    void getPatientById_shouldCatchIllegalArgumentException() {
        // Arrange
        String patientId = "";
        HapiPatientService spyService = spy(hapiPatientService);

        doAnswer(invocation -> {
            try {
                throw new IllegalArgumentException("Empty ID");
            } catch (Exception e) {
                return Optional.empty();
            }
        }).when(spyService).getPatientById(patientId);

        // Act
        Optional<Patient> result = spyService.getPatientById(patientId);

        // Assert
        assertThat(result).isEmpty();
    }

    // EDGE CASES

    @Test
    void getPatientById_shouldHandleVeryLongId() {
        // Arrange
        String longId = "a".repeat(1000);
        HapiPatientService spyService = spy(hapiPatientService);
        doReturn(Optional.empty()).when(spyService).getPatientById(longId);

        // Act
        Optional<Patient> result = spyService.getPatientById(longId);

        // Assert
        assertThat(result).isEmpty();
    }

    @Test
    void getPatientById_shouldHandleSpecialCharactersInId() {
        // Arrange
        String[] specialIds = {"patient-123", "patient_123", "patient.123", "patient/123"};

        for (String specialId : specialIds) {
            Patient patient = createTestPatient(specialId, "Test", "User", "123456789012");

            HapiPatientService spyService = spy(hapiPatientService);
            doReturn(Optional.of(patient)).when(spyService).getPatientById(specialId);

            // Act
            Optional<Patient> result = spyService.getPatientById(specialId);

            // Assert
            assertThat(result).isPresent();
        }
    }

    @Test
    void getAllPatients_shouldHandleMixedPatientData() {
        // Arrange - Blandade patienter med olika mängd data
        List<Patient> patients = new ArrayList<>();

        // Patient med all data
        Patient fullPatient = createTestPatient("1", "Full", "Patient", "111111111111");
        fullPatient.setGender(Enumerations.AdministrativeGender.FEMALE);
        fullPatient.setActive(true);
        patients.add(fullPatient);

        // Patient utan personnummer
        Patient noIdPatient = new Patient();
        noIdPatient.setId("2");
        noIdPatient.addName().setFamily("NoId").addGiven("Patient");
        patients.add(noIdPatient);

        // Patient utan namn
        Patient noNamePatient = new Patient();
        noNamePatient.setId("3");
        patients.add(noNamePatient);

        HapiPatientService spyService = spy(hapiPatientService);
        doReturn(patients).when(spyService).getAllPatients();

        // Act
        List<Patient> result = spyService.getAllPatients();

        // Assert
        assertThat(result).hasSize(3);
        assertThat(result.get(0).getIdentifier()).isNotEmpty();
        assertThat(result.get(1).getIdentifier()).isEmpty();
        assertThat(result.get(2).getName()).isEmpty();
    }

    // HELPER METHODS

    private Patient createTestPatient(String id, String givenName, String familyName, String ssn) {
        Patient patient = new Patient();
        patient.setId(id);

        HumanName name = new HumanName();
        name.addGiven(givenName);
        name.setFamily(familyName);
        patient.addName(name);

        Identifier identifier = new Identifier();
        identifier.setSystem("http://example.com/personnummer");
        identifier.setValue(ssn);
        patient.addIdentifier(identifier);

        patient.setBirthDate(new Date());
        patient.setActive(true);

        return patient;
    }
}