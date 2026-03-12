package com.pfe.campaignservice.repository;



import com.pfe.campaignservice.entity.ContactList;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ContactListRepository extends JpaRepository<ContactList, Long> {

    List<ContactList> findAllByOrderByCreatedAtDesc();

    boolean existsByName(String name);
}
