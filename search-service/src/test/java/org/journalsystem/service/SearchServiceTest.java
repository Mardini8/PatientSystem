package org.journalsystem.service;

import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.InjectMock;
import io.smallrye.mutiny.Uni;
import io.smallrye.mutiny.helpers.test.UniAssertSubscriber;
import jakarta.inject.Inject;
import org.eclipse.microprofile.rest.client.inject.RestClient;
import org.journalsystem.client.FhirClient;
import org.journalsystem.dto.EncounterSearchResult;
import org.journalsystem.dto.PatientSearchResult;
import org.journalsystem.dto.fhir.FhirBundle;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

@QuarkusTest
class SearchServiceTest {

    @Inject
    SearchService searchService;

    @InjectMock
    @RestClient
    FhirClient fhirClient;

    private FhirBundle testPatientBundle;
    private FhirBundle testConditionBundle;
    private FhirBundle testEncounterBundle;
    private FhirBundle.FhirResource testPatient;
    private FhirBundle.FhirResource testPractitioner;

    @BeforeEach
    void setUp() {
        Mockito.reset(fhirClient);

        // Setup test data
        testPatient = createTestPatient("123", "Anna", "Andersson", "197001011234", "1970-01-01");
        testPractitioner = createTestPractitioner("999", "Dr. Erik", "Karlsson");

        testPatientBundle = createBundleWithPatients(List.of(testPatient));
        testConditionBundle = createBundleWithConditions(List.of(
                createTestCondition("cond1", "Patient/123", "Diabetes")
        ));
        testEncounterBundle = createBundleWithEncounters(List.of(
                createTestEncounter("enc1", "Patient/123", "Practitioner/999", "2024-01-01T10:00:00", "2024-01-01T11:00:00")
        ));
    }

    // searchPatientsByName() TESTS

    @Test
    void searchPatientsByName_shouldReturnPatients_whenPatientsExist() {
        // Arrange
        when(fhirClient.searchPatients("Anna")).thenReturn(Uni.createFrom().item(testPatientBundle));

        // Act
        List<PatientSearchResult> result = searchService.searchPatientsByName("Anna")
                .subscribe().withSubscriber(UniAssertSubscriber.create())
                .awaitItem()
                .getItem();

        // Assert
        assertThat(result).isNotNull();
        assertThat(result).hasSize(1);
        assertThat(result.get(0).firstName()).isEqualTo("Anna");
        assertThat(result.get(0).lastName()).isEqualTo("Andersson");
        assertThat(result.get(0).socialSecurityNumber()).isEqualTo("197001011234");
    }

    @Test
    void searchPatientsByName_shouldReturnEmptyList_whenNoPatientsFound() {
        // Arrange
        FhirBundle emptyBundle = createEmptyBundle();
        when(fhirClient.searchPatients("NonExistent")).thenReturn(Uni.createFrom().item(emptyBundle));

        // Act
        List<PatientSearchResult> result = searchService.searchPatientsByName("NonExistent")
                .subscribe().withSubscriber(UniAssertSubscriber.create())
                .awaitItem()
                .getItem();

        // Assert
        assertThat(result).isEmpty();
    }

    @Test
    void searchPatientsByName_shouldReturnMultiplePatients() {
        // Arrange
        FhirBundle.FhirResource patient2 = createTestPatient("456", "Anna", "Berg", "198001011234", "1980-01-01");
        FhirBundle multiPatientBundle = createBundleWithPatients(List.of(testPatient, patient2));

        when(fhirClient.searchPatients("Anna")).thenReturn(Uni.createFrom().item(multiPatientBundle));

        // Act
        List<PatientSearchResult> result = searchService.searchPatientsByName("Anna")
                .subscribe().withSubscriber(UniAssertSubscriber.create())
                .awaitItem()
                .getItem();

        // Assert
        assertThat(result).hasSize(2);
        assertThat(result.get(0).firstName()).isEqualTo("Anna");
        assertThat(result.get(1).firstName()).isEqualTo("Anna");
        assertThat(result.get(0).lastName()).isEqualTo("Andersson");
        assertThat(result.get(1).lastName()).isEqualTo("Berg");
    }

