package com.systemdesign.banking.systemdesign.ratelimiter;

public record RateLimitResult(
        boolean allowed,
        long remainingTokens,
        long capacity,
        String bucketKey,
        long retryAfterSeconds
) {
}
