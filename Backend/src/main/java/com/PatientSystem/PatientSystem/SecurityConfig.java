package com.PatientSystem.PatientSystem;

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
        // Inaktivera CSRF-skydd (nödvändigt för API-anrop från en separat frontend)
        http.csrf(csrf -> csrf.disable())
                // Konfigurera auktorisation för inkommande HTTP-förfrågningar
                .authorizeHttpRequests(auth -> auth
                        // Tillåt ALLA anrop till vår test-slutpunkt /api/hello
                        .requestMatchers("/api/hello").permitAll()
                        // Alla andra förfrågningar kräver autentisering (standardinställningen för nu)
                        .anyRequest().authenticated()
                );

        // Eftersom vi inte skickar någon autentiseringsinformation i detta enkla test,
        // och vi bara vill se "Hello World", behöver vi inte konfigurera formulärlogin eller HTTP Basic.

        return http.build();
    }
}