    @Test
    void searchPatientsByName_shouldRecoverWithEmptyList_onError() {
        // Arrange
        when(fhirClient.searchPatients("Error")).thenReturn(Uni.createFrom().failure(new RuntimeException("FHIR error")));

        // Act
        List<PatientSearchResult> result = searchService.searchPatientsByName("Error")
                .subscribe().withSubscriber(UniAssertSubscriber.create())
                .awaitItem()
                .getItem();

        // Assert
        assertThat(result).isEmpty();
    }

    @Test
    void searchPatientsByName_shouldHandleNullBundle() {
        // Arrange
        when(fhirClient.searchPatients("Null")).thenReturn(Uni.createFrom().nullItem());

        // Act
        List<PatientSearchResult> result = searchService.searchPatientsByName("Null")
                .subscribe().withSubscriber(UniAssertSubscriber.create())
                .awaitItem()
                .getItem();

        // Assert
        assertThat(result).isEmpty();
    }

    @Test
    void searchPatientsByName_shouldHandleSpecialCharacters() {
        // Arrange
        FhirBundle.FhirResource patientWithSpecialChars = createTestPatient("789", "Åsa", "Öberg", "199001011234", "1990-01-01");
        FhirBundle specialBundle = createBundleWithPatients(List.of(patientWithSpecialChars));

        when(fhirClient.searchPatients("Åsa")).thenReturn(Uni.createFrom().item(specialBundle));

        // Act
        List<PatientSearchResult> result = searchService.searchPatientsByName("Åsa")
                .subscribe().withSubscriber(UniAssertSubscriber.create())
                .awaitItem()
                .getItem();

        // Assert
        assertThat(result).hasSize(1);
        assertThat(result.get(0).firstName()).isEqualTo("Åsa");
        assertThat(result.get(0).lastName()).isEqualTo("Öberg");
    }

    // searchPatientsByCondition() TESTS

    @Test
    void searchPatientsByCondition_shouldReturnPatients_whenConditionsExist() {
        // Arrange
        when(fhirClient.searchConditions("Diabetes")).thenReturn(Uni.createFrom().item(testConditionBundle));
        when(fhirClient.getPatient("123")).thenReturn(Uni.createFrom().item(testPatient));

        // Act
        List<PatientSearchResult> result = searchService.searchPatientsByCondition("Diabetes")
                .subscribe().withSubscriber(UniAssertSubscriber.create())

                .awaitItem()
                .getItem();

        // Assert
        assertThat(result).isNotNull();
        assertThat(result).hasSize(1);
        assertThat(result.get(0).id()).isEqualTo("123");
        assertThat(result.get(0).firstName()).isEqualTo("Anna");
    }

    @Test
    void searchPatientsByCondition_shouldReturnEmptyList_whenNoConditionsFound() {
        // Arrange
        FhirBundle emptyBundle = createEmptyBundle();
        when(fhirClient.searchConditions("NonExistent")).thenReturn(Uni.createFrom().item(emptyBundle));

        // Act
        List<PatientSearchResult> result = searchService.searchPatientsByCondition("NonExistent")
                .subscribe().withSubscriber(UniAssertSubscriber.create())
                .awaitItem()
                .getItem();

        // Assert
        assertThat(result).isEmpty();
    }

    @Test
    void searchPatientsByCondition_shouldHandleMultiplePatients() {
        // Arrange
        FhirBundle.FhirResource condition2 = createTestCondition("cond2", "Patient/456", "Diabetes");
        FhirBundle multiConditionBundle = createBundleWithConditions(List.of(
                createTestCondition("cond1", "Patient/123", "Diabetes"),
                condition2
        ));

        FhirBundle.FhirResource patient2 = createTestPatient("456", "Erik", "Svensson", "198001011234", "1980-01-01");

        when(fhirClient.searchConditions("Diabetes")).thenReturn(Uni.createFrom().item(multiConditionBundle));
        when(fhirClient.getPatient("123")).thenReturn(Uni.createFrom().item(testPatient));
        when(fhirClient.getPatient("456")).thenReturn(Uni.createFrom().item(patient2));

        // Act
        List<PatientSearchResult> result = searchService.searchPatientsByCondition("Diabetes")
                .subscribe().withSubscriber(UniAssertSubscriber.create())

                .awaitItem()
                .getItem();

        // Assert
        assertThat(result).hasSize(2);
    }

