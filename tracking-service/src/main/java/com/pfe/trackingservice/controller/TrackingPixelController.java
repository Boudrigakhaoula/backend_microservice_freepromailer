package com.pfe.trackingservice.controller;

import com.pfe.trackingservice.service.TrackingService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;

import java.util.Base64;

/**
 * Endpoints publics de tracking (appelés depuis le contenu de l'email).
 *
 * Ces URLs sont insérées dans le HTML de l'email :
 *   - Pixel d'ouverture : <img src="http://host/track/open/{trackingId}" />
 *   - Lien tracké : <a href="http://host/track/click/{trackingId}?url=...">
 *   - Désabonnement : <a href="http://host/track/unsubscribe/{trackingId}">
 *
 * Inspiré de Postal : create_load (pixel GIF 1x1), create_link (redirect)
 */
@Slf4j
@RestController
@RequestMapping("/track")
@RequiredArgsConstructor
public class TrackingPixelController {

    private final TrackingService trackingService;

    @Value("${tracking.pixel.cache-control:no-store, no-cache, must-revalidate}")
    private String cacheControl;

    @Value("${tracking.unsubscribe.redirect-url:http://localhost:4200/unsubscribed}")
    private String unsubscribeRedirectUrl;

    /**
     * Pixel GIF transparent 1x1 (tracking d'ouverture).
     *
     * Inséré dans le HTML de l'email par le smtp-service :
     *   <img src="http://localhost:8080/track/open/{trackingId}" width="1" height="1" />
     *
     * Quand le client mail charge l'image → on enregistre un event OPEN.
     */
    private static final byte[] TRANSPARENT_GIF = Base64.getDecoder().decode(
            "R0lGODlhAQABAIAAAAAAAP///yH5BAEAAAAALAAAAAABAAEAAAIBRAA7"
    );

    @GetMapping("/open/{trackingId}")
    public ResponseEntity<byte[]> trackOpen(
            @PathVariable String trackingId,
            @RequestHeader(value = "User-Agent", required = false) String userAgent,
            HttpServletRequest request) {

        String ipAddress = getClientIp(request);

        trackingService.trackOpen(trackingId, userAgent, ipAddress);

        return ResponseEntity.ok()
                .contentType(MediaType.IMAGE_GIF)
                .header("Cache-Control", cacheControl)
                .header("Pragma", "no-cache")
                .header("Expires", "0")
                .body(TRANSPARENT_GIF);
    }

    /**
     * Redirection de clic tracké.
     *
     * L'URL dans l'email est remplacée par :
     *   http://localhost:8080/track/click/{trackingId}?url=https://real-link.com
     *
     * On enregistre le clic puis on redirige vers l'URL réelle.
     */
    @GetMapping("/click/{trackingId}")
    public ResponseEntity<Void> trackClick(
            @PathVariable String trackingId,
            @RequestParam String url,
            @RequestHeader(value = "User-Agent", required = false) String userAgent,
            HttpServletRequest request) {

        String ipAddress = getClientIp(request);

        trackingService.trackClick(trackingId, url, userAgent, ipAddress);

        return ResponseEntity.status(HttpStatus.FOUND)
                .header("Location", url)
                .header("Cache-Control", cacheControl)
                .build();
    }

    /**
     * Page de désabonnement.
     *
     * Lien dans l'email :
     *   http://localhost:8080/track/unsubscribe/{trackingId}
     *
     * Enregistre un event UNSUBSCRIBE puis redirige vers la page Angular.
     */
    @GetMapping("/unsubscribe/{trackingId}")
    public ResponseEntity<Void> trackUnsubscribe(
            @PathVariable String trackingId,
            HttpServletRequest request) {

        String ipAddress = getClientIp(request);

        trackingService.trackUnsubscribe(trackingId, ipAddress);

        return ResponseEntity.status(HttpStatus.FOUND)
                .header("Location", unsubscribeRedirectUrl + "?id=" + trackingId)
                .build();
    }

    /**
     * Confirmation de désabonnement via POST (depuis la page Angular).
     */
    @PostMapping("/unsubscribe/{trackingId}/confirm")
    public ResponseEntity<String> confirmUnsubscribe(@PathVariable String trackingId) {
        trackingService.trackUnsubscribe(trackingId, null);
        return ResponseEntity.ok("Vous avez été désabonné avec succès.");
    }

    /**
     * Extraire l'IP réelle du client (supporte X-Forwarded-For).
     */
    private String getClientIp(HttpServletRequest request) {
        String xForwardedFor = request.getHeader("X-Forwarded-For");
        if (xForwardedFor != null && !xForwardedFor.isBlank()) {
            return xForwardedFor.split(",")[0].trim();
        }
        String xRealIp = request.getHeader("X-Real-IP");
        if (xRealIp != null && !xRealIp.isBlank()) {
            return xRealIp;
        }
        return request.getRemoteAddr();
    }
}
