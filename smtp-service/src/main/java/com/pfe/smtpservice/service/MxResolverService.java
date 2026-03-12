package com.pfe.smtpservice.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import javax.naming.directory.Attributes;
import javax.naming.directory.InitialDirContext;
import java.util.Hashtable;

/**
 * Résolution des records MX DNS.
 *
 * CHANGEMENT vs l'original :
 *   - Extrait de InMemoryDeliveryHandler.resolveMx() → service dédié
 *   - Ajout cache DNS (évite des résolutions répétées)
 *   - Meilleure gestion d'erreurs
 *
 * La logique de résolution MX est identique à l'original.
 */
@Slf4j
@Service
public class MxResolverService {

    // Cache simple pour éviter les résolutions DNS répétées
    private final java.util.concurrent.ConcurrentHashMap<String, CachedMx> cache
            = new java.util.concurrent.ConcurrentHashMap<>();

    private static final long CACHE_TTL_MS = 5 * 60 * 1000; // 5 minutes

    /**
     * Résout le serveur MX avec la meilleure priorité pour un domaine.
     * Code repris de votre InMemoryDeliveryHandler.resolveMx()
     */
    public String resolve(String domain) throws Exception {
        // Vérifier le cache
        CachedMx cached = cache.get(domain);
        if (cached != null && !cached.isExpired()) {
            log.debug("MX cache hit pour {} → {}", domain, cached.host);
            return cached.host;
        }

        Hashtable<String, String> env = new Hashtable<>();
        env.put("java.naming.factory.initial", "com.sun.jndi.dns.DnsContextFactory");
        env.put("java.naming.provider.url", "dns:");

        InitialDirContext ctx = new InitialDirContext(env);
        Attributes attrs = ctx.getAttributes(domain, new String[]{"MX"});

        javax.naming.directory.Attribute mx = attrs.get("MX");
        if (mx == null || mx.size() == 0) {
            log.warn("Pas de MX pour {} — tentative connexion directe", domain);
            return domain;
        }

        String bestMx = null;
        int bestPriority = Integer.MAX_VALUE;

        for (int i = 0; i < mx.size(); i++) {
            String record = mx.get(i).toString().trim();
            String[] parts = record.split("\\s+");
            int priority = Integer.parseInt(parts[0]);
            String host = parts[1].replaceAll("\\.$", "");
            if (priority < bestPriority) {
                bestPriority = priority;
                bestMx = host;
            }
        }

        log.info("MX résolu pour {} : {} (priorité {})", domain, bestMx, bestPriority);

        // Mettre en cache
        cache.put(domain, new CachedMx(bestMx, System.currentTimeMillis()));

        ctx.close();
        return bestMx;
    }

    private record CachedMx(String host, long timestamp) {
        boolean isExpired() {
            return System.currentTimeMillis() - timestamp > CACHE_TTL_MS;
        }
    }
}
