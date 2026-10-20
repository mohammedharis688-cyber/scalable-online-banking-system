package com.systemdesign.banking.systemdesign.ratelimiter;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.*;
import java.util.concurrent.atomic.AtomicLong;

@Service
public class RedisTokenBucketRateLimiter {

    private static final Logger log = LoggerFactory.getLogger(RedisTokenBucketRateLimiter.class);

    private final RedisTemplate<String, String> redisTemplate;
    @SuppressWarnings("rawtypes")
    private final DefaultRedisScript<List> tokenBucketScript;

    // In-memory telemetry counters for system design evaluation metrics
    private final AtomicLong totalRequests = new AtomicLong(0);
    private final AtomicLong allowedRequests = new AtomicLong(0);
    private final AtomicLong rejectedRequests = new AtomicLong(0);

    @SuppressWarnings("rawtypes")
    public RedisTokenBucketRateLimiter(RedisTemplate<String, String> redisTemplate,
                                       DefaultRedisScript<List> tokenBucketScript) {
        this.redisTemplate = redisTemplate;
        this.tokenBucketScript = tokenBucketScript;
    }

    /**
     * Attempts to acquire tokens from the token bucket stored in Redis.
     *
     * @param bucketKey Unique identity (e.g. rate_limit:login:127.0.0.1 or customerId)
     * @param capacity Maximum token bucket capacity
     * @param refillRate Tokens added per second
     * @param requested Number of tokens required for this request (usually 1)
     * @return RateLimitResult containing allowance, remaining tokens, capacity
     */
    @SuppressWarnings("unchecked")
    public RateLimitResult tryAcquire(String bucketKey, double capacity, double refillRate, int requested) {
        totalRequests.incrementAndGet();
        double nowSeconds = Instant.now().toEpochMilli() / 1000.0;

        try {
            List<Object> result = redisTemplate.execute(
                    tokenBucketScript,
                    Collections.singletonList(bucketKey),
                    String.valueOf(capacity),
                    String.valueOf(refillRate),
                    String.valueOf(requested),
                    String.valueOf(nowSeconds)
            );

            if (result != null && result.size() >= 2) {
                long allowedNum = ((Number) result.get(0)).longValue();
                long remaining = Long.parseLong(result.get(1).toString());
                boolean allowed = (allowedNum == 1);

                if (allowed) {
                    allowedRequests.incrementAndGet();
                    return new RateLimitResult(true, remaining, (long) capacity, bucketKey, 0);
                } else {
                    rejectedRequests.incrementAndGet();
                    long retryAfter = refillRate > 0 ? (long) Math.ceil(1.0 / refillRate) : 1;
                    return new RateLimitResult(false, remaining, (long) capacity, bucketKey, retryAfter);
                }
            }
        } catch (Exception ex) {
            log.warn("Redis rate limiter unavailable for key [{}]. Fallback: granting request and logging alert. Reason: {}",
                    bucketKey, ex.getMessage());
            // Fallback policy (ADR-004): fail-open for availability with warning logged,
            // while core financial consistency remains safeguarded by MongoDB transactions.
            allowedRequests.incrementAndGet();
            return new RateLimitResult(true, (long) capacity, (long) capacity, bucketKey, 0);
        }

        // Default allow if result is unexpectedly null
        allowedRequests.incrementAndGet();
        return new RateLimitResult(true, (long) capacity, (long) capacity, bucketKey, 0);
    }

    public Map<String, Object> getMetrics() {
        long total = totalRequests.get();
        long allowed = allowedRequests.get();
        long rejected = rejectedRequests.get();
        double rejectionRate = total > 0 ? ((double) rejected / total) * 100.0 : 0.0;

        Map<String, Object> metrics = new LinkedHashMap<>();
        metrics.put("totalRequests", total);
        metrics.put("allowedRequests", allowed);
        metrics.put("rejectedRequests", rejected);
        metrics.put("rejectionRatePercentage", Math.round(rejectionRate * 100.0) / 100.0);
        metrics.put("timestamp", Instant.now().toString());
        return metrics;
    }

    public void resetMetrics() {
        totalRequests.set(0);
        allowedRequests.set(0);
        rejectedRequests.set(0);
    }
}
