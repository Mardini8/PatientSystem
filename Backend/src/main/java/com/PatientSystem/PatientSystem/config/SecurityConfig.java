package com.PatientSystem.PatientSystem.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http.csrf(csrf -> csrf.disable())
                .authorizeHttpRequests(auth -> auth
                        // Tillåt ALLA anrop till både /api/hello och alla /api/patients-anrop
                        .requestMatchers("/api/hello", "/api/patients/**").permitAll()
                        // Alla andra förfrågningar kräver autentisering
                        .anyRequest().authenticated()
                );

        return http.build();
    }
}