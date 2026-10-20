package com.systemdesign.banking.systemdesign.crawler;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.SetOperations;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SyntheticWebCrawlerTest {

    @Mock
    private RedisTemplate<String, String> redisTemplate;

    @Mock
    private SetOperations<String, String> setOperations;

    private SyntheticWebCrawlerService crawlerService;

    @BeforeEach
    void setUp() {
        lenient().when(redisTemplate.opsForSet()).thenReturn(setOperations);
        lenient().when(setOperations.isMember(anyString(), anyString())).thenReturn(false);
        crawlerService = new SyntheticWebCrawlerService(redisTemplate);
    }

    @Test
    @DisplayName("Should traverse local synthetic HTML pages and extract synthetic billers without SSRF")
    void testCrawlExecution() {
        Map<String, Object> metrics = crawlerService.executeCrawl("/synthetic/page1.html", 10);

        assertNotNull(metrics);
        int pagesProcessed = (Integer) metrics.get("pagesProcessed");
        int recordsExtracted = (Integer) metrics.get("syntheticRecordsExtracted");
        int duplicatesAvoided = (Integer) metrics.get("duplicateUrlsAvoided");

        assertTrue(pagesProcessed >= 3, "Crawler should traverse through linked local synthetic pages");
        assertTrue(recordsExtracted >= 4, "Crawler should extract synthetic biller cards");
        assertTrue(duplicatesAvoided >= 1, "Crawler should avoid duplicate URLs found on pages");
    }
}
