package com.pfe.trackingservice.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.*;

/**
 * Payload reçu du smtp-service quand un email bounce.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BounceRequest {

    @NotBlank(message = "trackingId est obligatoire")
    private String trackingId;

    private String campaignId;
    private String contactId;
    private String recipientEmail;
    private String reason;
    private String smtpResponse;
    private boolean hardBounce;
}
