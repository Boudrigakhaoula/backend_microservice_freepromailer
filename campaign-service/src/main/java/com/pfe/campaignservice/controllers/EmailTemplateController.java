package com.pfe.campaignservice.controllers;

import com.pfe.campaignservice.dto.TemplateRequest;
import com.pfe.campaignservice.entity.EmailTemplate;
import com.pfe.campaignservice.enums.TemplateCategory;
import com.pfe.campaignservice.service.EmailTemplateService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/templates")
@RequiredArgsConstructor
public class EmailTemplateController {

    private final EmailTemplateService service;

    @GetMapping
    public ResponseEntity<List<EmailTemplate>> getAll() {
        return ResponseEntity.ok(service.getAll());
    }

    @GetMapping("/{id}")
    public ResponseEntity<EmailTemplate> getById(@PathVariable Long id) {
        return ResponseEntity.ok(service.getById(id));
    }

    @GetMapping("/category/{category}")
    public ResponseEntity<List<EmailTemplate>> getByCategory(@PathVariable TemplateCategory category) {
        return ResponseEntity.ok(service.getByCategory(category));
    }

    @PostMapping
    public ResponseEntity<EmailTemplate> create(@Valid @RequestBody TemplateRequest request) {
        return ResponseEntity.ok(service.create(request));
    }

    @PutMapping("/{id}")
    public ResponseEntity<EmailTemplate> update(
            @PathVariable Long id, @Valid @RequestBody TemplateRequest request) {
        return ResponseEntity.ok(service.update(id, request));
    }

    @PostMapping("/{id}/duplicate")
    public ResponseEntity<EmailTemplate> duplicate(@PathVariable Long id) {
        return ResponseEntity.ok(service.duplicate(id));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        service.delete(id);
        return ResponseEntity.noContent().build();
    }
}
