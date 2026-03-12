package com.pfe.campaignservice;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.core.env.Environment;

@SpringBootApplication
@EnableDiscoveryClient
@EnableFeignClients
@EnableScheduling
public class CampaignServiceApplication {

    public static void main(String[] args) {
        Environment env = SpringApplication.run(CampaignServiceApplication.class, args)
                .getEnvironment();

        String port = env.getProperty("server.port", "8081");
        String name = env.getProperty("spring.application.name", "campaign-service");

        System.out.println();
        System.out.println("╔══════════════════════════════════════════════════════════╗");
        System.out.println("║     FreeProMailer — Campaign Service démarré !          ║");
        System.out.println("╠══════════════════════════════════════════════════════════╣");
        System.out.println("║  Application : " + name);
        System.out.println("║  Port        : " + port);
        System.out.println("║  Swagger UI  : http://localhost:" + port + "/swagger-ui.html");
        System.out.println("║  API Docs    : http://localhost:" + port + "/v3/api-docs");
        System.out.println("║  Eureka      : http://localhost:8761");
        System.out.println("╚══════════════════════════════════════════════════════════╝");
        System.out.println();
    }
}