    @Test
    void searchPatientsByCondition_shouldRecoverWithEmptyList_onError() {
        // Arrange
        when(fhirClient.searchConditions("Error")).thenReturn(Uni.createFrom().failure(new RuntimeException("FHIR error")));

        // Act
        List<PatientSearchResult> result = searchService.searchPatientsByCondition("Error")
                .subscribe().withSubscriber(UniAssertSubscriber.create())
                .awaitItem()
                .getItem();

        // Assert
        assertThat(result).isEmpty();
    }

    @Test
    void searchPatientsByCondition_shouldFilterOutFailedPatientFetches() {
        // Arrange
        FhirBundle.FhirResource condition2 = createTestCondition("cond2", "Patient/456", "Diabetes");
        FhirBundle multiConditionBundle = createBundleWithConditions(List.of(
                createTestCondition("cond1", "Patient/123", "Diabetes"),
                condition2
        ));

        when(fhirClient.searchConditions("Diabetes")).thenReturn(Uni.createFrom().item(multiConditionBundle));
        when(fhirClient.getPatient("123")).thenReturn(Uni.createFrom().item(testPatient));
        when(fhirClient.getPatient("456")).thenReturn(Uni.createFrom().failure(new RuntimeException("Not found")));

        // Act
        List<PatientSearchResult> result = searchService.searchPatientsByCondition("Diabetes")
                .subscribe().withSubscriber(UniAssertSubscriber.create())

                .awaitItem()
                .getItem();

        // Assert
        assertThat(result).hasSize(1);
        assertThat(result.get(0).id()).isEqualTo("123");
    }

    // searchPatientsByPractitionerId() TESTS

    @Test
    void searchPatientsByPractitionerId_shouldReturnPatients_whenUsingUUID() {
        // Arrange
        String practitionerId = "30681750-1667-311a-a3e3-878ae10a35bb";

        when(fhirClient.searchEncountersByPractitioner("Practitioner/" + practitionerId))
                .thenReturn(Uni.createFrom().item(testEncounterBundle));
        when(fhirClient.searchCareTeamsByPractitioner("Practitioner/" + practitionerId))
                .thenReturn(Uni.createFrom().item(createEmptyBundle()));
        when(fhirClient.getPatient("123")).thenReturn(Uni.createFrom().item(testPatient));

        // Act
        List<PatientSearchResult> result = searchService.searchPatientsByPractitionerId(practitionerId)
                .subscribe().withSubscriber(UniAssertSubscriber.create())

                .awaitItem()
                .getItem();

        // Assert
        assertThat(result).isNotNull();
        assertThat(result).hasSize(1);
        assertThat(result.get(0).id()).isEqualTo("123");
    }

    @Test
    void searchPatientsByPractitionerId_shouldResolveIdentifier_whenUsingPersonnummer() {
        // Arrange
        String identifier = "9999994392";
        String resolvedId = "30681750-1667-311a-a3e3-878ae10a35bb";

        FhirBundle practitionerSearchBundle = createBundleWithPractitioners(List.of(
                createTestPractitionerWithId(resolvedId, "Dr. Test", "Doctor")
        ));

        when(fhirClient.searchPractitionerByIdentifier(identifier))
                .thenReturn(Uni.createFrom().item(practitionerSearchBundle));
        when(fhirClient.searchEncountersByPractitioner("Practitioner/" + resolvedId))
                .thenReturn(Uni.createFrom().item(testEncounterBundle));
        when(fhirClient.searchCareTeamsByPractitioner("Practitioner/" + resolvedId))
                .thenReturn(Uni.createFrom().item(createEmptyBundle()));
        when(fhirClient.getPatient("123")).thenReturn(Uni.createFrom().item(testPatient));

        // Act
        List<PatientSearchResult> result = searchService.searchPatientsByPractitionerId(identifier)
                .subscribe().withSubscriber(UniAssertSubscriber.create())

                .awaitItem()
                .getItem();

        // Assert
        assertThat(result).hasSize(1);
        assertThat(result.get(0).id()).isEqualTo("123");
    }

