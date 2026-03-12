package com.pfe.campaignservice.service;

import com.pfe.campaignservice.dto.TemplateRequest;
import com.pfe.campaignservice.entity.EmailTemplate;
import com.pfe.campaignservice.enums.TemplateCategory;
import com.pfe.campaignservice.exception.ResourceNotFoundException;
import com.pfe.campaignservice.repository.EmailTemplateRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class EmailTemplateService {

    private final EmailTemplateRepository repo;

    public List<EmailTemplate> getAll() {
        return repo.findAllByOrderByCreatedAtDesc();
    }

    public EmailTemplate getById(Long id) {
        return repo.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Template", id));
    }

    public List<EmailTemplate> getByCategory(TemplateCategory category) {
        return repo.findByCategory(category);
    }

    @Transactional
    public EmailTemplate create(TemplateRequest request) {
        EmailTemplate template = EmailTemplate.builder()
                .name(request.getName())
                .subject(request.getSubject())
                .htmlContent(request.getHtmlContent())
                .textContent(request.getTextContent())
                .category(request.getCategory() != null
                        ? request.getCategory() : TemplateCategory.OTHER)
                .description(request.getDescription())
                .previewText(request.getPreviewText())
                .build();
        EmailTemplate saved = repo.save(template);
        log.info("✅ Template créé : #{} '{}'", saved.getId(), saved.getName());
        return saved;
    }

    @Transactional
    public EmailTemplate update(Long id, TemplateRequest request) {
        EmailTemplate template = getById(id);
        template.setName(request.getName());
        template.setSubject(request.getSubject());
        template.setHtmlContent(request.getHtmlContent());
        template.setTextContent(request.getTextContent());
        template.setCategory(request.getCategory());
        template.setDescription(request.getDescription());
        template.setPreviewText(request.getPreviewText());
        return repo.save(template);
    }

    @Transactional
    public EmailTemplate duplicate(Long id) {
        EmailTemplate original = getById(id);
        EmailTemplate copy = EmailTemplate.builder()
                .name(original.getName() + " (copie)")
                .subject(original.getSubject())
                .htmlContent(original.getHtmlContent())
                .textContent(original.getTextContent())
                .category(original.getCategory())
                .description(original.getDescription())
                .previewText(original.getPreviewText())
                .build();
        return repo.save(copy);
    }

    @Transactional
    public void delete(Long id) {
        repo.deleteById(id);
        log.info("🗑️ Template #{} supprimé", id);
    }
}
