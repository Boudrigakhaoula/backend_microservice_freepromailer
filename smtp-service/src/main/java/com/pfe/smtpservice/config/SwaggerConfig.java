package com.pfe.smtpservice.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class SwaggerConfig {

    @Bean
    public OpenAPI smtpServiceOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("FreeProMailer — SMTP Service API")
                        .description("""
                            Serveur SMTP From Scratch avec queue PostgreSQL.
                            
                            Fonctionnalités :
                            - Envoi d'emails via résolution MX directe
                            - Queue d'envoi avec retry et backoff exponentiel
                            - Signature DKIM automatique
                            - Suppression list (inspiré de Postal)
                            - Injection automatique du pixel de tracking
                            """)
                        .version("1.0.0")
                        .contact(new Contact()
                                .name("Khaoula Boudriga")
                                .email("khaoula@khaoulaboudriga.me")
                        )
                );
    }
}