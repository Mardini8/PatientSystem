package com.journalSystem.clinical_service.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class FhirLookupServiceTest {

    @Mock
    private HapiClientService hapiClientService;

    private FhirLookupService fhirLookupService;

    @BeforeEach
    void setUp() {
        fhirLookupService = new FhirLookupService(hapiClientService);
    }

    // findPatientIdByPersonnummer() TESTS

    @Test
    void findPatientIdByPersonnummer_shouldReturnFhirId_whenPatientFoundByIdentifier() {
        // Arrange
        String personnummer = "197001011234";
        String expectedFhirId = "patient-fhir-123";

        FhirLookupService spyService = spy(fhirLookupService);
        doReturn(expectedFhirId).when(spyService).findPatientIdByPersonnummer(personnummer);

        // Act
        String result = spyService.findPatientIdByPersonnummer(personnummer);

        // Assert
        assertThat(result).isEqualTo(expectedFhirId);
    }

    @Test
    void findPatientIdByPersonnummer_shouldReturnFhirId_whenPatientFoundByDirectId() {
        // Arrange
        String personnummer = "12345";
        String expectedFhirId = "12345";

        FhirLookupService spyService = spy(fhirLookupService);
        doReturn(expectedFhirId).when(spyService).findPatientIdByPersonnummer(personnummer);

        // Act
        String result = spyService.findPatientIdByPersonnummer(personnummer);

        // Assert
        assertThat(result).isEqualTo(expectedFhirId);
    }

    @Test
    void findPatientIdByPersonnummer_shouldThrowException_whenPatientNotFound() {
        // Arrange
        String personnummer = "999999999999";

        FhirLookupService spyService = spy(fhirLookupService);
        doThrow(new RuntimeException("Patient not found with identifier or ID: " + personnummer))
                .when(spyService).findPatientIdByPersonnummer(personnummer);

        // Act & Assert
        assertThatThrownBy(() -> spyService.findPatientIdByPersonnummer(personnummer))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Patient not found");
    }

    @Test
    void findPatientIdByPersonnummer_shouldThrowException_whenPersonnummerIsNull() {
        // Arrange
        FhirLookupService spyService = spy(fhirLookupService);
        doThrow(new IllegalArgumentException("Personnummer cannot be null or empty"))
                .when(spyService).findPatientIdByPersonnummer(null);

        // Act & Assert
        assertThatThrownBy(() -> spyService.findPatientIdByPersonnummer(null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("cannot be null or empty");
    }

    @Test
    void findPatientIdByPersonnummer_shouldThrowException_whenPersonnummerIsEmpty() {
        // Arrange
        FhirLookupService spyService = spy(fhirLookupService);
        doThrow(new IllegalArgumentException("Personnummer cannot be null or empty"))
                .when(spyService).findPatientIdByPersonnummer("");

        // Act & Assert
        assertThatThrownBy(() -> spyService.findPatientIdByPersonnummer(""))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("cannot be null or empty");
    }

    // findPractitionerIdByPersonnummer() TESTS

    @Test
    void findPractitionerIdByPersonnummer_shouldReturnFhirId_whenPractitionerFoundByIdentifier() {
        // Arrange
        String personnummer = "198001011234";
        String expectedFhirId = "practitioner-fhir-456";

        FhirLookupService spyService = spy(fhirLookupService);
        doReturn(expectedFhirId).when(spyService).findPractitionerIdByPersonnummer(personnummer);

        // Act
        String result = spyService.findPractitionerIdByPersonnummer(personnummer);

        // Assert
        assertThat(result).isEqualTo(expectedFhirId);
    }

    @Test
    void findPractitionerIdByPersonnummer_shouldReturnFhirId_whenPractitionerFoundByDirectId() {
        // Arrange
        String personnummer = "67890";
        String expectedFhirId = "67890";

        FhirLookupService spyService = spy(fhirLookupService);
        doReturn(expectedFhirId).when(spyService).findPractitionerIdByPersonnummer(personnummer);

        // Act
        String result = spyService.findPractitionerIdByPersonnummer(personnummer);

        // Assert
        assertThat(result).isEqualTo(expectedFhirId);
    }

    @Test
    void findPractitionerIdByPersonnummer_shouldThrowException_whenPractitionerNotFound() {
        // Arrange
        String personnummer = "999999999999";

        FhirLookupService spyService = spy(fhirLookupService);
        doThrow(new RuntimeException("Practitioner not found with identifier or ID: " + personnummer))
                .when(spyService).findPractitionerIdByPersonnummer(personnummer);

        // Act & Assert
        assertThatThrownBy(() -> spyService.findPractitionerIdByPersonnummer(personnummer))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Practitioner not found");
    }

    @Test
    void findPractitionerIdByPersonnummer_shouldThrowException_whenPersonnummerIsNull() {
        // Arrange
        FhirLookupService spyService = spy(fhirLookupService);
        doThrow(new IllegalArgumentException("Personnummer cannot be null or empty"))
                .when(spyService).findPractitionerIdByPersonnummer(null);

        // Act & Assert
        assertThatThrownBy(() -> spyService.findPractitionerIdByPersonnummer(null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("cannot be null or empty");
    }

    @Test
    void findPractitionerIdByPersonnummer_shouldThrowException_whenPersonnummerIsEmpty() {
        // Arrange
        FhirLookupService spyService = spy(fhirLookupService);
        doThrow(new IllegalArgumentException("Personnummer cannot be null or empty"))
                .when(spyService).findPractitionerIdByPersonnummer("");

        // Act & Assert
        assertThatThrownBy(() -> spyService.findPractitionerIdByPersonnummer(""))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("cannot be null or empty");
    }

    // findPatientIdOptional() TESTS

    @Test
    void findPatientIdOptional_shouldReturnOptionalWithFhirId_whenPatientFound() {
        // Arrange
        String personnummer = "197001011234";
        String expectedFhirId = "patient-fhir-123";

        FhirLookupService spyService = spy(fhirLookupService);
        doReturn(Optional.of(expectedFhirId)).when(spyService).findPatientIdOptional(personnummer);

        // Act
        Optional<String> result = spyService.findPatientIdOptional(personnummer);

        // Assert
        assertThat(result).isPresent();
        assertThat(result.get()).isEqualTo(expectedFhirId);
    }

    @Test
    void findPatientIdOptional_shouldReturnEmptyOptional_whenPatientNotFound() {
        // Arrange
        String personnummer = "999999999999";

        FhirLookupService spyService = spy(fhirLookupService);
        doReturn(Optional.empty()).when(spyService).findPatientIdOptional(personnummer);

        // Act
        Optional<String> result = spyService.findPatientIdOptional(personnummer);

        // Assert
        assertThat(result).isEmpty();
    }

    // findPractitionerIdOptional() TESTS

    @Test
    void findPractitionerIdOptional_shouldReturnOptionalWithFhirId_whenPractitionerFound() {
        // Arrange
        String personnummer = "198001011234";
        String expectedFhirId = "practitioner-fhir-456";

        FhirLookupService spyService = spy(fhirLookupService);
        doReturn(Optional.of(expectedFhirId)).when(spyService).findPractitionerIdOptional(personnummer);

        // Act
        Optional<String> result = spyService.findPractitionerIdOptional(personnummer);

        // Assert
        assertThat(result).isPresent();
        assertThat(result.get()).isEqualTo(expectedFhirId);
    }

    @Test
    void findPractitionerIdOptional_shouldReturnEmptyOptional_whenPractitionerNotFound() {
        // Arrange
        String personnummer = "999999999999";

        FhirLookupService spyService = spy(fhirLookupService);
        doReturn(Optional.empty()).when(spyService).findPractitionerIdOptional(personnummer);

        // Act
        Optional<String> result = spyService.findPractitionerIdOptional(personnummer);

        // Assert
        assertThat(result).isEmpty();
    }

    // EDGE CASES

    @Test
    void findPatientIdByPersonnummer_shouldHandleDifferentPersonnummerFormats() {
        // Arrange
        String[] personnummers = {"197001011234", "19700101-1234", "7001011234"};

        for (String personnummer : personnummers) {
            FhirLookupService spyService = spy(fhirLookupService);
            doReturn("fhir-id-" + personnummer).when(spyService).findPatientIdByPersonnummer(personnummer);

            // Act
            String result = spyService.findPatientIdByPersonnummer(personnummer);

            // Assert
            assertThat(result).isNotNull();
            assertThat(result).contains(personnummer);
        }
    }

    @Test
    void findPractitionerIdByPersonnummer_shouldHandleDifferentPersonnummerFormats() {
        // Arrange
        String[] personnummers = {"198001011234", "19800101-1234", "8001011234"};

        for (String personnummer : personnummers) {
            FhirLookupService spyService = spy(fhirLookupService);
            doReturn("fhir-id-" + personnummer).when(spyService).findPractitionerIdByPersonnummer(personnummer);

            // Act
            String result = spyService.findPractitionerIdByPersonnummer(personnummer);

            // Assert
            assertThat(result).isNotNull();
            assertThat(result).contains(personnummer);
        }
    }
}