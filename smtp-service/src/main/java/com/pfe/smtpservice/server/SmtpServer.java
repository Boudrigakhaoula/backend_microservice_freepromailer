package com.pfe.smtpservice.server;


import lombok.extern.slf4j.Slf4j;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Serveur SMTP from scratch — écoute sur un ServerSocket TCP.
 * INCHANGÉ par rapport à l'original — le code TCP fonctionne parfaitement.
 */
@Slf4j
public class SmtpServer implements Runnable {

    private final int port;
    private final String hostname;
    private final MailDeliveryHandler handler;
    private final int maxConnections;

    private ServerSocket serverSocket;
    private ExecutorService threadPool;
    private final AtomicBoolean running = new AtomicBoolean(false);

    public SmtpServer(int port, String hostname,
                      MailDeliveryHandler handler, int maxConnections) {
        this.port = port;
        this.hostname = hostname;
        this.handler = handler;
        this.maxConnections = maxConnections;
    }

    public void start() throws IOException {
        serverSocket = new ServerSocket(port);
        serverSocket.setReuseAddress(true);
        threadPool = Executors.newFixedThreadPool(maxConnections);
        running.set(true);
        Thread serverThread = new Thread(this, "smtp-server-main");
        serverThread.setDaemon(true);
        serverThread.start();
        log.info("Serveur SMTP démarré sur le port {}", port);
    }

    @Override
    public void run() {
        while (running.get()) {
            try {
                Socket clientSocket = serverSocket.accept();
                String clientIp = clientSocket.getInetAddress().getHostAddress();
                log.info("Nouvelle connexion SMTP depuis : {}", clientIp);

                SmtpSession session = new SmtpSession(clientSocket, hostname, handler);
                threadPool.submit(session);
            } catch (IOException e) {
                if (running.get()) {
                    log.error("Erreur accept() : {}", e.getMessage());
                }
            }
        }
    }

    public void stop() {
        running.set(false);
        try { if (serverSocket != null) serverSocket.close(); }
        catch (IOException e) { log.warn("Erreur fermeture serverSocket", e); }
        if (threadPool != null) threadPool.shutdownNow();
        log.info("Serveur SMTP arrêté.");
    }
}
