package com.PatientSystem.PatientSystem.dto;

public record UserDTO(
        Long id,
        String username,
        String email,
        String role,
        String foreignId
) {}
