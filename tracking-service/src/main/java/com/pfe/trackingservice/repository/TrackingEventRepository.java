package com.pfe.trackingservice.repository;


import com.pfe.trackingservice.enums.EventType;
import com.pfe.trackingservice.entity.TrackingEvent;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface TrackingEventRepository extends JpaRepository<TrackingEvent, Long> {

    // ─── Par trackingId ───
    List<TrackingEvent> findByTrackingIdOrderByTimestampDesc(String trackingId);

    // ─── Par campagne ───
    List<TrackingEvent> findByCampaignIdOrderByTimestampDesc(String campaignId);

    List<TrackingEvent> findByCampaignIdAndType(String campaignId, EventType type);

    // ─── Compteurs par campagne et type ───
    long countByCampaignIdAndType(String campaignId, EventType type);

    // ─── Compteurs de clics/opens uniques (par trackingId distinct) ───
    @Query("""
        SELECT COUNT(DISTINCT e.trackingId) FROM TrackingEvent e
        WHERE e.campaignId = :campaignId AND e.type = :type
    """)
    long countUniqueByCampaignIdAndType(
            @Param("campaignId") String campaignId,
            @Param("type") EventType type);

    // ─── Par recipient ───
    List<TrackingEvent> findByRecipientEmailOrderByTimestampDesc(String recipientEmail);

    // ─── Par type ───
    List<TrackingEvent> findByTypeOrderByTimestampDesc(EventType type);

    // ─── Événements récents ───
    @Query("""
        SELECT e FROM TrackingEvent e
        ORDER BY e.timestamp DESC
        LIMIT :limit
    """)
    List<TrackingEvent> findLatestEvents(@Param("limit") int limit);

    // ─── Vérifier si un trackingId a déjà un événement de ce type ───
    boolean existsByTrackingIdAndType(String trackingId, EventType type);

    // ─── Compteur global par type ───
    long countByType(EventType type);

    // ─── Compteurs par période ───
    @Query("""
        SELECT COUNT(e) FROM TrackingEvent e
        WHERE e.type = :type
          AND e.timestamp BETWEEN :start AND :end
    """)
    long countByTypeAndPeriod(
            @Param("type") EventType type,
            @Param("start") LocalDateTime start,
            @Param("end") LocalDateTime end);

    // ─── Stats groupées par type pour une campagne ───
    @Query("""
        SELECT e.type, COUNT(e) FROM TrackingEvent e
        WHERE e.campaignId = :campaignId
        GROUP BY e.type
    """)
    List<Object[]> countGroupedByTypeForCampaign(@Param("campaignId") String campaignId);

    // ─── Nettoyage des vieux événements ───
    @Modifying
    @Query("DELETE FROM TrackingEvent e WHERE e.timestamp < :before")
    int deleteOlderThan(@Param("before") LocalDateTime before);

    // ─── Top liens cliqués pour une campagne ───
    @Query("""
        SELECT e.url, COUNT(e) as cnt FROM TrackingEvent e
        WHERE e.campaignId = :campaignId AND e.type = 'CLICK' AND e.url IS NOT NULL
        GROUP BY e.url
        ORDER BY cnt DESC
    """)
    List<Object[]> findTopClickedLinks(@Param("campaignId") String campaignId);

    // ─── Timeline des événements par heure pour une campagne ───
    @Query(value = """
        SELECT DATE_TRUNC('hour', e.timestamp) as hour, e.type, COUNT(*)
        FROM tracking_events e
        WHERE e.campaign_id = :campaignId
        GROUP BY DATE_TRUNC('hour', e.timestamp), e.type
        ORDER BY hour
    """, nativeQuery = true)
    List<Object[]> findTimelineForCampaign(@Param("campaignId") String campaignId);
}
