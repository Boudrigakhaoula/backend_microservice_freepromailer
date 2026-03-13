package com.pfe.campaignservice.config;

import io.swagger.v3.oas.models.*;
import io.swagger.v3.oas.models.info.*;
import io.swagger.v3.oas.models.security.*;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class SwaggerConfig {

    @Bean
    public OpenAPI campaignServiceOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("FreeProMailer — Campaign Service API")
                        .description("""
                            Gestion des campagnes d'email marketing.
                            
                            Fonctionnalités :
                            - CRUD Campagnes (brouillon, planifiée, envoyée)
                            - CRUD Listes de contacts
                            - CRUD Contacts (ajout, import CSV)
                            - CRUD Templates email (HTML + texte)
                            - Envoi immédiat ou planifié
                            - Personnalisation (merge tags)
                            """)
                        .version("1.0.0")
                        .contact(new Contact()
                                .name("Khaoula Boudriga")
                                .email("khaoula@khaoulaboudriga.me")
                        )
                )
                // ✅ AJOUTER CES LIGNES
                .addSecurityItem(new SecurityRequirement().addList("Bearer Auth"))
                .components(new Components()
                        .addSecuritySchemes("Bearer Auth",
                                new SecurityScheme()
                                        .name("Bearer Auth")
                                        .type(SecurityScheme.Type.HTTP)
                                        .scheme("bearer")
                                        .bearerFormat("JWT")));
    }
}