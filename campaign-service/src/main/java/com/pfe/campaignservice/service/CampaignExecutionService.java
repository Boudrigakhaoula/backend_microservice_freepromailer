package com.pfe.campaignservice.service;

import com.pfe.campaignservice.client.SmtpServiceClient;
import com.pfe.campaignservice.dto.SendEmailRequest;
import com.pfe.campaignservice.entity.*;
import com.pfe.campaignservice.enums.CampaignStatus;
import com.pfe.campaignservice.enums.ContactStatus;
import com.pfe.campaignservice.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class CampaignExecutionService {

    private final CampaignRepository campaignRepo;
    private final ContactRepository contactRepo;
    private final SmtpServiceClient smtpClient;

    @Value("${campaign.sending.batch-size:50}")
    private int batchSize;

    @Value("${campaign.sending.delay-between-batch-ms:1000}")
    private long delayBetweenBatch;

    @Scheduled(fixedDelayString = "${campaign.scheduler.poll-interval-ms:30000}")
    @Transactional
    public void processScheduledCampaigns() {
        List<Campaign> readyCampaigns = campaignRepo
                .findByStatusAndScheduledAtBefore(CampaignStatus.SCHEDULED, LocalDateTime.now());

        for (Campaign campaign : readyCampaigns) {
            log.info("⏰ Campagne planifiée prête : #{} '{}'",
                    campaign.getId(), campaign.getName());
            executeCampaign(campaign);
        }
    }

    @Transactional
    public void executeCampaign(Campaign campaign) {
        if (campaign.getContactList() == null) {
            log.error("❌ Campagne #{} n'a pas de liste de contacts", campaign.getId());
            campaign.setStatus(CampaignStatus.PAUSED);
            campaignRepo.save(campaign);
            return;
        }

        campaign.setStatus(CampaignStatus.SENDING);
        campaignRepo.save(campaign);

        // ← CORRIGÉ : findByContactList_IdAndStatus
        List<Contact> activeContacts = contactRepo.findByContactList_IdAndStatus(
                campaign.getContactList().getId(), ContactStatus.ACTIVE);

        campaign.setTotalRecipients(activeContacts.size());

        log.info("📧 Envoi campagne '{}' à {} contacts",
                campaign.getName(), activeContacts.size());

        int sent = 0;
        int failed = 0;

        for (int i = 0; i < activeContacts.size(); i++) {
            Contact contact = activeContacts.get(i);

            try {
                sendToContact(campaign, contact);
                sent++;
            } catch (Exception e) {
                failed++;
                log.error("❌ Échec envoi à {} : {}", contact.getEmail(), e.getMessage());
            }

            if ((i + 1) % batchSize == 0 && delayBetweenBatch > 0) {
                try {
                    log.info("⏸ Pause {}ms après batch de {} emails...",
                            delayBetweenBatch, batchSize);
                    Thread.sleep(delayBetweenBatch);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    break;
                }
            }
        }

        campaign.setTotalSent(sent);
        campaign.setTotalFailed(failed);
        campaign.setStatus(CampaignStatus.SENT);
        campaign.setSentAt(LocalDateTime.now());
        campaignRepo.save(campaign);

        log.info("✅ Campagne '{}' terminée : {}/{} envoyés, {} échoués",
                campaign.getName(), sent, activeContacts.size(), failed);
    }

    private void sendToContact(Campaign campaign, Contact contact) {
        String personalizedSubject = personalizeContent(campaign.getSubject(), contact);
        String personalizedHtml = personalizeContent(campaign.getHtmlContent(), contact);
        String personalizedText = personalizeContent(campaign.getTextContent(), contact);

        SendEmailRequest request = SendEmailRequest.builder()
                .from(campaign.getFromEmail())
                .to(contact.getEmail())
                .subject(personalizedSubject)
                .htmlBody(personalizedHtml)
                .body(personalizedText)
                .campaignId(campaign.getId().toString())
                .contactId(contact.getId().toString())
                .tag(campaign.getTag())
                .build();

        smtpClient.sendCampaignEmail(request);

        log.debug("✉️  Envoyé à {} pour campagne '{}'",
                contact.getEmail(), campaign.getName());
    }

    private String personalizeContent(String content, Contact contact) {
        if (content == null || content.isBlank()) return "";

        return content
                .replace("{{firstName}}", Optional.ofNullable(contact.getFirstName()).orElse(""))
                .replace("{{lastName}}", Optional.ofNullable(contact.getLastName()).orElse(""))
                .replace("{{email}}", contact.getEmail())
                .replace("{{company}}", Optional.ofNullable(contact.getCompany()).orElse(""))
                .replace("{{fullName}}", contact.getFullName())
                .replace("{{phone}}", Optional.ofNullable(contact.getPhone()).orElse(""));
    }
}