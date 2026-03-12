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

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class CampaignService {

    private final CampaignRepository campaignRepo;
    private final ContactListRepository contactListRepo;
    private final EmailTemplateRepository templateRepo;
    private final ContactRepository contactRepo;

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