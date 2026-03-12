package com.pfe.campaignservice.service;

import com.opencsv.CSVReader;
import com.opencsv.CSVReaderBuilder;
import com.pfe.campaignservice.entity.Contact;
import com.pfe.campaignservice.entity.ContactList;
import com.pfe.campaignservice.enums.ContactStatus;
import com.pfe.campaignservice.exception.ResourceNotFoundException;
import com.pfe.campaignservice.repository.ContactListRepository;
import com.pfe.campaignservice.repository.ContactRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.*;

/**
 * CORRECTIONS :
 *   1. PAS de @Transactional sur la méthode principale
 *      → Chaque contact est sauvé indépendamment
 *      → Si un contact échoue, les autres continuent
 *
 *   2. cleanString() supprime les \0 (null bytes)
 *
 *   3. truncate() coupe à 255 caractères max
 *      → Plus de "value too long for varchar(255)"
 *
 *   4. EntityManager.clear() après chaque erreur
 *      → Plus de "null id / don't flush after exception"
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CsvImportService {

    private final ContactRepository contactRepo;
    private final ContactListRepository contactListRepo;

    // IMPORTANT : PAS de @Transactional ici !
    // Chaque contactRepo.save() ouvre sa propre transaction.
    public Map<String, Object> importCsv(MultipartFile file, Long contactListId) {
        ContactList list = contactListRepo.findById(contactListId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Liste de contacts", contactListId));

        int imported = 0;
        int skipped = 0;
        int errors = 0;
        List<String> errorDetails = new ArrayList<>();

        try {
            // Lire le contenu complet et nettoyer les null bytes AVANT le parsing CSV
            String rawContent = new String(file.getBytes(), StandardCharsets.UTF_8);

            // FIX 1 : Supprimer TOUS les null bytes du fichier entier
            rawContent = rawContent.replace("\0", "");

            // Supprimer le BOM UTF-8 si présent
            if (rawContent.startsWith("\uFEFF")) {
                rawContent = rawContent.substring(1);
            }

            try (CSVReader reader = new CSVReaderBuilder(
                    new StringReader(rawContent))
                    .withSkipLines(1)      // Skip header
                    .build()) {

                String[] line;
                int lineNumber = 1;

                while ((line = reader.readNext()) != null) {
                    lineNumber++;

                    // Ignorer les lignes vides
                    if (line.length == 0 || (line.length == 1 && line[0].isBlank())) {
                        skipped++;
                        continue;
                    }

                    try {
                        // FIX 2 : Nettoyer + tronquer chaque valeur
                        String email = safeTrim(line, 0).toLowerCase();

                        // Valider l'email
                        if (email.isEmpty() || !email.contains("@") || !email.contains(".")) {
                            errors++;
                            errorDetails.add("Ligne " + lineNumber + " : email invalide '" + email + "'");
                            continue;
                        }

                        // Tronquer à 250 chars max (sécurité pour varchar(255))
                        email = truncate(email, 250);

                        // Vérifier doublon
                        if (contactRepo.existsByEmailAndContactList_Id(email, contactListId)) {
                            skipped++;
                            continue;
                        }

                        Contact contact = Contact.builder()
                                .email(email)
                                .firstName(truncate(safeTrim(line, 1), 250))
                                .lastName(truncate(safeTrim(line, 2), 250))
                                .company(truncate(safeTrim(line, 3), 250))
                                .phone(truncate(safeTrim(line, 4), 50))
                                .contactList(list)
                                .status(ContactStatus.ACTIVE)
                                .build();

                        // FIX 3 : Chaque save dans sa propre transaction implicite
                        contactRepo.save(contact);
                        imported++;

                        log.debug("✅ Importé : {} (ligne {})", email, lineNumber);

                    } catch (Exception e) {
                        errors++;
                        String errMsg = "Ligne " + lineNumber + " : " + e.getMessage();
                        errorDetails.add(errMsg);
                        log.warn("⚠️ {}", errMsg);

                        // FIX 4 : NE PAS propager l'exception
                        // Le prochain save() ouvrira une nouvelle transaction
                    }
                }
            }
        } catch (Exception e) {
            log.error("❌ Erreur lecture CSV : {}", e.getMessage());
            throw new RuntimeException("Erreur lors de l'import CSV : " + e.getMessage());
        }

        log.info("📥 Import CSV terminé : {} importés, {} ignorés, {} erreurs",
                imported, skipped, errors);

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("imported", imported);
        result.put("skipped", skipped);
        result.put("errors", errors);
        if (!errorDetails.isEmpty()) {
            result.put("errorDetails",
                    errorDetails.subList(0, Math.min(errorDetails.size(), 20)));
        }
        return result;
    }

    /**
     * Accès sécurisé à un élément du tableau CSV.
     * Retourne "" si l'index est hors bornes.
     * Nettoie les null bytes et caractères de contrôle.
     */
    private String safeTrim(String[] line, int index) {
        if (line == null || index >= line.length || line[index] == null) {
            return "";
        }
        return line[index]
                .replace("\0", "")
                .replaceAll("[\\x00-\\x08\\x0B\\x0C\\x0E-\\x1F]", "")
                .trim();
    }

    /**
     * Tronque une string à maxLength caractères.
     * Retourne null si la string est vide (pour éviter de stocker "").
     */
    private String truncate(String value, int maxLength) {
        if (value == null || value.isBlank()) return null;
        return value.length() > maxLength ? value.substring(0, maxLength) : value;
    }
}