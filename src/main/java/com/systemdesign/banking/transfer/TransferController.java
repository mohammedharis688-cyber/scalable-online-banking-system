package com.systemdesign.banking.transfer;

import com.systemdesign.banking.auth.CustomerPrincipal;
import com.systemdesign.banking.systemdesign.ratelimiter.RateLimitResult;
import com.systemdesign.banking.systemdesign.ratelimiter.RedisTokenBucketRateLimiter;
import com.systemdesign.banking.transfer.dto.TransferRequest;
import com.systemdesign.banking.transfer.dto.TransferResponse;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/transfers")
public class TransferController {

    private final TransferService transferService;
    private final RedisTokenBucketRateLimiter rateLimiter;

    @Value("${banking.ratelimit.transfer.capacity:20}")
    private double transferCapacity;

    @Value("${banking.ratelimit.transfer.refill-rate:2.0}")
    private double transferRefillRate;

    public TransferController(TransferService transferService, RedisTokenBucketRateLimiter rateLimiter) {
        this.transferService = transferService;
        this.rateLimiter = rateLimiter;
    }

    @PostMapping
    public ResponseEntity<?> transferFunds(
            @Valid @RequestBody TransferRequest request,
            @RequestHeader(value = "X-Idempotency-Key", required = false) String idempotencyHeader,
            @AuthenticationPrincipal CustomerPrincipal principal) {

        // 1. Check Rate Limiter for Financial Submissions
        String bucketKey = "rate_limit:transfer:" + principal.getCustomerId();
        RateLimitResult rateCheck = rateLimiter.tryAcquire(bucketKey, transferCapacity, transferRefillRate, 1);
        if (!rateCheck.allowed()) {
            return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS).body(Map.of(
                    "error", "Too Many Requests",
                    "message", "Transfer rate limit exceeded. Please wait before initiating another transfer.",
                    "status", 429,
                    "remainingTokens", rateCheck.remainingTokens(),
                    "retryAfterSeconds", rateCheck.retryAfterSeconds()
            ));
        }

        // 2. Set Idempotency Key from header if not provided in payload body
        if ((request.getIdempotencyKey() == null || request.getIdempotencyKey().isBlank()) && idempotencyHeader != null) {
            request.setIdempotencyKey(idempotencyHeader);
        }

        try {
            TransferResponse response = transferService.executeTransfer(principal.getCustomerId(), request);
            return ResponseEntity.ok(response);
        } catch (SecurityException se) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of("error", "Forbidden", "message", se.getMessage()));
        } catch (IllegalArgumentException | IllegalStateException ex) {
            return ResponseEntity.badRequest().body(Map.of("error", "Bad Request", "message", ex.getMessage()));
        }
    }
}
