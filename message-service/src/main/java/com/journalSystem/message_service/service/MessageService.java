package com.journalSystem.message_service.service;

import com.journalSystem.message_service.model.Message;
import com.journalSystem.message_service.repository.MessageRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class MessageService {
    private final MessageRepository messageRepository;

    public Message send(Message message) {
        return messageRepository.save(message);
    }

    public List<Message> forPatient(String patientPersonnummer) {
        return messageRepository.findByPatientPersonnummerOrderBySentAtDesc(patientPersonnummer);
    }

    public List<Message> fromUser(Long userId) {
        return messageRepository.findByFromUserIdOrderBySentAtDesc(userId);
    }

    public List<Message> toUser(Long userId) {
        return messageRepository.findByToUserIdOrderBySentAtDesc(userId);
    }
}