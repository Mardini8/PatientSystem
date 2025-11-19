package com.PatientSystem.PatientSystem.repository;

import com.PatientSystem.PatientSystem.model.*;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {
    boolean existsByUsername(String username);
    User findByUsername(String username);
    Optional<User> findByForeignId(String foreignId);
    Optional<User> findByForeignIdAndRoleIn(String foreignId, List<Role> roles);
}