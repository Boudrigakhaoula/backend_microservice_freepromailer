package com.pfe.campaignservice.controllers;

import com.pfe.campaignservice.dto.CampaignRequest;
import com.pfe.campaignservice.dto.CampaignResponse;
import com.pfe.campaignservice.enums.CampaignStatus;
import com.pfe.campaignservice.service.CampaignExecutionService;
import com.pfe.campaignservice.service.CampaignService;
import com.pfe.campaignservice.service.AttachmentService;
import io.swagger.v3.oas.annotations.Operation;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/campaigns")
@RequiredArgsConstructor
public class CampaignController {

    private final CampaignService campaignService;
    private final CampaignExecutionService executionService;
    private final AttachmentService attachmentService;

    // ─── CRUD ───

    @GetMapping
    @Operation(summary = "Lister toutes les campagnes")
    public ResponseEntity<List<CampaignResponse>> getAll() {
        return ResponseEntity.ok(campaignService.getAll());
    }

    @GetMapping("/{id}")
    @Operation(summary = "Obtenir une campagne par ID")
    public ResponseEntity<CampaignResponse> getById(@PathVariable Long id) {
        return ResponseEntity.ok(campaignService.getById(id));
    }

    @GetMapping("/status/{status}")
    @Operation(summary = "Filtrer les campagnes par statut")
    public ResponseEntity<List<CampaignResponse>> getByStatus(@PathVariable CampaignStatus status) {
        return ResponseEntity.ok(campaignService.getByStatus(status));
    }

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(summary = "Créer une campagne")
    public ResponseEntity<CampaignResponse> create(
            @RequestPart("name")                                         String name,
            @RequestPart("subject")                                      String subject,
            @RequestPart(value = "htmlContent",   required = false)      String htmlContent,
            @RequestPart(value = "textContent",   required = false)      String textContent,
            @RequestPart(value = "fromName",      required = false)      String fromName,
            @RequestPart(value = "fromEmail",     required = false)      String fromEmail,
            @RequestPart(value = "replyTo",       required = false)      String replyTo,
            @RequestPart(value = "tag",           required = false)      String tag,
            @RequestPart(value = "contactListId", required = false)      String contactListId,
            @RequestPart(value = "templateId",    required = false)      String templateId,
            @RequestPart(value = "scheduledAt",   required = false)      String scheduledAt,
            @RequestPart(value = "attachments",   required = false)      List<MultipartFile> attachments
    ) {
        CampaignRequest request = CampaignRequest.builder()
                .name(name).subject(subject)
                .htmlContent(htmlContent).textContent(textContent)
                .fromName(fromName).fromEmail(fromEmail).replyTo(replyTo)
                .tag(tag)
                .contactListId(parseLong(contactListId))
                .templateId(parseLong(templateId))
                .scheduledAt(parseDateTime(scheduledAt))
                .build();
        CampaignResponse response = campaignService.create(request);

        // Upload des pièces jointes si présentes
        if (attachments != null) {
            attachments.stream()
                    .filter(f -> f != null && !f.isEmpty())
                    .forEach(f -> attachmentService.upload(response.getId(), f));
        }
        return ResponseEntity.ok(response);
    }

