package com.journalSystem.clinical_service.controller;

import com.journalSystem.clinical_service.dto.PatientDTO;
import com.journalSystem.clinical_service.mapper.FhirMapper;
import com.journalSystem.clinical_service.service.HapiPatientService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/patients")
@CrossOrigin(origins = "http://localhost:3000")
@RequiredArgsConstructor
public class PatientController {

    private final HapiPatientService hapiPatientService;

    @GetMapping
    public List<PatientDTO> getAllPatients() {
        return hapiPatientService.getAllPatients()
                .stream()
                .map(FhirMapper::patientToDTO)
                .toList();
    }

    @GetMapping("/{id}")
    public ResponseEntity<PatientDTO> getPatientById(@PathVariable String id) {
        return hapiPatientService.getPatientById(id)
                .map(FhirMapper::patientToDTO)
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }
}