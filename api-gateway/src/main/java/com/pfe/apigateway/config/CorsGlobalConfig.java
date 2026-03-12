package com.pfe.apigateway.config;


import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.reactive.CorsWebFilter;
import org.springframework.web.cors.reactive.UrlBasedCorsConfigurationSource;

import java.util.List;

/**
 * Configuration CORS globale pour l'API Gateway.
 *
 * IMPORTANT : Spring Cloud Gateway utilise WebFlux (réactif),
 * donc on utilise CorsWebFilter (PAS CorsFilter de servlet).
 *
 * Permet au frontend Angular (localhost:4200) d'appeler l'API.
 */
@Configuration
public class CorsGlobalConfig {

    @Bean
    public CorsWebFilter corsWebFilter() {
        CorsConfiguration config = new CorsConfiguration();

        // Origines autorisées
        config.setAllowedOrigins(List.of(
                "http://localhost:4200",   // Angular dev
                "http://localhost:4201"    // Angular alt
        ));

        // Méthodes HTTP autorisées
        config.setAllowedMethods(List.of(
                "GET", "POST", "PUT", "DELETE", "PATCH", "OPTIONS"
        ));

        // Headers autorisés
        config.setAllowedHeaders(List.of("*"));

        // Autoriser les cookies / Authorization headers
        config.setAllowCredentials(true);

        // Durée du cache preflight (1 heure)
        config.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);

        return new CorsWebFilter(source);
    }
}
