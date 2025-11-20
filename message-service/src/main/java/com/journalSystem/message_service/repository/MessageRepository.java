package com.journalSystem.message_service.repository;

import com.journalSystem.message_service.model.Message;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface MessageRepository extends JpaRepository<Message, Long> {
    List<Message> findByPatientPersonnummerOrderBySentAtDesc(String patientPersonnummer);
    List<Message> findByFromUserIdOrderBySentAtDesc(Long fromUserId);
    List<Message> findByToUserIdOrderBySentAtDesc(Long toUserId);
}