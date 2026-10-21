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
- **Current Phase**: Phase 9 — Final Documentation, Testing & Handover
- **Active Branch**: `member-1`
- **Last Verified Commit**: `eae5abe` ("feat: optimize sharded balance updates, add jmeter benchmark dashboards, and update architectural decisions")
- **Last Completed Task**: Automated test verification with JUnit 5 on JDK 26 (15 tests run, 0 failures, 100% pass) and JMeter benchmark execution (Low, Medium, High dashboards generated in `reports/performance/`)
- **Current Task**: Complete handover report and milestone sign-off
- **Next Task**: Remote push to GitHub origin (`git push -u origin member-1`) when user credentials are provided
- **Last Checkpoint Timestamp**: 2026-10-21T15:35:00+05:30

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
1. None in application code or tests. 15 of 15 tests pass cleanly.
2. The remote GitHub repository has not yet been initialized on GitHub servers under `mohammedharis688-cyber`. Since git push requires user credentials or an active token, the local repository contains the complete commit history on `member-1`, ready for a one-line push (`git push -u origin member-1`).

---

## Latest Verified Results
- **JUnit 5 Test Suite**: 15 tests executed, 0 failures, 0 errors, 0 skipped.
- **Java Compilation**: 69 source files compiled with 0 warnings or errors.
- **Performance Evaluation**: Apache JMeter HTML dashboards generated for 10, 25, and 50 concurrent user workloads with 0% 5xx errors.
- **MongoDB Sharding**: Shard-key targeted balance update operations implemented in `AccountService`.
- **Branch State**: Active branch is `member-1`. No commits made to `main`.

---

## Recent Changes
- Updated `AccountService`, `TransferService`, and `BillPaymentService` with shard-key targeted balance updates via `mongoTemplate.updateFirst`.
- Refined sharded entity indexes to avoid non-shard-key unique index constraints in native MongoDB sharding.
- Added `/api/shorten` and `/api/consistent-hash` aliases and flexible request mappings.
- Generated complete JMeter HTML benchmark report dashboards and raw JTL logs in `reports/performance/`.
- Updated ADR records in `DECISIONS.md` (ADR-009, ADR-010).

---

## Exact Next Action
Commit all updates to branch `member-1` and deliver the comprehensive handover report to the user.
