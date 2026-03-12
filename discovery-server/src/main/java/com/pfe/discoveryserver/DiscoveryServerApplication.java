package com.pfe.discoveryserver;


import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.netflix.eureka.server.EnableEurekaServer;
import org.springframework.core.env.Environment;

@SpringBootApplication
@EnableEurekaServer
public class DiscoveryServerApplication {

    public static void main(String[] args) {
        Environment env = SpringApplication.run(DiscoveryServerApplication.class, args)
                .getEnvironment();

        String port = env.getProperty("server.port", "8761");

        System.out.println();
        System.out.println("╔══════════════════════════════════════════════════════════╗");
        System.out.println("║     FreeProMailer — Discovery Server démarré !          ║");
        System.out.println("╠══════════════════════════════════════════════════════════╣");
        System.out.println("║  Eureka Dashboard : http://localhost:" + port);
        System.out.println("║  Port             : " + port);
        System.out.println("╚══════════════════════════════════════════════════════════╝");
        System.out.println();
    }
}
