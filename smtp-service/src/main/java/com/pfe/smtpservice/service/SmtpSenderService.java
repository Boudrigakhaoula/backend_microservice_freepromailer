package com.pfe.smtpservice.service;



import com.pfe.smtpservice.dkim.DkimSigner;
import com.pfe.smtpservice.entity.QueuedMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.*;
import java.net.Socket;
import java.nio.charset.StandardCharsets;

/**
 * Envoi SMTP direct vers les serveurs MX.
 *
 * CHANGEMENT vs l'original (InMemoryDeliveryHandler.deliverDirectMx) :
 *   - Extrait dans un service dédié
 *   - Prend un QueuedMessage (entity JPA) au lieu de EmailMessage (POJO)
 *   - Support HTML + text/plain
 *   - Ajout tracking pixel dans le HTML
 *   - Retourne un SendResult au lieu de throw Exception
 *   - Timeout configurable via application.properties
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SmtpSenderService {

    private final DkimSigner dkimSigner;
    private final MxResolverService mxResolver;

    @Value("${outgoing.helo-hostname:khaoulaboudriga.me}")
    private String heloHostname;

    @Value("${outgoing.connection-timeout-ms:15000}")
    private int connectionTimeout;

    @Value("${outgoing.read-timeout-ms:30000}")
    private int readTimeout;

    /**
     * Envoie un message via connexion SMTP directe au serveur MX.
     * La logique SMTP est reprise de votre InMemoryDeliveryHandler.deliverDirectMx()
     */
    public SendResult send(QueuedMessage msg) {
        long startTime = System.currentTimeMillis();
        String mxHost = null;

        try {
            // ─── 1. Résoudre le MX (comme votre code original) ───
            String domain = msg.getRcptTo().substring(msg.getRcptTo().indexOf('@') + 1);
            mxHost = mxResolver.resolve(domain);
            log.info("MX trouvé : {} → {}", domain, mxHost);

            // ─── 2. Générer la signature DKIM (identique à l'original) ───
            String bodyForDkim = msg.getTextBody() != null ? msg.getTextBody() : "";
            String dkimHeader = dkimSigner.sign(
                    msg.getMailFrom(), msg.getRcptTo(),
                    msg.getSubject(), bodyForDkim);

            if (!dkimHeader.isEmpty()) {
                log.info("🔏 DKIM signé pour {}", msg.getRcptTo());
            } else {
                log.warn("⚠️  Envoi SANS DKIM vers {}", msg.getRcptTo());
            }

            // ─── 3. Connexion SMTP (repris de votre code original) ───
            try (Socket socket = new Socket(mxHost, 25);
                 BufferedReader in = new BufferedReader(
                         new InputStreamReader(socket.getInputStream(), StandardCharsets.UTF_8));
                 PrintWriter out = new PrintWriter(
                         new OutputStreamWriter(socket.getOutputStream(), StandardCharsets.UTF_8),
                         true)) {

                socket.setSoTimeout(readTimeout);

                // Banner
                String banner = in.readLine();
                log.debug("MX Banner: {}", banner);
                assertCode(banner, "220", mxHost);

                // EHLO
                sendAndExpect(out, in, "EHLO " + heloHostname, "250");

                // MAIL FROM
                sendAndExpect(out, in, "MAIL FROM:<" + msg.getMailFrom() + ">", "250");

                // RCPT TO
                sendAndExpect(out, in, "RCPT TO:<" + msg.getRcptTo() + ">", "250");

                // DATA
                sendAndExpect(out, in, "DATA", "354");

                // ─── Headers (DKIM en premier, comme votre code original) ───
                if (!dkimHeader.isEmpty()) {
                    out.print(dkimHeader + "\r\n");
                }
                out.print("From: " + msg.getMailFrom() + "\r\n");
                out.print("To: " + msg.getRcptTo() + "\r\n");
                out.print("Subject: " + msg.getSubject() + "\r\n");
                out.print("Date: " + new java.util.Date() + "\r\n");
                out.print("Message-ID: <" + msg.getTrackingId() + "@" + heloHostname + ">\r\n");

                // NOUVEAU : Ajout header X-Campaign-ID pour le tracking
                if (msg.getCampaignId() != null) {
                    out.print("X-Campaign-ID: " + msg.getCampaignId() + "\r\n");
                }
                if (msg.getTag() != null) {
                    out.print("X-Postal-Tag: " + msg.getTag() + "\r\n");
                }

                // ─── Body ───
                if (msg.getHtmlBody() != null && !msg.getHtmlBody().isBlank()) {
                    // Envoi HTML avec pixel de tracking injecté
                    String htmlWithTracking = injectTrackingPixel(
                            msg.getHtmlBody(), msg.getTrackingId());

                    out.print("MIME-Version: 1.0\r\n");
                    out.print("Content-Type: text/html; charset=UTF-8\r\n");
                    out.print("\r\n");

                    for (String line : htmlWithTracking.split("\n")) {
                        if (line.startsWith(".")) line = "." + line;
                        out.print(line + "\r\n");
                    }
                } else {
                    // Envoi texte brut (comme votre code original)
                    out.print("Content-Type: text/plain; charset=UTF-8\r\n");
                    out.print("\r\n");

                    String body = msg.getTextBody() != null ? msg.getTextBody() : "";
                    for (String line : body.split("\n")) {
                        if (line.startsWith(".")) line = "." + line;
                        out.print(line + "\r\n");
                    }
                }

                // Fin du message
                String finalResponse = sendAndCapture(out, in, ".", "250");

                // QUIT
                out.print("QUIT\r\n");
                out.flush();

                long deliveryTime = System.currentTimeMillis() - startTime;
                log.info("✅ Livré à {} via {} en {}ms", msg.getRcptTo(), mxHost, deliveryTime);

                return SendResult.success(mxHost, finalResponse,
                        !dkimHeader.isEmpty(), deliveryTime);
            }

        } catch (Exception e) {
            long deliveryTime = System.currentTimeMillis() - startTime;
            log.error("❌ Échec envoi à {} : {}", msg.getRcptTo(), e.getMessage());

            // Déterminer si c'est un hard fail (5xx) ou soft fail (4xx/timeout)
            boolean isHardFail = e.getMessage() != null
                    && e.getMessage().contains("5");

            return SendResult.failure(mxHost, e.getMessage(),
                    isHardFail, deliveryTime);
        }
    }

    /**
     * NOUVEAU : Injecte un pixel de tracking dans le HTML.
     * Le tracking-service écoutera sur /track/open/{trackingId}
     */
    private String injectTrackingPixel(String html, String trackingId) {
        if (trackingId == null) return html;

        String pixel = "<img src=\"http://localhost:8080/track/open/"
                + trackingId
                + "\" width=\"1\" height=\"1\" style=\"display:none\" alt=\"\" />";

        // Injecter avant </body> si possible, sinon à la fin
        if (html.contains("</body>")) {
            return html.replace("</body>", pixel + "</body>");
        }
        return html + pixel;
    }

    // ─── Utilitaires SMTP (repris de votre code original) ───

    private void sendAndExpect(PrintWriter out, BufferedReader in,
                               String command, String expectedCode) throws Exception {
        log.debug(">>> {}", command);
        out.print(command + "\r\n");
        out.flush();

        String line, lastLine = "";
        while ((line = in.readLine()) != null) {
            log.debug("<<< {}", line);
            lastLine = line;
            if (line.length() >= 4 && line.charAt(3) == ' ') break;
        }
        assertCode(lastLine, expectedCode, command);
    }

    private String sendAndCapture(PrintWriter out, BufferedReader in,
                                  String command, String expectedCode) throws Exception {
        log.debug(">>> {}", command);
        out.print(command + "\r\n");
        out.flush();

        String line, lastLine = "";
        while ((line = in.readLine()) != null) {
            log.debug("<<< {}", line);
            lastLine = line;
            if (line.length() >= 4 && line.charAt(3) == ' ') break;
        }
        assertCode(lastLine, expectedCode, command);
        return lastLine;
    }

    private void assertCode(String response, String expected, String ctx) throws Exception {
        if (response == null || !response.startsWith(expected)) {
            throw new Exception("SMTP [" + ctx + "] : " + response + " (attendu " + expected + ")");
        }
    }

    // ─── Résultat d'envoi ───

    public record SendResult(
            boolean success,
            String mxHost,
            String smtpResponse,
            boolean hardFail,
            boolean sentWithDkim,
            long deliveryTimeMs
    ) {
        static SendResult success(String mxHost, String response,
                                  boolean dkim, long time) {
            return new SendResult(true, mxHost, response, false, dkim, time);
        }

        static SendResult failure(String mxHost, String error,
                                  boolean hardFail, long time) {
            return new SendResult(false, mxHost, error, hardFail, false, time);
        }
    }
}
