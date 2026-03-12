package com.pfe.campaignservice.service;

import com.pfe.campaignservice.dto.ContactRequest;
import com.pfe.campaignservice.entity.*;
import com.pfe.campaignservice.enums.ContactStatus;
import com.pfe.campaignservice.exception.ResourceNotFoundException;
import com.pfe.campaignservice.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class ContactService {

    private final ContactRepository contactRepo;
    private final ContactListRepository contactListRepo;

    public List<Contact> getByListId(Long listId) {
        return contactRepo.findByContactList_Id(listId);       // ← CORRIGÉ
    }

    public List<Contact> getActiveByListId(Long listId) {
        return contactRepo.findByContactList_IdAndStatus(listId, ContactStatus.ACTIVE);  // ← CORRIGÉ
    }

    public Contact getById(Long id) {
        return contactRepo.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Contact", id));
    }

    @Transactional
    public Contact create(ContactRequest request) {
        ContactList list = contactListRepo.findById(request.getContactListId())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Liste de contacts", request.getContactListId()));

        // CORRIGÉ
        if (contactRepo.existsByEmailAndContactList_Id(
                request.getEmail().toLowerCase().trim(), list.getId())) {
            throw new IllegalArgumentException(
                    "Le contact " + request.getEmail() + " existe déjà dans cette liste");
        }

        Contact contact = Contact.builder()
                .email(request.getEmail().toLowerCase().trim())
                .firstName(request.getFirstName())
                .lastName(request.getLastName())
                .company(request.getCompany())
                .phone(request.getPhone())
                .contactList(list)
                .status(ContactStatus.ACTIVE)
                .build();

        return contactRepo.save(contact);
    }

    @Transactional
    public Contact update(Long id, ContactRequest request) {
        Contact contact = getById(id);
        contact.setEmail(request.getEmail().toLowerCase().trim());
        contact.setFirstName(request.getFirstName());
        contact.setLastName(request.getLastName());
        contact.setCompany(request.getCompany());
        contact.setPhone(request.getPhone());
        return contactRepo.save(contact);
    }

    @Transactional
    public void delete(Long id) {
        contactRepo.deleteById(id);
    }

    @Transactional
    public Contact unsubscribe(Long id) {
        Contact contact = getById(id);
        contact.setStatus(ContactStatus.UNSUBSCRIBED);
        contact.setUnsubscribedAt(LocalDateTime.now());
        log.info("📭 Contact {} désabonné", contact.getEmail());
        return contactRepo.save(contact);
    }

    @Transactional
    public Contact markBounced(Long id) {
        Contact contact = getById(id);
        contact.setStatus(ContactStatus.BOUNCED);
        contact.setBouncedAt(LocalDateTime.now());
        log.info("⚠️ Contact {} marqué comme bounced", contact.getEmail());
        return contactRepo.save(contact);
    }
}