package com.pfe.trackingservice.service;

import com.pfe.trackingservice.dto.BounceRequest;
import com.pfe.trackingservice.dto.EventResponse;
import com.pfe.trackingservice.enums.EventType;
import com.pfe.trackingservice.entity.TrackingEvent;
import com.pfe.trackingservice.repository.TrackingEventRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Service de tracking des événements email.
 *
 * Gère l'enregistrement de tous les événements :
 *   - OPEN : pixel de tracking chargé
 *   - CLICK : lien dans l'email cliqué
 *   - BOUNCE : email rejeté
 *   - UNSUBSCRIBE : destinataire désabonné
 *   - SENT/DELIVERED : confirmations d'envoi
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class TrackingService {

    private final TrackingEventRepository repo;

    @Value("${tracking.cleanup.retention-days:365}")
    private int retentionDays;

    // ─── Enregistrer une ouverture ───

    @Transactional
    public TrackingEvent trackOpen(String trackingId, String userAgent,
                                   String ipAddress) {
        TrackingEvent event = TrackingEvent.builder()
                .trackingId(trackingId)
                .type(EventType.OPEN)
                .userAgent(userAgent)
                .ipAddress(ipAddress)
                .timestamp(LocalDateTime.now())
                .build();

        // Enrichir avec les infos de la campagne si premier open
        enrichFromExistingEvents(event, trackingId);

        TrackingEvent saved = repo.save(event);
        log.info("👁️  Ouverture : tracking={} ip={}", trackingId, ipAddress);
        return saved;
    }

    // ─── Enregistrer un clic ───

    @Transactional
    public TrackingEvent trackClick(String trackingId, String url,
                                    String userAgent, String ipAddress) {
        TrackingEvent event = TrackingEvent.builder()
                .trackingId(trackingId)
                .type(EventType.CLICK)
                .url(url)
                .userAgent(userAgent)
                .ipAddress(ipAddress)
                .timestamp(LocalDateTime.now())
                .build();

        enrichFromExistingEvents(event, trackingId);

        TrackingEvent saved = repo.save(event);
        log.info("🖱️  Clic : tracking={} url={}", trackingId, url);
        return saved;
    }

    // ─── Enregistrer un bounce ───

    @Transactional
    public TrackingEvent trackBounce(BounceRequest request) {
        EventType type = request.isHardBounce()
                ? EventType.HARD_BOUNCE : EventType.SOFT_BOUNCE;

        TrackingEvent event = TrackingEvent.builder()
                .trackingId(request.getTrackingId())
                .campaignId(request.getCampaignId())
                .contactId(request.getContactId())
                .recipientEmail(request.getRecipientEmail())
                .type(type)
                .details(request.getReason())
                .timestamp(LocalDateTime.now())
                .build();

        // Aussi enregistrer un événement BOUNCE global
        repo.save(TrackingEvent.builder()
                .trackingId(request.getTrackingId())
                .campaignId(request.getCampaignId())
                .contactId(request.getContactId())
                .recipientEmail(request.getRecipientEmail())
                .type(EventType.BOUNCE)
                .details(request.getSmtpResponse())
                .timestamp(LocalDateTime.now())
                .build());

        TrackingEvent saved = repo.save(event);
        log.warn("📭 Bounce {} : tracking={} email={} raison={}",
                type, request.getTrackingId(),
                request.getRecipientEmail(), request.getReason());
        return saved;
    }

    // ─── Enregistrer un désabonnement ───

    @Transactional
    public TrackingEvent trackUnsubscribe(String trackingId, String ipAddress) {
        TrackingEvent event = TrackingEvent.builder()
                .trackingId(trackingId)
                .type(EventType.UNSUBSCRIBE)
                .ipAddress(ipAddress)
                .timestamp(LocalDateTime.now())
                .build();

        enrichFromExistingEvents(event, trackingId);

        TrackingEvent saved = repo.save(event);
        log.info("📭 Désabonnement : tracking={}", trackingId);
        return saved;
    }

    // ─── Enregistrer un envoi confirmé ───

    @Transactional
    public TrackingEvent trackSent(String trackingId, String campaignId,
                                   String contactId, String recipientEmail) {
        TrackingEvent event = TrackingEvent.builder()
                .trackingId(trackingId)
                .campaignId(campaignId)
                .contactId(contactId)
                .recipientEmail(recipientEmail)
                .type(EventType.SENT)
                .timestamp(LocalDateTime.now())
                .build();

        TrackingEvent saved = repo.save(event);
        log.debug("✅ Sent : tracking={} email={}", trackingId, recipientEmail);
        return saved;
    }

    // ─── Enregistrer une plainte spam ───

    @Transactional
    public TrackingEvent trackComplaint(String trackingId, String details) {
        TrackingEvent event = TrackingEvent.builder()
                .trackingId(trackingId)
                .type(EventType.COMPLAINT)
                .details(details)
                .timestamp(LocalDateTime.now())
                .build();

        enrichFromExistingEvents(event, trackingId);

        TrackingEvent saved = repo.save(event);
        log.warn("🚨 Plainte spam : tracking={}", trackingId);
        return saved;
    }

    // ─── Consulter les événements ───

    public List<TrackingEvent> getByTrackingId(String trackingId) {
        return repo.findByTrackingIdOrderByTimestampDesc(trackingId);
    }

    public List<TrackingEvent> getByCampaignId(String campaignId) {
        return repo.findByCampaignIdOrderByTimestampDesc(campaignId);
    }

    public List<TrackingEvent> getByRecipientEmail(String email) {
        return repo.findByRecipientEmailOrderByTimestampDesc(email);
    }

    public List<TrackingEvent> getByType(EventType type) {
        return repo.findByTypeOrderByTimestampDesc(type);
    }

    public List<TrackingEvent> getLatest(int limit) {
        return repo.findLatestEvents(limit);
    }

    // ─── Mapper Entity → DTO ───

    public EventResponse toResponse(TrackingEvent e) {
        return EventResponse.builder()
                .id(e.getId())
                .trackingId(e.getTrackingId())
                .campaignId(e.getCampaignId())
                .contactId(e.getContactId())
                .recipientEmail(e.getRecipientEmail())
                .type(e.getType())
                .url(e.getUrl())
                .details(e.getDetails())
                .userAgent(e.getUserAgent())
                .ipAddress(e.getIpAddress())
                .timestamp(e.getTimestamp())
                .build();
    }

    // ─── Enrichir un event avec les infos campagne/contact ───

    private void enrichFromExistingEvents(TrackingEvent event, String trackingId) {
        List<TrackingEvent> existing = repo.findByTrackingIdOrderByTimestampDesc(trackingId);
        if (!existing.isEmpty()) {
            TrackingEvent first = existing.get(existing.size() - 1);
            if (event.getCampaignId() == null) event.setCampaignId(first.getCampaignId());
            if (event.getContactId() == null) event.setContactId(first.getContactId());
            if (event.getRecipientEmail() == null) event.setRecipientEmail(first.getRecipientEmail());
        }
    }

    // ─── Nettoyage des vieux événements (scheduled task) ───

    @Scheduled(cron = "${tracking.cleanup.cron:0 0 3 * * ?}")
    @Transactional
    public void cleanupOldEvents() {
        LocalDateTime cutoff = LocalDateTime.now().minusDays(retentionDays);
        int deleted = repo.deleteOlderThan(cutoff);
        if (deleted > 0) {
            log.info("🧹 Cleanup tracking : {} événements supprimés (plus vieux que {} jours)",
                    deleted, retentionDays);
        }
    }
}
