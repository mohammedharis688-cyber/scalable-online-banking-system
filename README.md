# Scalable Online Banking System
### UCS3513 — System Design Laboratory Mini-Project
**Author**: Mohammed Haris ([@mohammedharis688-cyber](https://github.com/mohammedharis688-cyber))  
**Repository**: `mohammedharis688-cyber/scalable-online-banking-system`  
**Working Branch**: `member-1`  
**License**: MIT (Academic / Educational Demonstration)

---

## 1. Project Overview
The **Scalable Online Banking System** is an end-to-end, high-performance financial web platform built specifically for the **UCS3513 System Design Laboratory**. 

Unlike conventional web applications that rely on single-node databases or in-memory mockups, this system integrates genuine distributed system design paradigms:
1. **Native MongoDB Database Sharding**: A real cluster composed of a 3-shard replica set (`rs-shard1`, `rs-shard2`, `rs-shard3`), a Config Server replica set (`rs-cfg`), and a `mongos` query router.
2. **ACID Distributed Transactions & Double-Entry Ledger**: Complete financial consistency for fund transfers and bill settlements using Spring Data MongoDB multi-document transactions across shards.
3. **Transactional Outbox & Apache Kafka Event Streaming**: Reliable, at-least-once asynchronous notification processing without dual-write inconsistencies.
4. **Redis Token-Bucket Rate Limiter**: High-throughput rate limiting implemented using an atomic Redis Lua script (Experiment 6) returning HTTP 429 upon token depletion.
5. **Redis Cache-Aside**: High-speed caching for biller catalogues, URL shortener lookups, and autocomplete suggestions.
6. **Consistent Hashing Hash Ring**: Java implementation featuring virtual nodes, dynamic node additions/removals, and empirical key remapping evaluation (Experiment 5).
7. **Collision-Resistant Unique ID Generator**: High-speed identifier generator with verified 0.00% collision rate across 50,000+ samples.
8. **Public URL Shortener**: MD5-based 6-character code generator with collision resolution and Redis cache-aside (Experiments 3 & 4).
9. **Trie-Based Autocomplete**: In-memory Java prefix tree with ranked suggestions and microsecond-level latency (Experiment 7).
10. **Synthetic Web Crawler**: Breadth-first crawler with Redis-backed visited URL sets for local synthetic utility directories (Experiment 8).
11. **Vanilla Browser Frontend**: Served directly from Spring Boot's static resources without external frameworks or separate Node.js servers.

---

## 2. Technology Stack

| Purpose | Technology Selected | Version / Spec |
|---|---|---|
| Programming Language | Java | JDK 21+ / OpenJDK 26 compatible |
| Framework & REST APIs | Spring Boot | 3.4.3 |
| Primary Database | MongoDB Sharded Cluster | 7.0 (mongos + 3 shard RS + 1 config RS) |
| In-Memory Cache & Limiter | Redis | 7-alpine |
| Asynchronous Messaging | Apache Kafka | 3.8.0 (KRaft broker mode) |
| Containerisation | Docker & Docker Compose | v2.38+ |
| Load Testing | Apache JMeter | 5.x / 2.13 Headless CLI |
| API Testing | Postman | Collection v2.1.0 |
| Version Control | Git & GitHub | Branch `member-1` |
| Frontend | HTML5, CSS3, Vanilla JS | Served from `src/main/resources/static` |

---

## 3. Synthetic Demo Credentials

Pre-seeded accounts are immediately available upon application launch:

| Customer Name | Email Address | Password | Primary Account Number | Synthetic Starting Balance |
|---|---|---|---|---|
| **John Doe** | `john.doe@example.com` | `Password123!` | `ACC-1001-10000001` | **₹ 50,000.00 INR** |
| **Alice Smith** | `alice.smith@example.com` | `Password123!` | `ACC-1001-10000002` | **₹ 75,000.00 INR** |
| **Bob Jones** | `bob.jones@example.com` | `Password123!` | `ACC-1001-10000003` | **₹ 25,000.00 INR** |

*Note: All credentials, account numbers, and financial records are strictly synthetic.*

---

## 4. Quick Start & Launch Instructions

### Prerequisites
- Java 21 or later (`java -version`)
- Docker & Docker Compose (`docker compose version`)

### Option A: Complete Docker Compose Cluster (Recommended)
To launch the entire distributed architecture (mongos, 3 shards, config server, Redis, Kafka, and the Spring Boot application):

```bash
docker compose up --build
```

Wait for the cluster initialization script (`mongo-init` and `kafka-init`) to report readiness. Then access:
- **Banking Portal**: [http://localhost:8080](http://localhost:8080)
- **API Health Endpoint**: [http://localhost:8080/api/system/overview](http://localhost:8080/api/system/overview)

### Option B: Local Spring Boot Run (With Dockerized Infrastructure)
Start the supporting infrastructure first:
```bash
docker compose up mongo-configsvr mongo-shard1 mongo-shard2 mongo-shard3 mongos mongo-init redis kafka kafka-init -d
```

Then build and launch the application locally:
```bash
# Windows
.\mvnw.cmd spring-boot:run

# Linux / macOS / WSL
./mvnw spring-boot:run
```

---

## 5. Main Banking Workflows & Testing

1. **Authentication**: Sign in using `john.doe@example.com` / `Password123!` or click any quick-login button on the login screen.
2. **Authoritative Balance Enquiry**: View real-time balances authoritative from MongoDB.
3. **Fund Transfers**:
   - Go to **Transfers**.
   - Destination: `ACC-1001-10000002` (Alice Smith).
   - Enter amount (e.g., `1000.00`).
   - Notice the automatically generated `Idempotency Key`. Submitting twice with the same key safely returns the existing transaction without deducting funds twice!
4. **Double-Entry Ledger Audit**:
   - Go to **History**.
   - Click **Audit Ledger** next to any transfer to view the cryptographic verification proof that `Sum(Debits) == Sum(Credits)`.
5. **Bill Payments**:
   - Go to **Bill Payments**.
   - Search for a biller using Trie Autocomplete (`elec`, `fiber`, `water`).
   - Pay bill and check the resulting debit and outbox notification.
6. **In-App Notifications**:
   - Click the bell icon 🔔 to view asynchronous notifications dispatched through Kafka.

---

## 6. System Design Laboratory Experiments

All laboratory modules are interactively accessible from the **System Insights** tab:
1. **Token Bucket Rate Limiter**: Click **Trigger 15 Burst Requests** to observe atomic token depletion and HTTP 429 Too Many Requests responses.
2. **Consistent Hashing**: Click **+ Add Node 4** or **- Remove Node 4** to execute topology mutations across 1,000 keys and observe minimal key remapping ($< 25\%$).
3. **Unique ID Benchmark**: Generate 50,000 opaque IDs with live generation throughput ($> 50,000$ IDs/sec) and $0$ collisions.
4. **URL Shortener**: Shorten `/synthetic/page1.html` and verify the `/s/{code}` redirect with Redis cache-aside.
5. **Trie Autocomplete**: Test prefix queries and compare uncached Java Trie search versus Redis cache-aside lookup latency.
6. **Synthetic Web Crawler**: Run the BFS crawler on local synthetic HTML pages and observe pages processed, duplicate links avoided, and extracted utility records.
7. **Dataset Scaling**: Dynamically generate batches of 50 to 500 customers and 1,000+ cross-account transfers.

---

## 7. Performance Evaluation with Apache JMeter

Runnable Apache JMeter test plans and automated runner scripts are located in `reports/performance/`:

```bash
# Execute Low (10 users), Medium (25 users), High (50 users) workloads
# In WSL / Linux:
chmod +x reports/performance/run-benchmarks.sh
./reports/performance/run-benchmarks.sh localhost 8080

# In Windows:
reports\performance\run-benchmarks.bat localhost 8080
```

HTML dashboard reports will be saved to:
- `reports/performance/dashboard-low/index.html`
- `reports/performance/dashboard-medium/index.html`
- `reports/performance/dashboard-high/index.html`

---

## 8. Automated Testing

Run the full automated JUnit 5 test suite:
```bash
# Windows
.\mvnw.cmd test

# Linux / WSL
./mvnw test
```

All 15 automated test cases verify:
- Atomic transfer debit/credit balance consistency
- Double-entry ledger equality
- Idempotency replay prevention
- Customer account ownership security
- Token bucket rate limiter behavior
- Consistent hashing virtual nodes & minimal remapping
- Unique ID collision resistance
- Trie autocomplete prefix matching
- Local web crawler duplicate avoidance
- URL shortener route validation

---

## 9. Detailed Documentation Index

For in-depth laboratory analysis and architectural specifications:
- [Requirements & Component Mapping](docs/requirements-and-component-mapping.md)
- [System Architecture & Mermaid Diagrams](docs/architecture.md)
- [Database Design & Sharding Strategy](docs/database-design.md)
- [System Design Algorithms & Pseudocode](docs/algorithms.md)
- [Performance Evaluation & Benchmark Data](docs/performance-evaluation.md)
- [Scalability & Component Failure Analysis](docs/scalability-and-failure-analysis.md)
- [Step-by-Step Demonstration Guide](docs/demo-guide.md)
- [Security Safeguards & System Limitations](docs/security-and-limitations.md)
