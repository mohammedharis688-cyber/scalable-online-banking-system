# AGENTS.md — Permanent Working Instructions
## Scalable Online Banking System — UCS3513 System Design Laboratory

### 1. Project Objective and Mandatory Deliverables
Build a complete, working, tested, and documented mini-project named **Scalable Online Banking System** for the UCS3513 System Design Laboratory.
The system implements real core banking features backed by real system design laboratory techniques:
- Customer Registration, Login & Authentication (BCrypt hashing, JWT/Session, Redis-backed rate limiting).
- Account Management & Balance Enquiry (BigDecimal arithmetic, authoritative MongoDB storage, multi-account).
- Beneficiary Management (Add, list, validate, remove, safe transfer destinations).
- Fund Transfers (MongoDB multi-document atomic transactions across shards, idempotency key, double-entry ledger debit & credit, transactional outbox notification).
- Bill Payments (Synthetic billers: Electricity, Water, Internet, Mobile; balance deduction, ledger entry, outbox event).
- Transaction History (Database-backed, pagination, filter by type/status, scatter-gather query documentation).
- In-App Notifications (Transactional outbox -> Kafka topic -> Notification consumer -> MongoDB persistence).
- System Design Modules:
  1. Redis Token-Bucket Rate Limiter (atomic Redis Lua script, HTTP 429).
  2. Redis Key-Value Store & Cache-Aside (biller catalogue, autocomplete, URL shortener).
  3. Consistent Hashing Module (Java hash ring with virtual nodes, add/remove node simulations, routing metrics).
  4. Unique ID Generator (Collision-resistant, high throughput, generation benchmark).
  5. URL Shortener (Hash-based code generation, collision resolution, Redis cache-aside, internal public biller routes).
  6. Autocomplete Service (Java Trie prefix tree + Redis cache for billers & beneficiaries).
  7. Synthetic Web Crawler (Queue, visited set in Redis, breadth-first traversal of local synthetic HTML pages, biller data extraction).
- Performance & Scalability Evaluation:
  - Apache JMeter test plans (Low: 10, Medium: 25, High: 50 concurrent users).
  - Scalability and failure experiments (dataset scaling, adding shard, node failure, Redis/Kafka fallback).
- Deliverables:
  - Fully runnable Spring Boot application serving vanilla HTML/CSS/JS directly.
  - Complete Docker Compose cluster with MongoDB native sharding (mongos + 3 shard replica sets + 1 config replica set), Redis, and Kafka.
  - Comprehensive documentation in `docs/` and reports in `reports/performance/`.
  - Postman collection and environment.

---

### 2. Strict Technology Restrictions
**ALLOWED & REQUIRED:**
- **Language**: Java (JDK 24/26 compatible).
- **Backend & REST APIs**: Spring Boot (Spring Web, Spring Data MongoDB, Spring Data Redis, Spring Security, Spring Kafka).
- **Database**: MongoDB sharded cluster with mongos query router and replica sets supporting multi-document transactions.
- **Key-Value Store**: Redis.
- **Asynchronous Messaging**: Apache Kafka.
- **Containerisation**: Docker and Docker Compose.
- **Testing**: JUnit 5, Mockito, Spring Boot Test, Apache JMeter, Postman.
- **Frontend**: Plain HTML5, CSS3, and Vanilla JavaScript served statically from Spring Boot (`src/main/resources/static`).

**STRICTLY FORBIDDEN:**
- NO Python, FastAPI, Flask, or Python backend.
- NO Node.js or Node.js backend.
- NO React, Angular, Vue, Next.js, or external frontend framework.
- NO PostgreSQL, MySQL, or relational primary database.
- NO Kubernetes, paid cloud services, external banking APIs, or external payment gateways.
- NO Prometheus/Grafana or extra heavy monitoring platform.
- NO separate frontend server or build tool.

---

### 3. Git Branch Rules and Architecture Integrity
- Target repository: `mohammedharis688-cyber/scalable-online-banking-system`.
- Required branch structure:
  - `main`
  - `member-1` (Primary working branch for the entire implementation)
  - `member-2` (Clean starting point)
  - `member-3` (Clean starting point)
  - `member-4` (Clean starting point)
- **All implementation, test, documentation, and report commits MUST be on `member-1`.**
- Do NOT commit implementation directly to `main`.
- Never force-push, overwrite other people's work, or commit credentials/PATs.
- Always check the active branch before executing Git operations.

---

### 4. Memory Update and Continuation Protocol
When resuming work:
1. Read `AGENTS.md`, `PROJECT_STATE.md`, `NEXT_STEPS.md`, and `DECISIONS.md`.
2. Inspect the latest commit and `git status --short`.
3. Trust the recorded checkpoint and proceed directly to the next unfinished task in `NEXT_STEPS.md`.
4. Avoid re-auditing or rebuilding completed, verified components.
5. Update `PROJECT_STATE.md` and `NEXT_STEPS.md` after every meaningful milestone.
