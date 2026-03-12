package com.pfe.trackingservice.service;


import com.pfe.trackingservice.dto.CampaignStatsResponse;
import com.pfe.trackingservice.enums.EventType;
import com.pfe.trackingservice.repository.TrackingEventRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.*;

/**
 * Service de calcul des statistiques et KPI.
 * Fournit les données pour le dashboard Angular.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class StatsService {

    private final TrackingEventRepository repo;

    /**
     * Statistiques complètes pour une campagne.
     * Inspiré de Postal server stats dashboard.
     */
    public CampaignStatsResponse getCampaignStats(String campaignId) {
        long totalSent = repo.countByCampaignIdAndType(campaignId, EventType.SENT);
        long totalDelivered = repo.countByCampaignIdAndType(campaignId, EventType.DELIVERED);
        long totalOpens = repo.countByCampaignIdAndType(campaignId, EventType.OPEN);
        long uniqueOpens = repo.countUniqueByCampaignIdAndType(campaignId, EventType.OPEN);
        long totalClicks = repo.countByCampaignIdAndType(campaignId, EventType.CLICK);
        long uniqueClicks = repo.countUniqueByCampaignIdAndType(campaignId, EventType.CLICK);
        long hardBounces = repo.countByCampaignIdAndType(campaignId, EventType.HARD_BOUNCE);
        long softBounces = repo.countByCampaignIdAndType(campaignId, EventType.SOFT_BOUNCE);
        long totalBounces = repo.countByCampaignIdAndType(campaignId, EventType.BOUNCE);
        long totalUnsubscribes = repo.countByCampaignIdAndType(campaignId, EventType.UNSUBSCRIBE);
        long totalComplaints = repo.countByCampaignIdAndType(campaignId, EventType.COMPLAINT);

        // Calcul des taux
        double openRate = totalSent > 0 ? (double) uniqueOpens / totalSent * 100 : 0;
        double clickRate = totalSent > 0 ? (double) uniqueClicks / totalSent * 100 : 0;
        double clickToOpenRate = uniqueOpens > 0 ? (double) uniqueClicks / uniqueOpens * 100 : 0;
        double bounceRate = totalSent > 0 ? (double) totalBounces / totalSent * 100 : 0;
        double unsubscribeRate = totalSent > 0 ? (double) totalUnsubscribes / totalSent * 100 : 0;
        double deliveryRate = totalSent > 0 ? (double) totalDelivered / totalSent * 100 : 0;

        return CampaignStatsResponse.builder()
                .campaignId(campaignId)
                .totalSent(totalSent)
                .totalDelivered(totalDelivered)
                .totalOpens(totalOpens)
                .uniqueOpens(uniqueOpens)
                .totalClicks(totalClicks)
                .uniqueClicks(uniqueClicks)
                .totalBounces(totalBounces)
                .hardBounces(hardBounces)
                .softBounces(softBounces)
                .totalUnsubscribes(totalUnsubscribes)
                .totalComplaints(totalComplaints)
                .openRate(Math.round(openRate * 100.0) / 100.0)
                .clickRate(Math.round(clickRate * 100.0) / 100.0)
                .clickToOpenRate(Math.round(clickToOpenRate * 100.0) / 100.0)
                .bounceRate(Math.round(bounceRate * 100.0) / 100.0)
                .unsubscribeRate(Math.round(unsubscribeRate * 100.0) / 100.0)
                .deliveryRate(Math.round(deliveryRate * 100.0) / 100.0)
                .build();
    }

    /**
     * Statistiques globales (toutes campagnes confondues).
     */
    public Map<String, Object> getGlobalStats() {
        Map<String, Object> stats = new LinkedHashMap<>();
        stats.put("totalSent", repo.countByType(EventType.SENT));
        stats.put("totalOpens", repo.countByType(EventType.OPEN));
        stats.put("totalClicks", repo.countByType(EventType.CLICK));
        stats.put("totalBounces", repo.countByType(EventType.BOUNCE));
        stats.put("totalUnsubscribes", repo.countByType(EventType.UNSUBSCRIBE));
        stats.put("totalComplaints", repo.countByType(EventType.COMPLAINT));
        return stats;
    }

    /**
     * Statistiques d'aujourd'hui.
     */
    public Map<String, Object> getTodayStats() {
        LocalDateTime startOfDay = LocalDate.now().atStartOfDay();
        LocalDateTime endOfDay = LocalDate.now().atTime(LocalTime.MAX);

        Map<String, Object> stats = new LinkedHashMap<>();
        stats.put("date", LocalDate.now().toString());
        stats.put("sent", repo.countByTypeAndPeriod(EventType.SENT, startOfDay, endOfDay));
        stats.put("opens", repo.countByTypeAndPeriod(EventType.OPEN, startOfDay, endOfDay));
        stats.put("clicks", repo.countByTypeAndPeriod(EventType.CLICK, startOfDay, endOfDay));
        stats.put("bounces", repo.countByTypeAndPeriod(EventType.BOUNCE, startOfDay, endOfDay));
        stats.put("unsubscribes", repo.countByTypeAndPeriod(EventType.UNSUBSCRIBE, startOfDay, endOfDay));
        return stats;
    }

    /**
     * Top liens cliqués pour une campagne.
     */
    public List<Map<String, Object>> getTopClickedLinks(String campaignId) {
        List<Map<String, Object>> result = new ArrayList<>();
        for (Object[] row : repo.findTopClickedLinks(campaignId)) {
            Map<String, Object> link = new LinkedHashMap<>();
            link.put("url", row[0]);
            link.put("clicks", row[1]);
            result.add(link);
        }
        return result;
    }

    /**
     * Timeline des événements par heure (pour les graphiques).
     */
    public List<Map<String, Object>> getCampaignTimeline(String campaignId) {
        List<Map<String, Object>> result = new ArrayList<>();
        for (Object[] row : repo.findTimelineForCampaign(campaignId)) {
            Map<String, Object> point = new LinkedHashMap<>();
            point.put("hour", row[0] != null ? row[0].toString() : "");
            point.put("type", row[1] != null ? row[1].toString() : "");
            point.put("count", row[2]);
            result.add(point);
        }
        return result;
    }

    /**
     * Stats groupées par type pour une campagne (pour les pie charts).
     */
    public Map<String, Long> getCampaignEventBreakdown(String campaignId) {
        Map<String, Long> breakdown = new LinkedHashMap<>();
        for (Object[] row : repo.countGroupedByTypeForCampaign(campaignId)) {
            breakdown.put(row[0].toString(), (Long) row[1]);
        }
        return breakdown;
    }
}
