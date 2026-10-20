package com.systemdesign.banking.systemdesign;

import com.systemdesign.banking.systemdesign.cache.RedisCacheService;
import com.systemdesign.banking.systemdesign.consistenthashing.ConsistentHashRouter;
import com.systemdesign.banking.systemdesign.idgenerator.UniqueIdGenerator;
import com.systemdesign.banking.systemdesign.ratelimiter.RedisTokenBucketRateLimiter;
import com.systemdesign.banking.systemdesign.urlshortener.UrlShortenerService;
import org.bson.Document;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/system")
public class SystemInsightsController {

    private final UniqueIdGenerator idGenerator;
    private final RedisTokenBucketRateLimiter rateLimiter;
    private final RedisCacheService cacheService;
    private final ConsistentHashRouter hashRouter;
    private final UrlShortenerService shortenerService;
    private final MongoTemplate mongoTemplate;
    private final com.systemdesign.banking.seeder.DataScalingService dataScalingService;

    public SystemInsightsController(
            UniqueIdGenerator idGenerator,
            RedisTokenBucketRateLimiter rateLimiter,
            RedisCacheService cacheService,
            ConsistentHashRouter hashRouter,
            UrlShortenerService shortenerService,
            MongoTemplate mongoTemplate,
            com.systemdesign.banking.seeder.DataScalingService dataScalingService) {
        this.idGenerator = idGenerator;
        this.rateLimiter = rateLimiter;
        this.cacheService = cacheService;
        this.hashRouter = hashRouter;
        this.shortenerService = shortenerService;
        this.mongoTemplate = mongoTemplate;
        this.dataScalingService = dataScalingService;
    }

    @PostMapping("/seed/dataset")
    public ResponseEntity<?> generateScalingDataset(
            @RequestParam(defaultValue = "50") int customers,
            @RequestParam(defaultValue = "5") int transactionsPerCustomer) {
        Map<String, Object> result = dataScalingService.generateDataset(customers, transactionsPerCustomer);
        return ResponseEntity.ok(result);
    }

    @GetMapping("/id-benchmark")
    public ResponseEntity<?> runIdBenchmark(@RequestParam(defaultValue = "10000") int count) {
        if (count > 100000) {
            count = 100000; // Cap to prevent memory exhaustion
        }
        Map<String, Object> benchmark = idGenerator.benchmark(count);
        return ResponseEntity.ok(benchmark);
    }

    @GetMapping("/ratelimiter/metrics")
    public ResponseEntity<?> getRateLimiterMetrics() {
        return ResponseEntity.ok(rateLimiter.getMetrics());
    }

    @PostMapping("/ratelimiter/reset")
    public ResponseEntity<?> resetRateLimiterMetrics() {
        rateLimiter.resetMetrics();
        return ResponseEntity.ok(Map.of("message", "Rate limiter telemetry reset"));
    }

    @GetMapping("/cache/metrics")
    public ResponseEntity<?> getCacheMetrics() {
        return ResponseEntity.ok(cacheService.getMetrics());
    }

    @PostMapping("/cache/reset")
    public ResponseEntity<?> resetCacheMetrics() {
        cacheService.resetMetrics();
        return ResponseEntity.ok(Map.of("message", "Cache telemetry reset"));
    }

    @GetMapping("/sharding/status")
    public ResponseEntity<?> getShardingStatus() {
        Map<String, Object> status = new LinkedHashMap<>();
        try {
            Document ping = mongoTemplate.getDb().runCommand(new Document("ping", 1));
            status.put("databaseConnected", true);
            status.put("databaseName", mongoTemplate.getDb().getName());

            // Check if connected via mongos
            Document isMaster = mongoTemplate.getDb().runCommand(new Document("hello", 1));
            String msg = isMaster.getString("msg");
            boolean isMongos = "isdbgrid".equalsIgnoreCase(msg);
            status.put("isMongosRouter", isMongos);
            status.put("routerMessage", msg != null ? msg : "Standalone or ReplicaSet Member");
            status.put("serverStatus", ping);
        } catch (Exception ex) {
            status.put("databaseConnected", false);
            status.put("error", ex.getMessage());
        }
        return ResponseEntity.ok(status);
    }

    @GetMapping("/overview")
    public ResponseEntity<?> getSystemOverview() {
        Map<String, Object> overview = new LinkedHashMap<>();
        overview.put("timestamp", Instant.now().toString());
        overview.put("rateLimiter", rateLimiter.getMetrics());
        overview.put("cacheAside", cacheService.getMetrics());
        overview.put("consistentHashing", Map.of(
                "nodes", hashRouter.getNodes(),
                "virtualNodes", hashRouter.getRingSize()
        ));
        overview.put("urlShortener", shortenerService.getMetrics());

        return ResponseEntity.ok(overview);
    }
}
