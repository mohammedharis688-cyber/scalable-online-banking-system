package com.systemdesign.banking.systemdesign.urlshortener;

import com.systemdesign.banking.systemdesign.cache.RedisCacheService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.atomic.AtomicLong;

@Service
public class UrlShortenerService {

    private static final Logger log = LoggerFactory.getLogger(UrlShortenerService.class);

    private final ShortUrlRepository repository;
    private final RedisCacheService cacheService;

    private final AtomicLong linksCreated = new AtomicLong(0);
    private final AtomicLong collisionsResolved = new AtomicLong(0);
    private final AtomicLong totalLookups = new AtomicLong(0);

    private static final String ALLOWED_PREFIX = "/synthetic/";

    public UrlShortenerService(ShortUrlRepository repository, RedisCacheService cacheService) {
        this.repository = repository;
        this.cacheService = cacheService;
    }

    /**
     * Shortens a permitted local public information route with collision resolution
     * and Redis cache-aside population.
     */
    public ShortUrlMapping shortenUrl(String targetUrl) {
        validatePermittedRoute(targetUrl);

        // Check if mapping already exists
        Optional<ShortUrlMapping> existing = repository.findByTargetUrl(targetUrl);
        if (existing.isPresent()) {
            return existing.get();
        }

        String shortCode = generateCodeWithCollisionHandling(targetUrl);

        ShortUrlMapping mapping = new ShortUrlMapping(shortCode, targetUrl);
        ShortUrlMapping saved = repository.save(mapping);
        linksCreated.incrementAndGet();

        // Populate Redis cache-aside (1 hour TTL)
        cacheService.put("cache:shorturl:" + shortCode, saved.getTargetUrl(), 3600);
        return saved;
    }

    /**
     * Resolves short code using Cache-Aside.
     * Checks Redis first. On miss, queries MongoDB and repopulates Redis cache.
     */
    public Optional<String> resolveUrl(String shortCode) {
        totalLookups.incrementAndGet();
        String cacheKey = "cache:shorturl:" + shortCode;

        // 1. Check Redis Cache
        Optional<String> cached = cacheService.get(cacheKey, String.class);
        if (cached.isPresent()) {
            return cached;
        }

        // 2. Query MongoDB
        Optional<ShortUrlMapping> mapping = repository.findByShortCode(shortCode);
        if (mapping.isPresent()) {
            ShortUrlMapping m = mapping.get();
            m.setAccessCount(m.getAccessCount() + 1);
            m.setLastAccessed(Instant.now());
            repository.save(m);

            // 3. Populate Redis Cache
            cacheService.put(cacheKey, m.getTargetUrl(), 3600);
            return Optional.of(m.getTargetUrl());
        }

        return Optional.empty();
    }

    public Map<String, Object> getMetrics() {
        Map<String, Object> metrics = new LinkedHashMap<>();
        metrics.put("linksCreated", linksCreated.get());
        metrics.put("collisionsResolved", collisionsResolved.get());
        metrics.put("totalLookups", totalLookups.get());
        metrics.put("totalStoredInDatabase", repository.count());
        metrics.put("cacheMetrics", cacheService.getMetrics());
        return metrics;
    }

    private void validatePermittedRoute(String targetUrl) {
        if (targetUrl == null || !targetUrl.startsWith(ALLOWED_PREFIX)) {
            throw new IllegalArgumentException("Security Policy: Only local public synthetic routes starting with '"
                    + ALLOWED_PREFIX + "' are permitted to be shortened.");
        }
    }

    private String generateCodeWithCollisionHandling(String targetUrl) {
        int attempt = 0;
        while (attempt < 10) {
            String input = (attempt == 0) ? targetUrl : (targetUrl + "#salt" + attempt);
            String hash = md5Hex(input);
            String code = hash.substring(0, 6);

            Optional<ShortUrlMapping> collision = repository.findByShortCode(code);
            if (collision.isEmpty() || collision.get().getTargetUrl().equals(targetUrl)) {
                return code;
            }

            // Real collision detected on different URL
            collisionsResolved.incrementAndGet();
            log.info("URL Shortener collision detected for code [{}] on target [{}], resolving with salt attempt {}",
                    code, targetUrl, attempt + 1);
            attempt++;
        }

        // Fallback: 8 characters
        return md5Hex(targetUrl + UUID.randomUUID()).substring(0, 8);
    }

    private String md5Hex(String input) {
        try {
            MessageDigest md = MessageDigest.getInstance("MD5");
            byte[] digest = md.digest(input.getBytes(StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder();
            for (byte b : digest) {
                sb.append(String.format("%02x", b));
            }
            return sb.toString();
        } catch (NoSuchAlgorithmException e) {
            return Integer.toHexString(input.hashCode());
        }
    }
}
