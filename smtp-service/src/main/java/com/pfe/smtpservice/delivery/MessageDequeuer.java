package com.pfe.smtpservice.delivery;

import com.pfe.smtpservice.dkim.DkimSigner;
import com.pfe.smtpservice.entity.QueuedMessage;
import com.pfe.smtpservice.entity.SentMessage;
import com.pfe.smtpservice.enums.MessageStatus;
import com.pfe.smtpservice.repository.QueuedMessageRepository;
import com.pfe.smtpservice.repository.SentMessageRepository;
import com.pfe.smtpservice.repository.DeliveryLogRepository;
import com.pfe.smtpservice.entity.DeliveryLog;
import jakarta.mail.*;
import jakarta.mail.Address;
import jakarta.mail.Message;
import jakarta.mail.internet.InternetAddress;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.xbill.DNS.*;
import org.xbill.DNS.Record;

import java.io.*;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class MessageDequeuer {

    private final QueuedMessageRepository queuedRepo;
    private final SentMessageRepository sentRepo;
    private final DeliveryLogRepository deliveryLogRepo;
    private final DkimSigner dkimSigner;

    // ─── Configuration générale ───
    @Value("${smtp.dequeue.batch-size:10}")
    private int batchSize;

    @Value("${smtp.dequeue.lock-timeout-minutes:5}")
    private int lockTimeoutMinutes;

    @Value("${smtp.sending.timeout-ms:15000}")
    private int sendingTimeoutMs;

    @Value("${smtp.sending.connection-timeout-ms:10000}")
    private int connectionTimeoutMs;

    @Value("${smtp.hostname:khaoulaboudriga.me}")
    private String hostname;

    // ─── Configuration Relay SMTP ───
    @Value("${smtp.relay.enabled:false}")
    private boolean relayEnabled;

    @Value("${smtp.relay.host:}")
    private String relayHost;

    @Value("${smtp.relay.port:587}")
    private int relayPort;

    @Value("${smtp.relay.username:}")
    private String relayUsername;

    @Value("${smtp.relay.password:}")
    private String relayPassword;

    @Value("${smtp.relay.starttls:true}")
    private boolean relayStartTls;

    // ─────────────────────────────────────────────────────────
    //  SCHEDULER — Toutes les 5 secondes, traite les messages
    // ─────────────────────────��───────────────────────────────

    @Scheduled(fixedDelayString = "${smtp.dequeue.poll-interval-ms:5000}")
    public void processQueue() {
        // 1. Libérer les messages verrouillés depuis trop longtemps (crash recovery)
        unlockStaleMessages();

        // 2. Récupérer les messages prêts à envoyer
        List<QueuedMessage> messages = queuedRepo.findMessagesReadyToSend(
                MessageStatus.QUEUED,
                LocalDateTime.now(),
                org.springframework.data.domain.PageRequest.of(0, batchSize)
        );

        for (QueuedMessage msg : messages) {
            try {
                processMessage(msg);
            } catch (Exception e) {
                log.error("Erreur inattendue traitement message #{} : {}", msg.getId(), e.getMessage());
            }
        }
    }

    // ─────────────────────────────────────────────────────────
    //  TRAITEMENT D'UN MESSAGE
    // ─────────────────────────────────────────────────────────

    @Transactional
    public void processMessage(QueuedMessage msg) {
        // 1. Verrouiller le message (éviter double envoi)
        msg.setLockedBy("dequeuer-" + Thread.currentThread().getName());
        msg.setLockedAt(LocalDateTime.now());
        msg.setStatus(MessageStatus.SENDING);
        queuedRepo.save(msg);

        log.info("🔒 Message #{} verrouillé → {}", msg.getId(), msg.getRcptTo());

        long startTime = System.currentTimeMillis();
        String mxHost = null;

        try {
            // 2. Construire le contenu MIME
            String mimeContent = buildMimeMessage(msg);

            // 3. Signer avec DKIM
            String signedContent = signWithDkim(msg, mimeContent);

            // 4. Envoyer — soit via relay, soit via MX direct
            if (relayEnabled && relayHost != null && !relayHost.isBlank()) {
                mxHost = relayHost;
                deliverViaRelay(msg, signedContent);
            } else {
                mxHost = resolveMx(msg.getRcptTo());
                deliverViaMx(msg, signedContent, mxHost);
            }

            // 5. Succès ! Déplacer vers sent_messages
            long elapsed = System.currentTimeMillis() - startTime;
            onSuccess(msg, mxHost, elapsed, signedContent.contains("DKIM-Signature"));

        } catch (Exception e) {
            long elapsed = System.currentTimeMillis() - startTime;
            onFailure(msg, mxHost, e.getMessage(), elapsed);
        }
    }

    // ─────────────────────────────────────────────────────────
    //  CONSTRUCTION DU MESSAGE MIME
    // ─────────────────────────────────────────────────────────

    private String buildMimeMessage(QueuedMessage msg) throws Exception {
        Properties props = new Properties();
        Session session = Session.getInstance(props);

        MimeMessage mime = new MimeMessage(session);
        mime.setFrom(new InternetAddress(msg.getMailFrom()));
        mime.setRecipient(Message.RecipientType.TO, new InternetAddress(msg.getRcptTo()));
        mime.setSubject(msg.getSubject(), "UTF-8");
        mime.setSentDate(new Date());
        mime.setHeader("Message-ID",
                "<" + UUID.randomUUID() + "@" + hostname + ">");

        // Header de tracking
        if (msg.getTrackingId() != null) {
            mime.setHeader("X-FPM-Tracking-Id", msg.getTrackingId());
        }
        if (msg.getCampaignId() != null) {
            mime.setHeader("X-FPM-Campaign-Id", msg.getCampaignId());
        }
        if (msg.getTag() != null) {
            mime.setHeader("X-FPM-Tag", msg.getTag());
        }

        // Contenu HTML ou texte
        if (msg.getHtmlBody() != null && !msg.getHtmlBody().isBlank()) {
            // Injecter le pixel de tracking
            String html = injectTrackingPixel(msg.getHtmlBody(), msg.getTrackingId());
            // Réécrire les liens pour le tracking des clics
            html = rewriteLinks(html, msg.getTrackingId());
            // Remplacer {{trackingId}} dans le template
            html = html.replace("{{trackingId}}",
                    msg.getTrackingId() != null ? msg.getTrackingId() : "");

            if (msg.getTextBody() != null && !msg.getTextBody().isBlank()) {
                // Multipart : HTML + texte
                jakarta.mail.internet.MimeMultipart multipart =
                        new jakarta.mail.internet.MimeMultipart("alternative");

                jakarta.mail.internet.MimeBodyPart textPart =
                        new jakarta.mail.internet.MimeBodyPart();
                textPart.setText(msg.getTextBody(), "UTF-8");

                jakarta.mail.internet.MimeBodyPart htmlPart =
                        new jakarta.mail.internet.MimeBodyPart();
                htmlPart.setContent(html, "text/html; charset=UTF-8");

                multipart.addBodyPart(textPart);
                multipart.addBodyPart(htmlPart);
                mime.setContent(multipart);
            } else {
                mime.setContent(html, "text/html; charset=UTF-8");
            }
        } else {
            mime.setText(
                    msg.getTextBody() != null ? msg.getTextBody() : "", "UTF-8");
        }

        // Convertir en string
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        mime.writeTo(bos);
        return bos.toString(StandardCharsets.UTF_8);
    }

    // ─────────────────────────────────────────────────────────
    //  INJECTION DU PIXEL DE TRACKING
    // ─────────────────────────────────────────────────────────

    private String injectTrackingPixel(String html, String trackingId) {
        if (trackingId == null || trackingId.isBlank()) return html;

        String pixel = "<img src=\"http://localhost:8080/track/open/" + trackingId
                + "\" width=\"1\" height=\"1\" style=\"display:none\" alt=\"\" />";

        // Injecter avant </body>
        if (html.contains("</body>")) {
            return html.replace("</body>", pixel + "</body>");
        }
        // Sinon ajouter à la fin
        return html + pixel;
    }

    // ─────────────────────────────────────────────────────────
    //  RÉÉCRITURE DES LIENS POUR LE TRACKING DES CLICS
    // ─────────────────────────────────────────────────────────

    private String rewriteLinks(String html, String trackingId) {
        if (trackingId == null || trackingId.isBlank()) return html;

        // Remplacer les href="http..." par des liens de tracking
        // Sauf les liens de désabonnement (déjà corrects)
        return html.replaceAll(
                "href=\"(https?://(?!localhost:8080/track)[^\"]+)\"",
                "href=\"http://localhost:8080/track/click/" + trackingId + "?url=$1\""
        );
    }

    // ─────────────────────────────────────────────────────────
    //  SIGNATURE DKIM
    // ─────────────────────────────────────────────────────────

    private String signWithDkim(QueuedMessage msg, String mimeContent) {
        try {
            // Utiliser la NOUVELLE méthode signMessage() qui retourne
            // le message complet avec DKIM-Signature en première ligne
            String signed = dkimSigner.signMessage(mimeContent);
            log.info("🔏 DKIM signé pour {}", msg.getRcptTo());
            return signed;
        } catch (Exception e) {
            log.warn("⚠️ DKIM signature échouée pour {} : {} — envoi sans DKIM",
                    msg.getRcptTo(), e.getMessage());
            return mimeContent;
        }
    }

    // ─────────────────────────────────────────────────────────
    //  RÉSOLUTION MX
    // ─────────────────────────────────────────────────────────

    private String resolveMx(String email) throws Exception {
        String domain = email.substring(email.indexOf('@') + 1);

        Record[] records = new Lookup(domain, Type.MX).run();

        if (records == null || records.length == 0) {
            throw new RuntimeException("Aucun enregistrement MX trouvé pour " + domain);
        }

        // Trier par priorité (plus basse = meilleure)
        Arrays.sort(records, (a, b) ->
                ((MXRecord) a).getPriority() - ((MXRecord) b).getPriority());

        MXRecord best = (MXRecord) records[0];
        String mxHost = best.getTarget().toString(true);

        log.info("MX pour {} : {} (priorité {})", domain, mxHost, best.getPriority());
        log.info("MX résolu : {} → {}", domain, mxHost);

        return mxHost;
    }

    // ─────────────────────────────────────────────────────────
    //  ENVOI VIA MX DIRECT (production — VPS avec IP propre)
    // ─────────────────────────────────────────────────────────

    private void deliverViaMx(QueuedMessage msg, String signedContent, String mxHost)
            throws Exception {

        try (Socket socket = new Socket()) {
            socket.connect(
                    new java.net.InetSocketAddress(mxHost, 25), connectionTimeoutMs);
            socket.setSoTimeout(sendingTimeoutMs);

            BufferedReader reader = new BufferedReader(
                    new InputStreamReader(socket.getInputStream(), StandardCharsets.UTF_8));
            BufferedWriter writer = new BufferedWriter(
                    new OutputStreamWriter(socket.getOutputStream(), StandardCharsets.UTF_8));

            // Lire le banner
            String banner = readResponse(reader);
            expectCode(banner, 220, "BANNER");

            // EHLO
            sendCommand(writer, "EHLO " + hostname);
            String ehloResp = readMultilineResponse(reader);
            expectCode(ehloResp, 250, "EHLO");

            // STARTTLS si supporté
            if (ehloResp.contains("STARTTLS")) {
                sendCommand(writer, "STARTTLS");
                String starttlsResp = readResponse(reader);
                expectCode(starttlsResp, 220, "STARTTLS");

                // Upgrade vers TLS
                javax.net.ssl.SSLSocketFactory sslFactory =
                        (javax.net.ssl.SSLSocketFactory) javax.net.ssl.SSLSocketFactory.getDefault();
                javax.net.ssl.SSLSocket sslSocket =
                        (javax.net.ssl.SSLSocket) sslFactory.createSocket(
                                socket, mxHost, 25, true);
                sslSocket.startHandshake();
                sslSocket.setSoTimeout(sendingTimeoutMs);

                reader = new BufferedReader(
                        new InputStreamReader(sslSocket.getInputStream(), StandardCharsets.UTF_8));
                writer = new BufferedWriter(
                        new OutputStreamWriter(sslSocket.getOutputStream(), StandardCharsets.UTF_8));

                // Re-EHLO après STARTTLS
                sendCommand(writer, "EHLO " + hostname);
                readMultilineResponse(reader);
            }

            // MAIL FROM
            sendCommand(writer, "MAIL FROM:<" + msg.getMailFrom() + ">");
            String mailFromResp = readResponse(reader);
            expectCode(mailFromResp, 250, "MAIL FROM:<" + msg.getMailFrom() + ">");

            // RCPT TO
            sendCommand(writer, "RCPT TO:<" + msg.getRcptTo() + ">");
            String rcptToResp = readResponse(reader);
            expectCode(rcptToResp, 250, "RCPT TO:<" + msg.getRcptTo() + ">");

            // DATA
            sendCommand(writer, "DATA");
            String dataResp = readResponse(reader);
            expectCode(dataResp, 354, "DATA");

            // Envoyer le contenu signé DKIM
            writer.write(signedContent);
            if (!signedContent.endsWith("\r\n")) {
                writer.write("\r\n");
            }
            writer.write(".\r\n");
            writer.flush();

            String dataEndResp = readResponse(reader);
            expectCode(dataEndResp, 250, "DATA END");

            // QUIT
            sendCommand(writer, "QUIT");
            try {
                readResponse(reader);
            } catch (Exception ignored) {
                // Certains serveurs ferment la connexion immédiatement
            }
        }
    }

    /**
     * Envoi via relay SMTP en mode RAW (Socket direct).
     * Envoie le contenu DKIM-signé TEL QUEL via commandes SMTP brutes.
     * Compatible avec : MailHog, Mailtrap, Gmail SMTP, Brevo, etc.
     */
    private void deliverViaRelay(QueuedMessage msg, String signedContent) throws Exception {
        log.info("📡 Envoi via relay {}:{} → {}", relayHost, relayPort, msg.getRcptTo());

        long start = System.currentTimeMillis();

        try (Socket socket = new Socket()) {
            socket.connect(new java.net.InetSocketAddress(relayHost, relayPort), connectionTimeoutMs);
            socket.setSoTimeout(sendingTimeoutMs);

            BufferedReader reader = new BufferedReader(
                    new InputStreamReader(socket.getInputStream(), StandardCharsets.UTF_8));
            // IMPORTANT : Utiliser OutputStream brut, pas BufferedWriter
            OutputStream rawOut = socket.getOutputStream();

            // 1. Lire le banner
            String banner = readResponse(reader);
            expectCode(banner, 220, "BANNER");

            // 2. EHLO
            smtpSend(rawOut, "EHLO " + hostname);
            String ehloResp = readMultilineResponse(reader);
            expectCode(ehloResp, 250, "EHLO");

            // 3. AUTH LOGIN si nécessaire
            if (relayUsername != null && !relayUsername.isBlank()) {
                smtpSend(rawOut, "AUTH LOGIN");
                String authResp = readResponse(reader);
                expectCode(authResp, 334, "AUTH LOGIN");

                smtpSend(rawOut, Base64.getEncoder().encodeToString(
                        relayUsername.getBytes(StandardCharsets.UTF_8)));
                String userResp = readResponse(reader);
                expectCode(userResp, 334, "AUTH USER");

                smtpSend(rawOut, Base64.getEncoder().encodeToString(
                        relayPassword.getBytes(StandardCharsets.UTF_8)));
                String passResp = readResponse(reader);
                expectCode(passResp, 235, "AUTH PASS");
            }

            // 4. MAIL FROM
            smtpSend(rawOut, "MAIL FROM:<" + msg.getMailFrom() + ">");
            String mailFromResp = readResponse(reader);
            expectCode(mailFromResp, 250, "MAIL FROM");

            // 5. RCPT TO
            smtpSend(rawOut, "RCPT TO:<" + msg.getRcptTo() + ">");
            String rcptToResp = readResponse(reader);
            expectCode(rcptToResp, 250, "RCPT TO");

            // 6. DATA
            smtpSend(rawOut, "DATA");
            String dataResp = readResponse(reader);
            expectCode(dataResp, 354, "DATA");

            // 7. Envoyer le contenu DKIM-signé
            //    IMPORTANT : Normaliser TOUTES les fins de ligne en \r\n (CRLF)
            String normalized = signedContent
                    .replace("\r\n", "\n")   // d'abord unifier en \n
                    .replace("\r", "\n")     // cas \r seul
                    .replace("\n", "\r\n");  // puis tout en \r\n

            // Écrire le contenu brut en bytes
            rawOut.write(normalized.getBytes(StandardCharsets.UTF_8));
            rawOut.flush();

            // 8. Terminer le DATA avec \r\n.\r\n
            rawOut.write("\r\n.\r\n".getBytes(StandardCharsets.UTF_8));
            rawOut.flush();

            String dataEndResp = readResponse(reader);
            expectCode(dataEndResp, 250, "DATA END");

            // 9. QUIT
            smtpSend(rawOut, "QUIT");
            try { readResponse(reader); } catch (Exception ignored) {}
        }

        long elapsed = System.currentTimeMillis() - start;
        log.info("✅ Message #{} livré via relay {}:{} → {} en {}ms",
                msg.getId(), relayHost, relayPort, msg.getRcptTo(), elapsed);
    }

    /**
     * Envoie une commande SMTP via OutputStream brut (pas BufferedWriter).
     * Garantit que chaque commande se termine par \r\n.
     */
    private void smtpSend(OutputStream out, String command) throws IOException {
        out.write((command + "\r\n").getBytes(StandardCharsets.UTF_8));
        out.flush();
    }
    // ─────────────────────────────────────────────────────────
    //  SUCCÈS — Déplacer vers sent_messages
    // ─────────────────────────────────────────────────────────

    @Transactional
    public void onSuccess(QueuedMessage msg, String mxHost, long elapsed, boolean hasDkim) {
        // Sauvegarder dans sent_messages
        SentMessage sent = SentMessage.builder()
                .mailFrom(msg.getMailFrom())
                .rcptTo(msg.getRcptTo())
                .subject(msg.getSubject())
                .trackingId(msg.getTrackingId())
                .campaignId(msg.getCampaignId())
                .contactId(msg.getContactId())
                .tag(msg.getTag())
                .mxHost(mxHost)
                .smtpResponseCode(250)
                .sentWithDkim(hasDkim)
                .deliveryTimeMs(elapsed)
                .sentAt(LocalDateTime.now())
                .attempts(msg.getAttempts())          // ← AJOUTÉ
                .maxAttempts(msg.getMaxAttempts())     // ← AJOUTÉ
                .build();
        sentRepo.save(sent);

        // Log de livraison
        saveDeliveryLog(msg, "DELIVERED", mxHost, 250,
                "Livré en " + elapsed + "ms", elapsed);

        // Supprimer de la queue
        queuedRepo.delete(msg);

        log.info("✅ Message #{} livré à {} via {} en {}ms",
                msg.getId(), msg.getRcptTo(), mxHost, elapsed);
    }

    // ─────────────────────────────────────────────────────────
    //  ÉCHEC — Retry ou abandon
    // ─────────────────────────────────────────────────────────

    @Transactional
    public void onFailure(QueuedMessage msg, String mxHost, String error, long elapsed) {
        msg.setAttempts(msg.getAttempts() + 1);
        msg.setLastError(error);
        msg.setLockedBy(null);
        msg.setLockedAt(null);

        // Vérifier si c'est un hard bounce (erreur permanente)
        boolean isHardBounce = isHardBounce(error);

        if (isHardBounce || msg.getAttempts() >= msg.getMaxAttempts()) {
            // Abandon définitif
            msg.setStatus(MessageStatus.FAILED);
            queuedRepo.save(msg);

            saveDeliveryLog(msg, isHardBounce ? "HARD_BOUNCE" : "FAILED",
                    mxHost, extractSmtpCode(error), error, elapsed);

            log.error("💀 Message #{} → {} ABANDONNÉ après {} tentatives : {}",
                    msg.getId(), msg.getRcptTo(), msg.getAttempts(), error);
        } else {
            // Retry avec backoff exponentiel : 2^attempt * 60 secondes
            long delaySeconds = (long) Math.pow(2, msg.getAttempts()) * 60;
            msg.setRetryAfter(LocalDateTime.now().plusSeconds(delaySeconds));
            msg.setStatus(MessageStatus.QUEUED);
            queuedRepo.save(msg);

            saveDeliveryLog(msg, "RETRY",
                    mxHost, extractSmtpCode(error), error, elapsed);

            log.error("❌ Échec message #{} → {} : {}", msg.getId(), msg.getRcptTo(), error);
            log.warn("🔄 Message #{} → retry #{} à {}",
                    msg.getId(), msg.getAttempts(), msg.getRetryAfter());
        }
    }

    // ─────────────────────────────────────────────────────────
    //  HARD BOUNCE DETECTION
    // ─────────────────────────────────────────────────────────

    private boolean isHardBounce(String error) {
        if (error == null) return false;
        String lower = error.toLowerCase();
        return lower.contains("550 5.1.1")     // Mailbox does not exist
                || lower.contains("550 5.1.2")  // Domain not found
                || lower.contains("551")        // User not local
                || lower.contains("552")        // Mailbox full (considéré hard)
                || lower.contains("553")        // Mailbox name not allowed
                || lower.contains("554")        // Transaction failed
                || lower.contains("user unknown")
                || lower.contains("mailbox not found")
                || lower.contains("no such user")
                || lower.contains("recipient rejected")
                || lower.contains("does not exist");
    }

    // ─────────────────────────────────────────────────────────
    //  EXTRACTION DU CODE SMTP
    // ─────────────────────────────────────────────────────────

    private int extractSmtpCode(String error) {
        if (error == null) return 0;
        try {
            // Chercher un code 3 chiffres au début de l'erreur
            String stripped = error.replaceAll("[^0-9 ]", " ").trim();
            String[] parts = stripped.split("\\s+");
            for (String part : parts) {
                if (part.length() == 3) {
                    int code = Integer.parseInt(part);
                    if (code >= 200 && code < 600) return code;
                }
            }
        } catch (Exception ignored) {
        }
        return 0;
    }

    // ─────────────────────────────────────────────────────────
    //  DELIVERY LOG
    // ─────────────────────────────────────────────────────────

    private void saveDeliveryLog(QueuedMessage msg, String status,
                                 String mxHost, int smtpCode,
                                 String detail, long elapsed) {
        try {
            DeliveryLog logEntry = DeliveryLog.builder()
                    .messageId(msg.getId())
                    .trackingId(msg.getTrackingId())
                    .campaignId(msg.getCampaignId())
                    .recipientEmail(msg.getRcptTo())
                    .status(status)
                    .mxHost(mxHost)
                    .smtpResponseCode(smtpCode)
                    .detail(detail != null && detail.length() > 1000
                            ? detail.substring(0, 1000) : detail)
                    .deliveryTimeMs(elapsed)
                    .attemptNumber(msg.getAttempts())
                    .createdAt(LocalDateTime.now())
                    .build();
            deliveryLogRepo.save(logEntry);
        } catch (Exception e) {
            log.warn("Impossible de sauvegarder le delivery log : {}", e.getMessage());
        }
    }

    // ─────────────────────────────────────────────────────────
    //  LIBÉRER LES MESSAGES VERROUILLÉS TROP LONGTEMPS
    // ─────────────────────────────────────────────────────────

    private void unlockStaleMessages() {
        LocalDateTime staleThreshold = LocalDateTime.now().minusMinutes(lockTimeoutMinutes);
        List<QueuedMessage> stale = queuedRepo.findByStatusAndLockedAtBefore(
                MessageStatus.SENDING, staleThreshold);

        for (QueuedMessage msg : stale) {
            msg.setLockedBy(null);
            msg.setLockedAt(null);
            msg.setStatus(MessageStatus.QUEUED);
            msg.setRetryAfter(LocalDateTime.now());
            queuedRepo.save(msg);
            log.warn("🔓 Message #{} déverrouillé (lock expiré)", msg.getId());
        }
    }

    // ─────────────────────────────────────────────────────────
    //  UTILITAIRES SMTP
    // ─────────────────────────────────────────────────────────

    private void sendCommand(BufferedWriter writer, String command) throws IOException {
        writer.write(command + "\r\n");
        writer.flush();
    }

    private String readResponse(BufferedReader reader) throws IOException {
        String line = reader.readLine();
        if (line == null) throw new IOException("Connexion fermée par le serveur distant");
        return line;
    }

    private String readMultilineResponse(BufferedReader reader) throws IOException {
        StringBuilder sb = new StringBuilder();
        String line;
        do {
            line = reader.readLine();
            if (line == null) throw new IOException("Connexion fermée par le serveur distant");
            sb.append(line).append("\n");
        } while (line.length() >= 4 && line.charAt(3) == '-');
        return sb.toString();
    }

    private void expectCode(String response, int expectedCode, String context) {
        if (response == null || response.length() < 3) {
            throw new RuntimeException(
                    "SMTP error [" + context + "] : réponse vide (attendu " + expectedCode + ")");
        }
        try {
            int code = Integer.parseInt(response.substring(0, 3));
            if (code != expectedCode) {
                throw new RuntimeException(
                        "SMTP error [" + context + "] : " + response
                                + " (attendu " + expectedCode + ")");
            }
        } catch (NumberFormatException e) {
            throw new RuntimeException(
                    "SMTP error [" + context + "] : réponse invalide — " + response);
        }
    }
}