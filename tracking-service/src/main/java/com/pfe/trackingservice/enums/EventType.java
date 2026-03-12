package com.pfe.trackingservice.enums;

/**
 * Types d'événements de tracking.
 * Inspiré de Postal :
 *   - Loaded → OPEN
 *   - Clicked → CLICK
 *   - Bounced → BOUNCE
 *   - Held → HELD
 *   - Sent → SENT
 */
public enum EventType {
    SENT,         // Email envoyé avec succès
    OPEN,         // Email ouvert (pixel chargé)
    CLICK,        // Lien cliqué dans l'email
    BOUNCE,       // Email rejeté par le serveur distant
    SOFT_BOUNCE,  // Bounce temporaire (mailbox pleine, etc.)
    HARD_BOUNCE,  // Bounce permanent (email invalide)
    UNSUBSCRIBE,  // Destinataire désabonné
    COMPLAINT,    // Marqué comme spam
    DELIVERED     // Confirmé livré (réponse 250 du MX)
}
