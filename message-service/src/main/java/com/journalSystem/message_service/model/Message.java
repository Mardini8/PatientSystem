package com.journalSystem.message_service.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Message {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(length = 4000)
    private String content;

    private LocalDateTime sentAt = LocalDateTime.now();

    private Long fromUserId;
    private Long toUserId;

    @Column(length = 255)
    private String patientPersonnummer;
}