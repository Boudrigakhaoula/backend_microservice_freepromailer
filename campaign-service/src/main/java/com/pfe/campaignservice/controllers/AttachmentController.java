package com.pfe.campaignservice.controllers;

import com.pfe.campaignservice.entity.Attachment;
import com.pfe.campaignservice.service.AttachmentService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import java.io.IOException;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/campaigns/{campaignId}/attachments")
@RequiredArgsConstructor
public class AttachmentController {

    private final AttachmentService attachmentService;

    @GetMapping
    public ResponseEntity<List<Attachment>> getAll(@PathVariable Long campaignId) {
        return ResponseEntity.ok(attachmentService.getByCampaign(campaignId));
    }

    @PostMapping
    public ResponseEntity<Attachment> upload(
            @PathVariable Long campaignId,
            @RequestParam("file") MultipartFile file) throws IOException {

        if (file.isEmpty()) {
            throw new IllegalArgumentException("Le fichier est vide");
        }
        return ResponseEntity.ok(attachmentService.upload(campaignId, file));
    }

    @DeleteMapping("/{attachmentId}")
    public ResponseEntity<Map<String, String>> delete(
            @PathVariable Long campaignId,
            @PathVariable Long attachmentId) {
        attachmentService.delete(attachmentId);
        return ResponseEntity.ok(Map.of("message", "Pièce jointe supprimée"));
    }
}