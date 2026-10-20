# PROJECT_STATE.md — Authoritative Progress Memory
## Scalable Online Banking System — UCS3513 System Design Laboratory

## Project Identity
- **Project**: Scalable Online Banking System
- **Course**: UCS3513 — System Design Laboratory
- **GitHub Username**: `mohammedharis688-cyber`
- **Target Repository**: `https://github.com/mohammedharis688-cyber/scalable-online-banking-system`
- **Primary Working Branch**: `member-1`
- **Required Branches**: `main`, `member-1`, `member-2`, `member-3`, `member-4`
- **Primary Stack**: Java 21+ / Spring Boot 3.4.x, MongoDB Native Sharded Cluster, Redis, Apache Kafka, Docker Compose, Apache JMeter, Postman, Vanilla HTML/CSS/JS.

---

## Current Checkpoint
- **Current Phase**: Phase 1 — Environment, Git & Initial Setup
- **Active Branch**: `main` (switching to `member-1` after branch initialization)
- **Last Verified Commit**: Not yet committed (initializing bootstrap)
- **Last Completed Task**: Created Project Memory Specifications (`AGENTS.md`, `DECISIONS.md`, `NEXT_STEPS.md`)
- **Current Task**: Initialize Git branches (`main`, `member-1`, `member-2`, `member-3`, `member-4`), check out `member-1`, verify GitHub status
- **Next Task**: Establish Spring Boot project scaffold with Maven wrapper & pinned dependencies
- **Last Checkpoint Timestamp**: 2026-10-21T00:01:00+05:30

---

## Implementation Status

| Component | Status | Evidence |
|---|---|---|
| Git repository initialized | VERIFIED | Local `.git` initialized with origin remote |
| Branch structure (`main`, `member-1..4`) | IN_PROGRESS | Branch creation in progress |
| GitHub remote connectivity | BLOCKED (Auth) | Remote repo not found; manual push/creation credentials required |
| Docker & Docker Compose | VERIFIED | Docker 28.3.2 & Compose v2.38.2 active via WSL Ubuntu |
| Maven build tool | VERIFIED | Apache Maven 3.9.9 available & verified with Java 26 |
| Docker Compose Cluster definition | NOT_STARTED | `docker-compose.yml` to be created in T-005 |
| MongoDB native sharded cluster | NOT_STARTED | To be implemented in T-006 |
| Customer Auth & BCrypt | NOT_STARTED | To be implemented in T-007 |
| Accounts & Balances (BigDecimal) | NOT_STARTED | To be implemented in T-007 |
| Fund Transfers & Double-Entry Ledger | NOT_STARTED | To be implemented in T-008 |
| Beneficiaries & Bill Payments | NOT_STARTED | To be implemented in T-008, T-009 |
| Transaction History (paginated) | NOT_STARTED | To be implemented in T-010 |
| Redis Token Bucket Rate Limiter | NOT_STARTED | To be implemented in T-011 |
| Redis Cache-Aside Service | NOT_STARTED | To be implemented in T-012 |
| Consistent Hashing Module | NOT_STARTED | To be implemented in T-013 |
| Unique ID Generator | NOT_STARTED | To be implemented in T-014 |
| URL Shortener (public routes) | NOT_STARTED | To be implemented in T-015 |
| Trie Autocomplete Service | NOT_STARTED | To be implemented in T-016 |
| Local Web Crawler | NOT_STARTED | To be implemented in T-017 |
| Kafka Transactional Outbox & Notifications | NOT_STARTED | To be implemented in T-018 |
| Browser Frontend (Vanilla HTML/CSS/JS) | NOT_STARTED | To be implemented in T-019 |
| Automated JUnit / Integration Tests | NOT_STARTED | To be implemented in T-021 |
| Postman Collection | NOT_STARTED | To be implemented in T-022 |
| Apache JMeter Benchmark Plans | NOT_STARTED | To be implemented in T-023 |
| Scalability & Failure Experiments | NOT_STARTED | To be implemented in T-024 |
| Final Documentation (`docs/*`, `README.md`) | NOT_STARTED | To be implemented in T-025 |

---

## Known Errors and Unresolved Issues
1. GitHub authentication: `gh` CLI is not installed in the Windows environment, and remote repository `https://github.com/mohammedharis688-cyber/scalable-online-banking-system.git` does not exist on GitHub yet. All local work is being securely committed to local branch `member-1`. Clear instructions will be provided for the user to push to GitHub when ready.
2. Windows Docker Desktop service was stopped; Docker engine was activated and verified inside WSL Ubuntu (`dockerd` active, `docker compose version` v2.38.2 working).

---

## Latest Verified Results
- Java: `java version "26"` 2026-03-17 verified.
- Maven: `Apache Maven 3.9.9` verified.
- Docker: Docker daemon 28.3.2 running in WSL.
- Git: Local repository initialized on `main`, remote origin set.

---

## Recent Changes
- Created `.gitignore`
- Created `AGENTS.md`
- Created `DECISIONS.md`
- Created `NEXT_STEPS.md`
- Created `PROJECT_STATE.md`

---

## Exact Next Action
Create local branches `main`, `member-1`, `member-2`, `member-3`, `member-4`. Check out `member-1`. Make the initial bootstrap commit on `member-1`. Then proceed to Task T-004 (Spring Boot project scaffold).
