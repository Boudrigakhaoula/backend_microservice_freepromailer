package com.pfe.smtpservice.config;

import com.pfe.smtpservice.delivery.PersistentDeliveryHandler;
import com.pfe.smtpservice.server.SmtpServer;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.IOException;

/**
 * CORRIGÉ : utilise PersistentDeliveryHandler au lieu de InMemoryDeliveryHandler.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class SmtpServerBean {

    private final PersistentDeliveryHandler deliveryHandler;

    @Value("${smtp.server.port:2525}")
    private int port;

    @Value("${smtp.server.hostname:localhost}")
    private String hostname;

    @Value("${smtp.server.max-connections:50}")
    private int maxConnections;

    private SmtpServer smtpServer;

    @PostConstruct
    public void startServer() {
        smtpServer = new SmtpServer(port, hostname, deliveryHandler, maxConnections);
        try {
            smtpServer.start();
            log.info("✅ Serveur SMTP démarré — {}:{}", hostname, port);
        } catch (IOException e) {
            log.error("❌ Impossible de démarrer le serveur SMTP : {}", e.getMessage());
            throw new RuntimeException("SMTP Server startup failed", e);
        }
    }

    @PreDestroy
    public void stopServer() {
        if (smtpServer != null) smtpServer.stop();
        log.info("Serveur SMTP arrêté proprement.");
    }
}