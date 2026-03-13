package com.pfe.campaignservice.service;

import com.pfe.campaignservice.dto.CampaignRequest;
import com.pfe.campaignservice.dto.CampaignResponse;
import com.pfe.campaignservice.entity.*;
import com.pfe.campaignservice.enums.CampaignStatus;
import com.pfe.campaignservice.exception.ResourceNotFoundException;
import com.pfe.campaignservice.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class CampaignService {

    private final CampaignRepository campaignRepo;
    private final ContactListRepository contactListRepo;
    private final EmailTemplateRepository templateRepo;
    private final ContactRepository contactRepo;
    private final CampaignExecutionService executionService;

    /**
     * FIX : Utilise findAllWithRelations() → JOIN FETCH
     * Les relations sont DÉJÀ chargées → pas de LazyInit.
     */
    public List<CampaignResponse> getAll() {
        return campaignRepo.findAllWithRelations().stream()
                .map(this::toResponse)
                .toList();
    }

    public CampaignResponse getById(Long id) {
        Campaign campaign = campaignRepo.findByIdWithRelations(id)
                .orElseThrow(() -> new ResourceNotFoundException("Campagne", id));
        return toResponse(campaign);
    }

    public List<CampaignResponse> getByStatus(CampaignStatus status) {
        return campaignRepo.findByStatusWithRelations(status).stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional
    public CampaignResponse create(CampaignRequest request) {
        Campaign campaign = Campaign.builder()
                .name(request.getName())
                .subject(request.getSubject())
                .htmlContent(request.getHtmlContent())
                .textContent(request.getTextContent())
                .fromName(request.getFromName())
                .fromEmail(request.getFromEmail())
                .replyTo(request.getReplyTo())
                .tag(request.getTag())
                .status(CampaignStatus.DRAFT)
                .build();

        if (request.getContactListId() != null) {
            ContactList list = contactListRepo.findById(request.getContactListId())
                    .orElseThrow(() -> new ResourceNotFoundException(
                            "Liste de contacts", request.getContactListId()));
            campaign.setContactList(list);
        }

        if (request.getTemplateId() != null) {
            EmailTemplate template = templateRepo.findById(request.getTemplateId())
                    .orElseThrow(() -> new ResourceNotFoundException(
                            "Template", request.getTemplateId()));
            campaign.setTemplate(template);

            if (campaign.getHtmlContent() == null || campaign.getHtmlContent().isBlank()) {
                campaign.setHtmlContent(template.getHtmlContent());
            }
            if (campaign.getTextContent() == null || campaign.getTextContent().isBlank()) {
                campaign.setTextContent(template.getTextContent());
            }
            if (campaign.getSubject() == null || campaign.getSubject().isBlank()) {
                campaign.setSubject(template.getSubject());
            }
        }

        Campaign saved = campaignRepo.save(campaign);
        log.info("✅ Campagne créée : #{} '{}'", saved.getId(), saved.getName());
        return toResponse(saved);
    }

    @Transactional
    public CampaignResponse update(Long id, CampaignRequest request) {
        Campaign campaign = campaignRepo.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Campagne", id));

        if (campaign.getStatus() != CampaignStatus.DRAFT) {
            throw new IllegalStateException(
                    "Impossible de modifier une campagne avec le statut : " + campaign.getStatus());
        }

        campaign.setName(request.getName());
        campaign.setSubject(request.getSubject());
        campaign.setHtmlContent(request.getHtmlContent());
        campaign.setTextContent(request.getTextContent());
        campaign.setFromName(request.getFromName());
        campaign.setFromEmail(request.getFromEmail());
        campaign.setReplyTo(request.getReplyTo());
        campaign.setTag(request.getTag());

        if (request.getContactListId() != null) {
            ContactList list = contactListRepo.findById(request.getContactListId())
                    .orElseThrow(() -> new ResourceNotFoundException(
                            "Liste de contacts", request.getContactListId()));
            campaign.setContactList(list);
        }

        if (request.getTemplateId() != null) {
            EmailTemplate template = templateRepo.findById(request.getTemplateId())
                    .orElseThrow(() -> new ResourceNotFoundException(
                            "Template", request.getTemplateId()));
            campaign.setTemplate(template);
        }

        return toResponse(campaignRepo.save(campaign));
    }

    @Transactional
    public void delete(Long id) {
        campaignRepo.deleteById(id);
    }

    // ═══════════════════════════════════════════════════════════
    //  LIFECYCLE — start / restart / pause / resume / cancel
    // ═══════════════════════════════════════════════════════════

    /**
     * START — DRAFT ou PENDING → lance l'exécution immédiatement.
     * Si la campagne est COMPLETED, FAILED ou CANCELLED, réinitialise les compteurs
     * et relance (équivalent d'un restart automatique).
     */
    @Transactional
    public CampaignResponse startCampaign(Long id) {
        Campaign campaign = campaignRepo.findByIdWithRelations(id)
                .orElseThrow(() -> new ResourceNotFoundException("Campagne", id));

        if (campaign.getContactList() == null) {
            throw new IllegalStateException(
                    "La campagne n'a pas de liste de contacts. Assignez-en une avant de démarrer.");
        }
        if (campaign.getSubject() == null || campaign.getSubject().isBlank()) {
            throw new IllegalStateException(
                    "La campagne n'a pas de sujet (subject). Remplissez-le avant de démarrer.");
        }

        // Si campagne déjà terminée/annulée/échouée → reset et relance
        if (campaign.getStatus() == CampaignStatus.COMPLETED
                || campaign.getStatus() == CampaignStatus.CANCELLED
                || campaign.getStatus() == CampaignStatus.FAILED) {
            log.info("🔄 Restart automatique campagne #{} '{}' (statut: {})",
                    campaign.getId(), campaign.getName(), campaign.getStatus());
            resetCampaignCounters(campaign);
        } else if (campaign.getStatus() != CampaignStatus.DRAFT
                && campaign.getStatus() != CampaignStatus.PENDING) {
            throw new IllegalStateException(
                    "Impossible de démarrer une campagne avec le statut : "
                            + campaign.getStatus()
                            + ". Statuts autorisés : DRAFT, PENDING, COMPLETED, FAILED, CANCELLED");
        }

        log.info("▶ Démarrage campagne #{} '{}'", campaign.getId(), campaign.getName());
        executionService.executeCampaign(campaign);
        return toResponse(campaignRepo.findByIdWithRelations(id).orElseThrow());
    }

    /**
     * RESTART — COMPLETED, FAILED, CANCELLED ou PAUSED → réinitialise les compteurs et renvoie tout.
     * Utile pour renvoyer explicitement une campagne terminée à tous les contacts.
     */
    @Transactional
    public CampaignResponse restartCampaign(Long id) {
        Campaign campaign = campaignRepo.findByIdWithRelations(id)
                .orElseThrow(() -> new ResourceNotFoundException("Campagne", id));

        if (campaign.getStatus() != CampaignStatus.COMPLETED
                && campaign.getStatus() != CampaignStatus.FAILED
                && campaign.getStatus() != CampaignStatus.CANCELLED
                && campaign.getStatus() != CampaignStatus.PAUSED) {
            throw new IllegalStateException(
                    "Impossible de redémarrer une campagne avec le statut : "
                            + campaign.getStatus()
                            + ". Statuts autorisés : COMPLETED, FAILED, PAUSED, CANCELLED");
        }
        if (campaign.getContactList() == null) {
            throw new IllegalStateException(
                    "La campagne n'a pas de liste de contacts.");
        }

        log.info("🔄 Redémarrage campagne #{} '{}'", campaign.getId(), campaign.getName());
        resetCampaignCounters(campaign);
        executionService.executeCampaign(campaign);
        return toResponse(campaignRepo.findByIdWithRelations(id).orElseThrow());
    }

    /**
     * Réinitialise les compteurs d'une campagne avant un restart.
     * Remet totalSent, totalFailed, totalRecipients à 0 et efface la date d'envoi.
     */
    private void resetCampaignCounters(Campaign campaign) {
        campaign.setTotalSent(0);
        campaign.setTotalFailed(0);
        campaign.setTotalRecipients(0);
        campaign.setSentAt(null);
        campaignRepo.save(campaign);
        log.info("🔁 Compteurs réinitialisés pour campagne #{}", campaign.getId());
    }

    /**
     * Mise à jour des stats d'une campagne après chaque envoi batch.
     * Appelé par smtp-service via webhook ou par CampaignExecutionService.
     * Passe automatiquement en COMPLETED si tous les emails ont été traités.
     */
    @Transactional
    public void updateCampaignStats(Long campaignId, int sent, int failed) {
        Campaign campaign = campaignRepo.findById(campaignId)
                .orElseThrow(() -> new ResourceNotFoundException("Campagne", campaignId));

        campaign.setTotalSent(sent);
        campaign.setTotalFailed(failed);

        long totalProcessed = (long) sent + failed;
        if (campaign.getTotalRecipients() > 0
                && totalProcessed >= campaign.getTotalRecipients()
                && campaign.getStatus() == CampaignStatus.IN_PROGRESS) {
            campaign.setStatus(CampaignStatus.COMPLETED);
            campaign.setSentAt(LocalDateTime.now());
            log.info("✅ Campagne #{} automatiquement passée en COMPLETED ({} envoyés, {} échoués)",
                    campaignId, sent, failed);
        }

        campaignRepo.save(campaign);
    }

    /**
     * PAUSE — IN_PROGRESS → PAUSED.
     * Les messages déjà en queue dans smtp-service continueront d'être envoyés
     * (ils sont déjà sortis de la campagne). Seul le dispatch de nouveaux batches s'arrête.
     */
    @Transactional
    public CampaignResponse pauseCampaign(Long id) {
        Campaign campaign = campaignRepo.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Campagne", id));

        if (campaign.getStatus() != CampaignStatus.IN_PROGRESS
                && campaign.getStatus() != CampaignStatus.PENDING) {
            throw new IllegalStateException(
                    "Impossible de mettre en pause une campagne avec le statut : "
                            + campaign.getStatus()
                            + ". Statut requis : IN_PROGRESS");
        }

        campaign.setStatus(CampaignStatus.PAUSED);
        campaignRepo.save(campaign);
        log.info("⏸ Campagne #{} '{}' mise en pause", campaign.getId(), campaign.getName());
        return toResponse(campaign);
    }

    /**
     * RESUME — PAUSED → reprend l'exécution là où elle s'était arrêtée.
     * Renvoie uniquement aux contacts qui n'ont pas encore reçu l'email
     * (totalSent est conservé, on repart du contact suivant).
     */
    @Transactional
    public CampaignResponse resumeCampaign(Long id) {
        Campaign campaign = campaignRepo.findByIdWithRelations(id)
                .orElseThrow(() -> new ResourceNotFoundException("Campagne", id));

        if (campaign.getStatus() != CampaignStatus.PAUSED) {
            throw new IllegalStateException(
                    "Impossible de reprendre une campagne avec le statut : "
                            + campaign.getStatus()
                            + ". Statut requis : PAUSED");
        }

        log.info("▶ Reprise campagne #{} '{}' (déjà envoyé : {})",
                campaign.getId(), campaign.getName(), campaign.getTotalSent());
        executionService.resumeCampaign(campaign);
        return toResponse(campaignRepo.findByIdWithRelations(id).orElseThrow());
    }

    /**
     * CANCEL — n'importe quel statut actif → CANCELLED.
     * Arrête définitivement la campagne.
     */
    @Transactional
    public CampaignResponse cancelCampaign(Long id) {
        Campaign campaign = campaignRepo.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Campagne", id));

        if (campaign.getStatus() == CampaignStatus.COMPLETED) {
            throw new IllegalStateException(
                    "Impossible d'annuler une campagne déjà terminée (COMPLETED). Utilisez restart si nécessaire.");
        }
        if (campaign.getStatus() == CampaignStatus.CANCELLED) {
            throw new IllegalStateException("La campagne est déjà annulée.");
        }

        campaign.setStatus(CampaignStatus.CANCELLED);
        campaignRepo.save(campaign);
        log.info("🚫 Campagne #{} '{}' annulée", campaign.getId(), campaign.getName());
        return toResponse(campaign);
    }

    @Transactional(readOnly = true)
    public Campaign getCampaignEntity(Long id) {
        return campaignRepo.findByIdWithRelations(id)
                .orElseThrow(() -> new ResourceNotFoundException("Campagne", id));
    }

    /**
     * Convertit Campaign → CampaignResponse (DTO).
     *
     * Grâce au JOIN FETCH, contactList et template sont DÉJÀ chargés.
     * Plus de LazyInitializationException.
     */
    public CampaignResponse toResponse(Campaign c) {
        Long contactListId = null;
        String contactListName = null;
        long totalContacts = 0;

        if (c.getContactList() != null) {
            contactListId = c.getContactList().getId();
            contactListName = c.getContactList().getName();
            totalContacts = contactRepo.countByContactList_Id(contactListId);
        }

        Long templateId = null;
        String templateName = null;

        if (c.getTemplate() != null) {
            templateId = c.getTemplate().getId();
            templateName = c.getTemplate().getName();
        }

        return CampaignResponse.builder()
                .id(c.getId())
                .name(c.getName())
                .subject(c.getSubject())
                .status(c.getStatus() != null ? c.getStatus().name() : "DRAFT")
                .fromName(c.getFromName())
                .fromEmail(c.getFromEmail())
                .replyTo(c.getReplyTo())
                .tag(c.getTag())
                .contactListId(contactListId)
                .contactListName(contactListName)
                .templateId(templateId)
                .templateName(templateName)
                .totalContacts(totalContacts)
                .totalRecipients(c.getTotalRecipients())
                .totalSent(c.getTotalSent())
                .totalFailed(c.getTotalFailed())
                .scheduledAt(c.getScheduledAt())
                .sentAt(c.getSentAt())
                .createdAt(c.getCreatedAt())
                .updatedAt(c.getUpdatedAt())
                .build();
    }
}