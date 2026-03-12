package com.pfe.smtpservice.entity;

import com.pfe.smtpservice.enums.MessageStatus;
import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "queued_messages", indexes = {
        @Index(name = "idx_queued_status", columnList = "status"),
        @Index(name = "idx_queued_retry", columnList = "status, retryAfter"),
        @Index(name = "idx_queued_campaign", columnList = "campaignId"),
        @Index(name = "idx_queued_tracking", columnList = "trackingId")
})
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class QueuedMessage {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String mailFrom;

    @Column(nullable = false)
    private String rcptTo;

    private String subject;

    @Column(columnDefinition = "TEXT")
    private String htmlBody;

    @Column(columnDefinition = "TEXT")
    private String textBody;

    private String campaignId;
    private String contactId;
    private String trackingId;
    private String tag;

    @Builder.Default
    private int attempts = 0;

    @Builder.Default
    private int maxAttempts = 18;

    private String lockedBy;
    private LocalDateTime lockedAt;
    private LocalDateTime retryAfter;

    @Enumerated(EnumType.STRING)
    @Builder.Default
    private MessageStatus status = MessageStatus.QUEUED;

    private LocalDateTime createdAt;
    private LocalDateTime sentAt;

    @Column(columnDefinition = "TEXT")
    private String lastError;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }

    /**
     * Un message est "prêt" si : status=QUEUED, pas verrouillé, retryAfter passé
     */
    public boolean isReady() {
        return status == MessageStatus.QUEUED
                && lockedBy == null
                && (retryAfter == null || retryAfter.isBefore(LocalDateTime.now()));
    }
}