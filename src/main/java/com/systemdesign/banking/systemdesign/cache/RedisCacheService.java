package com.systemdesign.banking.systemdesign.cache;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.atomic.AtomicLong;

@Service
public class RedisCacheService {

    private static final Logger log = LoggerFactory.getLogger(RedisCacheService.class);

    private final RedisTemplate<String, Object> redisTemplate;
    private final ObjectMapper objectMapper;

    private final AtomicLong cacheHits = new AtomicLong(0);
    private final AtomicLong cacheMisses = new AtomicLong(0);
    private final AtomicLong cacheWrites = new AtomicLong(0);
    private final AtomicLong cacheEvictions = new AtomicLong(0);

    public RedisCacheService(RedisTemplate<String, Object> redisTemplate, ObjectMapper objectMapper) {
        this.redisTemplate = redisTemplate;
        this.objectMapper = objectMapper;
    }

    public <T> Optional<T> get(String key, Class<T> clazz) {
        try {
            Object value = redisTemplate.opsForValue().get(key);
            if (value != null) {
                cacheHits.incrementAndGet();
                log.debug("Cache HIT for key: {}", key);
                T deserialized = objectMapper.convertValue(value, clazz);
                return Optional.of(deserialized);
            } else {
                cacheMisses.incrementAndGet();
                log.debug("Cache MISS for key: {}", key);
                return Optional.empty();
            }
        } catch (Exception ex) {
            log.warn("Redis cache read failure for key [{}]. Fallback: treating as cache miss. Reason: {}",
                    key, ex.getMessage());
            cacheMisses.incrementAndGet();
            return Optional.empty();
        }
    }

    public void put(String key, Object value, long ttlSeconds) {
        try {
            redisTemplate.opsForValue().set(key, value, Duration.ofSeconds(ttlSeconds));
            cacheWrites.incrementAndGet();
            log.debug("Cache PUT for key: {} (TTL: {}s)", key, ttlSeconds);
        } catch (Exception ex) {
            log.warn("Redis cache write failure for key [{}]: {}", key, ex.getMessage());
        }
    }

    public void evict(String key) {
        try {
            redisTemplate.delete(key);
            cacheEvictions.incrementAndGet();
            log.debug("Cache EVICT for key: {}", key);
        } catch (Exception ex) {
            log.warn("Redis cache eviction failure for key [{}]: {}", key, ex.getMessage());
        }
    }

    public Map<String, Object> getMetrics() {
        long hits = cacheHits.get();
        long misses = cacheMisses.get();
        long writes = cacheWrites.get();
        long evictions = cacheEvictions.get();
        long totalLookups = hits + misses;
        double hitRatio = totalLookups > 0 ? ((double) hits / totalLookups) * 100.0 : 0.0;

        Map<String, Object> metrics = new LinkedHashMap<>();
        metrics.put("cacheHits", hits);
        metrics.put("cacheMisses", misses);
        metrics.put("cacheWrites", writes);
        metrics.put("cacheEvictions", evictions);
        metrics.put("totalLookups", totalLookups);
        metrics.put("hitRatioPercentage", Math.round(hitRatio * 100.0) / 100.0);
        metrics.put("timestamp", Instant.now().toString());
        return metrics;
    }

    public void resetMetrics() {
        cacheHits.set(0);
        cacheMisses.set(0);
        cacheWrites.set(0);
        cacheEvictions.set(0);
    }
}
