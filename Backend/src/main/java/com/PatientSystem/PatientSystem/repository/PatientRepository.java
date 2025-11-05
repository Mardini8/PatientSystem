package com.PatientSystem.PatientSystem.repository;

import com.PatientSystem.PatientSystem.model.Patient;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface PatientRepository extends JpaRepository<Patient, Long> {

    // Spring Data JPA skapar automatiskt implementeringen för denna metod
    // för att hitta en Patient baserat på personnummer.
    Optional<Patient> findBySocialSecurityNumber(String socialSecurityNumber);
}