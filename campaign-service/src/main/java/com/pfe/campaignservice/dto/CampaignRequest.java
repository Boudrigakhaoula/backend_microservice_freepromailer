package com.pfe.campaignservice.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.*;
import org.springframework.web.multipart.MultipartFile;
import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CampaignRequest {

    @NotBlank(message = "Le nom de la campagne est obligatoire")
    private String name;

    @NotBlank(message = "Le sujet est obligatoire")
    private String subject;

    private String htmlContent;
    private String textContent;
    private String fromName;
    private String fromEmail;
    private String replyTo;
    private Long contactListId;
    private Long templateId;
    private LocalDateTime scheduledAt;
    private String tag;

    // ← NOUVEAU : pièces jointes (multipart)
    private List<MultipartFile> attachments;
}