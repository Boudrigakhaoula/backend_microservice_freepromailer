package com.pfe.smtpservice.controller;

import com.pfe.smtpservice.delivery.MessageQueue;
import com.pfe.smtpservice.entity.QueuedMessage;
import com.pfe.smtpservice.repository.QueuedMessageRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/queue")
@RequiredArgsConstructor
public class QueueController {

    private final QueuedMessageRepository queuedRepo;
    private final MessageQueue messageQueue;

    @GetMapping
    public ResponseEntity<List<QueuedMessage>> getQueue() {
        return ResponseEntity.ok(queuedRepo.findAll());
    }

    @GetMapping("/stats")
    public ResponseEntity<Map<String, Long>> getStats() {
        return ResponseEntity.ok(messageQueue.getStats());
    }

    @GetMapping("/campaign/{campaignId}")
    public ResponseEntity<List<QueuedMessage>> getByCampaign(@PathVariable String campaignId) {
        return ResponseEntity.ok(queuedRepo.findByCampaignId(campaignId));
    }
}