# Security Safeguards & System Limitations
## Scalable Online Banking System — UCS3513 System Design Laboratory

This document reviews the security safeguards implemented in the application and transparently documents the technical limitations of this local educational system design prototype.

---

## 1. Security Safeguards Implemented

### 1.1 Synthetic Data Guarantee
- The application contains **zero real customer data**, real account numbers, real bank credentials, or live payment gateways.
- All pre-seeded accounts, billers, and transactions use clearly marked synthetic formats (e.g., `ACC-1001-XXXX`, `CUST-XXXX`).

### 1.2 Authentication & Password Hashing
- **BCrypt Encoding**: Customer passwords are encrypted using Spring Security's `BCryptPasswordEncoder` with salted cryptographic work factors. Raw passwords are never persisted.
- **Stateless JWT**: Sessions are authenticated using signed HMAC-SHA256 JWT tokens with 24-hour expiration.
- **Customer Ownership Enforcement**: Every protected API verifies that the authenticated `customerId` matches the owner of the requested account, beneficiary, or transfer source. A user cannot view or debit another customer's balance by tampering with request parameters.

### 1.3 Rate Limiting & Denial-of-Service Defense
- Redis-backed token bucket rate limiters protect sensitive endpoints (login, transfer).
- Excessive failed login attempts or financial submission bursts return **HTTP 429 Too Many Requests**, preventing credential-stuffing and algorithmic brute-force attacks.

### 1.4 SSRF & Open-Redirect Prevention
- **URL Shortener Guard**: Restricts shortened paths strictly to `/synthetic/*`. External domain URLs are explicitly rejected, preventing the shortener from serving as an open-redirect vehicle for phishing.
- **Web Crawler Guard**: The crawler will only enqueue and traverse paths matching the approved local `/synthetic/*` prefix, completely neutralizing Server-Side Request Forgery (SSRF).

### 1.5 Financial Concurrency & Precision Safeguards
- **BigDecimal Storage**: All balances and monetary computations strictly utilize `java.math.BigDecimal` and MongoDB `Decimal128`. Floating-point types (`float`, `double`) are strictly prohibited to eliminate binary representation rounding errors.
- **Idempotency Protection**: Every financial operation supports unique idempotency keys (`X-Idempotency-Key`). Network retries or accidental double-clicks safely return the existing transaction without repeating debits or credits.
- **Double-Entry Ledger Invariance**: For every financial mutation, balanced DEBIT and CREDIT entries are committed atomically within the same MongoDB transaction.

---

## 2. Limitations of the Local Laboratory Prototype

1. **Educational Replica Set Topology**:
   - In our Docker Compose arrangement, the Config Server and each of the three shards (`rs-shard1`, `rs-shard2`, `rs-shard3`) are configured as **single-member replica sets**.
   - *Production Requirement*: High-availability production clusters require at least 3 voting members per shard replica set across independent physical availability zones to provide automatic failover during hardware failure.
2. **Single-Node Kafka Broker (KRaft Mode)**:
   - Kafka runs as a single broker in KRaft mode with replication factor 1.
   - *Production Requirement*: A production messaging cluster requires a 3-broker or 5-broker Kafka cluster with `min.insync.replicas=2` and multi-AZ topic partitioning.
3. **Application Instance Consistency Hashing**:
   - The consistent-hashing module simulates routing decisions across virtual nodes using a Java `TreeMap` data structure. It demonstrates algorithmic routing and key remapping metrics rather than acting as a physical Layer-7 reverse proxy or load balancer.
4. **Local Hardware Resource Constraints**:
   - Benchmarks are conducted on a single host machine sharing CPU and disk I/O between the Docker containers, JMeter load generator, and Spring Boot application. Production throughput would scale significantly higher on dedicated server hardware with separate NVMe storage channels for each shard.
