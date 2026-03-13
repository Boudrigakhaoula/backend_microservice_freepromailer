package com.pfe.campaignservice.dto;


import lombok.*;

/**
 * Payload envoyé au smtp-service via Feign.
 */
@Data
@Builder
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

    /** userId du sender — utilisé par le fair scheduler du smtp-service */
    private String senderId;

    /** Chemins des pièces jointes sur le filesystem */
    private java.util.List<String> attachmentPaths;
}