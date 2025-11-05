package com.PatientSystem.PatientSystem.service;

import com.PatientSystem.PatientSystem.model.Patient;
import com.PatientSystem.PatientSystem.repository.PatientRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class PatientService {

    // Dependency Injection: Spring injicerar PatientRepository automatiskt
    private final PatientRepository patientRepository;

    /**
     * Sparar en ny patient i databasen.
     * @param patient Patientobjektet att spara.
     * @return Det sparade patientobjektet, nu med ett genererat ID.
     */
    public Patient createPatient(Patient patient) {
        // Här kan vi lägga till valideringslogik, t.ex. kolla om personnumret redan finns.
        return patientRepository.save(patient);
    }

    /**
     * Hämtar en patient baserat på ID.
     * @param id Patientens unika ID.
     * @return En Optional som innehåller patienten om den hittades.
     */
    public Optional<Patient> getPatientById(Long id) {
        // Här skulle vi i framtiden kunna lägga till auktoriseringskontroller:
        // if (användaren_är_läkare || användaren_är_patienten_själv) { ... }
        return patientRepository.findById(id);
    }

    /**
     * Hämtar alla patienter. Denna funktion är avsedd för personal/läkare.
     * @return En lista över alla patienter.
     */
    public List<Patient> getAllPatients() {
        // Denna metod kräver stark auktorisering (Läkare/Personal) i en riktig applikation.
        return patientRepository.findAll();
    }

    /**
     * Hämtar en patient baserat på personnummer.
     */
    public Optional<Patient> getPatientBySsn(String socialSecurityNumber) {
        return patientRepository.findBySocialSecurityNumber(socialSecurityNumber);
    }
}