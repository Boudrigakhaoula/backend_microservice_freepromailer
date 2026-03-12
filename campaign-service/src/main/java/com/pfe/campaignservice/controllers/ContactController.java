package com.pfe.campaignservice.controllers;

import com.pfe.campaignservice.dto.ContactRequest;
import com.pfe.campaignservice.entity.Contact;
import com.pfe.campaignservice.service.ContactService;
import com.pfe.campaignservice.service.CsvImportService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/contacts")
@RequiredArgsConstructor
public class ContactController {

    private final ContactService contactService;
    private final CsvImportService csvImportService;

    @GetMapping("/list/{listId}")
    public ResponseEntity<List<Contact>> getByList(@PathVariable Long listId) {
        return ResponseEntity.ok(contactService.getByListId(listId));
    }

    @GetMapping("/list/{listId}/active")
    public ResponseEntity<List<Contact>> getActiveByList(@PathVariable Long listId) {
        return ResponseEntity.ok(contactService.getActiveByListId(listId));
    }

    @GetMapping("/{id}")
    public ResponseEntity<Contact> getById(@PathVariable Long id) {
        return ResponseEntity.ok(contactService.getById(id));
    }

    @PostMapping
    public ResponseEntity<Contact> create(@Valid @RequestBody ContactRequest request) {
        return ResponseEntity.ok(contactService.create(request));
    }

    @PutMapping("/{id}")
    public ResponseEntity<Contact> update(
            @PathVariable Long id, @Valid @RequestBody ContactRequest request) {
        return ResponseEntity.ok(contactService.update(id, request));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        contactService.delete(id);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{id}/unsubscribe")
    public ResponseEntity<Contact> unsubscribe(@PathVariable Long id) {
        return ResponseEntity.ok(contactService.unsubscribe(id));
    }

    @PostMapping("/{id}/bounced")
    public ResponseEntity<Contact> markBounced(@PathVariable Long id) {
        return ResponseEntity.ok(contactService.markBounced(id));
    }

    /**
     * Import CSV.
     *
     * URL CORRECTE dans Postman :
     *   POST http://localhost:8080/api/contacts/import?listId=1
     *
     * ❌ ERREUR COURANTE :
     *   POST http://localhost:8080/api/contacts/import/1
     *   → retourne "No static resource api/contacts/import/1"
     *
     * Body → form-data :
     *   Key: file   Type: File   Value: (sélectionner le fichier CSV)
     */
    @PostMapping("/import")
    public ResponseEntity<Map<String, Object>> importCsv(
            @RequestParam("file") MultipartFile file,
            @RequestParam("listId") Long listId) {

        if (file.isEmpty()) {
            return ResponseEntity.badRequest()
                    .body(Map.of("error", "Le fichier CSV est vide"));
        }

        Map<String, Object> result = csvImportService.importCsv(file, listId);
        return ResponseEntity.ok(result);
    }
}