    @Test
    void searchPatientsByPractitionerId_shouldReturnEmpty_whenPractitionerNotFound() {
        // Arrange
        String identifier = "9999999999";

        when(fhirClient.searchPractitionerByIdentifier(identifier))
                .thenReturn(Uni.createFrom().item(createEmptyBundle()));

        // Act
        List<PatientSearchResult> result = searchService.searchPatientsByPractitionerId(identifier)
                .subscribe().withSubscriber(UniAssertSubscriber.create())

                .awaitItem()
                .getItem();

        // Assert
        assertThat(result).isEmpty();
    }

    @Test
    void searchPatientsByPractitionerId_shouldCombineEncountersAndCareTeams() {
        // Arrange
        String practitionerId = "30681750-1667-311a-a3e3-878ae10a35bb";

        FhirBundle encounterBundle = createBundleWithEncounters(List.of(
                createTestEncounter("enc1", "Patient/123", "Practitioner/999", "2024-01-01T10:00:00", "2024-01-01T11:00:00")
        ));

        FhirBundle careTeamBundle = createBundleWithCareTeams(List.of(
                createTestCareTeam("ct1", "Patient/456")
        ));

        FhirBundle.FhirResource patient2 = createTestPatient("456", "Karin", "Lundberg", "199001011234", "1990-01-01");

        when(fhirClient.searchEncountersByPractitioner("Practitioner/" + practitionerId))
                .thenReturn(Uni.createFrom().item(encounterBundle));
        when(fhirClient.searchCareTeamsByPractitioner("Practitioner/" + practitionerId))
                .thenReturn(Uni.createFrom().item(careTeamBundle));
        when(fhirClient.getPatient("123")).thenReturn(Uni.createFrom().item(testPatient));
        when(fhirClient.getPatient("456")).thenReturn(Uni.createFrom().item(patient2));

        // Act
        List<PatientSearchResult> result = searchService.searchPatientsByPractitionerId(practitionerId)
                .subscribe().withSubscriber(UniAssertSubscriber.create())

                .awaitItem()
                .getItem();

        // Assert
        assertThat(result).hasSize(2);
    }

    @Test
    void searchPatientsByPractitionerId_shouldRecoverWithEmptyList_onError() {
        // Arrange
        String practitionerId = "error-id";

        when(fhirClient.searchEncountersByPractitioner(anyString()))
                .thenReturn(Uni.createFrom().failure(new RuntimeException("FHIR error")));

        // Act
        List<PatientSearchResult> result = searchService.searchPatientsByPractitionerId(practitionerId)
                .subscribe().withSubscriber(UniAssertSubscriber.create())
                .awaitItem()
                .getItem();

        // Assert
        assertThat(result).isEmpty();
    }

    // searchEncountersByPractitioner() TESTS

    @Test
    void searchEncountersByPractitioner_shouldReturnEncounters_whenNoDateProvided() {
        // Arrange
        String practitionerId = "30681750-1667-311a-a3e3-878ae10a35bb";

        when(fhirClient.searchEncountersByPractitionerOnly(practitionerId))
                .thenReturn(Uni.createFrom().item(testEncounterBundle));
        when(fhirClient.getPatient("123")).thenReturn(Uni.createFrom().item(testPatient));
        when(fhirClient.getPractitioner(practitionerId)).thenReturn(Uni.createFrom().item(testPractitioner));

        // Act
        List<EncounterSearchResult> result = searchService.searchEncountersByPractitioner(practitionerId, null)
                .subscribe().withSubscriber(UniAssertSubscriber.create())

                .awaitItem()
                .getItem();

        // Assert
        assertThat(result).isNotNull();
        assertThat(result).hasSize(1);
        assertThat(result.get(0).id()).isEqualTo("enc1");
        assertThat(result.get(0).patientId()).isEqualTo("123");
    }

