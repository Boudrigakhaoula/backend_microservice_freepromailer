package com.pfe.smtpservice.dkim;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import jakarta.annotation.PostConstruct;
import java.nio.charset.StandardCharsets;
import java.security.*;
import java.security.spec.PKCS8EncodedKeySpec;
import java.util.Base64;

@Slf4j
@Component
public class DkimSigner {

    @Value("${dkim.domain:khaoulaboudriga.me}")
    private String domain;

    @Value("${dkim.selector:mail}")
    private String selector;

    @Value("${dkim.private-key:}")
    private String privateKeyBase64;

    private PrivateKey privateKey;

    @PostConstruct
    public void init() {
        if (privateKeyBase64 == null || privateKeyBase64.isBlank()) {
            log.warn("⚠️  DKIM désactivé — dkim.private-key non configuré");
            return;
        }
        try {
            String clean = privateKeyBase64
                    .replace("-----BEGIN PRIVATE KEY-----", "")
                    .replace("-----END PRIVATE KEY-----", "")
                    .replaceAll("\\s+", "");

            byte[] keyBytes = Base64.getDecoder().decode(clean);
            PKCS8EncodedKeySpec spec = new PKCS8EncodedKeySpec(keyBytes);
            KeyFactory kf = KeyFactory.getInstance("RSA");
            this.privateKey = kf.generatePrivate(spec);

            log.info("✅ DKIM activé — domaine={} sélecteur={}", domain, selector);
        } catch (Exception e) {
            log.error("❌ Erreur chargement clé DKIM : {}", e.getMessage());
        }
    }

    /**
     * Signe un message MIME complet et retourne le message complet
     * avec le header DKIM-Signature ajouté EN PREMIER.
     *
     * @param mimeContent Le message MIME complet (headers + body)
     * @return Le message MIME complet AVEC le header DKIM-Signature en première ligne
     */
    public String signMessage(String mimeContent) {
        if (privateKey == null) return mimeContent;

        try {
            // 1. Séparer headers et body
            String[] parts = splitHeadersAndBody(mimeContent);
            String headers = parts[0];
            String body = parts[1];

            // 2. Extraire les headers From, To, Subject
            String from = extractHeader(headers, "From");
            String to = extractHeader(headers, "To");
            String subject = extractHeader(headers, "Subject");

            // 3. Calculer le body hash (bh=)
            String canonBody = canonicalizeBody(body);
            MessageDigest sha256 = MessageDigest.getInstance("SHA-256");
            String bh = Base64.getEncoder().encodeToString(
                    sha256.digest(canonBody.getBytes(StandardCharsets.UTF_8)));

            // 4. Construire le header DKIM (sans la valeur b=)
            String dkimHeaderValue =
                    "v=1; a=rsa-sha256; c=relaxed/relaxed;" +
                            " d=" + domain + "; s=" + selector + ";" +
                            " h=from:to:subject;" +
                            " bh=" + bh + "; b=";

            // 5. Construire la chaîne à signer
            String toSign =
                    "from:" + relaxHeader(from) + "\r\n" +
                            "to:" + relaxHeader(to) + "\r\n" +
                            "subject:" + relaxHeader(subject) + "\r\n" +
                            "dkim-signature:" + relaxHeader(dkimHeaderValue);

            // 6. Signer avec RSA-SHA256
            Signature sig = Signature.getInstance("SHA256withRSA");
            sig.initSign(privateKey);
            sig.update(toSign.getBytes(StandardCharsets.UTF_8));
            String b = Base64.getEncoder().encodeToString(sig.sign());

            // 7. Header DKIM complet
            String dkimHeader = "DKIM-Signature: " + dkimHeaderValue + b;

            // 8. Retourner le message complet : DKIM + headers originaux + body
            return dkimHeader + "\r\n" + headers + "\r\n\r\n" + body;

        } catch (Exception e) {
            log.error("Erreur DKIM sign : {}", e.getMessage());
            return mimeContent; // En cas d'erreur, retourner le message original
        }
    }

    /**
     * Ancienne méthode — gardée pour compatibilité.
     * Retourne uniquement le header DKIM (pas le message complet).
     */
    public String sign(String from, String to, String subject, String body) {
        if (privateKey == null) return "";
        try {
            String canonBody = canonicalizeBody(body);
            MessageDigest sha256 = MessageDigest.getInstance("SHA-256");
            String bh = Base64.getEncoder().encodeToString(
                    sha256.digest(canonBody.getBytes(StandardCharsets.UTF_8)));

            String dkimHeaderValue =
                    "v=1; a=rsa-sha256; c=relaxed/relaxed;" +
                            " d=" + domain + "; s=" + selector + ";" +
                            " h=from:to:subject;" +
                            " bh=" + bh + "; b=";

            String toSign =
                    "from:" + relaxHeader(from) + "\r\n" +
                            "to:" + relaxHeader(to) + "\r\n" +
                            "subject:" + relaxHeader(subject) + "\r\n" +
                            "dkim-signature:" + relaxHeader(dkimHeaderValue);

            Signature sig = Signature.getInstance("SHA256withRSA");
            sig.initSign(privateKey);
            sig.update(toSign.getBytes(StandardCharsets.UTF_8));
            String b = Base64.getEncoder().encodeToString(sig.sign());

            return "DKIM-Signature: " + dkimHeaderValue + b;
        } catch (Exception e) {
            log.error("Erreur DKIM sign : {}", e.getMessage());
            return "";
        }
    }

    /**
     * Sépare le message MIME en headers et body.
     * Le séparateur est une ligne vide (\r\n\r\n).
     */
    private String[] splitHeadersAndBody(String mimeContent) {
        int idx = mimeContent.indexOf("\r\n\r\n");
        if (idx == -1) {
            // Essayer avec \n\n
            idx = mimeContent.indexOf("\n\n");
            if (idx == -1) {
                return new String[]{mimeContent, ""};
            }
            return new String[]{
                    mimeContent.substring(0, idx),
                    mimeContent.substring(idx + 2)
            };
        }
        return new String[]{
                mimeContent.substring(0, idx),
                mimeContent.substring(idx + 4)
        };
    }

    /**
     * Extrait la valeur d'un header depuis le bloc de headers.
     */
    private String extractHeader(String headers, String headerName) {
        String lower = headers.toLowerCase();
        String search = headerName.toLowerCase() + ":";
        int idx = lower.indexOf(search);
        if (idx == -1) return "";

        int start = idx + search.length();
        int end = headers.indexOf("\r\n", start);
        if (end == -1) {
            end = headers.indexOf("\n", start);
        }
        if (end == -1) {
            end = headers.length();
        }
        return headers.substring(start, end).trim();
    }

    private String canonicalizeBody(String body) {
        if (body == null || body.isEmpty()) return "\r\n";
        String normalized = body.replace("\r\n", "\n").replace("\r", "\n");
        String[] lines = normalized.split("\n", -1);
        StringBuilder sb = new StringBuilder();
        for (String line : lines) {
            sb.append(line.replaceAll("[ \t]+$", "")).append("\r\n");
        }
        return sb.toString().replaceAll("(\r\n)+$", "") + "\r\n";
    }

    private String relaxHeader(String value) {
        return value == null ? "" : value.replaceAll("\\s+", " ").trim();
    }

    public boolean isEnabled() { return privateKey != null; }
}