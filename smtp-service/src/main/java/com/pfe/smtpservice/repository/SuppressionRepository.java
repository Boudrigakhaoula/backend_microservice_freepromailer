package com.pfe.smtpservice.repository;

import com.pfe.smtpservice.entity.SuppressionEntry;
import com.pfe.smtpservice.enums.SuppressionType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface SuppressionRepository extends JpaRepository<SuppressionEntry, Long> {

    // ═══════════════════════════════════════════════════════════
    //  LES 3 MÉTHODES DE RECHERCHE — couvrent TOUS les appels
    // ═══════════════════════════════════════════════════════════

    /**
     * findActive(email, type, now)  ← 3 paramètres
     * Cherche par email ET type ET non expiré.
     *
     * C'EST CELLE QUI MANQUAIT et causait l'erreur :
     * "Cannot resolve method 'findActive' in 'SuppressionRepository'"
     */
    @Query("""
        SELECT s FROM SuppressionEntry s
        WHERE LOWER(s.email) = LOWER(:email)
          AND s.type = :type
          AND s.keepUntil >= :now
    """)
    Optional<SuppressionEntry> findActive(
            @Param("email") String email,
            @Param("type") SuppressionType type,
            @Param("now") LocalDateTime now);

    /**
     * findActiveByEmail(email, now)  ← 2 paramètres
     * Cherche par email uniquement (tous types).
     * Utilisé par MessageDequeuer.
     */
    @Query("""
        SELECT s FROM SuppressionEntry s
        WHERE LOWER(s.email) = LOWER(:email)
          AND s.keepUntil >= :now
    """)
    Optional<SuppressionEntry> findActiveByEmail(
            @Param("email") String email,
            @Param("now") LocalDateTime now);

    /**
     * findActiveByEmailAndType(email, type, now)  ← 3 paramètres
     * Alias de findActive (pour la lisibilité).
     */
    @Query("""
        SELECT s FROM SuppressionEntry s
        WHERE LOWER(s.email) = LOWER(:email)
          AND s.type = :type
          AND s.keepUntil >= :now
    """)
    Optional<SuppressionEntry> findActiveByEmailAndType(
            @Param("email") String email,
            @Param("type") SuppressionType type,
            @Param("now") LocalDateTime now);

    // ═══════════════════════════════════════════════════════════
    //  VÉRIFICATION RAPIDE
    // ═══════════════════════════════════════════════════════════

    /**
     * isEmailSuppressed(email, now)  ← boolean rapide
     * Utilisé par SendController.
     */
    @Query("""
        SELECT COUNT(s) > 0 FROM SuppressionEntry s
        WHERE LOWER(s.email) = LOWER(:email)
          AND s.keepUntil >= :now
    """)
    boolean isEmailSuppressed(
            @Param("email") String email,
            @Param("now") LocalDateTime now);

    // ═══════════════════════════════════════════════════════════
    //  LECTURE / LISTE
    // ═══════════════════════════════════════════════════════════

    List<SuppressionEntry> findAllByOrderByCreatedAtDesc();

    // ═══════════════════════════════════════════════════════════
    //  SUPPRESSION
    // ═══════════════════════════════════════════════════════════

    void deleteByEmailAndType(String email, SuppressionType type);

    void deleteByEmail(String email);

    // ═══════════════════════════════════════════════════════════
    //  NETTOYAGE (entrées expirées)
    // ═══════════════════════════════════════════════════════════

    @Modifying
    @Query("DELETE FROM SuppressionEntry s WHERE s.keepUntil < :now")
    int pruneExpired(@Param("now") LocalDateTime now);
}