    @Test
    void searchEncountersByPractitioner_shouldReturnEncounters_whenDateProvided() {
        // Arrange
        String practitionerId = "30681750-1667-311a-a3e3-878ae10a35bb";
        String date = "2024-01-01";

        when(fhirClient.searchEncountersByPractitionerAndDate(practitionerId, date))
                .thenReturn(Uni.createFrom().item(testEncounterBundle));
        when(fhirClient.getPatient("123")).thenReturn(Uni.createFrom().item(testPatient));
        when(fhirClient.getPractitioner(practitionerId)).thenReturn(Uni.createFrom().item(testPractitioner));

        // Act
        List<EncounterSearchResult> result = searchService.searchEncountersByPractitioner(practitionerId, date)
                .subscribe().withSubscriber(UniAssertSubscriber.create())

                .awaitItem()
                .getItem();

        // Assert
        assertThat(result).hasSize(1);
        assertThat(result.get(0).startTime()).contains("2024-01-01");
    }

    @Test
    void searchEncountersByPractitioner_shouldResolveIdentifier() {
        // Arrange
        String identifier = "9999994392";
        String resolvedId = "30681750-1667-311a-a3e3-878ae10a35bb";

        FhirBundle practitionerSearchBundle = createBundleWithPractitioners(List.of(
                createTestPractitionerWithId(resolvedId, "Dr. Test", "Doctor")
        ));

        when(fhirClient.searchPractitionerByIdentifier(identifier))
                .thenReturn(Uni.createFrom().item(practitionerSearchBundle));
        when(fhirClient.searchEncountersByPractitionerOnly(resolvedId))
                .thenReturn(Uni.createFrom().item(testEncounterBundle));
        when(fhirClient.getPatient("123")).thenReturn(Uni.createFrom().item(testPatient));
        when(fhirClient.getPractitioner(resolvedId)).thenReturn(Uni.createFrom().item(testPractitioner));

        // Act
        List<EncounterSearchResult> result = searchService.searchEncountersByPractitioner(identifier, null)
                .subscribe().withSubscriber(UniAssertSubscriber.create())

                .awaitItem()
                .getItem();

        // Assert
        assertThat(result).hasSize(1);
    }

    @Test
    void searchEncountersByPractitioner_shouldReturnEmpty_whenPractitionerNotFound() {
        // Arrange
        String identifier = "9999999999";

        when(fhirClient.searchPractitionerByIdentifier(identifier))
                .thenReturn(Uni.createFrom().item(createEmptyBundle()));

        // Act
        List<EncounterSearchResult> result = searchService.searchEncountersByPractitioner(identifier, null)
                .subscribe().withSubscriber(UniAssertSubscriber.create())

                .awaitItem()
                .getItem();

        // Assert
        assertThat(result).isEmpty();
    }

    @Test
    void searchEncountersByPractitioner_shouldHandleMultipleEncounters() {
        // Arrange
        String practitionerId = "30681750-1667-311a-a3e3-878ae10a35bb";

        FhirBundle multiEncounterBundle = createBundleWithEncounters(List.of(
                createTestEncounter("enc1", "Patient/123", "Practitioner/999", "2024-01-01T10:00:00", "2024-01-01T11:00:00"),
                createTestEncounter("enc2", "Patient/123", "Practitioner/999", "2024-01-02T10:00:00", "2024-01-02T11:00:00")
        ));

        when(fhirClient.searchEncountersByPractitionerOnly(practitionerId))
                .thenReturn(Uni.createFrom().item(multiEncounterBundle));
        when(fhirClient.getPatient("123")).thenReturn(Uni.createFrom().item(testPatient));
        when(fhirClient.getPractitioner(practitionerId)).thenReturn(Uni.createFrom().item(testPractitioner));

        // Act
        List<EncounterSearchResult> result = searchService.searchEncountersByPractitioner(practitionerId, null)
                .subscribe().withSubscriber(UniAssertSubscriber.create())

                .awaitItem()
                .getItem();

        // Assert
        assertThat(result).hasSize(2);
        assertThat(result.get(0).id()).isEqualTo("enc1");
        assertThat(result.get(1).id()).isEqualTo("enc2");
    }

