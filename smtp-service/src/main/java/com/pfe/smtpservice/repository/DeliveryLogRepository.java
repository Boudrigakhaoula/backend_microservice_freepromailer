package com.pfe.smtpservice.repository;

import com.pfe.smtpservice.entity.DeliveryLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface DeliveryLogRepository extends JpaRepository<DeliveryLog, Long> {

    List<DeliveryLog> findByTrackingId(String trackingId);

    List<DeliveryLog> findByCampaignId(String campaignId);

    List<DeliveryLog> findByRecipientEmail(String email);

    List<DeliveryLog> findByStatus(String status);

    List<DeliveryLog> findByMessageId(Long messageId);
}