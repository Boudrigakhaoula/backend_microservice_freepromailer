package com.pfe.trackingservice.dto;

import com.pfe.trackingservice.enums.EventType;
import lombok.*;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EventResponse {

    private Long id;
    private String trackingId;
    private String campaignId;
    private String contactId;
    private String recipientEmail;
    private EventType type;
    private String url;
    private String details;
    private String userAgent;
    private String ipAddress;
    private LocalDateTime timestamp;
}
