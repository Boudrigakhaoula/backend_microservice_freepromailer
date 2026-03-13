package com.pfe.campaignservice.controllers;

import com.pfe.campaignservice.dto.ContactRequest;
import com.pfe.campaignservice.entity.Contact;
import com.pfe.campaignservice.service.ContactService;
import com.pfe.campaignservice.service.CsvImportService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.parameters.RequestBody;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
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
     * ✅ Import CSV/Excel — multipart/form-data
     * Swagger affichera un bouton "Choose File"
     */
    @Operation(
            summary = "Importer des contacts depuis un fichier CSV ou Excel",
            requestBody = @RequestBody(
                    content = @Content(
                            mediaType = MediaType.MULTIPART_FORM_DATA_VALUE,
                            schema = @Schema(implementation = ImportRequest.class)
                    )
            )
    )
    @PostMapping(value = "/import", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<Map<String, Object>> importContacts(
            @Parameter(description = "Fichier CSV ou Excel", required = true)
            @RequestParam("file") MultipartFile file,
            @Parameter(description = "ID de la liste de contacts", required = true)
            @RequestParam("listId") Long listId) {

        if (file.isEmpty()) {
            return ResponseEntity.badRequest()
                    .body(Map.of("error", "Le fichier est vide"));
        }

        String filename = file.getOriginalFilename() != null
                ? file.getOriginalFilename().toLowerCase() : "";

        Map<String, Object> result;

        if (filename.endsWith(".xlsx") || filename.endsWith(".xls")) {
            // Import Excel
            result = csvImportService.importExcel(file, listId);
        } else if (filename.endsWith(".csv")) {
            // Import CSV
            result = csvImportService.importCsv(file, listId);
        } else {
            return ResponseEntity.badRequest()
                    .body(Map.of("error", "Format non supporté. Utilisez CSV ou Excel (.xlsx, .xls)"));
        }

        return ResponseEntity.ok(result);
    }

    // ─── Classe interne pour la doc Swagger ───
    @Schema(name = "ImportRequest", description = "Formulaire d'import de contacts")
    static class ImportRequest {
        @Schema(description = "Fichier CSV ou Excel", type = "string", format = "binary", required = true)
        public MultipartFile file;

        @Schema(description = "ID de la liste de contacts", example = "1", required = true)
        public Long listId;
    }
}