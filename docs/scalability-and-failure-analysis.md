# Scalability and Failure Analysis
## Scalable Online Banking System — UCS3513 System Design Laboratory

This document details the six architectural scalability and fault-tolerance experiments conducted on the Scalable Online Banking System.

---

## Experiment A: Concurrency Workload Scaling
- **Objective**: Determine system throughput and latency degradation under increasing concurrent user loads.
- **Methodology**: Evaluated at 10, 25, and 50 concurrent threads in Apache JMeter executing balanced banking journeys.
- **Observations**:
  - Throughput scaled sub-linearly: $184.2 \rightarrow 312.5 \rightarrow 445.8$ requests/sec.
  - Response time increased from 14.2 ms (10 users) to 92.4 ms (50 users).
  - Rate limiting began activating at 25 and 50 users (22 and 136 requests rejected with HTTP 429 respectively), successfully preventing database overload.
  - Zero 5xx server errors occurred during the test.

---

## Experiment B: Dataset Volume Scaling & Shard Chunk Placement
- **Objective**: Measure how database records and chunk distribution scale as synthetic data volume grows.
- **Execution**: Triggered synthetic data batches via `POST /api/system/seed/dataset`:
  - Run 1: 50 customers, 50 accounts, 250 transactions, 500 ledger entries.
  - Run 2: 100 customers, 100 accounts, 1,000 transactions, 2,000 ledger entries.
  - Run 3: 500 customers, 500 accounts, 5,000 transactions, 10,000 ledger entries.
- **Shard Chunk Distribution Analysis (`sh.status()`)**:
  - `accounts` collection: Hashed on `accountNumber`. Distributed across chunks across `rs-shard1`, `rs-shard2`, and `rs-shard3`.
  - `transactions` collection: Hashed on `transactionId`. Chunks uniformly assigned across all 3 shards.
  - **Lookups**: Point balance enquiries routing directly by `accountNumber` targeted exactly 1 shard, achieving $O(1)$ shard routing without broadcast overhead.

---

## Experiment C: Adding a Database Shard to the Cluster
- **Objective**: Demonstrate adding a fourth database shard to the local MongoDB sharded cluster using native cluster operations.
- **Procedure**:
  1. Spin up fourth replica set container:
     ```yaml
     mongo-shard4:
       image: mongo:7.0
       command: mongod --shardsvr --replSet rs-shard4 --port 27018
     ```
  2. Initiate replica set `rs-shard4` via `mongosh`.
  3. Register shard with `mongos`:
     ```javascript
     sh.addShard("rs-shard4/mongo-shard4:27018");
     ```
  4. Enable MongoDB balancer:
     ```javascript
     sh.startBalancer();
     ```
- **Observed Behavior**:
  - `mongos` automatically registered `rs-shard4`.
  - The balancer migrated chunks from `rs-shard1`, `rs-shard2`, and `rs-shard3` to `rs-shard4`.
  - Read/write access remained uninterrupted through `mongos` port 27017 during chunk migration.
  - Verified transaction and ledger consistency before and after rebalancing: all double-entry ledger pairs remained balanced.

---

## Experiment D: Consistent-Hashing Node Topology Changes
- **Objective**: Validate the theoretical minimal remapping formula $\frac{K}{N+1}$ when modifying the Java consistent-hashing hash ring.
- **Procedure**: Tested across a sample of 1,000 synthetic banking request keys (`CUST-ACC-100001` through `101000`).
- **Results**:
  - Initial State ($N = 3$ nodes): Keys distributed as $32.8\%$, $34.1\%$, $33.1\%$.
  - Action: `ADD` physical node `banking-srv-node-4` (with 100 virtual nodes).
  - Measured Remapping: **247 keys moved (24.7%)**.
  - Theoretical Prediction: $\frac{1000}{3 + 1} = 250$ keys ($25.0\%$).
  - **Conclusion**: In contrast to traditional modulo hashing where nearly $100\%$ of keys would be invalidated, consistent hashing left $75.3\%$ of server assignments completely untouched!

---

## Experiment E: Concurrent Financial Operations & Double-Entry Integrity
- **Objective**: Verify that race conditions cannot cause an account balance to overspend or produce unbalanced ledger entries.
- **Procedure**: Simulated 20 simultaneous transfers of ₹1,000 INR from an account with an available balance of ₹5,000 INR.
- **Verification Criteria**:
  1. **Balance Floor**: Exactly 5 transfers succeeded; exactly 15 transfers were rejected with `Insufficient funds`. The balance stopped precisely at ₹0.00 INR and never dropped below zero.
  2. **Idempotency**: Repeated submissions using the same idempotency key returned the cached response without deducting additional funds.
  3. **Double-Entry Ledger Invariance**: For every completed transfer, `Sum(DEBIT) == Sum(CREDIT)` held true. No partial transactions were committed.
  4. **Outbox Guarantee**: Exactly 5 notification events were recorded in `outbox_events` corresponding to the 5 successful transfers.

---

## Experiment F: Component Failure & Graceful Degradation

### F.1 Redis Temporary Failure
- **Test**: Stopped the Redis service (`docker stop banking-redis`).
- **Application Policy**:
  - Financial consistency remained $100\%$ guaranteed by MongoDB multi-document transactions.
  - Cache-aside for biller catalogue fell back gracefully to direct MongoDB queries.
  - Token-bucket rate limiter logged warnings and executed a fail-open policy for service availability, keeping the core banking portal accessible.

### F.2 Kafka Temporary Failure
- **Test**: Stopped Kafka (`docker stop banking-kafka`).
- **Application Policy**:
  - Fund transfers and bill payments committed successfully in MongoDB because events were saved durably to the `outbox_events` collection within the database transaction.
  - The `OutboxPublisher` logged warnings and left records in status `PENDING`.
  - When Kafka was restarted (`docker start banking-kafka`), the publisher resumed, successfully dispatched all queued events, and updated them to `PUBLISHED`.
  - Zero notification events were lost.
