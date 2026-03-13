package com.pfe.smtpservice.dto;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.util.List;

/**
 * DTO reçu depuis le campaign-service via Feign.
 * Doit correspondre exactement aux champs de SendEmailRequest du campaign-service.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class SendEmailRequest {

    private String from;
    private String to;
    private String subject;
    private String body;
    private String htmlBody;
    private String campaignId;
    private String contactId;
    private String tag;
    private List<String> attachmentPaths;

    /** userId du sender — propagé depuis campaign-service pour l'équilibrage */
    private String senderId;
}