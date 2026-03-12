package com.pfe.smtpservice.server;

import org.springframework.context.ApplicationEvent;

/**
 * Event Spring publié quand un email est reçu via SMTP.
 * INCHANGÉ par rapport à l'original.
 */
public class EmailReceivedEvent extends ApplicationEvent {

    private final EmailMessage email;
    private final String messageId;

    public EmailReceivedEvent(Object source, EmailMessage email, String messageId) {
        super(source);
        this.email = email;
        this.messageId = messageId;
    }

    public EmailMessage getEmail()   { return email; }
    public String getMessageId()     { return messageId; }
}
