package com.systemdesign.banking.systemdesign.idgenerator;

import org.springframework.stereotype.Component;

import java.security.SecureRandom;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Reusable Collision-Resistant Unique ID Generator.
 * Used across Customers, Accounts, Transactions, Ledgers, and Bill Payments.
 */
@Component
public class UniqueIdGenerator {

    private static final SecureRandom RANDOM = new SecureRandom();
    private static final char[] ALPHANUMERIC = "0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZ".toCharArray();

    public String generateCustomerId() {
        return "CUST-" + randomString(8);
    }

    public String generateAccountNumber() {
        // Format: ACC-XXXX-XXXXXXXX (e.g. ACC-1001-49281729)
        return "ACC-1001-" + (10000000 + RANDOM.nextInt(90000000));
    }

    public String generateTransactionReference() {
        // Format: TXN-YYYYMMDD-HHMMSS-XXXXXXXX
        long epoch = Instant.now().toEpochMilli();
        return "TXN-" + epoch + "-" + randomString(6);
    }

    public String generateLedgerId() {
        return "LDG-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
    }

    public String generatePaymentReference() {
        return "PAY-" + Instant.now().toEpochMilli() + "-" + randomString(6);
    }

    public String generateNotificationId() {
        return "NOTIF-" + UUID.randomUUID().toString();
    }

    public String generateOutboxEventId() {
        return "EVT-" + UUID.randomUUID().toString();
    }

    private String randomString(int length) {
        char[] buf = new char[length];
        for (int i = 0; i < length; i++) {
            buf[i] = ALPHANUMERIC[RANDOM.nextInt(ALPHANUMERIC.length)];
        }
        return new String(buf);
    }

    /**
     * Executes empirical collision and throughput evaluation for the System Design component.
     *
     * @param count Number of IDs to generate in benchmark
     * @return Benchmark metrics map containing total, durationMs, ratePerSec, and collisions
     */
    public Map<String, Object> benchmark(int count) {
        Set<String> set = ConcurrentHashMap.newKeySet(count);
        int collisions = 0;

        long start = System.nanoTime();
        for (int i = 0; i < count; i++) {
            String id = generateTransactionReference();
            if (!set.add(id)) {
                collisions++;
            }
        }
        long durationNanos = System.nanoTime() - start;
        double durationMs = durationNanos / 1_000_000.0;
        double durationSec = durationNanos / 1_000_000_000.0;
        double ratePerSec = durationSec > 0 ? (count / durationSec) : count;

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("sampleSize", count);
        result.put("durationMs", Math.round(durationMs * 100.0) / 100.0);
        result.put("ratePerSecond", Math.round(ratePerSec));
        result.put("collisionsObserved", collisions);
        result.put("collisionRatePercentage", (collisions / (double) count) * 100.0);
        result.put("sampleId", generateTransactionReference());
        result.put("timestamp", Instant.now().toString());
        return result;
    }
}
