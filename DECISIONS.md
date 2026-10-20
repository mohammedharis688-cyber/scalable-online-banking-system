# DECISIONS.md — Architectural Decisions Record
## Scalable Online Banking System — UCS3513 System Design Laboratory

### ADR-001: Technology Stack and Framework Pinned Versions
- **Status**: ACCEPTED
- **Context**: UCS3513 requires Java, Spring Boot, MongoDB sharding, Redis, Kafka, Docker Compose, JMeter, and vanilla HTML/CSS/JS. Java 26 / JDK 24 is installed locally.
- **Decision**:
  - Spring Boot version: `3.4.3` (or latest compatible Spring Boot 3.4.x).
  - Java version: 21+ source/target compatibility (tested on local Java 26).
  - Maven Wrapper: included directly in the repo for reproducible builds.
  - Dependencies:
    - `spring-boot-starter-web`
    - `spring-boot-starter-data-mongodb`
    - `spring-boot-starter-data-redis`
    - `spring-boot-starter-security`
    - `spring-kafka`
    - `spring-boot-starter-validation`
    - `jjwt-api` / `jjwt-impl` / `jjwt-jackson` (0.12.6) for stateless JWT auth
    - `jsoup` (1.18.3) for local HTML crawler parsing
    - `redisson` or Jedis/Lettuce for Redis Lua scripts
- **Consequences**: Zero external frontend frameworks; all assets served from `src/main/resources/static`. Zero relational database dependencies.

---

### ADR-002: MongoDB Native Sharded Cluster Architecture & Shard Keys
- **Status**: ACCEPTED
- **Context**: The laboratory requires demonstrating actual native MongoDB sharding with 3 data shards and 1 config server replica set using `mongos` query router. Multi-document transactions must be supported.
- **Decision**:
  - Educational Replica Set topology:
    - Config server replica set: `rs-cfg` (1 member)
    - Shard 1 replica set: `rs-shard1` (1 member)
    - Shard 2 replica set: `rs-shard2` (1 member)
    - Shard 3 replica set: `rs-shard3` (1 member)
    - Query router: `mongos` on port `27017`
  - Shard Keys:
    - `customers`: `{ "customerId": "hashed" }` — uniform distribution across shards for user lookups.
    - `accounts`: `{ "accountNumber": "hashed" }` — balances and account records distributed across all shards; balance inquiry routes directly via shard key.
    - `transactions`: `{ "transactionId": "hashed" }` — high write throughput distributed across shards.
    - `ledgers`: `{ "transactionId": "hashed" }` — collocated with transaction ID for atomic double-entry balance audits.
  - Multi-Document Transactions:
    - Since MongoDB 4.2+, multi-document distributed transactions are supported across sharded clusters when shards are replica sets.
    - We use Spring Data MongoDB's `MongoTransactionManager` pointing to `mongos`.
- **Consequences**: Realistic native sharding with proven routing, multi-document atomic transfers, and documented scatter-gather queries for cross-shard customer history.

---

### ADR-003: Double-Entry Ledger & Transfer Idempotency
- **Status**: ACCEPTED
- **Context**: Financial consistency demands that transfers cannot overspend, duplicate, or lose ledger balance integrity.
- **Decision**:
  - For every transfer of amount $A$:
    - Check sender available balance $\ge A$ conditionally.
    - Debit sender account $-A$.
    - Credit recipient account $+A$.
    - Persist Transaction record with status `COMMITTED`, unique `transactionReference`, and `idempotencyKey`.
    - Persist two Ledger entries (DEBIT for sender, CREDIT for recipient) with matching `transactionReference`.
    - Persist Outbox event in `outbox_events` collection within the SAME MongoDB transaction.
  - Idempotency:
    - Client sends unique header or field `X-Idempotency-Key`.
    - If a key has already succeeded, return the existing transaction response immediately without modifying balances.
- **Consequences**: Complete ACID guarantee at the database layer. No partial transfers.

---

### ADR-004: Redis Rate-Limiting & Cache-Aside Architecture
- **Status**: ACCEPTED
- **Context**: Sensitive endpoints (e.g. login, transfer) require rate limiting following Experiment 6 (token bucket with Redis Lua script).
- **Decision**:
  - Redis Token Bucket:
    - Capacity $C$, refill rate $R$ (tokens/sec).
    - Redis Lua script executes:
      $tokens = \min(C, oldTokens + (now - lastRefill) \times R)$
      if $tokens \ge requested$: $tokens = tokens - 1$, allow; else reject with HTTP 429.
  - Cache-Aside:
    - Biller catalog, URL shortener lookups, and Autocomplete prefix suggestions.
    - Authoritative source of truth remains MongoDB.
    - Financial balances are NEVER cached.
  - Failure Policy:
    - If Redis is unreachable, read-through to MongoDB continues; rate limiter operates with conservative in-memory/fail-open fallback logged with alerts.
- **Consequences**: Ultra-fast atomic rate limiting and deterministic caching behavior.

---

### ADR-005: Kafka Transactional Outbox Pattern for Notifications
- **Status**: ACCEPTED
- **Context**: Financial operations must commit atomically in MongoDB without blocking on Kafka brokers or losing notification events if Kafka is temporarily down.
- **Decision**:
  - Transactional Outbox:
    - When transfer or bill payment commits, an outbox record is saved in MongoDB inside the atomic transaction.
    - A scheduled background poller reads pending outbox events, publishes to Kafka topic `bank.notifications`, and marks the outbox event as `PUBLISHED`.
    - Spring Kafka `@KafkaListener` receives the message, creates the persistent in-app notification in MongoDB `notifications` collection.
  - The frontend queries `GET /api/notifications` to display user notifications with unread badge and mark-as-read capability.
- **Consequences**: Zero message loss, eventual consistency for notifications, decoupled financial path.
