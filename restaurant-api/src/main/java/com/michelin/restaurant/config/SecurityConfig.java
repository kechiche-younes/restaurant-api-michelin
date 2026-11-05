package com.michelin.restaurant.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.web.SecurityFilterChain;


import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.access.AccessDeniedHandler;

@Configuration
@EnableMethodSecurity
public class SecurityConfig {


    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http)
            throws Exception {

        AuthenticationEntryPoint erreurAuthentification =
                (request, response, exception) -> {
                    response.setStatus(401);
                    response.setHeader("WWW-Authenticate", "Bearer");
                    response.setContentType("application/json");
                    response.setCharacterEncoding("UTF-8");

                    response.getWriter().write(
                            """
                            {
                                "code": 401,
                                "message": "Authentification requise ou jeton invalide"
                            }
                            """
                    );
                };

        AccessDeniedHandler erreurAutorisation =
                (request, response, exception) -> {
                    response.setStatus(403);
                    response.setContentType("application/json");
                    response.setCharacterEncoding("UTF-8");

                    response.getWriter().write(
                            """
                            {
                                "code": 403,
                                "message": "Vous ne disposez pas des droits nécessaires"
                            }
                            """
                    );
                };

        http.exceptionHandling(erreurs -> erreurs
                .authenticationEntryPoint(erreurAuthentification)
                .accessDeniedHandler(erreurAutorisation)
        );

        http.oauth2ResourceServer(oauth2 -> oauth2
                .jwt(jwt -> jwt.jwtAuthenticationConverter(
                        jwtAuthenticationConverter()
                ))
                .authenticationEntryPoint(erreurAuthentification)
                .accessDeniedHandler(erreurAutorisation)
        );

        return http.build();
    }
    @Bean
    public JwtAuthenticationConverter jwtAuthenticationConverter() {
        JwtAuthenticationConverter converter =
                new JwtAuthenticationConverter();

        converter.setJwtGrantedAuthoritiesConverter(jwt -> {
            Collection<GrantedAuthority> droits = new ArrayList<>();

            Map<String, Object> resourceAccess =
                    jwt.getClaim("resource_access");

            if (resourceAccess == null) {
                return droits;
            }

            Object client = resourceAccess.get("coursm2");

            if (!(client instanceof Map)) {
                return droits;
            }

            Map<?, ?> clientAccess = (Map<?, ?>) client;
            Object roles = clientAccess.get("roles");

            if (!(roles instanceof List)) {
                return droits;
            }

            for (Object role : (List<?>) roles) {
                if ("USER".equals(role) || "ADMIN".equals(role)) {
                    droits.add(
                            new SimpleGrantedAuthority("ROLE_" + role)
                    );
                }
            }

            return droits;
        });

        return converter;
    }
}