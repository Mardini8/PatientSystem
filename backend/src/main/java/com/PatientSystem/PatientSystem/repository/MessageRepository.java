package com.PatientSystem.PatientSystem.repository;

import com.PatientSystem.PatientSystem.model.*;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface MessageRepository extends JpaRepository<Message, Long> {
    List<Message> findByPatientPersonnummerOrderBySentAtDesc(String patientPersonnummer);

    List<Message> findByFromUserIdOrderBySentAtDesc(Long fromUserId);

    List<Message> findByToUserIdOrderBySentAtDesc(Long toUserId);
}