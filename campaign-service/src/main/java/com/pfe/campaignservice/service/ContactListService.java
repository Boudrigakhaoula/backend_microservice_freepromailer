package com.pfe.campaignservice.service;

import com.pfe.campaignservice.dto.ContactListRequest;
import com.pfe.campaignservice.entity.ContactList;
import com.pfe.campaignservice.exception.ResourceNotFoundException;
import com.pfe.campaignservice.repository.ContactListRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class ContactListService {

    private final ContactListRepository repo;

    public List<ContactList> getAll() {
        return repo.findAllByOrderByCreatedAtDesc();
    }

    public ContactList getById(Long id) {
        return repo.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Liste de contacts", id));
    }

    @Transactional
    public ContactList create(ContactListRequest request) {
        ContactList list = ContactList.builder()
                .name(request.getName())
                .description(request.getDescription())
                .build();
        ContactList saved = repo.save(list);
        log.info("✅ Liste de contacts créée : #{} '{}'", saved.getId(), saved.getName());
        return saved;
    }

    @Transactional
    public ContactList update(Long id, ContactListRequest request) {
        ContactList list = getById(id);
        list.setName(request.getName());
        list.setDescription(request.getDescription());
        return repo.save(list);
    }

    @Transactional
    public void delete(Long id) {
        repo.deleteById(id);
        log.info("🗑️ Liste de contacts #{} supprimée", id);
    }
}
