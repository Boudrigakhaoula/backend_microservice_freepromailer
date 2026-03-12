package com.pfe.aiservice.controller;

import org.springframework.web.bind.annotation.*;
import java.util.*;

@RestController
@RequestMapping("/api/ai")
@RequiredArgsConstructor
public class AiSuggestionController {

    private final AiService aiService;

    @PostMapping("/suggest-subjects")
    public ResponseEntity<Map<String, Object>> suggestSubjects(
            @RequestBody Map<String, String> request) {
        String content = request.get("content");
        String audience = request.getOrDefault("audience", "general");

        List<String> suggestions = aiService.generateSubjectLines(content, audience);

        return ResponseEntity.ok(Map.of(
                "suggestions", suggestions,
                "audience", audience
        ));
    }

    @PostMapping("/improve-content")
    public ResponseEntity<Map<String, String>> improveContent(
            @RequestBody Map<String, String> request) {
        String content = request.get("content");
        String improved = aiService.improveEmailContent(content);
        return ResponseEntity.ok(Map.of("improved", improved));
    }
}
