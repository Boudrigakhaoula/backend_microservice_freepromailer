package com.pfe.smtpservice.controller;

import com.pfe.smtpservice.delivery.MessageQueue;
import com.pfe.smtpservice.dto.SendEmailRequest;
import com.pfe.smtpservice.entity.QueuedMessage;
import com.pfe.smtpservice.repository.SuppressionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/send")
@RequiredArgsConstructor
public class SendController {

    private final MessageQueue messageQueue;
    private final SuppressionRepository suppressionRepo;

    /**
     * Envoi simple (ad hoc)
     */
    @PostMapping
    public ResponseEntity<Map<String, String>> send(@RequestBody SendEmailRequest request) {
        return doSend(request);
    }

    /**
     * Envoi campagne — appelé par campaign-service via Feign.
     * Même logique, endpoint séparé pour la clarté.
     */
    @PostMapping("/campaign")
    public ResponseEntity<Map<String, String>> sendCampaignEmail(@RequestBody SendEmailRequest request) {
        return doSend(request);
    }

    private ResponseEntity<Map<String, String>> doSend(SendEmailRequest request) {
        if (request.getTo() == null || request.getTo().isBlank()) {
            return ResponseEntity.badRequest()
                    .body(Map.of("error", "Le champ 'to' est obligatoire"));
        }

        if (suppressionRepo.isEmailSuppressed(request.getTo(), LocalDateTime.now())) {
            log.info("⛔ Email supprimé (suppression list) : {}", request.getTo());
            return ResponseEntity.ok(Map.of(
                    "status", "held",
                    "reason", "Recipient is on the suppression list"
            ));
        }

        String from = (request.getFrom() != null && !request.getFrom().isBlank())
                ? request.getFrom()
                : "noreply@khaoulaboudriga.me";

        QueuedMessage msg = messageQueue.enqueue(
                from,
                request.getTo(),
                request.getSubject() != null ? request.getSubject() : "(sans objet)",
                request.getBody(),
                request.getHtmlBody(),
                request.getCampaignId(),
                request.getContactId(),
                request.getTag(),
                request.getSenderId()
        );

        log.info("📩 Queued → {} [campaign={}]", request.getTo(), request.getCampaignId());

        return ResponseEntity.ok(Map.of(
                "status", "queued",
                "messageId", msg.getId().toString(),
                "trackingId", msg.getTrackingId(),
                "to", request.getTo(),
                "subject", msg.getSubject()
        ));
    }
}