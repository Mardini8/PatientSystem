package com.PatientSystem.PatientSystem.service;

import com.PatientSystem.PatientSystem.model.Role;
import com.PatientSystem.PatientSystem.model.User;
import com.PatientSystem.PatientSystem.repository.UserRepository;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
public class AuthService {
    private final UserRepository users;

    public AuthService(UserRepository users) {
        this.users = users;
    }

    public User register(String username, String email, String password, Role role, String foreignId) {
        if(users.existsByUsername(username)) {
            throw new IllegalArgumentException("Username taken");
        }

        if(foreignId != null && users.findByForeignId(foreignId).isPresent()) {
            throw new IllegalArgumentException("This person is already registered");
        }

        User u = new User();
        u.setUsername(username);
        u.setEmail(email);
        u.setPassword(password);
        u.setRole(role);
        u.setForeignId(foreignId);

        return users.save(u);
    }

    public User login(String username, String password) {
        User u = users.findByUsername(username);
        if(u == null || !u.getPassword().equals(password)) {
            return null;
        }
        return u;
    }

    public Optional<User> getUserById(Long id) {
        return users.findById(id);
    }

    public Optional<User> getUserByForeignId(String foreignId) {
        return users.findByForeignIdAndRoleIn(foreignId, java.util.Arrays.asList(Role.DOCTOR, Role.STAFF));
    }
}