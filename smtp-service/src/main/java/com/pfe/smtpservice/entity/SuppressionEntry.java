package com.pfe.smtpservice.entity;

import com.pfe.smtpservice.enums.SuppressionType;
import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

/**
 * Entrée dans la suppression list.
 *
 * CORRECTION : SuppressionType est maintenant un enum EXTERNE
 * dans le package enums (plus d'enum interne).
 */
@Entity
@Table(name = "suppression_list", indexes = {
        @Index(name = "idx_suppression_email", columnList = "email"),
        @Index(name = "idx_suppression_keep", columnList = "keepUntil")
})
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SuppressionEntry {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String email;

    private String reason;

    @Enumerated(EnumType.STRING)
    @Builder.Default
    private SuppressionType type = SuppressionType.RECIPIENT;

    private LocalDateTime createdAt;
    private LocalDateTime keepUntil;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        if (keepUntil == null) keepUntil = LocalDateTime.now().plusDays(30);
    }

    public boolean isExpired() {
        return keepUntil.isBefore(LocalDateTime.now());
    }
}