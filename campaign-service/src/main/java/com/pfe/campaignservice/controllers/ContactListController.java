package com.pfe.campaignservice.controllers;

import com.pfe.campaignservice.dto.ContactListRequest;
import com.pfe.campaignservice.entity.ContactList;
import com.pfe.campaignservice.service.ContactListService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/contact-lists")
@RequiredArgsConstructor
public class ContactListController {

    private final ContactListService service;

    /**
     * FIX : plus besoin de @Transactional ici car
     * ContactList.contacts est @JsonIgnore.
     * Jackson ne tente plus d'accéder aux contacts LAZY.
     */
    @GetMapping
    public ResponseEntity<List<ContactList>> getAll() {
        return ResponseEntity.ok(service.getAll());
    }

    @GetMapping("/{id}")
    public ResponseEntity<ContactList> getById(@PathVariable Long id) {
        return ResponseEntity.ok(service.getById(id));
    }

    @PostMapping
    public ResponseEntity<ContactList> create(@Valid @RequestBody ContactListRequest request) {
        return ResponseEntity.ok(service.create(request));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ContactList> update(
            @PathVariable Long id, @Valid @RequestBody ContactListRequest request) {
        return ResponseEntity.ok(service.update(id, request));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        service.delete(id);
        return ResponseEntity.noContent().build();
    }
}