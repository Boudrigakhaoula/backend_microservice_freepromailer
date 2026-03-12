package com.pfe.trackingservice.entity;

import com.pfe.trackingservice.enums.EventType;
import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

/**
 * Événement de tracking enregistré pour chaque interaction.
 *
 * Chaque email envoyé a un trackingId unique (UUID).
 * Quand le destinataire ouvre l'email, clique un lien, etc.,
 * un TrackingEvent est créé avec ce trackingId.
 */
@Entity
@Table(name = "tracking_events", indexes = {
        @Index(name = "idx_tracking_id", columnList = "trackingId"),
        @Index(name = "idx_tracking_campaign", columnList = "campaignId"),
        @Index(name = "idx_tracking_type", columnList = "type"),
        @Index(name = "idx_tracking_timestamp", columnList = "timestamp"),
        @Index(name = "idx_tracking_email", columnList = "recipientEmail"),
        @Index(name = "idx_tracking_campaign_type", columnList = "campaignId, type")
})
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TrackingEvent {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * Identifiant unique du message (UUID généré par le smtp-service).
     * Lie cet événement à un email spécifique.
     */
    @Column(nullable = false)
    private String trackingId;

    /**
     * ID de la campagne (peut être null pour un envoi direct).
     */
    private String campaignId;

    /**
     * ID du contact dans le campaign-service.
     */
    private String contactId;

    /**
     * Email du destinataire.
     */
    private String recipientEmail;

    /**
     * Type d'événement (OPEN, CLICK, BOUNCE, etc.)
     */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private EventType type;

    /**
     * URL cliquée (pour les événements CLICK).
     */
    @Column(columnDefinition = "TEXT")
    private String url;

    /**
     * Détails supplémentaires :
     *   - Pour BOUNCE : raison du bounce
     *   - Pour COMPLAINT : type de plainte
     *   - Pour CLICK : texte du lien
     */
    @Column(columnDefinition = "TEXT")
    private String details;

    /**
     * User-Agent du client (navigateur, client mail).
     */
    @Column(columnDefinition = "TEXT")
    private String userAgent;

    /**
     * Adresse IP du client.
     */
    private String ipAddress;

    /**
     * Timestamp de l'événement.
     */
    @Column(nullable = false)
    private LocalDateTime timestamp;

    @PrePersist
    protected void onCreate() {
        if (timestamp == null) timestamp = LocalDateTime.now();
    }
}
