package com.pfe.trackingservice.controller;


import com.pfe.trackingservice.dto.BounceRequest;
import com.pfe.trackingservice.dto.EventResponse;
import com.pfe.trackingservice.enums.EventType;
import com.pfe.trackingservice.entity.TrackingEvent;
import com.pfe.trackingservice.service.TrackingService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * API interne pour le tracking — appelée par les autres microservices
 * (smtp-service, campaign-service) et par le dashboard Angular.
 */
@RestController
@RequestMapping("/api/tracking")
@RequiredArgsConstructor
public class TrackingApiController {

    private final TrackingService trackingService;

    // ═══════════════════════════════════════════
    // Enregistrement d'événements (depuis les autres services)
    // ═══════════════════════════════════════════

    /**
     * Enregistrer un bounce (appelé par le smtp-service).
     */
    @PostMapping("/bounce")
    public ResponseEntity<EventResponse> trackBounce(
            @Valid @RequestBody BounceRequest request) {
        TrackingEvent event = trackingService.trackBounce(request);
        return ResponseEntity.ok(trackingService.toResponse(event));
    }

    /**
     * Enregistrer un envoi confirmé (appelé par le smtp-service).
     */
    @PostMapping("/sent")
    public ResponseEntity<EventResponse> trackSent(@RequestBody Map<String, String> payload) {
        TrackingEvent event = trackingService.trackSent(
                payload.get("trackingId"),
                payload.get("campaignId"),
                payload.get("contactId"),
                payload.get("recipientEmail")
        );
        return ResponseEntity.ok(trackingService.toResponse(event));
    }

    /**
     * Enregistrer une plainte spam.
     */
    @PostMapping("/complaint")
    public ResponseEntity<EventResponse> trackComplaint(@RequestBody Map<String, String> payload) {
        TrackingEvent event = trackingService.trackComplaint(
                payload.get("trackingId"),
                payload.getOrDefault("details", "Spam complaint")
        );
        return ResponseEntity.ok(trackingService.toResponse(event));
    }

    // ═══════════════════════════════════════════
    // Consultation des événements (pour le dashboard)
    // ═══════════════════════════════════════════

    /**
     * Tous les événements d'un email spécifique.
     */
    @GetMapping("/events/{trackingId}")
    public ResponseEntity<List<EventResponse>> getByTrackingId(
            @PathVariable String trackingId) {
        List<EventResponse> events = trackingService.getByTrackingId(trackingId)
                .stream()
                .map(trackingService::toResponse)
                .toList();
        return ResponseEntity.ok(events);
    }

    /**
     * Tous les événements d'une campagne.
     */
    @GetMapping("/campaign/{campaignId}/events")
    public ResponseEntity<List<EventResponse>> getByCampaign(
            @PathVariable String campaignId) {
        List<EventResponse> events = trackingService.getByCampaignId(campaignId)
                .stream()
                .map(trackingService::toResponse)
                .toList();
        return ResponseEntity.ok(events);
    }

    /**
     * Événements par type (OPEN, CLICK, BOUNCE, etc.)
     */
    @GetMapping("/type/{type}")
    public ResponseEntity<List<EventResponse>> getByType(@PathVariable EventType type) {
        List<EventResponse> events = trackingService.getByType(type)
                .stream()
                .map(trackingService::toResponse)
                .toList();
        return ResponseEntity.ok(events);
    }

    /**
     * Historique de tracking d'un destinataire.
     */
    @GetMapping("/recipient")
    public ResponseEntity<List<EventResponse>> getByRecipient(
            @RequestParam String email) {
        List<EventResponse> events = trackingService.getByRecipientEmail(email)
                .stream()
                .map(trackingService::toResponse)
                .toList();
        return ResponseEntity.ok(events);
    }

    /**
     * Derniers événements (flux temps réel pour le dashboard).
     */
    @GetMapping("/latest")
    public ResponseEntity<List<EventResponse>> getLatest(
            @RequestParam(defaultValue = "50") int limit) {
        List<EventResponse> events = trackingService.getLatest(Math.min(limit, 200))
                .stream()
                .map(trackingService::toResponse)
                .toList();
        return ResponseEntity.ok(events);
    }
}
