package com.pfe.apigateway;


import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.core.env.Environment;

@SpringBootApplication
@EnableDiscoveryClient
public class ApiGatewayApplication {

    public static void main(String[] args) {
        Environment env = SpringApplication.run(ApiGatewayApplication.class, args)
                .getEnvironment();

        String port = env.getProperty("server.port", "8080");

        System.out.println();
        System.out.println("╔══════════════════════════════════════════════════════════╗");
        System.out.println("║       FreeProMailer — API Gateway démarré !             ║");
        System.out.println("╠══════════════════════════════════════════════════════════╣");
        System.out.println("║  Gateway URL     : http://localhost:" + port);
        System.out.println("║  Eureka          : http://localhost:8761");
        System.out.println("║  Routes Actuator : http://localhost:" + port + "/actuator/gateway/routes");
        System.out.println("║                                                          ║");
        System.out.println("║  Routes configurées :                                    ║");
        System.out.println("║    /api/campaigns/**    → campaign-service (8081)        ║");
        System.out.println("║    /api/contacts/**     → campaign-service (8081)        ║");
        System.out.println("║    /api/templates/**    → campaign-service (8081)        ║");
        System.out.println("║    /api/send/**         → smtp-service     (8082)        ║");
        System.out.println("║    /api/mailbox/**      → smtp-service     (8082)        ║");
        System.out.println("║    /api/queue/**        → smtp-service     (8082)        ║");
        System.out.println("║    /api/tracking/**     → tracking-service (8083)        ║");
        System.out.println("║    /track/**            → tracking-service (8083)        ║");
        System.out.println("╚══════════════════════════════════════════════════════════╝");
        System.out.println();
    }
}
