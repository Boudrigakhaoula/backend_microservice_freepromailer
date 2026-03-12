package com.pfe.smtpservice.service;

import com.pfe.smtpservice.entity.SuppressionEntry;
import com.pfe.smtpservice.enums.SuppressionType;
import com.pfe.smtpservice.repository.SuppressionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * Gestion de la suppression list.
 *
 * Inspiré DIRECTEMENT de Postal lib/postal/message_db/suppression_list.rb :
 *   - add(type, address, reason) → ajouter
 *   - get(type, address)         → vérifier si supprimé
 *   - remove(type, address)      → retirer
 *   - prune()                    → nettoyer les expirées
 *
 * NOUVEAU fichier : l'original n'avait pas de suppression list.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SuppressionService {

    private final SuppressionRepository repo;

    @Value("${suppression.auto-removal-days:30}")
    private int autoRemovalDays;

    /**
     * Ajoute un email à la suppression list.
     * Comme Postal : suppression_list.add(:recipient, address, reason: "...")
     */
    public void add(String email, SuppressionType type, String reason) {
        Optional<SuppressionEntry> existing = repo.findActive(
                email, type, LocalDateTime.now());

        if (existing.isPresent()) {
            // Met à jour la raison et prolonge l'expiration
            SuppressionEntry entry = existing.get();
            entry.setReason(reason);
            entry.setKeepUntil(LocalDateTime.now().plusDays(autoRemovalDays));
            repo.save(entry);
            log.info("⛔ Suppression list mise à jour : {} ({})", email, reason);
        } else {
            repo.save(SuppressionEntry.builder()
                    .email(email.toLowerCase().trim())
                    .type(type)
                    .reason(reason)
                    .keepUntil(LocalDateTime.now().plusDays(autoRemovalDays))
                    .build());
            log.info("⛔ Ajouté à la suppression list : {} ({})", email, reason);
        }
    }

    /**
     * Vérifie si un email est sur la suppression list.
     * Comme Postal : suppression_list.get(:recipient, address)
     */
    public Optional<SuppressionEntry> get(String email, SuppressionType type) {
        return repo.findActive(email.toLowerCase().trim(), type, LocalDateTime.now());
    }

    /**
     * Vérifie rapidement si un recipient est supprimé.
     */
    public boolean isSuppressed(String email) {
        return get(email, SuppressionType.RECIPIENT).isPresent();
    }

    /**
     * Retire un email de la suppression list.
     * Comme Postal : suppression_list.remove(type, address)
     */
    @Transactional
    public void remove(String email, SuppressionType type) {
        repo.deleteByEmailAndType(email.toLowerCase().trim(), type);
        log.info("✅ Retiré de la suppression list : {}", email);
    }

    /**
     * Retourne toutes les entrées de la suppression list.
     */
    public List<SuppressionEntry> getAll() {
        return repo.findAllByOrderByCreatedAtDesc();
    }

    /**
     * Nettoie les entrées expirées.
     * Comme Postal : PruneSuppressionListsScheduledTask
     * Exécuté toutes les 6 heures.
     */
    @Scheduled(fixedDelayString = "${suppression.prune-interval-hours:6}",
            timeUnit = java.util.concurrent.TimeUnit.HOURS)
    @Transactional
    public void pruneExpired() {
        int deleted = repo.pruneExpired(LocalDateTime.now());
        if (deleted > 0) {
            log.info("🧹 Suppression list : {} entrées expirées supprimées", deleted);
        }
    }
}
