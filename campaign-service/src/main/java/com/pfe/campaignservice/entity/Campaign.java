package com.pfe.campaignservice.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.pfe.campaignservice.enums.CampaignStatus;
import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "campaigns", indexes = {
        @Index(name = "idx_campaign_status", columnList = "status"),
        @Index(name = "idx_campaign_scheduled", columnList = "status, scheduledAt")
})
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Campaign {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    private String subject;

    @Column(columnDefinition = "TEXT")
    private String htmlContent;

    @Column(columnDefinition = "TEXT")
    private String textContent;

    @Enumerated(EnumType.STRING)
    @Builder.Default
    private CampaignStatus status = CampaignStatus.DRAFT;

    private LocalDateTime scheduledAt;
    private LocalDateTime sentAt;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    private String fromName;
    private String fromEmail;
    private String replyTo;

    /**
     * FIX : @JsonIgnore sur les relations LAZY.
     * On expose uniquement l'ID et le nom via @JsonProperty.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "contact_list_id")
    @JsonIgnore
    private ContactList contactList;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "template_id")
    @JsonIgnore
    private EmailTemplate template;

    private String tag;

    @Builder.Default
    private int totalRecipients = 0;

    @Builder.Default
    private int totalSent = 0;

    @Builder.Default
    private int totalFailed = 0;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

    // ─── Champs virtuels exposés dans le JSON ───

    @JsonProperty("contactListId")
    public Long getContactListId() {
        return contactList != null ? contactList.getId() : null;
    }

    @JsonProperty("contactListName")
    public String getContactListName() {
        try {
            return contactList != null ? contactList.getName() : null;
        } catch (Exception e) {
            return null;
        }
    }

    @JsonProperty("templateId")
    public Long getTemplateId() {
        return template != null ? template.getId() : null;
    }

    @JsonProperty("templateName")
    public String getTemplateName() {
        try {
            return template != null ? template.getName() : null;
        } catch (Exception e) {
            return null;
        }
    }
}