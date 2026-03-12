package com.pfe.smtpservice.controller;

import com.pfe.smtpservice.delivery.MessageQueue;
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

    @PostMapping
    public ResponseEntity<Map<String, String>> send(@RequestBody Map<String, String> payload) {
        String to = payload.get("to");
        if (to == null || to.isBlank()) {
            return ResponseEntity.badRequest()
                    .body(Map.of("error", "Le champ 'to' est obligatoire"));
        }

        // Vérifier la suppression list
        if (suppressionRepo.isEmailSuppressed(to, LocalDateTime.now())) {
            return ResponseEntity.ok(Map.of(
                    "status", "held",
                    "reason", "Recipient is on the suppression list"
            ));
        }

        QueuedMessage msg = messageQueue.enqueue(
                payload.getOrDefault("from", "noreply@khaoulaboudriga.me"),
                to,
                payload.getOrDefault("subject", "(sans objet)"),
                payload.getOrDefault("body", ""),
                payload.get("htmlBody"),
                payload.get("campaignId"),
                payload.get("contactId"),
                payload.get("tag")
        );

        return ResponseEntity.ok(Map.of(
                "status", "queued",
                "messageId", msg.getId().toString(),
                "trackingId", msg.getTrackingId(),
                "to", to,
                "subject", msg.getSubject()
        ));
    }

    @PostMapping("/campaign")
    public ResponseEntity<Map<String, String>> sendCampaignEmail(
            @RequestBody Map<String, String> payload) {
        return send(payload);
    }
}