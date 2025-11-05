package com.PatientSystem.PatientSystem.controller;

import com.PatientSystem.PatientSystem.model.Patient;
import com.PatientSystem.PatientSystem.service.PatientService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/patients")
@RequiredArgsConstructor
@CrossOrigin(origins = "http://localhost:3000") // CORS-konfiguration för frontend
public class PatientController {

    private final PatientService patientService;

    /**
     * POST /api/patients
     * Skapar en ny patient (Läkar- eller Personal-funktion)
     */
    @PostMapping
    public ResponseEntity<Patient> createPatient(@RequestBody Patient patient) {
        Patient createdPatient = patientService.createPatient(patient);
        // Returnerar den skapade patienten med HTTP-status 201 (Created)
        return new ResponseEntity<>(createdPatient, HttpStatus.CREATED);
    }

    /**
     * GET /api/patients
     * Hämtar en lista över alla patienter (Läkar- eller Personal-funktion)
     */
    @GetMapping
    public ResponseEntity<List<Patient>> getAllPatients() {
        List<Patient> patients = patientService.getAllPatients();
        return ResponseEntity.ok(patients); // HTTP-status 200 (OK)
    }

    /**
     * GET /api/patients/{id}
     * Hämtar en specifik patient efter ID (Läkare/Personal/Patient-funktion)
     */
    @GetMapping("/{id}")
    public ResponseEntity<Patient> getPatientById(@PathVariable Long id) {
        // Försöker hitta patienten. Om den inte hittas, returneras 404 (Not Found)
        return patientService.getPatientById(id)
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }
}