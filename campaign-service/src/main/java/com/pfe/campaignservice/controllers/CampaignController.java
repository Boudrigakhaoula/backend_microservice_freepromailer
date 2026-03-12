package com.pfe.campaignservice.controllers;

import com.pfe.campaignservice.dto.CampaignRequest;
import com.pfe.campaignservice.dto.CampaignResponse;
import com.pfe.campaignservice.enums.CampaignStatus;
import com.pfe.campaignservice.service.CampaignExecutionService;
import com.pfe.campaignservice.service.CampaignService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/campaigns")
@RequiredArgsConstructor
public class CampaignController {

    private final CampaignService campaignService;
    private final CampaignExecutionService executionService;

    @GetMapping
    public ResponseEntity<List<CampaignResponse>> getAll() {
        return ResponseEntity.ok(campaignService.getAll());
    }

    @GetMapping("/{id}")
    public ResponseEntity<CampaignResponse> getById(@PathVariable Long id) {
        return ResponseEntity.ok(campaignService.getById(id));
    }

    @GetMapping("/status/{status}")
    public ResponseEntity<List<CampaignResponse>> getByStatus(
            @PathVariable CampaignStatus status) {
        return ResponseEntity.ok(campaignService.getByStatus(status));
    }

    @PostMapping
    public ResponseEntity<CampaignResponse> create(
            @Valid @RequestBody CampaignRequest request) {
        return ResponseEntity.ok(campaignService.create(request));
    }

    @PutMapping("/{id}")
    public ResponseEntity<CampaignResponse> update(
            @PathVariable Long id,
            @Valid @RequestBody CampaignRequest request) {
        return ResponseEntity.ok(campaignService.update(id, request));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        campaignService.delete(id);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{id}/send")
    public ResponseEntity<Map<String, String>> send(@PathVariable Long id) {
        var campaign = campaignService.getCampaignEntity(id);

        executionService.executeCampaign(campaign);

        return ResponseEntity.ok(Map.of(
                "status", "started",
                "message", "Envoi de la campagne '" + campaign.getName() + "' lance",
                "totalRecipients", String.valueOf(campaign.getTotalRecipients())
        ));
    }
}