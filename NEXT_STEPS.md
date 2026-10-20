# NEXT_STEPS.md — Execution Queue
## Scalable Online Banking System — UCS3513 System Design Laboratory

| Task ID | Description | Dependencies | Status | Exact Next Action | Evidence Required |
|---|---|---|---|---|---|
| T-001 | Inspect environment, Git, tools & initialize memory files | None | IN_PROGRESS | Finalize memory files & Git branch setup | Memory files committed, branches verified |
| T-002 | Initialize Git branches: `main`, `member-1`, `member-2`, `member-3`, `member-4`; checkout `member-1` | T-001 | PENDING | Run Git branch creation and checkout member-1 | `git branch` shows all 5 branches, active on member-1 |
| T-003 | Check GitHub remote access & create repo if authorized | T-002 | PENDING | Check GitHub authentication & remote status | Documented remote status / instructions |
| T-004 | Establish Spring Boot project scaffold with Maven wrapper & pinned dependencies | T-002 | PENDING | Create `pom.xml`, `mvnw`, directory structure, application main | Successful clean compile with `./mvnw compile` |
| T-005 | Build Docker Compose infrastructure for MongoDB 3-shard cluster, Config RS, Mongos, Redis, Kafka | T-004 | PENDING | Create `docker-compose.yml` and cluster init scripts | Services startup and cluster health verified |
| T-006 | Implement MongoDB sharding initialization and collection shard keys | T-005 | PENDING | Create `init-sharding.js` and verify with `sh.status()` | Cluster status showing 3 active shards |
| T-007 | Implement Core Banking Backend: Customer Auth (BCrypt, JWT), Account Management & authoritative Balance Enquiry | T-004, T-006 | PENDING | Implement entities, repositories, services, controllers | JUnit tests passing for auth and accounts |
| T-008 | Implement Beneficiary Management & Fund Transfers (ACID transactions, Idempotency, Double-entry Ledger, Outbox) | T-007 | PENDING | Implement transfer service with `@Transactional`, ledger, idempotency | Atomic transfer test, overspend rejection test |
| T-009 | Implement Bill Payments & Synthetic Biller Catalogue | T-008 | PENDING | Implement biller entities, payment flow, ledger deduction | Bill payment test & ledger balance |
| T-010 | Implement Transaction History with Pagination & Filters (scatter-gather documented) | T-008, T-009 | PENDING | Implement history repository query & controller | History returns debits, credits, payments |
| T-011 | Implement System Design: Redis Token Bucket Rate Limiter (atomic Lua script, HTTP 429) | T-007 | PENDING | Implement Redis script, filter/interceptor, burst test | 429 response when tokens depleted |
| T-012 | Implement System Design: Redis Cache-Aside & Biller/Shortener Caching | T-009 | PENDING | Implement cache service with metrics, hit/miss tracking | Cache hit and miss metrics verified |
| T-013 | Implement System Design: Consistent Hashing Ring with Virtual Nodes | T-004 | PENDING | Implement Java hash ring, add/remove nodes, REST endpoints | Remapping metrics before/after node changes |
| T-014 | Implement System Design: Unique ID Generator & Collision Benchmark | T-004 | PENDING | Implement ID generator service and throughput benchmark | 100k+ ID benchmark with 0 collisions |
| T-015 | Implement System Design: URL Shortener with Redis Cache-Aside & Collision Handling | T-004, T-012 | PENDING | Implement hash-based shortener for synthetic biller info pages | Short link creation, redirect, 404 handling |
| T-016 | Implement System Design: Trie Autocomplete Service + Redis Cache | T-004, T-012 | PENDING | Implement Trie data structure, prefix query, Redis caching | Prefix search returns matching billers/beneficiaries |
| T-017 | Implement System Design: Synthetic Web Crawler (Queue, Visited Set, Redis) | T-004, T-012 | PENDING | Implement crawler service, local synthetic HTML pages | Crawl throughput, duplicate avoidance metrics |
| T-018 | Implement Kafka Notification Pipeline (Outbox poller -> Kafka -> Consumer -> MongoDB) | T-008, T-009 | PENDING | Implement Kafka producer, consumer, persistent notification records | Notification created upon committed transfer |
| T-019 | Implement Rich Browser Frontend (HTML5, CSS3, Vanilla JS, served by Spring Boot static) | T-007 - T-018 | PENDING | Build dashboard, accounts, transfer, bills, history, notifications, insights UI | Interactive UI tested via browser |
| T-020 | Create Synthetic Database Seed Data (idempotent bootstrap) | T-007 - T-010 | PENDING | Implement `DatabaseSeeder` with demo users, accounts, billers | Seed data verified on startup |
| T-021 | Automated Testing: JUnit 5 & Integration Tests | T-007 - T-018 | PENDING | Write comprehensive test suite for all workflows | All automated tests passing |
| T-022 | Postman Collection & Environment Export | T-019 | PENDING | Create collection covering all endpoints with tests and variables | Runnable Postman JSON file |
| T-023 | Apache JMeter Performance Test Plans & Experiments (Workloads: 10, 25, 50 users) | T-019 | PENDING | Create JMeter JMX test plans, execute workloads, collect metrics | Reports, graphs, HTML dashboard |
| T-024 | Scalability & Failure Experiments (Shard addition, consistent hashing, component failure) | T-006, T-023 | PENDING | Execute experiments, document observations | Actual measured outputs in reports |
| T-025 | Comprehensive Documentation & Handover | T-001 - T-024 | PENDING | Complete README.md, docs/*, reports/*, verify branch & commit | Complete project delivery |
