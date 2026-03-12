package com.pfe.smtpservice.controller;

import com.pfe.smtpservice.entity.SentMessage;
import com.pfe.smtpservice.repository.SentMessageRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/mailbox")
@RequiredArgsConstructor
public class MailboxController {

    private final SentMessageRepository sentRepo;

    @GetMapping
    public ResponseEntity<List<SentMessage>> getAllSent() {
        return ResponseEntity.ok(sentRepo.findAll());
    }

    @GetMapping("/count")
    public ResponseEntity<Map<String, Long>> getCount() {
        return ResponseEntity.ok(Map.of("count", sentRepo.count()));
    }

    @GetMapping("/latest")
    public ResponseEntity<?> getLatest() {
        List<SentMessage> all = sentRepo.findAll();
        if (all.isEmpty()) {
            return ResponseEntity.ok(Map.of("message", "Aucun email envoyé"));
        }
        return ResponseEntity.ok(all.get(all.size() - 1));
    }

    @GetMapping("/campaign/{campaignId}")
    public ResponseEntity<List<SentMessage>> getByCampaign(@PathVariable String campaignId) {
        return ResponseEntity.ok(sentRepo.findByCampaignId(campaignId));
    }

    @GetMapping("/tracking/{trackingId}")
    public ResponseEntity<?> getByTrackingId(@PathVariable String trackingId) {
        return sentRepo.findByTrackingId(trackingId)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }
}