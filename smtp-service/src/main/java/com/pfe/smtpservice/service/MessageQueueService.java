package com.pfe.smtpservice.service;

import com.pfe.smtpservice.enums.MessageStatus;
import com.pfe.smtpservice.entity.QueuedMessage;
import com.pfe.smtpservice.repository.QueuedMessageRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.*;

/**
 * Service pour mettre des messages en queue.
 *
 * CHANGEMENT vs l'original :
 *   - L'original appelait directement deliveryHandler.deliver() de façon synchrone
 *   - Maintenant on persiste en DB (comme Postal: queued_messages table)
 *   - Le MessageDequeuer traite les messages de façon asynchrone
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class MessageQueueService {

    private final QueuedMessageRepository repo;

    /**
     * Met un message en queue pour envoi asynchrone.
     * Retourne le trackingId unique.
     */
    public QueuedMessage enqueue(String from, String to, String subject,
                                 String textBody, String htmlBody,
                                 String campaignId, String contactId, String tag) {

        String trackingId = UUID.randomUUID().toString();

        QueuedMessage msg = QueuedMessage.builder()
                .mailFrom(from != null ? from : "noreply@khaoulaboudriga.me")
                .rcptTo(to.toLowerCase().trim())
                .subject(subject != null ? subject : "(sans objet)")
                .textBody(textBody)
                .htmlBody(htmlBody)
                .campaignId(campaignId)
                .contactId(contactId)
                .tag(tag)
                .trackingId(trackingId)
                .status(MessageStatus.QUEUED)
                .build();

        repo.save(msg);
        log.info("📩 Message en queue #{} → {} [tracking={}]",
                msg.getId(), to, trackingId);
        return msg;
    }

    /**
     * Statistiques de la queue (pour le dashboard).
     */
    public Map<String, Long> getQueueStats() {
        Map<String, Long> stats = new LinkedHashMap<>();
        for (Object[] row : repo.countByStatusGrouped()) {
            stats.put(row[0].toString(), (Long) row[1]);
        }
        return stats;
    }
}
