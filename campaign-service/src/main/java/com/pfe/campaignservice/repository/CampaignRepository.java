package com.pfe.campaignservice.repository;

import com.pfe.campaignservice.entity.Campaign;
import com.pfe.campaignservice.enums.CampaignStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface CampaignRepository extends JpaRepository<Campaign, Long> {

    /**
     * FIX DÉFINITIF : JOIN FETCH charge contactList et template
     * EN UNE SEULE requête SQL.
     * Plus JAMAIS de LazyInitializationException.
     */
    @Query("""
        SELECT c FROM Campaign c
        LEFT JOIN FETCH c.contactList
        LEFT JOIN FETCH c.template
        ORDER BY c.createdAt DESC
    """)
    List<Campaign> findAllWithRelations();

    @Query("""
        SELECT c FROM Campaign c
        LEFT JOIN FETCH c.contactList
        LEFT JOIN FETCH c.template
        WHERE c.id = :id
    """)
    Optional<Campaign> findByIdWithRelations(@Param("id") Long id);

    @Query("""
        SELECT c FROM Campaign c
        LEFT JOIN FETCH c.contactList
        LEFT JOIN FETCH c.template
        WHERE c.status = :status
        ORDER BY c.createdAt DESC
    """)
    List<Campaign> findByStatusWithRelations(@Param("status") CampaignStatus status);

    List<Campaign> findByStatus(CampaignStatus status);

    List<Campaign> findByStatusAndScheduledAtBefore(CampaignStatus status, LocalDateTime now);
    @Query("""
    SELECT c FROM Campaign c
    LEFT JOIN FETCH c.contactList
    LEFT JOIN FETCH c.template
    WHERE c.userId = :userId
    ORDER BY c.createdAt DESC
""")
    List<Campaign> findByUserIdWithRelations(@Param("userId") Long userId);
}