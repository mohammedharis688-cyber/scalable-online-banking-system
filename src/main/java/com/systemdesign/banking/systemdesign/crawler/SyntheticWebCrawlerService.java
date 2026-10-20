package com.systemdesign.banking.systemdesign.crawler;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.ClassPathResource;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.CopyOnWriteArrayList;

@Service
public class SyntheticWebCrawlerService {

    private static final Logger log = LoggerFactory.getLogger(SyntheticWebCrawlerService.class);

    private final RedisTemplate<String, String> redisTemplate;

    private static final String REDIS_VISITED_KEY = "crawler:visited";
    private static final String ALLOWED_PREFIX = "/synthetic/";

    private final List<Map<String, String>> extractedRecords = new CopyOnWriteArrayList<>();
    private Map<String, Object> lastCrawlMetrics = new LinkedHashMap<>();

    public SyntheticWebCrawlerService(RedisTemplate<String, String> redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    /**
     * Executes synthetic web crawling of approved local synthetic HTML pages.
     * Guarantees zero SSRF risk by strictly restricting URLs to /synthetic/* classpath files.
     */
    public synchronized Map<String, Object> executeCrawl(String seedUrl, int maxPages) {
        if (seedUrl == null || !seedUrl.startsWith(ALLOWED_PREFIX)) {
            seedUrl = "/synthetic/page1.html";
        }
        if (maxPages <= 0) {
            maxPages = 20;
        }

        extractedRecords.clear();
        try {
            redisTemplate.delete(REDIS_VISITED_KEY);
        } catch (Exception ignored) {
        }

        Queue<String> queue = new LinkedList<>();
        Set<String> localVisited = new HashSet<>();
        queue.add(seedUrl);

        int pagesProcessed = 0;
        int pagesDiscovered = 1;
        int duplicatesAvoided = 0;

        long startNanos = System.nanoTime();

        while (!queue.isEmpty() && pagesProcessed < maxPages) {
            String currentUrl = queue.poll();

            // Check visited set in Redis and memory
            Boolean isVisitedInRedis = false;
            try {
                isVisitedInRedis = redisTemplate.opsForSet().isMember(REDIS_VISITED_KEY, currentUrl);
            } catch (Exception ignored) {
            }

            if (localVisited.contains(currentUrl) || Boolean.TRUE.equals(isVisitedInRedis)) {
                duplicatesAvoided++;
                continue;
            }

            // Mark as visited in memory and Redis
            localVisited.add(currentUrl);
            try {
                redisTemplate.opsForSet().add(REDIS_VISITED_KEY, currentUrl);
            } catch (Exception ignored) {
            }

            pagesProcessed++;
            log.info("Crawler visiting local permitted route: {}", currentUrl);

            // Fetch and parse local synthetic HTML page safely from classpath static resources
            String htmlContent = loadLocalSyntheticPage(currentUrl);
            if (htmlContent == null) {
                continue;
            }

            Document doc = Jsoup.parse(htmlContent);

            // 1. Extract synthetic biller entities
            Elements billerCards = doc.select(".biller-card");
            for (Element card : billerCards) {
                String code = card.attr("data-code");
                String name = card.attr("data-name");
                String category = card.attr("data-category");

                if (!code.isBlank() && !name.isBlank()) {
                    Map<String, String> record = new LinkedHashMap<>();
                    record.put("billerCode", code);
                    record.put("name", name);
                    record.put("category", category);
                    record.put("sourceUrl", currentUrl);
                    extractedRecords.add(record);
                }
            }

            // 2. Discover hyperlinks and enqueue eligible unseen paths
            Elements links = doc.select("a[href]");
            for (Element link : links) {
                String href = link.attr("href").trim();
                if (href.startsWith(ALLOWED_PREFIX)) {
                    pagesDiscovered++;
                    if (!localVisited.contains(href)) {
                        queue.add(href);
                    } else {
                        duplicatesAvoided++;
                    }
                }
            }
        }

        long durationNanos = System.nanoTime() - startNanos;
        double durationMs = durationNanos / 1_000_000.0;
        double durationSec = durationNanos / 1_000_000_000.0;
        double pagesPerSec = durationSec > 0 ? (pagesProcessed / durationSec) : pagesProcessed;

        Map<String, Object> metrics = new LinkedHashMap<>();
        metrics.put("seedUrl", seedUrl);
        metrics.put("pagesProcessed", pagesProcessed);
        metrics.put("pagesDiscovered", pagesDiscovered);
        metrics.put("uniqueUrlsVisited", localVisited.size());
        metrics.put("duplicateUrlsAvoided", duplicatesAvoided);
        metrics.put("syntheticRecordsExtracted", extractedRecords.size());
        metrics.put("durationMs", Math.round(durationMs * 100.0) / 100.0);
        metrics.put("pagesPerSecond", Math.round(pagesPerSec * 100.0) / 100.0);
        metrics.put("completedAt", Instant.now().toString());

        this.lastCrawlMetrics = metrics;
        return metrics;
    }

    public Map<String, Object> getLastCrawlMetrics() {
        return lastCrawlMetrics.isEmpty() ? Map.of("status", "No crawl executed yet") : lastCrawlMetrics;
    }

    public List<Map<String, String>> getExtractedRecords() {
        return new ArrayList<>(extractedRecords);
    }

    private String loadLocalSyntheticPage(String urlPath) {
        try {
            // Strip leading /
            String resourcePath = "static" + (urlPath.startsWith("/") ? urlPath : "/" + urlPath);
            ClassPathResource resource = new ClassPathResource(resourcePath);
            try (InputStream is = resource.getInputStream()) {
                return new String(is.readAllBytes(), StandardCharsets.UTF_8);
            }
        } catch (Exception ex) {
            log.warn("Failed to read local synthetic page [{}]: {}", urlPath, ex.getMessage());
            return null;
        }
    }
}
