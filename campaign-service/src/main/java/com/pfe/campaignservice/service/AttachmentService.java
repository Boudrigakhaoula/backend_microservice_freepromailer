package com.pfe.campaignservice.service;

import com.pfe.campaignservice.entity.Attachment;
import com.pfe.campaignservice.entity.Campaign;
import com.pfe.campaignservice.exception.ResourceNotFoundException;
import com.pfe.campaignservice.repository.AttachmentRepository;
import com.pfe.campaignservice.repository.CampaignRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.*;
import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class AttachmentService {

    private final AttachmentRepository attachmentRepo;
    private final CampaignRepository campaignRepo;

    @Value("${attachment.upload.dir:./uploads/attachments}")
    private String uploadDir;

    public List<Attachment> getByCampaign(Long campaignId) {
        return attachmentRepo.findByCampaign_Id(campaignId);
    }

    public Attachment upload(Long campaignId, MultipartFile file) {
        Campaign campaign = campaignRepo.findById(campaignId)
                .orElseThrow(() -> new ResourceNotFoundException("Campagne", campaignId));

        try {
            Path dir = Paths.get(uploadDir, campaignId.toString());
            Files.createDirectories(dir);

            String originalName = file.getOriginalFilename() != null
                    ? file.getOriginalFilename() : "file";
            String storedName = UUID.randomUUID() + "_" + originalName;
            Path dest = dir.resolve(storedName);
            Files.copy(file.getInputStream(), dest, StandardCopyOption.REPLACE_EXISTING);

            Attachment attachment = Attachment.builder()
                    .fileName(originalName)
                    .filePath(dest.toString())
                    .fileSize(file.getSize())
                    .contentType(file.getContentType())
                    .campaign(campaign)
                    .build();

            Attachment saved = attachmentRepo.save(attachment);
            log.info("📎 Pièce jointe uploadée : {} → {}", originalName, dest);
            return saved;

        } catch (IOException e) {
            throw new RuntimeException("Erreur upload fichier : " + e.getMessage(), e);
        }
    }

    public void delete(Long attachmentId) {
        Attachment attachment = attachmentRepo.findById(attachmentId)
                .orElseThrow(() -> new ResourceNotFoundException("Pièce jointe", attachmentId));
        try {
            Files.deleteIfExists(Paths.get(attachment.getFilePath()));
        } catch (IOException e) {
            log.warn("Impossible de supprimer le fichier physique : {}", attachment.getFilePath());
        }
        attachmentRepo.delete(attachment);
        log.info("🗑️ Pièce jointe #{} supprimée", attachmentId);
    }

    public List<String> getPathsByCampaign(Long campaignId) {
        return attachmentRepo.findByCampaign_Id(campaignId)
                .stream()
                .map(Attachment::getFilePath)
                .toList();
    }
}