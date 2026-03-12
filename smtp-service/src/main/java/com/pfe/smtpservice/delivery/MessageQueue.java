package com.pfe.smtpservice.delivery;

import com.pfe.smtpservice.entity.QueuedMessage;
import com.pfe.smtpservice.enums.MessageStatus;
import com.pfe.smtpservice.repository.QueuedMessageRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.*;

/**
 * Service pour mettre des messages en queue.
 * Remplace l'ancien InMemoryDeliveryHandler synchrone.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class MessageQueue {

    private final QueuedMessageRepository repo;

    /**
     * Met un message en queue pour envoi asynchrone.
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
        log.info("📩 En queue #{} → {} [tracking={}]", msg.getId(), to, trackingId);
        return msg;
    }

    /**
     * Statistiques de la queue.
     */
    public Map<String, Long> getStats() {
        Map<String, Long> stats = new LinkedHashMap<>();
        for (Object[] row : repo.countByStatusGrouped()) {
            stats.put(row[0].toString(), (Long) row[1]);
        }
        return stats;
    }
}