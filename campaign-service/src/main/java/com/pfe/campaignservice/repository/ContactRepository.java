package com.pfe.campaignservice.repository;

import com.pfe.campaignservice.entity.Contact;
import com.pfe.campaignservice.enums.ContactStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ContactRepository extends JpaRepository<Contact, Long> {

    /**
     * RÈGLE Spring Data JPA :
     *   - Le champ dans Contact.java est : ContactList contactList
     *   - Pour accéder à contactList.id, il faut écrire : ContactList_Id
     *
     * ❌ AVANT (causait l'erreur) :
     *   findByContactListId(Long listId)
     *   → cherche un champ "contactListId" qui n'existe pas
     *
     * ✅ APRÈS (corrigé) :
     *   findByContactList_Id(Long listId)
     *   → navigue dans contactList → puis accède à id
     */

    List<Contact> findByContactList_Id(Long listId);

    List<Contact> findByContactList_IdAndStatus(Long listId, ContactStatus status);

    Optional<Contact> findByEmailAndContactList_Id(String email, Long listId);

    boolean existsByEmailAndContactList_Id(String email, Long listId);

    long countByContactList_Id(Long listId);

    long countByContactList_IdAndStatus(Long listId, ContactStatus status);

    List<Contact> findByEmail(String email);
}