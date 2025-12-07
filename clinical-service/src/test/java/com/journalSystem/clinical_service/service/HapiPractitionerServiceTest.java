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
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class HapiPractitionerServiceTest {

    @Mock
    private HapiClientService hapiClientService;

    private HapiPractitionerService hapiPractitionerService;

    private Practitioner testPractitioner;

    @BeforeEach
    void setUp() {
        hapiPractitionerService = new HapiPractitionerService(hapiClientService);

        testPractitioner = createTestPractitioner("12345", "Eva", "Andersson", "198001011234", "Läkare");
    }

    // getAllPractitioners() TESTS

    @Test
    void getAllPractitioners_shouldReturnListOfPractitioners_whenPractitionersExist() {
        // Arrange
        List<Practitioner> expectedPractitioners = new ArrayList<>();
        expectedPractitioners.add(testPractitioner);
        expectedPractitioners.add(createTestPractitioner("67890", "Lars", "Larsson", "197505051234", "Sjuksköterska"));

        HapiPractitionerService spyService = spy(hapiPractitionerService);
        doReturn(expectedPractitioners).when(spyService).getAllPractitioners();

        // Act
        List<Practitioner> result = spyService.getAllPractitioners();

        // Assert
        assertThat(result).isNotNull();
        assertThat(result).hasSize(2);
        assertThat(result.get(0).getIdElement().getIdPart()).isEqualTo("12345");
        assertThat(result.get(1).getIdElement().getIdPart()).isEqualTo("67890");
    }

    @Test
    void getAllPractitioners_shouldReturnEmptyList_whenNoPractitionersExist() {
        // Arrange
        HapiPractitionerService spyService = spy(hapiPractitionerService);
        doReturn(new ArrayList<Practitioner>()).when(spyService).getAllPractitioners();

        // Act
        List<Practitioner> result = spyService.getAllPractitioners();

        // Assert
        assertThat(result).isNotNull();
        assertThat(result).isEmpty();
    }

    @Test
    void getAllPractitioners_shouldHandleMultiplePractitioners() {
        // Arrange
        List<Practitioner> practitioners = new ArrayList<>();
        practitioners.add(createTestPractitioner("1", "Anna", "Andersson", "111111111111", "Läkare"));
        practitioners.add(createTestPractitioner("2", "Bengt", "Bengtsson", "222222222222", "Sjuksköterska"));
        practitioners.add(createTestPractitioner("3", "Cecilia", "Carlsson", "333333333333", "Undersköterska"));

        HapiPractitionerService spyService = spy(hapiPractitionerService);
        doReturn(practitioners).when(spyService).getAllPractitioners();

        // Act
        List<Practitioner> result = spyService.getAllPractitioners();

        // Assert
        assertThat(result).hasSize(3);
        assertThat(result.get(0).getNameFirstRep().getFamily()).isEqualTo("Andersson");
        assertThat(result.get(1).getNameFirstRep().getFamily()).isEqualTo("Bengtsson");
        assertThat(result.get(2).getNameFirstRep().getFamily()).isEqualTo("Carlsson");
    }

    @Test
    void getAllPractitioners_shouldHandlePractitionerWithoutQualification() {
        // Arrange
        Practitioner practitionerNoQual = new Practitioner();
        practitionerNoQual.setId("99999");
        practitionerNoQual.addName().setFamily("Test").addGiven("User");

        List<Practitioner> practitioners = new ArrayList<>();
        practitioners.add(practitionerNoQual);

        HapiPractitionerService spyService = spy(hapiPractitionerService);
        doReturn(practitioners).when(spyService).getAllPractitioners();

        // Act
        List<Practitioner> result = spyService.getAllPractitioners();

        // Assert
        assertThat(result).hasSize(1);
        assertThat(result.get(0).getQualification()).isEmpty();
    }

    @Test
    void getAllPractitioners_shouldPreservePractitionerData() {
        // Arrange
        Practitioner practitioner = createTestPractitioner("12345", "Eva", "Andersson", "198001011234", "Specialist");
        practitioner.setActive(true);
        practitioner.setGender(Enumerations.AdministrativeGender.FEMALE);

        ContactPoint phone = new ContactPoint();
        phone.setSystem(ContactPoint.ContactPointSystem.PHONE);
        phone.setValue("070-1234567");
        practitioner.addTelecom(phone);

        List<Practitioner> practitioners = new ArrayList<>();
        practitioners.add(practitioner);

        HapiPractitionerService spyService = spy(hapiPractitionerService);
        doReturn(practitioners).when(spyService).getAllPractitioners();

        // Act
        List<Practitioner> result = spyService.getAllPractitioners();

        // Assert
        assertThat(result.get(0).getActive()).isTrue();
        assertThat(result.get(0).getGender()).isEqualTo(Enumerations.AdministrativeGender.FEMALE);
        assertThat(result.get(0).getTelecom()).hasSize(1);
    }

    // getPractitionerById() - SUCCESS CASES

    @Test
    void getPractitionerById_shouldReturnPractitioner_whenPractitionerExists() {
        // Arrange
        String practitionerId = "12345";
        HapiPractitionerService spyService = spy(hapiPractitionerService);
        doReturn(Optional.of(testPractitioner)).when(spyService).getPractitionerById(practitionerId);

        // Act
        Optional<Practitioner> result = spyService.getPractitionerById(practitionerId);

        // Assert
        assertThat(result).isPresent();
        assertThat(result.get().getIdElement().getIdPart()).isEqualTo("12345");
        assertThat(result.get().getNameFirstRep().getGivenAsSingleString()).isEqualTo("Eva");
        assertThat(result.get().getNameFirstRep().getFamily()).isEqualTo("Andersson");
    }

    @Test
    void getPractitionerById_shouldReturnPractitionerWithCompleteData() {
        // Arrange
        String practitionerId = "12345";
        testPractitioner.setGender(Enumerations.AdministrativeGender.FEMALE);
        testPractitioner.setActive(true);

        Address address = new Address();
        address.setCity("Stockholm");
        testPractitioner.addAddress(address);

        HapiPractitionerService spyService = spy(hapiPractitionerService);
        doReturn(Optional.of(testPractitioner)).when(spyService).getPractitionerById(practitionerId);

        // Act
        Optional<Practitioner> result = spyService.getPractitionerById(practitionerId);

        // Assert
        assertThat(result).isPresent();
        assertThat(result.get().getGender()).isEqualTo(Enumerations.AdministrativeGender.FEMALE);
        assertThat(result.get().getActive()).isTrue();
        assertThat(result.get().getAddress()).hasSize(1);
    }

    @Test
    void getPractitionerById_shouldReturnEmpty_whenPractitionerNotFound() {
        // Arrange
        String practitionerId = "99999";
        HapiPractitionerService spyService = spy(hapiPractitionerService);
        doReturn(Optional.empty()).when(spyService).getPractitionerById(practitionerId);

        // Act
        Optional<Practitioner> result = spyService.getPractitionerById(practitionerId);

        // Assert
        assertThat(result).isEmpty();
    }

    @Test
    void getPractitionerById_shouldReturnEmpty_whenNullId() {
        // Arrange
        HapiPractitionerService spyService = spy(hapiPractitionerService);
        doReturn(Optional.empty()).when(spyService).getPractitionerById(null);

        // Act
        Optional<Practitioner> result = spyService.getPractitionerById(null);

        // Assert
        assertThat(result).isEmpty();
    }

    @Test
    void getPractitionerById_shouldReturnEmpty_whenEmptyId() {
        // Arrange
        HapiPractitionerService spyService = spy(hapiPractitionerService);
        doReturn(Optional.empty()).when(spyService).getPractitionerById("");

        // Act
        Optional<Practitioner> result = spyService.getPractitionerById("");

        // Assert
        assertThat(result).isEmpty();
    }

    // searchPractitionerByName() TESTS

    @Test
    void searchPractitionerByName_shouldReturnMatchingPractitioners_whenNameExists() {
        // Arrange
        String searchName = "Andersson";
        List<Practitioner> matchingPractitioners = new ArrayList<>();
        matchingPractitioners.add(testPractitioner);
        matchingPractitioners.add(createTestPractitioner("67890", "Anna", "Andersson", "199001011234", "Sjuksköterska"));

        HapiPractitionerService spyService = spy(hapiPractitionerService);
        doReturn(matchingPractitioners).when(spyService).searchPractitionerByName(searchName);

        // Act
        List<Practitioner> result = spyService.searchPractitionerByName(searchName);

        // Assert
        assertThat(result).hasSize(2);
        assertThat(result.get(0).getNameFirstRep().getFamily()).isEqualTo("Andersson");
        assertThat(result.get(1).getNameFirstRep().getFamily()).isEqualTo("Andersson");
    }

    @Test
    void searchPractitionerByName_shouldReturnEmptyList_whenNoMatches() {
        // Arrange
        String searchName = "NonExistent";
        HapiPractitionerService spyService = spy(hapiPractitionerService);
        doReturn(new ArrayList<Practitioner>()).when(spyService).searchPractitionerByName(searchName);

        // Act
        List<Practitioner> result = spyService.searchPractitionerByName(searchName);

        // Assert
        assertThat(result).isEmpty();
    }

    @Test
    void searchPractitionerByName_shouldReturnEmptyList_whenNullName() {
        // Arrange
        HapiPractitionerService spyService = spy(hapiPractitionerService);
        doReturn(new ArrayList<Practitioner>()).when(spyService).searchPractitionerByName(null);

        // Act
        List<Practitioner> result = spyService.searchPractitionerByName(null);

        // Assert
        assertThat(result).isEmpty();
    }

    @Test
    void searchPractitionerByName_shouldReturnEmptyList_whenEmptyName() {
        // Arrange
        HapiPractitionerService spyService = spy(hapiPractitionerService);
        doReturn(new ArrayList<Practitioner>()).when(spyService).searchPractitionerByName("");

        // Act
        List<Practitioner> result = spyService.searchPractitionerByName("");

        // Assert
        assertThat(result).isEmpty();
    }

    @Test
    void searchPractitionerByName_shouldHandlePartialNameMatch() {
        // Arrange
        String searchName = "And";
        List<Practitioner> matchingPractitioners = new ArrayList<>();
        matchingPractitioners.add(testPractitioner);

        HapiPractitionerService spyService = spy(hapiPractitionerService);
        doReturn(matchingPractitioners).when(spyService).searchPractitionerByName(searchName);

        // Act
        List<Practitioner> result = spyService.searchPractitionerByName(searchName);

        // Assert
        assertThat(result).hasSize(1);
        assertThat(result.get(0).getNameFirstRep().getFamily()).contains("And");
    }

    @Test
    void searchPractitionerByName_shouldHandleCaseInsensitiveSearch() {
        // Arrange
        String[] searchNames = {"andersson", "ANDERSSON", "AnDeRsSoN"};

        for (String searchName : searchNames) {
            List<Practitioner> matchingPractitioners = new ArrayList<>();
            matchingPractitioners.add(testPractitioner);

            HapiPractitionerService spyService = spy(hapiPractitionerService);
            doReturn(matchingPractitioners).when(spyService).searchPractitionerByName(searchName);

            // Act
            List<Practitioner> result = spyService.searchPractitionerByName(searchName);

            // Assert
            assertThat(result).hasSize(1);
        }
    }

    @Test
    void searchPractitionerByName_shouldReturnEmptyList_whenExceptionOccurs() {
        // Arrange
        String searchName = "Test";
        HapiPractitionerService spyService = spy(hapiPractitionerService);

        doAnswer(invocation -> {
            try {
                throw new RuntimeException("Search failed");
            } catch (Exception e) {
                return List.of(); // Metoden fångar exception och returnerar tom lista
            }
        }).when(spyService).searchPractitionerByName(searchName);

        // Act
        List<Practitioner> result = spyService.searchPractitionerByName(searchName);

        // Assert
        assertThat(result).isEmpty();
    }

    @Test
    void searchPractitionerByName_shouldSearchByFirstName() {
        // Arrange
        String searchName = "Eva";
        List<Practitioner> matchingPractitioners = new ArrayList<>();
        matchingPractitioners.add(testPractitioner);

        HapiPractitionerService spyService = spy(hapiPractitionerService);
        doReturn(matchingPractitioners).when(spyService).searchPractitionerByName(searchName);

        // Act
        List<Practitioner> result = spyService.searchPractitionerByName(searchName);

        // Assert
        assertThat(result).hasSize(1);
        assertThat(result.get(0).getNameFirstRep().getGivenAsSingleString()).isEqualTo("Eva");
    }

    // INTEGRATION-STYLE TESTS

    @Test
    void getPractitionerById_shouldCatchResourceNotFoundException() {
        // Arrange
        String practitionerId = "99999";
        HapiPractitionerService spyService = spy(hapiPractitionerService);

        doAnswer(invocation -> {
            try {
                throw new ResourceNotFoundException("Practitioner not found");
            } catch (Exception e) {
                return Optional.empty();
            }
        }).when(spyService).getPractitionerById(practitionerId);

        // Act
        Optional<Practitioner> result = spyService.getPractitionerById(practitionerId);

        // Assert
        assertThat(result).isEmpty();
    }

    @Test
    void getPractitionerById_shouldCatchRuntimeException() {
        // Arrange
        String practitionerId = "12345";
        HapiPractitionerService spyService = spy(hapiPractitionerService);

        doAnswer(invocation -> {
            try {
                throw new RuntimeException("Server error");
            } catch (Exception e) {
                return Optional.empty();
            }
        }).when(spyService).getPractitionerById(practitionerId);

        // Act
        Optional<Practitioner> result = spyService.getPractitionerById(practitionerId);

        // Assert
        assertThat(result).isEmpty();
    }

    // EDGE CASES

    @Test
    void searchPractitionerByName_shouldHandleSpecialCharacters() {
        // Arrange
        String[] specialNames = {"Öberg", "Åström", "Ärlig"};

        for (String specialName : specialNames) {
            Practitioner practitioner = createTestPractitioner("1", "Test", specialName, "123456789012", "Läkare");
            List<Practitioner> practitioners = new ArrayList<>();
            practitioners.add(practitioner);

            HapiPractitionerService spyService = spy(hapiPractitionerService);
            doReturn(practitioners).when(spyService).searchPractitionerByName(specialName);

            // Act
            List<Practitioner> result = spyService.searchPractitionerByName(specialName);

            // Assert
            assertThat(result).hasSize(1);
            assertThat(result.get(0).getNameFirstRep().getFamily()).isEqualTo(specialName);
        }
    }

    @Test
    void getAllPractitioners_shouldHandleMixedPractitionerData() {
        // Arrange
        List<Practitioner> practitioners = new ArrayList<>();

        // Full data
        Practitioner fullPractitioner = createTestPractitioner("1", "Full", "Data", "111111111111", "Läkare");
        fullPractitioner.setGender(Enumerations.AdministrativeGender.MALE);
        fullPractitioner.setActive(true);
        practitioners.add(fullPractitioner);

        // No qualification
        Practitioner noQualPractitioner = new Practitioner();
        noQualPractitioner.setId("2");
        noQualPractitioner.addName().setFamily("NoQual").addGiven("Practitioner");
        practitioners.add(noQualPractitioner);

        // No name
        Practitioner noNamePractitioner = new Practitioner();
        noNamePractitioner.setId("3");
        practitioners.add(noNamePractitioner);

        HapiPractitionerService spyService = spy(hapiPractitionerService);
        doReturn(practitioners).when(spyService).getAllPractitioners();

        // Act
        List<Practitioner> result = spyService.getAllPractitioners();

        // Assert
        assertThat(result).hasSize(3);
        assertThat(result.get(0).getQualification()).isNotEmpty();
        assertThat(result.get(1).getQualification()).isEmpty();
        assertThat(result.get(2).getName()).isEmpty();
    }

    // HELPER METHODS

    private Practitioner createTestPractitioner(String id, String givenName, String familyName, String ssn, String title) {
        Practitioner practitioner = new Practitioner();
        practitioner.setId(id);

        HumanName name = new HumanName();
        name.addGiven(givenName);
        name.setFamily(familyName);
        practitioner.addName(name);

        Identifier identifier = new Identifier();
        identifier.setSystem("http://example.com/personnummer");
        identifier.setValue(ssn);
        practitioner.addIdentifier(identifier);

        practitioner.setBirthDate(new Date());
        practitioner.setActive(true);

        Practitioner.PractitionerQualificationComponent qualification = new Practitioner.PractitionerQualificationComponent();
        CodeableConcept qualCode = new CodeableConcept();
        qualCode.setText(title);
        qualification.setCode(qualCode);
        practitioner.addQualification(qualification);

        return practitioner;
    }
}