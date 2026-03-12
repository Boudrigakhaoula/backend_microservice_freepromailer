package com.pfe.smtpservice.server;

/**
 * Interface de livraison des emails reçus par le serveur SMTP.
 * INCHANGÉ par rapport à l'original.
 */
public interface MailDeliveryHandler {
    String deliver(EmailMessage email);
}
