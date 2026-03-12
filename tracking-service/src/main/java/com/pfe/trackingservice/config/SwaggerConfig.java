package com.pfe.trackingservice.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class SwaggerConfig {

    @Bean
    public OpenAPI trackingServiceOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("FreeProMailer — Tracking Service API")
                        .description("""
                            Suivi des interactions email.
                            
                            Fonctionnalités :
                            - Pixel de tracking (ouvertures)
                            - Redirection de liens (clics)
                            - Enregistrement des bounces
                            - Désabonnement
                            - Statistiques et KPI par campagne
                            - Timeline et top liens cliqués
                            """)
                        .version("1.0.0")
                        .contact(new Contact()
                                .name("Khaoula Boudriga")
                                .email("khaoula@khaoulaboudriga.me")
                        )
                );
    }
}
