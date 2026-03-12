package com.pfe.campaignservice.dto;


import lombok.*;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CampaignStatsResponse {

    private Long campaignId;
    private String campaignName;
    private String status;

    private int totalRecipients;
    private int totalSent;
    private int totalFailed;

    private double sendRate;       // % envoyés
    private double failRate;       // % échoués
}
