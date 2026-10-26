package com.systemdesign.banking.systemdesign.autocomplete;

import com.systemdesign.banking.beneficiary.Beneficiary;
import com.systemdesign.banking.beneficiary.BeneficiaryRepository;
import com.systemdesign.banking.billpayment.Biller;
import com.systemdesign.banking.billpayment.BillerRepository;
import com.systemdesign.banking.systemdesign.cache.RedisCacheService;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.*;
import java.util.concurrent.atomic.AtomicLong;

@Service
public class AutocompleteService {

    private static final Logger log = LoggerFactory.getLogger(AutocompleteService.class);

    private final Trie trie;
    private final BillerRepository billerRepository;
    private final BeneficiaryRepository beneficiaryRepository;
    private final RedisCacheService cacheService;

    private final AtomicLong totalQueries = new AtomicLong(0);

    public AutocompleteService(Trie trie,
                               BillerRepository billerRepository,
                               BeneficiaryRepository beneficiaryRepository,
                               RedisCacheService cacheService) {
        this.trie = trie;
        this.billerRepository = billerRepository;
        this.beneficiaryRepository = beneficiaryRepository;
        this.cacheService = cacheService;
    }

    @PostConstruct
    public void initTrieFromDatabase() {
        log.info("Initializing Trie Autocomplete terms from MongoDB Biller and Beneficiary repositories...");
        List<Biller> billers = billerRepository.findAll();
        for (Biller biller : billers) {
            trie.insert(biller.getName(), biller.getCategory(), biller.getBillerCode(), 100);
            trie.insert(biller.getCategory(), biller.getCategory(), biller.getBillerCode(), 95);
            trie.insert(biller.getBillerCode(), biller.getCategory(), biller.getBillerCode(), 90);
        }
        List<Beneficiary> beneficiaries = beneficiaryRepository.findAll();
        for (Beneficiary b : beneficiaries) {
            trie.insert(b.getName(), "BENEFICIARY", b.getAccountNumber(), 90);
            trie.insert(b.getAccountNumber(), "BENEFICIARY", b.getAccountNumber(), 85);
        }
    }

    public void addTerm(String term, String category, String referenceCode, int score) {
        trie.insert(term, category, referenceCode, score);
        // Invalidate cached suggestions for prefixes matching the term
        if (term != null && !term.isBlank()) {
            String normalized = term.trim().toLowerCase();
            for (int len = 1; len <= Math.min(normalized.length(), 4); len++) {
                String prefix = normalized.substring(0, len);
                cacheService.evict("cache:autocomplete:" + prefix);
            }
        }
    }

    /**
     * Safely refreshes the in-memory Trie index from database repositories
     * and clears stale cache entries.
     */
    public synchronized void refreshIndex() {
        log.info("Refreshing Trie autocomplete index from authoritative repositories...");
        trie.clear();
        initTrieFromDatabase();
    }

    /**
     * Executes autocomplete prefix query with Redis cache-aside optimization.
     */
    @SuppressWarnings("unchecked")
    public Map<String, Object> getSuggestions(String prefix, int limit) {
        totalQueries.incrementAndGet();
        long startNano = System.nanoTime();

        if (prefix == null || prefix.trim().isBlank()) {
            return Map.of(
                    "prefix", "",
                    "suggestions", Collections.emptyList(),
                    "cached", false,
                    "latencyMicroseconds", 0
            );
        }

        String normalized = prefix.trim().toLowerCase();
        String cacheKey = "cache:autocomplete:" + normalized;

        // 1. Check Redis Cache
        Optional<List> cached = cacheService.get(cacheKey, List.class);
        if (cached.isPresent()) {
            long latencyNanos = System.nanoTime() - startNano;
            return Map.of(
                    "prefix", prefix,
                    "suggestions", cached.get(),
                    "cached", true,
                    "latencyMicroseconds", latencyNanos / 1000.0,
                    "source", "Redis Cache-Aside"
            );
        }

        // 2. Query Java Trie
        List<AutocompleteSuggestion> suggestions = trie.search(normalized, limit > 0 ? limit : 10);
        long latencyNanos = System.nanoTime() - startNano;

        // 3. Populate Redis Cache
        cacheService.put(cacheKey, suggestions, 300); // 5 minutes TTL

        Map<String, Object> response = new LinkedHashMap<>();
        response.put("prefix", prefix);
        response.put("suggestions", suggestions);
        response.put("cached", false);
        response.put("latencyMicroseconds", latencyNanos / 1000.0);
        response.put("source", "Java Trie Data Structure");
        response.put("timestamp", Instant.now().toString());
        return response;
    }

    public Map<String, Object> getMetrics() {
        Map<String, Object> metrics = new LinkedHashMap<>();
        metrics.put("totalQueries", totalQueries.get());
        metrics.put("cacheMetrics", cacheService.getMetrics());
        return metrics;
    }
}
