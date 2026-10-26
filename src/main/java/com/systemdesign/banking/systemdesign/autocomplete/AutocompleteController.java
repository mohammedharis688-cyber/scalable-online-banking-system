package com.systemdesign.banking.systemdesign.autocomplete;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/autocomplete")
public class AutocompleteController {

    private final AutocompleteService autocompleteService;

    public AutocompleteController(AutocompleteService autocompleteService) {
        this.autocompleteService = autocompleteService;
    }

    @GetMapping
    public ResponseEntity<?> getSuggestions(
            @RequestParam(defaultValue = "") String prefix,
            @RequestParam(defaultValue = "10") int limit) {
        Map<String, Object> response = autocompleteService.getSuggestions(prefix, limit);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/metrics")
    public ResponseEntity<?> getMetrics() {
        return ResponseEntity.ok(autocompleteService.getMetrics());
    }

    @PostMapping("/refresh")
    public ResponseEntity<?> refreshIndex() {
        autocompleteService.refreshIndex();
        return ResponseEntity.ok(Map.of(
                "message", "Trie autocomplete index successfully refreshed from database",
                "timestamp", java.time.Instant.now().toString()
        ));
    }
}
