package com.PatientSystem.PatientSystem.service;

import com.PatientSystem.PatientSystem.model.Message;
import com.PatientSystem.PatientSystem.repository.MessageRepository;
import com.PatientSystem.PatientSystem.repository.UserRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class MessageService {
    private final MessageRepository messageRepository;
    private final UserRepository userRepository;
    private final HapiPatientService hapiPatientService;

    public Message send(Message m) {
        if (m.getFromUserId() == null || !userRepository.existsById(m.getFromUserId())) {
            throw new EntityNotFoundException("Sender not found: " + m.getFromUserId());
        }

        if (m.getToUserId() == null || !userRepository.existsById(m.getToUserId())) {
            throw new EntityNotFoundException("Recipient not found: " + m.getToUserId());
        }

        if (m.getPatientPersonnummer() == null || m.getPatientPersonnummer().isEmpty()) {
            throw new EntityNotFoundException("Patient personnummer is required");
        }

        try {
            boolean patientExists = hapiPatientService.getPatientByPersonnummer(m.getPatientPersonnummer()).isPresent();
            if (!patientExists) {
                System.err.println("VARNING: Patient med personnummer " + m.getPatientPersonnummer() + " finns inte i HAPI FHIR, men meddelandet sparas ändå");
            }
        } catch (Exception e) {
            System.err.println("VARNING: Kunde inte validera patient i HAPI FHIR: " + e.getMessage());
            System.err.println("Meddelandet sparas ändå med personnummer: " + m.getPatientPersonnummer());
        }

        return messageRepository.save(m);
    }

    public List<Message> forPatient(String patientPersonnummer) {
        try {
            boolean patientExists = hapiPatientService.getPatientByPersonnummer(patientPersonnummer).isPresent();
            if (!patientExists) {
                System.err.println("VARNING: Patient med personnummer " + patientPersonnummer + " finns inte i HAPI FHIR");
            }
        } catch (Exception e) {
            System.err.println("VARNING: Kunde inte validera patient i HAPI FHIR: " + e.getMessage());
        }

        return messageRepository.findByPatientPersonnummerOrderBySentAtDesc(patientPersonnummer);
    }

    public List<Message> fromUser(Long userId) {
        return messageRepository.findByFromUserIdOrderBySentAtDesc(userId);
    }

    public List<Message> toUser(Long userId) {
        return messageRepository.findByToUserIdOrderBySentAtDesc(userId);
    }
}