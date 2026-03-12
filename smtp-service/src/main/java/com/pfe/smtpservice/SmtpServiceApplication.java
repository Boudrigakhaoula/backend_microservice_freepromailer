package com.pfe.smtpservice;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.core.env.Environment;

@SpringBootApplication
@EnableDiscoveryClient
@EnableScheduling
public class SmtpServiceApplication {

    public static void main(String[] args) {
        Environment env = SpringApplication.run(SmtpServiceApplication.class, args)
                .getEnvironment();

        String port = env.getProperty("server.port", "8082");
        String name = env.getProperty("spring.application.name", "smtp-service");

        System.out.println();
        System.out.println("╔══════════════════════════════════════════════════════════╗");
        System.out.println("║       FreeProMailer — SMTP Service démarré !            ║");
        System.out.println("╠══════════════════════════════════════════════════════════╣");
        System.out.println("║  Application : " + name);
        System.out.println("║  Port        : " + port);
        System.out.println("║  Swagger UI  : http://localhost:" + port + "/swagger-ui.html");
        System.out.println("║  API Docs    : http://localhost:" + port + "/v3/api-docs");
        System.out.println("║  Eureka      : http://localhost:8761");
        System.out.println("║  SMTP TCP    : port 2525");
        System.out.println("╚══════════════════════════════════════════════════════════╝");
        System.out.println();
    }
}
