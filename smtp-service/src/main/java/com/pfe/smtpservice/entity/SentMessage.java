package com.pfe.smtpservice.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "sent_messages", indexes = {
        @Index(name = "idx_sent_campaign", columnList = "campaignId"),
        @Index(name = "idx_sent_tracking", columnList = "trackingId"),
        @Index(name = "idx_sent_rcpt", columnList = "rcptTo")
})
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SentMessage {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String mailFrom;
    private String rcptTo;
    private String subject;
    private String trackingId;
    private String campaignId;
    private String contactId;
    private String tag;
    private String mxHost;
    private int smtpResponseCode;
    private boolean sentWithDkim;
    private long deliveryTimeMs;
    private LocalDateTime sentAt;

    @Builder.Default
    private int attempts = 0;

    @Builder.Default
    private int maxAttempts = 18;
}