    @PutMapping(value = "/{id}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(summary = "Modifier une campagne (DRAFT uniquement)")
    public ResponseEntity<CampaignResponse> update(
            @PathVariable Long id,
            @RequestPart("name")                                         String name,
            @RequestPart("subject")                                      String subject,
            @RequestPart(value = "htmlContent",   required = false)      String htmlContent,
            @RequestPart(value = "textContent",   required = false)      String textContent,
            @RequestPart(value = "fromName",      required = false)      String fromName,
            @RequestPart(value = "fromEmail",     required = false)      String fromEmail,
            @RequestPart(value = "replyTo",       required = false)      String replyTo,
            @RequestPart(value = "tag",           required = false)      String tag,
            @RequestPart(value = "contactListId", required = false)      String contactListId,
            @RequestPart(value = "templateId",    required = false)      String templateId,
            @RequestPart(value = "scheduledAt",   required = false)      String scheduledAt,
            @RequestPart(value = "attachments",   required = false)      List<MultipartFile> attachments
    ) {
        CampaignRequest request = CampaignRequest.builder()
                .name(name).subject(subject)
                .htmlContent(htmlContent).textContent(textContent)
                .fromName(fromName).fromEmail(fromEmail).replyTo(replyTo)
                .tag(tag)
                .contactListId(parseLong(contactListId))
                .templateId(parseLong(templateId))
                .scheduledAt(parseDateTime(scheduledAt))
                .build();
        CampaignResponse response = campaignService.update(id, request);

        // Upload des nouvelles pièces jointes si présentes
        if (attachments != null) {
            attachments.stream()
                    .filter(f -> f != null && !f.isEmpty())
                    .forEach(f -> attachmentService.upload(id, f));
        }
        return ResponseEntity.ok(response);
    }

    // ─── Helpers de parsing ───

    private Long parseLong(String value) {
        if (value == null || value.isBlank()) return null;
        try { return Long.parseLong(value.trim()); }
        catch (NumberFormatException e) { return null; }
    }

    private java.time.LocalDateTime parseDateTime(String value) {
        if (value == null || value.isBlank()) return null;
        try { return java.time.LocalDateTime.parse(value.trim()); }
        catch (Exception e) { return null; }  // ignore "string", formats invalides, etc.
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Supprimer une campagne")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        campaignService.delete(id);
        return ResponseEntity.noContent().build();
    }

    // ─── Envoi ───

    @PostMapping("/{id}/send")
    @Operation(summary = "Lancer l'envoi d'une campagne (legacy)")
    public ResponseEntity<Map<String, String>> send(@PathVariable Long id) {
        var campaign = campaignService.getCampaignEntity(id);
        executionService.executeCampaign(campaign);
        return ResponseEntity.ok(Map.of(
                "status", "started",
                "message", "Envoi de la campagne '" + campaign.getName() + "' lancé",
                "totalRecipients", String.valueOf(campaign.getTotalRecipients())
        ));
    }

    // ─── Lifecycle ───

    @PostMapping("/{id}/start")
    @Operation(summary = "Démarrer une campagne (DRAFT ou SCHEDULED)")
    public ResponseEntity<CampaignResponse> startCampaign(@PathVariable Long id) {
        log.info("▶ Starting campaign: {}", id);
        return ResponseEntity.ok(campaignService.startCampaign(id));
    }

    @PostMapping("/{id}/restart")
    @Operation(summary = "Redémarrer une campagne terminée")
    public ResponseEntity<CampaignResponse> restartCampaign(@PathVariable Long id) {
        log.info("🔄 Restarting campaign: {}", id);
        return ResponseEntity.ok(campaignService.restartCampaign(id));
    }

    @PostMapping("/{id}/pause")
    @Operation(summary = "Mettre en pause une campagne en cours")
    public ResponseEntity<CampaignResponse> pauseCampaign(@PathVariable Long id) {
        log.info("⏸ Pausing campaign: {}", id);
        return ResponseEntity.ok(campaignService.pauseCampaign(id));
    }

    @PostMapping("/{id}/resume")
    @Operation(summary = "Reprendre une campagne en pause")
    public ResponseEntity<CampaignResponse> resumeCampaign(@PathVariable Long id) {
        log.info("▶ Resuming campaign: {}", id);
        return ResponseEntity.ok(campaignService.resumeCampaign(id));
    }

    @PostMapping("/{id}/cancel")
    @Operation(summary = "Annuler définitivement une campagne")
    public ResponseEntity<CampaignResponse> cancelCampaign(@PathVariable Long id) {
        log.info("🚫 Cancelling campaign: {}", id);
        return ResponseEntity.ok(campaignService.cancelCampaign(id));
    }
}