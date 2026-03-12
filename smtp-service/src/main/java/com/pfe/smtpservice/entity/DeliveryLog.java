package com.pfe.smtpservice.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "delivery_logs", indexes = {
        @Index(name = "idx_dlog_tracking", columnList = "trackingId"),
        @Index(name = "idx_dlog_campaign", columnList = "campaignId"),
        @Index(name = "idx_dlog_status", columnList = "status")
})
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DeliveryLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Long messageId;
    private String trackingId;
    private String campaignId;
    private String recipientEmail;
    private String status;
    private String mxHost;
    private int smtpResponseCode;

    @Column(columnDefinition = "TEXT")
    private String detail;

    private long deliveryTimeMs;
    private int attemptNumber;
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        if (createdAt == null) createdAt = LocalDateTime.now();
    }
}