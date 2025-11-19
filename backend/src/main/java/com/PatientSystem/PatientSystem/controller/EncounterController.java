package com.PatientSystem.PatientSystem.controller;

import com.PatientSystem.PatientSystem.dto.EncounterDTO;
import com.PatientSystem.PatientSystem.mapper.FhirMapper;
import com.PatientSystem.PatientSystem.service.HapiEncounterService;
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

            Date startTime;
            try {
                startTime = sdf.parse(request.startTime());
                System.out.println("Parsed startTime: " + startTime);
            } catch (Exception e) {
                System.err.println("Kunde inte parsa startTime: " + request.startTime());
                return ResponseEntity.badRequest().body("Ogiltigt startTime-format. Använd: yyyy-MM-dd'T'HH:mm");
            }

            Date endTime = null;
            if (request.endTime() != null && !request.endTime().isEmpty()) {
                try {
                    endTime = sdf.parse(request.endTime());
                    System.out.println("Parsed endTime: " + endTime);
                } catch (Exception e) {
                    System.err.println("Kunde inte parsa endTime: " + request.endTime());
                    return ResponseEntity.badRequest().body("Ogiltigt endTime-format. Använd: yyyy-MM-dd'T'HH:mm");
                }
            }

            if (request.patientPersonnummer() == null || request.patientPersonnummer().isEmpty()) {
                return ResponseEntity.badRequest().body("Patient personnummer saknas");
            }

            org.hl7.fhir.r4.model.Encounter encounter = hapiEncounterService.createEncounter(
                    request.patientPersonnummer(),
                    request.practitionerPersonnummer(),
                    startTime,
                    endTime
            );

            System.out.println("✓ Encounter skapat med ID: " + encounter.getIdElement().getIdPart());

            EncounterDTO dto = FhirMapper.encounterToDTO(encounter);

            return ResponseEntity.ok("Encounter skapat: " + encounter.getIdElement().getIdPart());
        } catch (Exception e) {
            System.err.println("=== FEL VID SKAPANDE AV ENCOUNTER ===");
            System.err.println("Felmeddelande: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.badRequest()
                    .body("Kunde inte skapa encounter: " + e.getMessage());
        }
    }

    public record CreateEncounterRequest(
            String patientPersonnummer,
            String practitionerPersonnummer,
            String startTime,
            String endTime
    ) {}
}