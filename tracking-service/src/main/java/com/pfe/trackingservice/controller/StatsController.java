package com.pfe.trackingservice.controller;

import com.pfe.trackingservice.dto.CampaignStatsResponse;
import com.pfe.trackingservice.service.StatsService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * API de statistiques et KPI — données pour le dashboard Angular.
 *
 * Fournit :
 *   - Stats par campagne (taux d'ouverture, clics, bounces...)
 *   - Stats globales
 *   - Stats du jour
 *   - Top liens cliqués
 *   - Timeline pour les graphiques
 */
@RestController
@RequestMapping("/api/tracking/stats")
@RequiredArgsConstructor
public class StatsController {

    private final StatsService statsService;

    /**
     * Statistiques complètes d'une campagne (KPI).
     * Utilisé pour le détail d'une campagne dans le dashboard.
     *
     * Retourne : taux d'ouverture, clic, bounce, unsub, etc.
     */
    @GetMapping("/campaign/{campaignId}")
    public ResponseEntity<CampaignStatsResponse> getCampaignStats(
            @PathVariable String campaignId) {
        return ResponseEntity.ok(statsService.getCampaignStats(campaignId));
    }

    /**
     * Statistiques globales (toutes campagnes).
     * Utilisé pour la page d'accueil du dashboard.
     */
    @GetMapping("/global")
    public ResponseEntity<Map<String, Object>> getGlobalStats() {
        return ResponseEntity.ok(statsService.getGlobalStats());
    }

    /**
     * Statistiques d'aujourd'hui.
     * Utilisé pour le widget "Aujourd'hui" du dashboard.
     */
    @GetMapping("/today")
    public ResponseEntity<Map<String, Object>> getTodayStats() {
        return ResponseEntity.ok(statsService.getTodayStats());
    }

    /**
     * Top liens cliqués pour une campagne.
     * Utilisé pour le classement des liens dans le rapport de campagne.
     */
    @GetMapping("/campaign/{campaignId}/top-links")
    public ResponseEntity<List<Map<String, Object>>> getTopLinks(
            @PathVariable String campaignId) {
        return ResponseEntity.ok(statsService.getTopClickedLinks(campaignId));
    }

    /**
     * Timeline des événements par heure (pour les graphiques time-series).
     * Utilisé pour le graphique d'activité dans le rapport de campagne.
     */
    @GetMapping("/campaign/{campaignId}/timeline")
    public ResponseEntity<List<Map<String, Object>>> getTimeline(
            @PathVariable String campaignId) {
        return ResponseEntity.ok(statsService.getCampaignTimeline(campaignId));
    }

    /**
     * Répartition des événements par type (pour les pie charts).
     * Utilisé pour le diagramme circulaire dans le rapport de campagne.
     */
    @GetMapping("/campaign/{campaignId}/breakdown")
    public ResponseEntity<Map<String, Long>> getBreakdown(
            @PathVariable String campaignId) {
        return ResponseEntity.ok(statsService.getCampaignEventBreakdown(campaignId));
    }
}
