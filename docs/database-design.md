# Database Design & Sharding Strategy
## Scalable Online Banking System — UCS3513 System Design Laboratory

This document explains the MongoDB database architecture, collection schemas, indexes, native sharding strategies, multi-document transaction semantics, and double-entry ledger consistency.

---

## 1. MongoDB Collections & Schema Definitions

### 1.1 `customers` Collection
Stores registered bank customers and credentials.
- **Shard Key**: `{ "customerId": "hashed" }`
- **Fields**:
  - `_id`: ObjectId (System)
  - `customerId`: String (Unique business ID, e.g., `CUST-1001`)
  - `email`: String (Unique index)
  - `passwordHash`: String (BCrypt hash)
  - `fullName`: String
  - `phoneNumber`: String
  - `role`: String (`ROLE_CUSTOMER`, `ROLE_ADMIN`)
  - `createdAt`: ISODate

### 1.2 `accounts` Collection
Stores authoritative customer bank accounts and monetary balances.
- **Shard Key**: `{ "accountNumber": "hashed" }`
- **Fields**:
  - `_id`: ObjectId
  - `accountNumber`: String (Unique business identifier, e.g., `ACC-1001-10000001`)
  - `customerId`: String (Index for customer account listing)
  - `accountType`: String (`SAVINGS`, `CHECKING`, `BUSINESS`)
  - `balance`: Decimal128 (Authoritative monetary balance with exact precision)
  - `currency`: String (`INR`)
  - `status`: String (`ACTIVE`, `FROZEN`, `CLOSED`)
  - `createdAt`, `updatedAt`: ISODate

### 1.3 `transactions` Collection
Durable record of all completed financial events (transfers, bill payments).
- **Shard Key**: `{ "transactionId": "hashed" }`
- **Fields**:
  - `_id`: ObjectId
  - `transactionId`: String (Internal unique ID)
  - `transactionReference`: String (Unique public opaque reference, e.g., `TXN-17180000-AB12CD`)
  - `sourceAccountNumber`: String (Indexed)
  - `destinationAccountNumber`: String (Indexed)
  - `amount`: Decimal128
  - `currency`: String (`INR`)
  - `type`: String (`TRANSFER`, `BILL_PAYMENT`, `DEPOSIT`)
  - `status`: String (`COMMITTED`, `FAILED`, `REJECTED`)
  - `idempotencyKey`: String (Indexed)
  - `timestamp`: ISODate
  - `description`: String

### 1.4 `ledgers` Collection
Immutable double-entry audit records. Every financial transaction writes exactly two ledger entries (one DEBIT, one CREDIT).
- **Shard Key**: `{ "transactionId": "hashed" }`
- **Fields**:
  - `_id`: ObjectId
  - `ledgerId`: String (Unique ID, e.g., `LDG-984A2B11`)
  - `transactionId`: String (Shard key, collocated with transaction)
  - `transactionReference`: String (Reference link)
  - `accountNumber`: String (Target account)
  - `entryType`: String (`DEBIT` or `CREDIT`)
  - `amount`: Decimal128
  - `balanceAfter`: Decimal128 (Snapshot of balance immediately following this entry)
  - `timestamp`: ISODate
  - `description`: String

### 1.5 Supporting Collections
- `beneficiaries`: Unsharded or partitioned on `customerId`.
- `billers`: Unsharded reference catalogue cached in Redis.
- `bill_payments`: Utility payment logs.
- `outbox_events`: Transactional outbox buffer for Kafka notifications.
- `notifications`: In-app notification messages.
- `short_urls`: Compressed URL-to-path mappings.

---

## 2. Shard Key Selection & Analysis

| Collection | Shard Key | Type | Routing Behavior & Rationale |
|---|---|---|---|
| `customers` | `customerId` | Hashed | Ensures customer records are uniformly dispersed across all 3 shards. Eliminates monotonically increasing insertion hotspots. |
| `accounts` | `accountNumber` | Hashed | Balances are high-frequency read/write targets. Hashing by account number distributes account lookups and transfers uniformly across shard replica sets. |
| `transactions` | `transactionId` | Hashed | Guarantees write scalability. Sequential timestamps would create a write bottleneck on the latest chunk shard. Hashed transaction IDs distribute concurrent transfer writes evenly. |
| `ledgers` | `transactionId` | Hashed | Collocates ledger entries for the same transfer on the same shard chunk, optimizing ledger generation and auditing. |

---

## 3. Scatter-Gather Query Documentation
When a customer queries their paginated history via `GET /api/transactions`:
- The query criteria searches `sourceAccountNumber IN (...) OR destinationAccountNumber IN (...)`.
- Because the `transactions` collection is sharded on `transactionId` (not `accountNumber`), the `mongos` router cannot determine in advance which shard contains matching records.
- **Routing Strategy**: `mongos` issues a **scatter-gather query** in parallel to all data shards (`rs-shard1`, `rs-shard2`, `rs-shard3`), merges the result sets, sorts them by `timestamp DESC`, applies `skip`/`limit` pagination, and returns the response to the application.
- **Trade-off Analysis**: We prioritize **write throughput distribution** during transfers over single-shard read routing during history queries. In banking systems, transfer write operations occur continuously and must never bottleneck, whereas history queries are read operations well-suited to scatter-gather parallel execution.

---

## 4. Multi-Document ACID Transactions & Concurrency Control

Because each shard in our cluster is configured as a replica set (`rs-shard1`, `rs-shard2`, `rs-shard3`), MongoDB supports distributed multi-document transactions coordinated by `mongos`.

### Transaction Workflow:
```java
@Transactional
public TransferResponse executeTransfer(String customerId, TransferRequest request) {
    // 1. Idempotency Check
    // 2. Conditional balance validation (source.balance >= amount)
    // 3. Atomically update source balance (- amount)
    // 4. Atomically update destination balance (+ amount)
    // 5. Insert Transaction document
    // 6. Insert DEBIT LedgerEntry
    // 7. Insert CREDIT LedgerEntry
    // 8. Insert OutboxEvent document
    // Commit Transaction
}
```

If any write fails or a network partition occurs before commit, the two-phase commit protocol implemented by `mongos` aborts all writes across all affected shards, leaving the balances and ledger intact.
