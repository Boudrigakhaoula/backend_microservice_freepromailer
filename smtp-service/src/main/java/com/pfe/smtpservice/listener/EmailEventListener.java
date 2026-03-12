package com.pfe.smtpservice.listener;



import com.pfe.smtpservice.server.EmailMessage;
import com.pfe.smtpservice.server.EmailReceivedEvent;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

/**
 * CHANGEMENTS vs l'original :
 *   - Package renommé
 *   - Log enrichi avec plus d'infos
 *   - Prêt pour envoyer des notifications WebSocket (future feature)
 */
@Component
@Slf4j
public class EmailEventListener {

    @EventListener
    public void onEmailReceived(EmailReceivedEvent event) {
        EmailMessage mail = event.getEmail();
        log.info("📬 [EVENT] Email reçu via SMTP : {} → {} | Sujet: {} | ID: {}",
                mail.getFrom(),
                mail.getRecipients(),
                mail.getSubject(),
                event.getMessageId());

        // TODO: Envoyer notification WebSocket au dashboard Angular
        // TODO: Enregistrer dans les métriques Prometheus
    }
}
