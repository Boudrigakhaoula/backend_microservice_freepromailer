package com.pfe.campaignservice.repository;

import com.pfe.campaignservice.entity.EmailTemplate;
import com.pfe.campaignservice.enums.TemplateCategory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface EmailTemplateRepository extends JpaRepository<EmailTemplate, Long> {

    List<EmailTemplate> findByCategory(TemplateCategory category);

    List<EmailTemplate> findAllByOrderByCreatedAtDesc();
}
