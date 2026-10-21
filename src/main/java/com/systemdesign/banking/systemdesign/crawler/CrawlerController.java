package com.systemdesign.banking.systemdesign.crawler;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/crawler")
public class CrawlerController {

    private final SyntheticWebCrawlerService crawlerService;

    public CrawlerController(SyntheticWebCrawlerService crawlerService) {
        this.crawlerService = crawlerService;
    }

    @PostMapping("/start")
    public ResponseEntity<?> startCrawl(
            @RequestParam(name = "seedUrl", required = false) String seedUrlParam,
            @RequestParam(name = "maxPages", required = false, defaultValue = "20") int maxPagesParam,
            @RequestBody(required = false) String bodyString) {
        String seedUrl = seedUrlParam != null ? seedUrlParam : "/synthetic/page1.html";
        int maxPages = maxPagesParam;

        if (bodyString != null && !bodyString.isBlank()) {
            try {
                com.fasterxml.jackson.databind.ObjectMapper mapper = new com.fasterxml.jackson.databind.ObjectMapper();
                @SuppressWarnings("unchecked")
                Map<String, Object> map = mapper.readValue(bodyString, Map.class);
                if (map.containsKey("seedUrl")) {
                    seedUrl = map.get("seedUrl").toString();
                }
                if (map.containsKey("maxPages")) {
                    maxPages = Integer.parseInt(map.get("maxPages").toString());
                }
            } catch (Exception ignored) {
            }
        }

        Map<String, Object> metrics = crawlerService.executeCrawl(seedUrl, maxPages);
        return ResponseEntity.ok(metrics);
    }

    @GetMapping("/status")
    public ResponseEntity<?> getStatus() {
        return ResponseEntity.ok(crawlerService.getLastCrawlMetrics());
    }

    @GetMapping("/results")
    public ResponseEntity<List<Map<String, String>>> getResults() {
        return ResponseEntity.ok(crawlerService.getExtractedRecords());
    }
}
