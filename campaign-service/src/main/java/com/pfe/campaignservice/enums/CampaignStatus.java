package com.pfe.campaignservice.enums;

public enum CampaignStatus {
    /** Campagne créée, pas encore lancée */
    DRAFT,
    /** En attente de lancement (planifiée) */
    PENDING,
    /** Envoi en cours */
    IN_PROGRESS,
    /** Envoi mis en pause */
    PAUSED,
    /** Tous les emails envoyés avec succès */
    COMPLETED,
    /** Envoi échoué (pas de liste, erreur critique) */
    FAILED,
    /** Annulée manuellement */
    CANCELLED
}