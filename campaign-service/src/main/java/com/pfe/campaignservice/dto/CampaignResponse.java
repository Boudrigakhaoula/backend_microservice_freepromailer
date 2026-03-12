package com.pfe.campaignservice.dto;

import lombok.*;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CampaignResponse {
    private Long id;
    private String name;
    private String subject;
    private String status;
    private String fromName;
    private String fromEmail;
    private String replyTo;
    private String tag;
    private Long contactListId;
    private String contactListName;
    private Long templateId;
    private String templateName;
    private long totalContacts;
    private int totalRecipients;
    private int totalSent;
    private int totalFailed;
    private LocalDateTime scheduledAt;
    private LocalDateTime sentAt;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}