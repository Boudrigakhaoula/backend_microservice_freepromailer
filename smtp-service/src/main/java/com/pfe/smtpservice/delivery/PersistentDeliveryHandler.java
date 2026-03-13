package com.pfe.smtpservice.delivery;

import com.pfe.smtpservice.server.EmailMessage;
import com.pfe.smtpservice.server.EmailReceivedEvent;
import com.pfe.smtpservice.server.MailDeliveryHandler;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;

import java.util.UUID;

/**
 * Remplace InMemoryDeliveryHandler.
 *
 * Au lieu d'envoyer directement, met les emails en queue PostgreSQL.
 * Le MessageDequeuer traite la queue de façon asynchrone.
 *
 * Implémente MailDeliveryHandler pour être utilisé par SmtpSession.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class PersistentDeliveryHandler implements MailDeliveryHandler {

    private final MessageQueue messageQueue;
    private final ApplicationEventPublisher eventPublisher;

    @Override
    public String deliver(EmailMessage email) {
        String messageId = UUID.randomUUID().toString();

        // Mettre chaque destinataire en queue séparément
        for (String recipient : email.getRecipients()) {
            messageQueue.enqueue(
                    email.getFrom(),
                    recipient,
                    email.getSubject(),
                    email.getBody(),      // textBody
                    null,                 // htmlBody (SMTP entrant = text)
                    null,                 // campaignId
                    null,                 // contactId
                    null,                 // tag
                    null                  // senderId (SMTP entrant = anonymous)
            );
        }

        // Publier l'événement Spring
        eventPublisher.publishEvent(new EmailReceivedEvent(this, email, messageId));

        log.info("📨 Email reçu via SMTP et mis en queue : {} → {} ({})",
                email.getFrom(), email.getRecipients(), email.getSubject());
        return messageId;
    }
}