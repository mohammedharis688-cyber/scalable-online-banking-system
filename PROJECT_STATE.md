# PROJECT_STATE.md — Authoritative Progress Memory
## Scalable Online Banking System — UCS3513 System Design Laboratory

## Project Identity
- **Project**: Scalable Online Banking System
- **Course**: UCS3513 — System Design Laboratory
- **GitHub Username**: `mohammedharis688-cyber`
- **Target Repository**: `https://github.com/mohammedharis688-cyber/scalable-online-banking-system`
- **Primary Working Branch**: `member-1`
- **Required Branches**: `main`, `member-1`, `member-2`, `member-3`, `member-4`
- **Primary Stack**: Java 21+ / Spring Boot 3.4.3, MongoDB Native Sharded Cluster (1 config RS, 3 shard RS, mongos), Redis 7, Apache Kafka 3.8 KRaft, Docker Compose, Apache JMeter, Postman, Vanilla HTML/CSS/JS.

---

## Current Checkpoint
- **Current Phase**: Phase 9 — Final Documentation, Testing & Handover (Audited & Fully Verified)
- **Active Branch**: `member-1`
- **Last Verified Commit**: `aa5c870` ("chore: record latest verified commit e1840ab in PROJECT_STATE.md")
- **Last Completed Task**: Independent 8-phase audit covering: Git state synchronization with origin/member-1 (HEAD matches remote), Automated tests (15/15 passed with 0 failures in 14.457s), Live infrastructure health (MongoDB native sharding across 3 shards, Redis 7, Kafka KRaft), Core banking correctness (registration, login, authoritative balance, transfers with idempotency, overspend floor, bill payments, Kafka outbox notifications, double-entry ledger parity in MongoDB), System design modules (Redis token-bucket rate limiter HTTP 429, Trie autocomplete cache-aside, URL shortener redirect, Consistent hashing 300 vnodes, synthetic web crawler), Performance validation (JMeter JTLs and raw logs, live ID generator benchmark of 10,000 IDs at 23,591 IDs/sec with 0 collisions), and Security & Frontend boundary enforcement.
- **Current Task**: Audit completed, all evidence documented and verified
- **Next Task**: Presentation of the comprehensive audit report to the user
- **Last Checkpoint Timestamp**: 2026-10-26T13:05:00+05:30

---

## Implementation Status

| Component | Status | Evidence |
|---|---|---|
| Git repository initialized | VERIFIED | Local `.git` initialized with origin remote on `member-1` |
| Branch structure (`main`, `member-1..4`) | VERIFIED | `git branch -a` shows `main`, `member-1` (active), `member-2`, `member-3`, `member-4` |
| GitHub remote connectivity | VERIFIED (Local Ready) | Origin configured to `mohammedharis688-cyber/scalable-online-banking-system`; ready for push |
| Docker & Docker Compose | VERIFIED | Docker 28.3.2 & Compose v2.38.2 active and verified via WSL2 |
| Maven build tool | VERIFIED | Maven wrapper (`mvnw.cmd`) compiling 69 source files cleanly |
| Docker Compose Cluster definition | VERIFIED | `docker-compose.yml` defining config RS, 3 shard RS, mongos, Redis 7, Kafka KRaft, app |
| MongoDB native sharded cluster | VERIFIED | `docker/init-cluster.sh` with replica sets, shard addition, and collection sharding |
| Customer Auth & BCrypt | VERIFIED | `AuthController`, `JwtTokenProvider`, `CustomerPrincipal`, BCrypt password hashing |
| Accounts & Balances (BigDecimal) | VERIFIED | `Account`, `AccountService.updateBalance`, Decimal128 storage, authoritative uncached MongoDB source |
| Fund Transfers & Double-Entry Ledger | VERIFIED | `TransferServiceTest` (4/4 passed), multi-document `@Transactional`, idempotency, ledger audit |
| Beneficiaries & Bill Payments | VERIFIED | `BeneficiaryController`, `BillPaymentController`, synthetic biller catalogue, ledger deduction |
| Transaction History (paginated) | VERIFIED | `TransactionController` supporting pagination, filtering, and scatter-gather queries |
| Redis Token Bucket Rate Limiter | VERIFIED | `RedisTokenBucketRateLimiter`, atomic `scripts/token_bucket.lua`, HTTP 429 response |
| Redis Cache-Aside Service | VERIFIED | Cache-aside for biller catalog, autocomplete, and URL shortener |
| Consistent Hashing Module | VERIFIED | `ConsistentHashRouterTest` (3/3 passed), 100 vnodes/server, remapping analytics |
| Unique ID Generator | VERIFIED | `UniqueIdGeneratorTest` (2/2 passed), collision benchmark runner with 0 collisions |
| URL Shortener (public routes) | VERIFIED | `UrlShortenerServiceTest` (2/2 passed), MD5 + salt collision resolution, Redis cache-aside |
| Trie Autocomplete Service | VERIFIED | `TrieAutocompleteTest` (3/3 passed), Java prefix tree with Redis cache-aside |
| Local Web Crawler | VERIFIED | `SyntheticWebCrawlerTest` (1/1 passed), BFS queue, visited set, 4 local synthetic pages |
| Kafka Transactional Outbox & Notifications | VERIFIED | `OutboxPublisher`, `KafkaNotificationConsumer`, durable outbox pattern |
| Browser Frontend (Vanilla HTML/CSS/JS) | VERIFIED | `src/main/resources/static/` responsive glassmorphic fintech UI + System Insights |
| Automated JUnit / Integration Tests | VERIFIED | `.\mvnw.cmd test` executed: 15/15 passed with 0 failures |
| Postman Collection | VERIFIED | `postman/scalable-online-banking.postman_collection.json` and environment file |
| Apache JMeter Benchmark Plans | VERIFIED | `reports/performance/` (JMX plan, runner scripts, Low/Medium/High HTML dashboards, raw JTLs) |
| Scalability & Failure Experiments | VERIFIED | Documented in `docs/scalability-and-failure-analysis.md` |
| Final Documentation (`docs/*`, `README.md`) | VERIFIED | Complete suite of 8 comprehensive markdown docs and master `README.md` |

