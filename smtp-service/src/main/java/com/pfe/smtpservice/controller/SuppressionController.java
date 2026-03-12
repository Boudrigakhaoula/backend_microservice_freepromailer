package com.pfe.smtpservice.controller;

import com.pfe.smtpservice.entity.SuppressionEntry;
import com.pfe.smtpservice.enums.SuppressionType;
import com.pfe.smtpservice.repository.SuppressionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/suppression")
@RequiredArgsConstructor
public class SuppressionController {

    private final SuppressionRepository repo;

    @GetMapping
    public ResponseEntity<List<SuppressionEntry>> getAll() {
        return ResponseEntity.ok(repo.findAllByOrderByCreatedAtDesc());
    }

    @GetMapping("/check")
    public ResponseEntity<Map<String, Object>> check(@RequestParam String email) {
        boolean suppressed = repo.isEmailSuppressed(email, LocalDateTime.now());
        return ResponseEntity.ok(Map.of(
                "email", email,
                "suppressed", suppressed
        ));
    }

    @PostMapping
    public ResponseEntity<Map<String, String>> add(@RequestBody Map<String, String> payload) {
        repo.save(SuppressionEntry.builder()
                .email(payload.get("email").toLowerCase().trim())
                .reason(payload.getOrDefault("reason", "manually added"))
                .type(SuppressionType.RECIPIENT)
                .build());
        return ResponseEntity.ok(Map.of("status", "added"));
    }

    @DeleteMapping
    @Transactional
    public ResponseEntity<Map<String, String>> remove(@RequestParam String email) {
        repo.deleteByEmail(email.toLowerCase().trim());
        return ResponseEntity.ok(Map.of("status", "removed"));
    }
}