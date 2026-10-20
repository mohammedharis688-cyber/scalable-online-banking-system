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
    public ResponseEntity<?> startCrawl(@RequestBody(required = false) Map<String, Object> request) {
        String seedUrl = "/synthetic/page1.html";
        int maxPages = 20;

        if (request != null) {
            if (request.containsKey("seedUrl")) {
                seedUrl = request.get("seedUrl").toString();
            }
            if (request.containsKey("maxPages")) {
                maxPages = Integer.parseInt(request.get("maxPages").toString());
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
