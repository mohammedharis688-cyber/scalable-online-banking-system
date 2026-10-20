package com.systemdesign.banking.systemdesign.urlshortener;

import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.util.Map;
import java.util.Optional;

@RestController
public class UrlShortenerController {

    private final UrlShortenerService shortenerService;

    public UrlShortenerController(UrlShortenerService shortenerService) {
        this.shortenerService = shortenerService;
    }

    @PostMapping("/api/shortener")
    public ResponseEntity<?> createShortUrl(@RequestBody Map<String, String> request) {
        String targetUrl = request.get("targetUrl");
        if (targetUrl == null || targetUrl.isBlank()) {
            return ResponseEntity.badRequest().body(Map.of("error", "targetUrl is required"));
        }

        try {
            ShortUrlMapping mapping = shortenerService.shortenUrl(targetUrl);
            return ResponseEntity.status(HttpStatus.CREATED).body(Map.of(
                    "shortCode", mapping.getShortCode(),
                    "targetUrl", mapping.getTargetUrl(),
                    "shortUrl", "/s/" + mapping.getShortCode(),
                    "createdAt", mapping.getCreatedAt()
            ));
        } catch (IllegalArgumentException ex) {
            return ResponseEntity.badRequest().body(Map.of("error", ex.getMessage()));
        }
    }

    @GetMapping("/api/shortener/metrics")
    public ResponseEntity<?> getShortenerMetrics() {
        return ResponseEntity.ok(shortenerService.getMetrics());
    }

    @GetMapping("/s/{code}")
    public void redirectShortUrl(@PathVariable String code, HttpServletResponse response) throws IOException {
        Optional<String> targetUrl = shortenerService.resolveUrl(code);
        if (targetUrl.isPresent()) {
            response.sendRedirect(targetUrl.get());
        } else {
            response.sendError(HttpServletResponse.SC_NOT_FOUND, "Short URL code not found: " + code);
        }
    }
}
