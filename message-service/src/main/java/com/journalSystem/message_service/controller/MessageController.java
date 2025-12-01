package com.journalSystem.message_service.controller;

import com.journalSystem.message_service.dto.MessageDTO;
import com.journalSystem.message_service.model.Message;
import com.journalSystem.message_service.service.MessageService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.util.List;

@RestController
@RequestMapping("/api/v1/messages")
@CrossOrigin(origins = {"http://localhost:30000", "http://localhost:3000"})
@RequiredArgsConstructor
public class MessageController {
    private final MessageService service;

    @GetMapping("/patient/{patientPersonnummer}")
    public List<MessageDTO> forPatient(@PathVariable String patientPersonnummer) {
        return service.forPatient(patientPersonnummer)
                .stream()
                .map(this::toDTO)
                .toList();
    }

    @GetMapping("/from-user/{userId}")
    public List<MessageDTO> fromUser(@PathVariable Long userId) {
        return service.fromUser(userId)
                .stream()
                .map(this::toDTO)
                .toList();
    }

    @GetMapping("/to-user/{userId}")
    public List<MessageDTO> toUser(@PathVariable Long userId) {
        return service.toUser(userId)
                .stream()
                .map(this::toDTO)
                .toList();
    }

    @PostMapping
    public ResponseEntity<MessageDTO> send(@RequestBody MessageDTO dto) {
        Message message = toEntity(dto);
        Message saved = service.send(message);
        return ResponseEntity
                .created(URI.create("/api/v1/messages/" + saved.getId()))
                .body(toDTO(saved));
    }

    private MessageDTO toDTO(Message m) {
        return new MessageDTO(m.getId(), m.getFromUserId(), m.getToUserId(),
                m.getPatientPersonnummer(), m.getContent(), m.getSentAt());
    }

    private Message toEntity(MessageDTO dto) {
        Message m = new Message();
        m.setId(dto.id());
        m.setFromUserId(dto.fromUserId());
        m.setToUserId(dto.toUserId());
        m.setPatientPersonnummer(dto.patientPersonnummer());
        m.setContent(dto.content());
        m.setSentAt(dto.sentAt());
        return m;
    }
}