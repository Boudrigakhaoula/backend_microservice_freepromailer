package com.pfe.smtpservice.repository;

import com.pfe.smtpservice.entity.SentMessage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface SentMessageRepository extends JpaRepository<SentMessage, Long> {

    List<SentMessage> findByCampaignId(String campaignId);

    Optional<SentMessage> findByTrackingId(String trackingId);

    List<SentMessage> findByRcptTo(String rcptTo);

    long countByCampaignId(String campaignId);
}