    @Test
    void searchEncountersByPractitioner_shouldRecoverWithEmptyList_onError() {
        // Arrange
        String practitionerId = "error-id";

        when(fhirClient.searchEncountersByPractitionerOnly(practitionerId))
                .thenReturn(Uni.createFrom().failure(new RuntimeException("FHIR error")));

        // Act
        List<EncounterSearchResult> result = searchService.searchEncountersByPractitioner(practitionerId, null)
                .subscribe().withSubscriber(UniAssertSubscriber.create())
                .awaitItem()
                .getItem();

        // Assert
        assertThat(result).isEmpty();
    }

    @Test
    void searchEncountersByPractitioner_shouldFilterOutEncountersWithoutPatient() {
        // Arrange
        String practitionerId = "30681750-1667-311a-a3e3-878ae10a35bb";

        FhirBundle.FhirResource encounterWithoutPatient = createTestEncounter("enc2", null, "Practitioner/999", "2024-01-01T10:00:00", "2024-01-01T11:00:00");
        FhirBundle mixedBundle = createBundleWithEncounters(List.of(
                createTestEncounter("enc1", "Patient/123", "Practitioner/999", "2024-01-01T10:00:00", "2024-01-01T11:00:00"),
                encounterWithoutPatient
        ));

        when(fhirClient.searchEncountersByPractitionerOnly(practitionerId))
                .thenReturn(Uni.createFrom().item(mixedBundle));
        when(fhirClient.getPatient("123")).thenReturn(Uni.createFrom().item(testPatient));
        when(fhirClient.getPractitioner(practitionerId)).thenReturn(Uni.createFrom().item(testPractitioner));

        // Act
        List<EncounterSearchResult> result = searchService.searchEncountersByPractitioner(practitionerId, null)
                .subscribe().withSubscriber(UniAssertSubscriber.create())

                .awaitItem()
                .getItem();

        // Assert
        assertThat(result).hasSize(1);
        assertThat(result.get(0).id()).isEqualTo("enc1");
    }

    // HELPER METHODS

    private FhirBundle.FhirResource createTestPatient(String id, String firstName, String lastName, String ssn, String birthDate) {
        FhirBundle.FhirResource patient = new FhirBundle.FhirResource();
        patient.resourceType = "Patient";
        patient.id = id;

        FhirBundle.HumanName name = new FhirBundle.HumanName();
        name.given = List.of(firstName);
        name.family = lastName;
        patient.name = List.of(name);

        FhirBundle.Identifier identifier = new FhirBundle.Identifier();
        identifier.value = ssn;
        patient.identifier = List.of(identifier);

        patient.birthDate = birthDate;

        return patient;
    }

    private FhirBundle.FhirResource createTestPractitioner(String id, String firstName, String lastName) {
        FhirBundle.FhirResource practitioner = new FhirBundle.FhirResource();
        practitioner.resourceType = "Practitioner";
        practitioner.id = id;

        FhirBundle.HumanName name = new FhirBundle.HumanName();
        name.given = List.of(firstName);
        name.family = lastName;
        practitioner.name = List.of(name);

        return practitioner;
    }

    private FhirBundle.FhirResource createTestPractitionerWithId(String id, String firstName, String lastName) {
        return createTestPractitioner(id, firstName, lastName);
    }

    private FhirBundle.FhirResource createTestCondition(String id, String patientRef, String conditionText) {
        FhirBundle.FhirResource condition = new FhirBundle.FhirResource();
        condition.resourceType = "Condition";
        condition.id = id;

        FhirBundle.Reference subject = new FhirBundle.Reference();
        subject.reference = patientRef;
        condition.subject = subject;

        FhirBundle.CodeableConcept code = new FhirBundle.CodeableConcept();
        code.text = conditionText;
        condition.code = code;

        condition.recordedDate = "2024-01-01";

        return condition;
    }

