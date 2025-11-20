package com.journalSystem.clinical_service.controller;

import com.journalSystem.clinical_service.dto.PractitionerDTO;
import com.journalSystem.clinical_service.mapper.FhirMapper;
import com.journalSystem.clinical_service.service.HapiPractitionerService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/practitioners")
@CrossOrigin(origins = "http://localhost:3000")
@RequiredArgsConstructor
public class PractitionerController {

    private final HapiPractitionerService hapiPractitionerService;

    @GetMapping
    public List<PractitionerDTO> getAllPractitioners() {
        return hapiPractitionerService.getAllPractitioners()
                .stream()
                .map(FhirMapper::practitionerToDTO)
                .toList();
    }

    @GetMapping("/{id}")
    public ResponseEntity<PractitionerDTO> getPractitionerById(@PathVariable String id) {
        return hapiPractitionerService.getPractitionerById(id)
                .map(FhirMapper::practitionerToDTO)
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    @GetMapping("/search")
    public List<PractitionerDTO> searchPractitioner(@RequestParam String name) {
        return hapiPractitionerService.searchPractitionerByName(name)
                .stream()
                .map(FhirMapper::practitionerToDTO)
                .toList();
    }
}