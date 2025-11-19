package com.PatientSystem.PatientSystem.controller;

import com.PatientSystem.PatientSystem.dto.ConditionDTO;
import com.PatientSystem.PatientSystem.mapper.FhirMapper;
import com.PatientSystem.PatientSystem.service.HapiConditionService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

@RestController
@RequestMapping("/api/v1/clinical/conditions")
@CrossOrigin(origins = "http://localhost:3000")
@RequiredArgsConstructor
public class ConditionController {

    private final HapiConditionService hapiConditionService;

    @GetMapping("/patient/{patientId}")
    public List<ConditionDTO> getConditionsForPatient(@PathVariable String patientId) {
        return hapiConditionService.getConditionsForPatient(patientId)
                .stream()
                .map(FhirMapper::conditionToDTO)
                .toList();
    }

    @GetMapping("/{id}")
    public ResponseEntity<ConditionDTO> getConditionById(@PathVariable String id) {
        return hapiConditionService.getConditionById(id)
                .map(FhirMapper::conditionToDTO)
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    @PostMapping
    public ResponseEntity<ConditionDTO> createCondition(@RequestBody CreateConditionRequest request) {
        try {
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
            Date recordedDate = sdf.parse(request.assertedDate());

            org.hl7.fhir.r4.model.Condition condition = hapiConditionService.createCondition(
                    request.patientPersonnummer(),
                    request.practitionerPersonnummer(),
                    request.description(),
                    recordedDate
            );

            ConditionDTO dto = FhirMapper.conditionToDTO(condition);

            return ResponseEntity.ok(dto);
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.badRequest().build();
        }
    }

    public record CreateConditionRequest(
            String patientPersonnummer,
            String practitionerPersonnummer,
            String description,
            String assertedDate
    ) {}
}