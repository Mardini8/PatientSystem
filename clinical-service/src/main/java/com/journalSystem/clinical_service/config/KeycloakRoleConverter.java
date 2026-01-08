package com.journalSystem.clinical_service.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.convert.converter.Converter;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Converts Keycloak roles from JWT token to Spring Security GrantedAuthorities.
 */
public class KeycloakRoleConverter implements Converter<Jwt, Collection<GrantedAuthority>> {

    private static final Logger logger = LoggerFactory.getLogger(KeycloakRoleConverter.class);

    @Override
    public Collection<GrantedAuthority> convert(Jwt jwt) {
        Collection<GrantedAuthority> authorities = new ArrayList<>();

        logger.debug("Converting JWT for user: {}", jwt.getSubject());
        logger.debug("JWT claims: {}", jwt.getClaims().keySet());

        // Extract realm roles
        Map<String, Object> realmAccess = jwt.getClaimAsMap("realm_access");
        if (realmAccess != null && realmAccess.containsKey("roles")) {
            @SuppressWarnings("unchecked")
            List<String> roles = (List<String>) realmAccess.get("roles");

            logger.info("Found realm roles: {}", roles);

            authorities.addAll(roles.stream()
                    .map(role -> {
                        String authority = "ROLE_" + role.toUpperCase();
                        logger.debug("Mapped role '{}' to authority '{}'", role, authority);
                        return new SimpleGrantedAuthority(authority);
                    })
                    .collect(Collectors.toList()));
        } else {
            logger.warn("No realm_access.roles found in JWT token");
        }

        logger.info("Final authorities for user {}: {}", jwt.getSubject(), authorities);
        return authorities;
    }
}