package com.pfe.smtpservice.server;

import lombok.Data;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Représente un email SMTP reçu — construit pendant la session.
 * INCHANGÉ par rapport à l'original.
 */
@Data
public class EmailMessage {

    private String from;
    private List<String> recipients = new ArrayList<>();
    private Map<String, String> headers = new HashMap<>();
    private List<String> rawHeaders  = new ArrayList<>();
    private String body;
    private long   receivedTimestamp;

    public void addRecipient(String email) {
        recipients.add(email.toLowerCase().trim());
    }

    public void addRawHeader(String raw) {
        rawHeaders.add(raw);
        int colon = raw.indexOf(':');
        if (colon > 0) {
            String key   = raw.substring(0, colon).trim().toLowerCase();
            String value = raw.substring(colon + 1).trim();
            headers.put(key, value);
        }
    }

    public String getSubject()     { return headers.getOrDefault("subject",     "(sans objet)"); }
    public String getContentType() { return headers.getOrDefault("content-type", "text/plain"); }
    public String getDate()        { return headers.getOrDefault("date",        ""); }

    @Override
    public String toString() {
        return String.format("EmailMessage{from=%s, to=%s, subject=%s}",
                from, recipients, getSubject());
    }
}
