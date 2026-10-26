# NEXT_STEPS.md — Execution Queue
## Scalable Online Banking System — UCS3513 System Design Laboratory

| Task ID | Description | Dependencies | Status | Exact Next Action | Evidence Required |
|---|---|---|---|---|---|
| T-001 | Inspect environment, Git, tools & initialize memory files | None | VERIFIED | Completed | Memory files created and verified |
| T-002 | Initialize Git branches: `main`, `member-1`, `member-2`, `member-3`, `member-4`; checkout `member-1` | T-001 | VERIFIED | Completed | `git branch` shows all 5 branches, active on member-1 |
| T-003 | Check GitHub remote access & create repo if authorized | T-002 | VERIFIED | Completed | Origin configured to `mohammedharis688-cyber/scalable-online-banking-system` |
| T-004 | Establish Spring Boot project scaffold with Maven wrapper & pinned dependencies | T-002 | VERIFIED | Completed | `pom.xml`, `mvnw`, directory structure, clean compile |
| T-005 | Build Docker Compose infrastructure for MongoDB 3-shard cluster, Config RS, Mongos, Redis, Kafka | T-004 | VERIFIED | Completed | `docker-compose.yml` and `docker/init-cluster.sh` verified |
| T-006 | Implement MongoDB sharding initialization and collection shard keys | T-005 | VERIFIED | Completed | Hashed shard keys configured for customers, accounts, transactions |
| T-007 | Implement Core Banking Backend: Customer Auth (BCrypt, JWT), Account Management & authoritative Balance Enquiry | T-004, T-006 | VERIFIED | Completed | JWT authentication, Customer, Account, balance enquiry |
| T-008 | Implement Beneficiary Management & Fund Transfers (ACID transactions, Idempotency, Double-entry Ledger, Outbox) | T-007 | VERIFIED | Completed | `TransferServiceTest` passed (4/4): atomic, overspend, idempotency |
| T-009 | Implement Bill Payments & Synthetic Biller Catalogue | T-008 | VERIFIED | Completed | Biller catalogue, BillPaymentService, ledger deduction |
| T-010 | Implement Transaction History with Pagination & Filters (scatter-gather documented) | T-008, T-009 | VERIFIED | Completed | History repository query, pagination, filters |
| T-011 | Implement System Design: Redis Token Bucket Rate Limiter (atomic Lua script, HTTP 429) | T-007 | VERIFIED | Completed | Redis Lua script, RateLimitResult, HTTP 429 |
| T-012 | Implement System Design: Redis Cache-Aside & Biller/Shortener Caching | T-009 | VERIFIED | Completed | Cache-aside for billers, shortener, and autocomplete |
| T-013 | Implement System Design: Consistent Hashing Ring with Virtual Nodes | T-004 | VERIFIED | Completed | `ConsistentHashRouterTest` passed (3/3): virtual nodes, remapping |
| T-014 | Implement System Design: Unique ID Generator & Collision Benchmark | T-004 | VERIFIED | Completed | `UniqueIdGeneratorTest` passed (2/2): zero collisions |
| T-015 | Implement System Design: URL Shortener with Redis Cache-Aside & Collision Handling | T-004, T-012 | VERIFIED | Completed | `UrlShortenerServiceTest` passed (2/2): MD5, collision resolution |
| T-016 | Implement System Design: Trie Autocomplete Service + Redis Cache | T-004, T-012 | VERIFIED | Completed | `TrieAutocompleteTest` passed (3/3): prefix search, Redis cache |
| T-017 | Implement System Design: Synthetic Web Crawler (Queue, Visited Set, Redis) | T-004, T-012 | VERIFIED | Completed | `SyntheticWebCrawlerTest` passed (1/1): BFS queue, visited set |
| T-018 | Implement Kafka Notification Pipeline (Outbox poller -> Kafka -> Consumer -> MongoDB) | T-008, T-009 | VERIFIED | Completed | Transactional outbox publisher & Kafka listener |
| T-019 | Implement Rich Browser Frontend (HTML5, CSS3, Vanilla JS, served by Spring Boot static) | T-007 - T-018 | VERIFIED | Completed | Glassmorphic fintech dashboard, transfers, bills, history, insights |
| T-020 | Create Synthetic Database Seed Data (idempotent bootstrap) | T-007 - T-010 | VERIFIED | Completed | `DatabaseSeeder` with John Doe, Alice Smith, Bob Jones, accounts |
| T-021 | Automated Testing: JUnit 5 & Integration Tests | T-007 - T-018 | VERIFIED | Completed | 15/15 tests run, 0 failures, 100% pass |
| T-022 | Postman Collection & Environment Export | T-019 | VERIFIED | Completed | `postman/scalable-online-banking.postman_collection.json` ready |
| T-023 | Apache JMeter Performance Test Plans & Experiments (Workloads: 10, 25, 50 users) | T-019 | VERIFIED | Completed | JMX plan, shell and bat execution scripts |
| T-024 | Scalability & Failure Experiments (Shard addition, consistent hashing, component failure) | T-006, T-023 | VERIFIED | Completed | Experiments A through F documented with metrics and analysis |
| T-025 | Comprehensive Documentation & Handover | T-001 - T-024 | VERIFIED | Completed | Master README.md, 8 docs in `docs/`, and final handover report |
| T-026 | Redesign Banking Frontend (Northline) & Integrate Functional Autocomplete | T-016, T-019 | VERIFIED | Completed | Northline CSS, global search Trie integration, recipient search, browser verified |
