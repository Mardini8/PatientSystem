package com.PatientSystem.PatientSystem.mapper;

import com.PatientSystem.PatientSystem.dto.*;
import com.PatientSystem.PatientSystem.model.*;

public class ApiMapper {

    public static UserDTO toDTO(User u) {
        if (u == null) return null;
        return new UserDTO(
                u.getId(),
                u.getUsername(),
                u.getEmail(),
                u.getRole() != null ? u.getRole().name() : null,
                u.getForeignId()
        );
    }

    public static User toEntity(UserDTO dto) {
        if (dto == null) return null;
        User u = new User();
        u.setId(dto.id());
        u.setUsername(dto.username());
        u.setEmail(dto.email());
        if (dto.role() != null) {
            u.setRole(Role.valueOf(dto.role()));
        }
        u.setForeignId(dto.foreignId());
        return u;
    }

    public static MessageDTO toDTO(Message m) {
        if (m == null) return null;
        return new MessageDTO(
                m.getId(),
                m.getFromUserId(),
                m.getToUserId(),
                m.getPatientPersonnummer(),
                m.getContent(),
                m.getSentAt()
        );
    }

    public static Message toEntity(MessageDTO dto) {
        if (dto == null) return null;
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