    private FhirBundle.FhirResource createTestEncounter(String id, String patientRef, String practitionerRef, String start, String end) {
        FhirBundle.FhirResource encounter = new FhirBundle.FhirResource();
        encounter.resourceType = "Encounter";
        encounter.id = id;

        if (patientRef != null) {
            FhirBundle.Reference subject = new FhirBundle.Reference();
            subject.reference = patientRef;
            encounter.subject = subject;
        }

        FhirBundle.Period period = new FhirBundle.Period();
        period.start = start;
        period.end = end;
        encounter.period = period;

        FhirBundle.Participant participant = new FhirBundle.Participant();
        FhirBundle.Reference practRef = new FhirBundle.Reference();
        practRef.reference = practitionerRef;
        participant.individual = practRef;
        encounter.participant = List.of(participant);

        return encounter;
    }

    private FhirBundle.FhirResource createTestCareTeam(String id, String patientRef) {
        FhirBundle.FhirResource careTeam = new FhirBundle.FhirResource();
        careTeam.resourceType = "CareTeam";
        careTeam.id = id;

        FhirBundle.Reference subject = new FhirBundle.Reference();
        subject.reference = patientRef;
        careTeam.subject = subject;

        return careTeam;
    }

    private FhirBundle createBundleWithPatients(List<FhirBundle.FhirResource> patients) {
        FhirBundle bundle = new FhirBundle();
        bundle.resourceType = "Bundle";
        bundle.type = "searchset";
        bundle.total = patients.size();
        bundle.entry = new ArrayList<>();

        for (FhirBundle.FhirResource patient : patients) {
            FhirBundle.BundleEntry entry = new FhirBundle.BundleEntry();
            entry.resource = patient;
            bundle.entry.add(entry);
        }

        return bundle;
    }

    private FhirBundle createBundleWithConditions(List<FhirBundle.FhirResource> conditions) {
        FhirBundle bundle = new FhirBundle();
        bundle.resourceType = "Bundle";
        bundle.type = "searchset";
        bundle.total = conditions.size();
        bundle.entry = new ArrayList<>();

        for (FhirBundle.FhirResource condition : conditions) {
            FhirBundle.BundleEntry entry = new FhirBundle.BundleEntry();
            entry.resource = condition;
            bundle.entry.add(entry);
        }

        return bundle;
    }

    private FhirBundle createBundleWithEncounters(List<FhirBundle.FhirResource> encounters) {
        FhirBundle bundle = new FhirBundle();
        bundle.resourceType = "Bundle";
        bundle.type = "searchset";
        bundle.total = encounters.size();
        bundle.entry = new ArrayList<>();

        for (FhirBundle.FhirResource encounter : encounters) {
            FhirBundle.BundleEntry entry = new FhirBundle.BundleEntry();
            entry.resource = encounter;
            bundle.entry.add(entry);
        }

        return bundle;
    }

    private FhirBundle createBundleWithPractitioners(List<FhirBundle.FhirResource> practitioners) {
        FhirBundle bundle = new FhirBundle();
        bundle.resourceType = "Bundle";
        bundle.type = "searchset";
        bundle.total = practitioners.size();
        bundle.entry = new ArrayList<>();

        for (FhirBundle.FhirResource practitioner : practitioners) {
            FhirBundle.BundleEntry entry = new FhirBundle.BundleEntry();
            entry.resource = practitioner;
            bundle.entry.add(entry);
        }

        return bundle;
    }

    private FhirBundle createBundleWithCareTeams(List<FhirBundle.FhirResource> careTeams) {
        FhirBundle bundle = new FhirBundle();
        bundle.resourceType = "Bundle";
        bundle.type = "searchset";
        bundle.total = careTeams.size();
        bundle.entry = new ArrayList<>();

        for (FhirBundle.FhirResource careTeam : careTeams) {
            FhirBundle.BundleEntry entry = new FhirBundle.BundleEntry();
            entry.resource = careTeam;
            bundle.entry.add(entry);
        }

        return bundle;
    }

    private FhirBundle createEmptyBundle() {
        FhirBundle bundle = new FhirBundle();
        bundle.resourceType = "Bundle";
        bundle.type = "searchset";
        bundle.total = 0;
        bundle.entry = new ArrayList<>();
        return bundle;
    }
}