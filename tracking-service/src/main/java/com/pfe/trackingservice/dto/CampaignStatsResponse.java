package com.pfe.trackingservice.dto;



import lombok.*;

/**
 * Statistiques de tracking pour une campagne.
 * Utilisé par le dashboard Angular pour afficher les KPI.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CampaignStatsResponse {

    private String campaignId;

    // ─── Compteurs bruts ───
    private long totalSent;
    private long totalDelivered;
    private long totalOpens;
    private long uniqueOpens;
    private long totalClicks;
    private long uniqueClicks;
    private long totalBounces;
    private long hardBounces;
    private long softBounces;
    private long totalUnsubscribes;
    private long totalComplaints;

    // ─── Taux (pourcentages) ───
    private double openRate;          // uniqueOpens / totalSent * 100
    private double clickRate;         // uniqueClicks / totalSent * 100
    private double clickToOpenRate;   // uniqueClicks / uniqueOpens * 100
    private double bounceRate;        // totalBounces / totalSent * 100
    private double unsubscribeRate;   // totalUnsubscribes / totalSent * 100
    private double deliveryRate;      // totalDelivered / totalSent * 100
}
