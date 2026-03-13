package com.pfe.campaignservice.repository;

import com.pfe.campaignservice.entity.Attachment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface AttachmentRepository extends JpaRepository<Attachment, Long> {
    List<Attachment> findByCampaign_Id(Long campaignId);
    void deleteByCampaign_Id(Long campaignId);
}