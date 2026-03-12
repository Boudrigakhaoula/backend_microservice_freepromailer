package com.pfe.smtpservice.repository;

import com.pfe.smtpservice.entity.QueuedMessage;
import com.pfe.smtpservice.enums.MessageStatus;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface QueuedMessageRepository extends JpaRepository<QueuedMessage, Long> {

    @Query("""
        SELECT m FROM QueuedMessage m
        WHERE m.status = :status
          AND (m.retryAfter IS NULL OR m.retryAfter <= :now)
          AND m.lockedBy IS NULL
        ORDER BY m.createdAt ASC
    """)
    List<QueuedMessage> findMessagesReadyToSend(
            @Param("status") MessageStatus status,
            @Param("now") LocalDateTime now,
            Pageable pageable);

    List<QueuedMessage> findByStatusAndLockedAtBefore(
            MessageStatus status, LocalDateTime threshold);

    List<QueuedMessage> findByStatus(MessageStatus status);

    List<QueuedMessage> findByCampaignId(String campaignId);

    Optional<QueuedMessage> findByTrackingId(String trackingId);

    long countByStatus(MessageStatus status);

    void deleteByStatus(MessageStatus status);

    /**
     * Compte les messages groupés par statut.
     * Retourne : [["QUEUED", 5], ["SENDING", 1], ["FAILED", 2]]
     */
    @Query("SELECT m.status, COUNT(m) FROM QueuedMessage m GROUP BY m.status")
    List<Object[]> countByStatusGrouped();
}