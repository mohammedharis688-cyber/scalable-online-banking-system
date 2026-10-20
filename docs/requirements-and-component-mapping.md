# Requirements and Component Mapping
## Scalable Online Banking System — UCS3513 System Design Laboratory

This document provides a formal mapping between every required banking functionality specified in the laboratory problem statement and the architectural component(s) implementing it.

---

## 1. Functional Requirements to Architectural Component Matrix

| ID | Mandatory Banking Feature | System Design Component | Underlying Technology | Justification & Architectural Role |
|---|---|---|---|---|
| **REQ-1** | Customer Registration & Auth | Authentication Service + Redis Rate Limiter | Spring Security, BCrypt, JJWT, Redis Lua Script | Protects authentication endpoints from credential-stuffing and brute-force attacks via token-bucket limiting; enforces stateless session verification. |
| **REQ-2** | Account Management & Balances | Account Service + MongoDB Sharding | Spring Data MongoDB, `mongos` Query Router | Balances are authoritative and uncached. Stored as `Decimal128` across sharded replica sets; routed via hashed `accountNumber`. |
| **REQ-3** | Beneficiary Management | Beneficiary Service | MongoDB `beneficiaries` collection | Manages verified internal transfer destinations; prevents circular transfers, duplicate entries, and unauthorized account access. |
| **REQ-4** | Fund Transfers & Ledger | Transfer Service + Distributed Transaction Manager | Spring Data MongoDB Multi-Document Transactions, Double-Entry Ledger | Guarantees ACID atomicity across sharded collections; creates matched DEBIT/CREDIT ledger entries; prevents balance overspending; enforces idempotency. |
| **REQ-5** | Bill Payments | Bill Payment Service + Redis Cache-Aside | Spring Data MongoDB, Redis Cache-Aside | Enables utility settlements (electricity, water, internet, mobile); biller catalogue cached in Redis; debits account atomically with double-entry ledger. |
| **REQ-6** | Transaction History | Transaction Service | MongoDB `transactions` collection, Scatter-Gather Routing | Provides paginated, directional history. Demonstrates scatter-gather query behavior across shards for customer-level queries. |
| **REQ-7** | In-App Notifications | Transactional Outbox + Kafka Consumer | Apache Kafka (KRaft), Outbox Poller, MongoDB `notifications` | Asynchronous notification pipeline decoupled from the database transaction boundary, preventing dual-write vulnerabilities. |
| **REQ-8** | Token Bucket Rate Limiting | Rate Limiting Interceptor / Service | Redis Lua Script (`scripts/token_bucket.lua`) | Enforces capacity and refill rates atomically in Redis without race conditions; returns HTTP 429 upon exhaustion. |
| **REQ-9** | Distributed Load Routing | Consistent Hashing Ring | Java `TreeMap`, Virtual Nodes, MD5 Hash | Simulates distributed server node selection; proves minimal key remapping property during cluster scaling or failure. |
| **REQ-10** | Opaque Identifier Generation | Unique ID Generator | SecureRandom, UUID, High-Precision Clock | Generates unguessable, collision-resistant IDs for transactions, accounts, ledgers, and events. |
| **REQ-11** | Biller Information Lookup | URL Shortener Service | MD5 Hash, Collision Resolver, Redis Cache-Aside | Compresses local public information paths into 6-character links with sub-millisecond Redis resolution and collision safety. |
| **REQ-12** | Fast Search Suggestions | Trie Autocomplete Service | Java Trie Prefix Tree, Redis Cache-Aside | Delivers ranked prefix suggestions for billers and beneficiaries in microseconds; reduces database search load. |
| **REQ-13** | Utility Directory Traversal | Synthetic Web Crawler | Breadth-First Search, Redis Visited Set, Jsoup | Traverses local synthetic HTML pages without SSRF risks; extracts utility records; demonstrates queueing and duplicate avoidance. |

---

## 2. Component Design Justifications

### 2.1 Native MongoDB Sharding vs. In-Memory Simulations
- **Justification**: In high-volume retail banking, transaction write throughput and ledger storage quickly exceed the capacity of a single monolithic database instance. 
- **Implementation**: The application communicates exclusively through the `mongos` query router on port `27017`. Shard keys are chosen to balance load:
  - `accounts`: `{ "accountNumber": "hashed" }` distributes accounts evenly across the 3 shards.
  - `transactions`: `{ "transactionId": "hashed" }` ensures write-intensive transfers do not create a hotspot on any single shard.

### 2.2 Redis-Backed Token Bucket vs. Sliding Window or Leaky Bucket
- **Justification**: Experiment 6 emphasizes the token-bucket algorithm because it allows controlled bursts of traffic while guaranteeing a strict long-term average rate limit.
- **Implementation**: An atomic Redis Lua script executes the mathematical formula:
  $$\text{tokens} = \min\left(C, \text{oldTokens} + (\text{now} - \text{lastRefill}) \times R\right)$$
  Executing this calculation entirely inside Redis prevents race conditions under high concurrency and eliminates multi-step network latency.

### 2.3 Transactional Outbox Pattern with Apache Kafka
- **Justification**: A common architectural flaw in financial systems is attempting to write to the database and publish to Kafka in a single method ("dual-write"). If Kafka is temporarily unreachable, either the transaction fails or the notification is lost.
- **Implementation**: When a transfer commits, an `outbox_events` record is written inside the **same MongoDB ACID transaction**. A background `OutboxPublisher` polls pending records, sends them to Kafka topic `bank.notifications`, and updates the status to `PUBLISHED`. If Kafka is offline, financial transfers continue uninterrupted, and notifications are delivered when Kafka resumes.

### 2.4 Consistent Hashing with Virtual Nodes
- **Justification**: Simple modulo hashing ($hash(key) \pmod N$) remaps almost $100\%$ of keys whenever a server is added or removed. Consistent hashing places nodes on a $2^{64}-1$ ring, limiting remapping to only $\frac{K}{N+1}$ keys.
- **Implementation**: Each logical server is assigned 100 virtual nodes dispersed across the ring to prevent hot spots and achieve uniform load distribution.
