# System Architecture & Component Interactions
## Scalable Online Banking System — UCS3513 System Design Laboratory

This document describes the layered distributed architecture of the Scalable Online Banking System, detailing how Spring Boot, MongoDB native sharding, Redis, Kafka, and the laboratory system-design components interact.

---

## 1. High-Level Architecture Diagram

```mermaid
flowchart TD
    subgraph ClientLayer ["Client Presentation Layer"]
        Browser["Vanilla Browser Frontend\n(HTML5, CSS3, Vanilla JS)"]
    end

    subgraph AppServer ["Spring Boot Application Server (Port 8080)"]
        direction TB
        StaticHost["Static Asset Serving\n(/, /css, /js, /synthetic)"]
        SecFilter["Spring Security Filter Chain\n(BCrypt & JWT Authentication)"]
        RateLimiter["Token-Bucket Rate Limiter\n(Lua Script Interceptor)"]
        
        subgraph BankingServices ["Core Banking Application Services"]
            AuthSvc["Auth Service"]
            AccSvc["Account Service"]
            TxnSvc["Transfer Service\n(@Transactional)"]
            BillSvc["Bill Payment Service"]
            BenSvc["Beneficiary Service"]
            HistSvc["Transaction History Service"]
        end

        subgraph LabModules ["System Design Lab Modules"]
            TrieSvc["Trie Autocomplete\n(Prefix Tree)"]
            ShortSvc["URL Shortener\n(MD5 + Collision Guard)"]
            CrawlerSvc["Synthetic Web Crawler\n(BFS + Jsoup)"]
            HashRouter["Consistent Hash Router\n(Virtual Nodes Ring)"]
            IdGen["Unique ID Generator\n(Collision Resistant)"]
        end

        subgraph EventPipeline ["Asynchronous Notification Pipeline"]
            OutboxPub["Outbox Publisher\n(@Scheduled Poller)"]
            KafkaCons["Kafka Notification Consumer\n(@KafkaListener)"]
        end
    end

    subgraph RedisStore ["In-Memory Key-Value & Rate Limiting"]
        Redis["Redis 7 (Port 6379)\n• Token Bucket Lua Atomic State\n• Cache-Aside (Billers, URLs, Prefixes)\n• Crawler Visited URLs Set"]
    end

    subgraph KafkaBroker ["Message Broker (KRaft Mode)"]
        Kafka["Apache Kafka 3.8 (Port 9092/9094)\n• Topic: bank.notifications\n• Topic: bank.audit"]
    end

    subgraph ShardedMongoCluster ["MongoDB 7.0 Native Sharded Cluster"]
        direction TB
        MongosRouter["mongos Query Router (Port 27017)"]
        ConfigRS["Config Server Replica Set\nrs-cfg: mongo-configsvr:27019"]
        
        subgraph DataShards ["MongoDB Data Shards"]
            Shard1["Shard 1 Replica Set\nrs-shard1: mongo-shard1:27018"]
            Shard2["Shard 2 Replica Set\nrs-shard2: mongo-shard2:27018"]
            Shard3["Shard 3 Replica Set\nrs-shard3: mongo-shard3:27018"]
        end
    end

    %% Client Interactions
    Browser -->|HTTP Requests / REST APIs| SecFilter
    Browser -->|Fetch Static UI Assets| StaticHost
    SecFilter -->|Rate Limit Check| RateLimiter
    RateLimiter <-->|HMGET / HMSET via Lua| Redis
    RateLimiter --> BankingServices
    RateLimiter --> LabModules

    %% Banking Database Path
    BankingServices -->|Spring Data Mongo (ACID Transactions)| MongosRouter
    MongosRouter -->|Cluster Metadata| ConfigRS
    MongosRouter -->|Partitioned Reads/Writes| Shard1
    MongosRouter -->|Partitioned Reads/Writes| Shard2
    MongosRouter -->|Partitioned Reads/Writes| Shard3

    %% Cache-Aside Interactions
    BillSvc <-->|Cache-Aside Biller Catalogue| Redis
    ShortSvc <-->|Cache-Aside Short URLs| Redis
    TrieSvc <-->|Cache-Aside Prefix Suggestions| Redis
    CrawlerSvc <-->|Visited Set & De-duplication| Redis

    %% Event Outbox Flow
    TxnSvc -->|1. Write Outbox Event (Atomic Txn)| MongosRouter
    OutboxPub -->|2. Poll Pending Events| MongosRouter
    OutboxPub -->|3. Publish Event| Kafka
    Kafka -->|4. Push Notification Message| KafkaCons
    KafkaCons -->|5. Persist Notification| MongosRouter
    Browser -->|6. Query Notifications| MongosRouter
```

---

## 2. Request and Data Flow Walkthroughs

### 2.1 Fund Transfer Execution Flow (ACID + Outbox)
1. **Request Ingestion**: Customer initiates a transfer via `POST /api/transfers` with `X-Idempotency-Key`.
2. **Security & Rate Limiting**: The security filter validates the JWT token. The rate limiter executes `token_bucket.lua` in Redis to ensure the client has sufficient tokens.
3. **Idempotency Verification**: `TransferService` checks if the idempotency key already exists. If yes, it returns the existing committed transaction immediately.
4. **MongoDB Distributed Transaction Initiation**: Spring's `MongoTransactionManager` creates a multi-document transaction through `mongos`:
   - Validates ownership of the source account.
   - Validates that the destination account exists and is active.
   - Verifies that `sourceAccount.balance >= amount`.
   - Debits the source account and credits the destination account.
   - Creates a `transactions` record with status `COMMITTED`.
   - Creates balanced double-entry `ledgers` records:
     - Entry 1: `DEBIT` on source account with updated balance.
     - Entry 2: `CREDIT` on destination account with updated balance.
   - Creates an `outbox_events` document with status `PENDING`.
5. **Atomic Commit**: `mongos` coordinates the multi-document commit across the target shard replica sets. If any operation fails, all writes roll back.
6. **Asynchronous Notification**:
   - `OutboxPublisher` polls pending outbox records and sends them to Kafka topic `bank.notifications`.
   - `KafkaNotificationConsumer` reads the message and writes the persistent notification into the `notifications` collection.

---

## 3. Disambiguation: Native Sharding vs. Consistent Hashing

| Dimension | Native MongoDB Sharding (`mongos`) | Consistent Hashing Module (`ConsistentHashRouter`) |
|---|---|---|
| **Layer** | Database Infrastructure Layer | Application Routing Experiment Module |
| **Execution** | Real Dockerized MongoDB cluster with 3 shards | Pure Java in-memory hash ring with virtual nodes |
| **Router** | `mongos` query router (Port 27017) | `ConsistentHashRouter.java` |
| **Purpose** | Partitioning durable banking data across disks | Simulating distributed cache/server request placement |
| **Topology Changes** | Adding/rebalancing MongoDB chunk allocations | Dynamically adding/removing virtual nodes on a 64-bit ring |
| **Storage** | Persisted on disk across shard replica sets | In-memory evaluation and metrics collection |
