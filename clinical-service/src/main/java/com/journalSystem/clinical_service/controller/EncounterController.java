package com.journalSystem.clinical_service.controller;

import com.journalSystem.clinical_service.dto.EncounterDTO;
import com.journalSystem.clinical_service.mapper.FhirMapper;
import com.journalSystem.clinical_service.service.HapiEncounterService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.TimeZone;

@RestController
@RequestMapping("/api/v1/clinical/encounters")
@CrossOrigin(origins = "http://localhost:3000")
@RequiredArgsConstructor
public class EncounterController {

    private final HapiEncounterService hapiEncounterService;

    @GetMapping("/patient/{patientId}")
    public List<EncounterDTO> getEncountersForPatient(@PathVariable String patientId) {
        return hapiEncounterService.getEncountersForPatient(patientId)
                .stream()
                .map(FhirMapper::encounterToDTO)
                .toList();
    }

    @GetMapping("/{id}")
    public ResponseEntity<EncounterDTO> getEncounterById(@PathVariable String id) {
        return hapiEncounterService.getEncounterById(id)
                .map(FhirMapper::encounterToDTO)
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    @PostMapping
    public ResponseEntity<String> createEncounter(@RequestBody CreateEncounterRequest request) {
        try {
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm");
            sdf.setTimeZone(TimeZone.getTimeZone("Europe/Stockholm"));

            Date startTime = sdf.parse(request.startTime());
            Date endTime = null;
            if (request.endTime() != null && !request.endTime().isEmpty()) {
                endTime = sdf.parse(request.endTime());
            }

            org.hl7.fhir.r4.model.Encounter encounter = hapiEncounterService.createEncounter(
                    request.patientPersonnummer(),
                    request.practitionerPersonnummer(),
                    startTime,
                    endTime
            );

            return ResponseEntity.ok("Encounter created: " + encounter.getIdElement().getIdPart());
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.badRequest().body("Could not create encounter: " + e.getMessage());
        }
    }

    public record CreateEncounterRequest(
            String patientPersonnummer,
            String practitionerPersonnummer,
            String startTime,
            String endTime
    ) {}
}