package com.journalSystem.clinical_service.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.Arrays;
import java.util.List;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class SecurityConfig {

    @Value("${spring.security.oauth2.resourceserver.jwt.jwk-set-uri}")
    private String jwkSetUri;

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .csrf(csrf -> csrf.disable())
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))
                .sessionManagement(session -> session
                        .sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        // Health check - public
                        .requestMatchers("/actuator/health").permitAll()

                        // GET patients/practitioners - authenticated users can read
                        .requestMatchers(HttpMethod.GET, "/api/patients/**").authenticated()
                        .requestMatchers(HttpMethod.GET, "/api/practitioners/**").authenticated()

                        // Create/Update patients - only DOCTOR and STAFF
                        .requestMatchers(HttpMethod.POST, "/api/patients/**").hasAnyRole("DOCTOR", "STAFF")
                        .requestMatchers(HttpMethod.PUT, "/api/patients/**").hasAnyRole("DOCTOR", "STAFF")
                        .requestMatchers(HttpMethod.DELETE, "/api/patients/**").hasRole("DOCTOR")

                        // Observations - DOCTOR can do everything, STAFF can create, PATIENT can read own
                        .requestMatchers(HttpMethod.GET, "/api/v1/clinical/observations/**").authenticated()
                        .requestMatchers(HttpMethod.POST, "/api/v1/clinical/observations/**").hasAnyRole("DOCTOR", "STAFF")
                        .requestMatchers(HttpMethod.PUT, "/api/v1/clinical/observations/**").hasRole("DOCTOR")
                        .requestMatchers(HttpMethod.DELETE, "/api/v1/clinical/observations/**").hasRole("DOCTOR")

                        // Conditions - DOCTOR can do everything, STAFF can create
                        .requestMatchers(HttpMethod.GET, "/api/v1/clinical/conditions/**").authenticated()
                        .requestMatchers(HttpMethod.POST, "/api/v1/clinical/conditions/**").hasAnyRole("DOCTOR", "STAFF")
                        .requestMatchers(HttpMethod.PUT, "/api/v1/clinical/conditions/**").hasRole("DOCTOR")
                        .requestMatchers(HttpMethod.DELETE, "/api/v1/clinical/conditions/**").hasRole("DOCTOR")

                        // Encounters - all authenticated can read, DOCTOR/STAFF can create
                        .requestMatchers(HttpMethod.GET, "/api/v1/clinical/encounters/**").authenticated()
                        .requestMatchers(HttpMethod.POST, "/api/v1/clinical/encounters/**").hasAnyRole("DOCTOR", "STAFF")
                        .requestMatchers(HttpMethod.PUT, "/api/v1/clinical/encounters/**").hasAnyRole("DOCTOR", "STAFF")
                        .requestMatchers(HttpMethod.DELETE, "/api/v1/clinical/encounters/**").hasRole("DOCTOR")

                        // All other requests require authentication
                        .anyRequest().authenticated()
                )
                .oauth2ResourceServer(oauth2 -> oauth2
                        .jwt(jwt -> jwt
                                .jwtAuthenticationConverter(jwtAuthenticationConverter())
                        )
                );

        return http.build();
    }

    @Bean
    public JwtAuthenticationConverter jwtAuthenticationConverter() {
        JwtAuthenticationConverter converter = new JwtAuthenticationConverter();
        converter.setJwtGrantedAuthoritiesConverter(new KeycloakRoleConverter());
        return converter;
    }

    @Bean
    public JwtDecoder jwtDecoder() {
        return NimbusJwtDecoder.withJwkSetUri(jwkSetUri).build();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        configuration.setAllowedOrigins(Arrays.asList(
                "http://localhost:3000",
                "http://localhost:30000",
                "https://patientsystem-frontend.app.cloud.cbh.kth.se"
        ));
        configuration.setAllowedMethods(Arrays.asList("GET", "POST", "PUT", "DELETE", "OPTIONS"));
        configuration.setAllowedHeaders(List.of("*"));
        configuration.setAllowCredentials(true);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }
}