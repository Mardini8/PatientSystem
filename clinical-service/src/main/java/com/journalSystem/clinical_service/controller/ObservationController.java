package com.journalSystem.clinical_service.controller;

import com.journalSystem.clinical_service.dto.ObservationDTO;
import com.journalSystem.clinical_service.mapper.FhirMapper;
import com.journalSystem.clinical_service.service.HapiObservationService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

@RestController
@RequestMapping("/api/v1/clinical/observations")
@CrossOrigin(origins = "http://localhost:3000")
@RequiredArgsConstructor
public class ObservationController {

    private final HapiObservationService hapiObservationService;

    @GetMapping("/patient/{patientId}")
    public List<ObservationDTO> getObservationsForPatient(@PathVariable String patientId) {
        return hapiObservationService.getObservationsForPatient(patientId)
                .stream()
                .map(FhirMapper::observationToDTO)
                .toList();
    }

    @GetMapping("/{id}")
    public ResponseEntity<ObservationDTO> getObservationById(@PathVariable String id) {
        return hapiObservationService.getObservationById(id)
                .map(FhirMapper::observationToDTO)
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    @PostMapping
    public ResponseEntity<String> createObservation(@RequestBody CreateObservationRequest request) {
        try {
            Date effectiveDate;
            try {
                SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
                effectiveDate = sdf.parse(request.effectiveDate());
            } catch (Exception e) {
                return ResponseEntity.badRequest().body("Invalid date format. Use: yyyy-MM-dd");
            }

            org.hl7.fhir.r4.model.Observation observation = hapiObservationService.createObservation(
                    request.patientPersonnummer(),
                    request.performerPersonnummer(),
                    request.description(),
                    request.value(),
                    request.unit(),
                    effectiveDate
            );

            return ResponseEntity.ok("Observation created: " + observation.getIdElement().getIdPart());
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.badRequest().body("Could not create observation: " + e.getMessage());
        }
    }

    public record CreateObservationRequest(
            String patientPersonnummer,
            String performerPersonnummer,
            String description,
            String value,
            String unit,
            String effectiveDate
    ) {}
}