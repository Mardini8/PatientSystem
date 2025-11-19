package com.PatientSystem.PatientSystem.controller;

import com.PatientSystem.PatientSystem.dto.UserDTO;
import com.PatientSystem.PatientSystem.mapper.ApiMapper;
import com.PatientSystem.PatientSystem.model.Role;
import com.PatientSystem.PatientSystem.model.User;
import com.PatientSystem.PatientSystem.service.AuthService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/auth")
@CrossOrigin(origins = "http://localhost:3000")
@RequiredArgsConstructor
public class AuthController {
    private final AuthService auth;
    public record RegisterRequest(
            String username,
            String email,
            String password,
            Role role,
            String foreignId
    ) {}

    public record LoginRequest(String username, String password) {}

    @PostMapping("/register")
    public ResponseEntity<UserDTO> register(@RequestBody RegisterRequest req) {
        User u = auth.register(
                req.username(),
                req.email(),
                req.password(),
                req.role(),
                req.foreignId()
        );
        return ResponseEntity.ok(ApiMapper.toDTO(u));
    }

    @PostMapping("/login")
    public ResponseEntity<UserDTO> login(@RequestBody LoginRequest req) {
        User u = auth.login(req.username(), req.password());
        if (u == null) {
            return ResponseEntity.status(401).build();
        }
        return ResponseEntity.ok(ApiMapper.toDTO(u));
    }

    @GetMapping("/user/{id}")
    public ResponseEntity<UserDTO> getUserById(@PathVariable Long id) {
        return auth.getUserById(id)
                .map(ApiMapper::toDTO)
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    @GetMapping("/user-by-foreign/{foreignId}")
    public ResponseEntity<UserDTO> getUserByForeignId(@PathVariable String foreignId) {
        return auth.getUserByForeignId(foreignId)
                .map(ApiMapper::toDTO)
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }
}