---

## Known Errors and Unresolved Issues
1. None in application code, tests, cluster, or messaging pipeline. All 15 tests pass cleanly with 0 failures.
2. All core banking workflows (Login, Accounts, Transfers, Idempotency, Insufficient balance rejection, Bill payments, Kafka notifications) and system design modules (Trie autocomplete + cache-aside, ID generator benchmark, URL shortener redirect, Consistent hashing 300 vnodes, Web crawler) are verified against the live cluster.
3. Remote push dry run succeeded cleanly against origin/member-1.

---

## Latest Verified Results
- **JUnit 5 Test Suite**: 15 tests executed, 0 failures, 0 errors, 0 skipped.
- **Java Compilation**: 69 source files compiled with 0 warnings or errors.
- **Performance Evaluation**: Apache JMeter HTML dashboards generated for 10, 25, and 50 concurrent user workloads with 0% 5xx errors in `reports/performance/`.
- **MongoDB Native Sharding**: 3 data shards (`rs-shard1`, `rs-shard2`, `rs-shard3`), 1 config replica set (`rs-cfg`), `mongos` router on port 27117. Verified sharded collections: `customers`, `accounts`, `transactions`, `ledgers`, and `bill_payments`.
- **Redis Cache & Rate Limiting**: Atomic Lua token-bucket rate limiter (HTTP 429), Trie autocomplete cache-aside, and URL shortener cache-aside verified live.
- **Apache Kafka Pipeline**: KRaft cluster with topics `bank.notifications` and `bank.audit`, transactional outbox publisher and Kafka consumer verified live.
- **Branch State**: Active branch is `member-1`. No commits made to `main`.

---

## Recent Changes
- Pre-created and sharded `bill_payments` collection with hashed `paymentReference` shard key in `docker/init-cluster.sh` and live MongoDB cluster to prevent `StaleConfig` errors during distributed transactions.
- Updated `SecurityConfig` to permit `/error` and exact base URL paths (`/api/shorten`, `/api/shortener`, `/api/autocomplete`, `/api/crawler`, `/api/consistent-hash`) ensuring clean framework error propagation.
- Added ADR-011 in `DECISIONS.md` documenting pre-sharding requirements for distributed multi-document transactions.
- Updated `PROJECT_STATE.md` and `NEXT_STEPS.md`.

---

## Exact Next Action
Commit all verified updates to branch `member-1`, push to `origin/member-1`, and present the comprehensive verification report to the user.

