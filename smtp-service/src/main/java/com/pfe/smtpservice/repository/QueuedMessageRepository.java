package com.pfe.smtpservice.repository;

import com.pfe.smtpservice.entity.QueuedMessage;
import com.pfe.smtpservice.enums.MessageStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface QueuedMessageRepository extends JpaRepository<QueuedMessage, Long> {

    // ═══════════════════════════════════════════════════════════
    //  FAIR SCHEDULER — cœur de l'algorithme
    // ═══════════════════════════════════════════════════════════

    /**
     * Étape 1 : compter les messages PENDING par sender.
     * Retourne [[senderId, count], ...] triés par count ASC (petits volumes en premier).
     */
    @Query(value = """
        SELECT COALESCE(sender_id, 'anonymous') AS sender_id,
               COUNT(*) AS pending_count
        FROM queued_messages
        WHERE status = 'QUEUED'
          AND locked_by IS NULL
          AND (retry_after IS NULL OR retry_after <= :now)
        GROUP BY COALESCE(sender_id, 'anonymous')
        ORDER BY pending_count ASC
    """, nativeQuery = true)
    List<Object[]> countPendingBySender(@Param("now") LocalDateTime now);

    /**
     * Étape 2 : prendre N messages d'un sender spécifique.
     * FOR UPDATE SKIP LOCKED = multi-worker safe.
     */
    @Query(value = """
        SELECT * FROM queued_messages
        WHERE status = 'QUEUED'
          AND COALESCE(sender_id, 'anonymous') = :senderId
          AND locked_by IS NULL
          AND (retry_after IS NULL OR retry_after <= :now)
        ORDER BY created_at ASC
        LIMIT :quota
        FOR UPDATE SKIP LOCKED
    """, nativeQuery = true)
    List<QueuedMessage> pickMessagesForSender(
            @Param("senderId") String senderId,
            @Param("quota")    int quota,
            @Param("now")      LocalDateTime now);

    /**
     * Quota journalier : compte les emails envoyés depuis minuit.
     */
    @Query(value = """
        SELECT COUNT(*) FROM queued_messages
        WHERE status = 'SENT'
          AND sent_at >= :startOfDay
    """, nativeQuery = true)
    long countSentToday(@Param("startOfDay") LocalDateTime startOfDay);

    // ═══════════════════════════════════════════════════════════
    //  LOCK RECOVERY
    // ═══════════════════════════════════════════════════════════

    List<QueuedMessage> findByStatusAndLockedAtBefore(
            MessageStatus status, LocalDateTime threshold);

    // ═══════════════════════════════════════════════════════════
    //  LECTURE STANDARD
    // ═══════════════════════════════════════════════════════════

    List<QueuedMessage> findByStatus(MessageStatus status);

    List<QueuedMessage> findByCampaignId(String campaignId);

    Optional<QueuedMessage> findByTrackingId(String trackingId);

    long countByStatus(MessageStatus status);

    void deleteByStatus(MessageStatus status);

    @Query("SELECT m.status, COUNT(m) FROM QueuedMessage m GROUP BY m.status")
    List<Object[]> countByStatusGrouped();
}