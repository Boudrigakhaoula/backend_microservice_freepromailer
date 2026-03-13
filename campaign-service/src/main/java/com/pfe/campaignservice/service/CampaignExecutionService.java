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
    private final AttachmentService attachmentService;

    @Value("${campaign.sending.batch-size:50}")
    private int batchSize;

    @Value("${campaign.sending.delay-between-batch-ms:1000}")
    private long delayBetweenBatch;

    @Scheduled(fixedDelayString = "${campaign.scheduler.poll-interval-ms:30000}")
    @Transactional
    public void processScheduledCampaigns() {
        List<Campaign> readyCampaigns = campaignRepo
                .findByStatusAndScheduledAtBefore(CampaignStatus.PENDING, LocalDateTime.now());

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
            campaign.setStatus(CampaignStatus.FAILED);
            campaignRepo.save(campaign);
            return;
        }

        campaign.setStatus(CampaignStatus.IN_PROGRESS);
        campaignRepo.save(campaign);

        List<Contact> activeContacts = contactRepo.findByContactList_IdAndStatus(
                campaign.getContactList().getId(), ContactStatus.ACTIVE);

        campaign.setTotalRecipients(activeContacts.size());

        log.info("📧 Envoi campagne '{}' à {} contacts",
                campaign.getName(), activeContacts.size());

        int sent = 0;
        int failed = 0;

        for (int i = 0; i < activeContacts.size(); i++) {
            // ── PAUSE CHECK : si la campagne a été mise en pause entre-temps ──
            if (isCampaignPaused(campaign.getId())) {
                log.info("⏸ Campagne #{} mise en pause après {} envois", campaign.getId(), sent);
                campaign.setTotalSent(campaign.getTotalSent() + sent);
                campaign.setTotalFailed(campaign.getTotalFailed() + failed);
                campaignRepo.save(campaign);
                return;
            }

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
        campaign.setStatus(CampaignStatus.COMPLETED);
        campaign.setSentAt(LocalDateTime.now());
        campaignRepo.save(campaign);

        log.info("✅ Campagne '{}' terminée : {}/{} envoyés, {} échoués",
                campaign.getName(), sent, activeContacts.size(), failed);
    }

    /**
     * RESUME — reprend depuis l'index totalSent (skip les contacts déjà traités).
     */
    @Transactional
    public void resumeCampaign(Campaign campaign) {
        if (campaign.getContactList() == null) {
            throw new IllegalStateException("La campagne n'a pas de liste de contacts.");
        }

        campaign.setStatus(CampaignStatus.IN_PROGRESS);
        campaignRepo.save(campaign);

        List<Contact> activeContacts = contactRepo.findByContactList_IdAndStatus(
                campaign.getContactList().getId(), ContactStatus.ACTIVE);

        // Reprendre à partir du nombre déjà envoyé (skip les premiers)
        int alreadySent   = campaign.getTotalSent();
        int alreadyFailed = campaign.getTotalFailed();

        if (alreadySent >= activeContacts.size()) {
            log.info("✅ Campagne #{} déjà complète ({}/{} contacts), passage à SENT",
                    campaign.getId(), alreadySent, activeContacts.size());
            campaign.setStatus(CampaignStatus.COMPLETED);
            campaign.setSentAt(LocalDateTime.now());
            campaignRepo.save(campaign);
            return;
        }

        List<Contact> remaining = activeContacts.subList(alreadySent, activeContacts.size());
        log.info("▶ Reprise campagne '{}' : {} contacts restants (déjà traités : {})",
                campaign.getName(), remaining.size(), alreadySent);

        int sent = 0;
        int failed = 0;

        for (int i = 0; i < remaining.size(); i++) {
            // ── PAUSE CHECK ──
            if (isCampaignPaused(campaign.getId())) {
                log.info("⏸ Campagne #{} mise en pause après {} envois supplémentaires",
                        campaign.getId(), sent);
                campaign.setTotalSent(alreadySent + sent);
                campaign.setTotalFailed(alreadyFailed + failed);
                campaignRepo.save(campaign);
                return;
            }

            Contact contact = remaining.get(i);
            try {
                sendToContact(campaign, contact);
                sent++;
            } catch (Exception e) {
                failed++;
                log.error("❌ Échec envoi à {} : {}", contact.getEmail(), e.getMessage());
            }

            if ((i + 1) % batchSize == 0 && delayBetweenBatch > 0) {
                try {
                    Thread.sleep(delayBetweenBatch);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    break;
                }
            }
        }

        campaign.setTotalSent(alreadySent + sent);
        campaign.setTotalFailed(alreadyFailed + failed);
        campaign.setStatus(CampaignStatus.COMPLETED);
        campaign.setSentAt(LocalDateTime.now());
        campaignRepo.save(campaign);

        log.info("✅ Campagne '{}' terminée après reprise : {}/{} envoyés total",
                campaign.getName(), alreadySent + sent, activeContacts.size());
    }

    /**
     * Vérifie en DB si la campagne a été mise en pause ou annulée pendant l'exécution.
     * Appelé à chaque batch pour détecter une interruption externe.
     */
    private boolean isCampaignPaused(Long campaignId) {
        return campaignRepo.findById(campaignId)
                .map(c -> c.getStatus() == CampaignStatus.PAUSED
                        || c.getStatus() == CampaignStatus.CANCELLED)
                .orElse(false);
    }

    private void sendToContact(Campaign campaign, Contact contact) {
        String personalizedSubject = personalizeContent(campaign.getSubject(), contact);
        String personalizedHtml    = personalizeContent(campaign.getHtmlContent(), contact);
        String personalizedText    = personalizeContent(campaign.getTextContent(), contact);

        List<String> attachmentPaths = attachmentService.getPathsByCampaign(campaign.getId());

        SendEmailRequest request = SendEmailRequest.builder()
                .from(campaign.getFromEmail())
                .to(contact.getEmail())
                .subject(personalizedSubject)
                .htmlBody(personalizedHtml)
                .body(personalizedText)
                .campaignId(campaign.getId().toString())
                .contactId(contact.getId().toString())
                .tag(campaign.getTag())
                .senderId(campaign.getUserId() != null ? campaign.getUserId().toString() : null)
                .attachmentPaths(attachmentPaths.isEmpty() ? null : attachmentPaths)
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