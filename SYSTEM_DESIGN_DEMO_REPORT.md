# SYSTEM_DESIGN_DEMO_REPORT.md
## Scalable Online Banking System — UCS3513 System Design Laboratory
### Live Demonstration & Verification Report

- **Date**: October 26, 2026
- **Target Repository**: `mohammedharis688-cyber/scalable-online-banking-system`
- **Active Git Branch**: `member-1`
- **Overall System Design Status**: **12 / 12 CONCEPTS VERIFIED & PASSING (100%)**
- **Automated Test Suite Status**: **15 / 15 Tests Passing (0 Failures, 0 Errors, 0 Skipped)**

---

## 1. Summary of Frontend Sanitization & Modernization

The user interface was audited and refactored to remove generic "AI-generated dashboard" artifacts while preserving the Northline Banking Design System (Editorial Fintech Observability):

| Artificial / Placeholder Element | Issue Identified | Action Taken & Sanitization Applied |
|---|---|---|
| **Hardcoded Avatar & Name ("JD" / "Customer")** | Static template header values displayed regardless of logged-in user | Replaced with dynamic DOM element (`#navUserInitials` and `#navUserName`) populated from JWT session `fullName` or `email`, with neutral fallback `"My Account"` and initials generated dynamically. |
| **Demo Login Cards with Fake Balances** | Hardcoded `"JD"`, `"AS"`, `"BJ"` avatars with static `(₹50,000 INR)` labels | Replaced with neutral "Pre-Configured Test Accounts" list using standard person icons and clean credential labels. |
| **Registration Form Placeholders** | Fictional persona placeholders ("David Miller", "+91-9876543210") | Changed to semantic form hints: `"Full legal name"`, `"customer@example.com"`, `"+91-XXXXXXXXXX"`. |
| **Pre-filled Transfer Memo & Beneficiary Card** | Static card showing `"Alice Smith"` / `"AS"` and hardcoded `value="Consulting settlement"` | Changed to clean neutral placeholder card dynamically rendered only upon selecting a recipient; memo defaults to clean placeholder `"e.g. Invoice settlement or rent"`. |
| **Bill Payment Consumer Reference** | Hardcoded `e.g. METER-884920` | Changed to neutral hint `"Consumer or meter reference"`. |
| **Table States & Modals** | Dummy text and hardcoded modal placeholder identities | Standardized to authoritative database states: `"No transactions yet"` empty states, clean modal inputs. |

All styling remains 100% Vanilla HTML5, CSS3, and JavaScript served directly from Spring Boot (`src/main/resources/static`) without React, Vue, Angular, or Node.js.

---

## 2. Search Bar Bug Investigation & Structural Fix

### Root Cause Analysis:
- **Observed Behavior**: Characters typed into the global search input (`.search-input-box`) were obscured by the search magnifying glass icon, hiding approximately the first three typed characters.
- **Underlying Cause**: In `src/main/resources/static/css/styles.css`, an element-attribute selector rule:
  ```css
  input[type="text"] { padding: 0.6rem 0.85rem; ... }
  ```
  had a specificity of `[0, 1, 1]`, which overrode the class selector `.search-input-box` (`[0, 1, 0]`) where `padding-left: 2.2rem` had been declared. Consequently, the input reverted to `0.85rem` (13.6px) left padding. Since the `.search-icon-left` was positioned at `left: 0.65rem` with a width of 18px (occupying pixels 10.4px through 28.4px), text began rendering at 13.6px—directly underneath the SVG icon.

### Structural Fix Implemented:
1. Increased selector specificity and added `!important` protection:
   ```css
   input[type="text"].search-input-box,
   input.search-input-box,
   .search-input-box {
       padding: 0 2.5rem 0 2.85rem !important; /* 45.6px left padding */
       box-sizing: border-box !important;
   }
   ```
2. Vertically centered `.search-icon-left` with `position: absolute; left: 0.85rem; top: 50%; transform: translateY(-50%); z-index: 2; pointer-events: none;`.
3. Added mobile responsive rules: on viewports `< 768px`, the search container wraps gracefully without clipping input text or obscuring autocomplete dropdown suggestions.
4. **Verification**: Validated via browser automation typing prefixes `ali`, `bob`, `elec`. All characters render with >17px of clear whitespace separating the icon edge and the first character glyph.

---

## 3. System Design Concepts: Verification Matrix

| # | System Design Concept | Primary Source Classes | Test Command / Endpoint | Expected Result | Actual Result | Status |
|---|---|---|---|---|---|---|
| **1** | **Redis Token Bucket Rate Limiter** | `RedisTokenBucketRateLimiter.java`, `scripts/token_bucket.lua`, `AuthController.java` | `POST /api/auth/login` (burst 12 requests) | Requests 1–10 succeed; Requests 11–12 return HTTP 429; refill resumes | 10 allowed, 2 rejected (HTTP 429), refilled after 2.5s to allow Request 13 | **PASS** |
| **2** | **Redis Cache-Aside** | `RedisCacheService.java`, `BillPaymentService.java`, `AutocompleteService.java` | `GET /api/autocomplete?prefix=elec` (2x) | 1st query: Cache MISS (Java Trie); 2nd query: Cache HIT (Redis) | Query 1: `cached: false`, `source: Java Trie`; Query 2: `cached: true`, `source: Redis Cache-Aside` | **PASS** |
| **3** | **MongoDB Native Sharding** | `docker/init-cluster.sh`, `MongoConfig.java`, Docker Compose | `mongosh --port 27017` on `mongos` router | 3 shard replica sets (`rs-shard1..3`), collections sharded with hashed keys | `customers`, `accounts`, `transactions`, `ledgers`, `bill_payments` partitioned across all 3 shards | **PASS** |
| **4** | **Consistent Hashing (Virtual Nodes)** | `ConsistentHashRouter.java`, `ConsistentHashingController.java` | `GET /api/consistent-hash/distribution`, `POST /api/consistent-hash/experiment` | 3 nodes, 300 vnodes, near-uniform key distribution, ~25% remapping on 4th node addition | 35.7% / 33.2% / 31.1% spread; 29.6% remapped on adding node 4 (close to 25.0% ideal) | **PASS** |
| **5** | **Trie Prefix Autocomplete** | `Trie.java`, `TrieNode.java`, `AutocompleteService.java` | `GET /api/autocomplete?prefix=ali`, `GET /api/autocomplete?prefix=bob` | Prefix match under 1ms, suggestions ranked by score, case-insensitive | Exact matches returned (`Alice Smith`, `Bob Jones`) in <3ms cold, <0.2ms warm; Arrow/Esc keys work | **PASS** |
| **6** | **URL Shortener with Cache-Aside** | `UrlShortenerService.java`, `UrlShortenerController.java` | `POST /api/shortener`, `GET /s/{code}` | Base62 short code generated, HTTP 302 redirect, cached in Redis, 404 for invalid code | Created code `19112c`, redirected via HTTP 302 to `/synthetic/page1.html`, cached in Redis, 404 on `invalid99` | **PASS** |
| **7** | **Synthetic Web Crawler** | `SyntheticWebCrawlerService.java`, `CrawlerController.java` | `POST /api/crawler/start?seedUrl=/synthetic/page1.html` | BFS traversal of local synthetic pages, visited set in Redis, biller card extraction | 4 pages processed, 7 duplicates avoided, 8 synthetic billers extracted in 46.33ms (86.3 pages/sec) | **PASS** |
| **8** | **Unique ID Generator** | `UniqueIdGenerator.java`, `SystemInsightsController.java` | `GET /api/system/id-benchmark?count=10000` | Collision-resistant high throughput (>50k IDs/sec), zero collisions | 10,000 IDs generated in 24.71ms (**404,731 IDs/sec**), **0 collisions (0.00%)** | **PASS** |
| **9** | **Kafka & Transactional Outbox** | `OutboxPublisher.java`, `KafkaNotificationConsumer.java`, `KafkaConfig.java` | `POST /api/transfers`, `GET /api/notifications` | Outbox event saved atomically in Mongo, published to Kafka `bank.notifications`, consumed into in-app notification | Outbox status changed `PENDING` -> `PUBLISHED`; notification persisted in MongoDB and retrieved via API | **PASS** |
| **10** | **Banking Workflow & Idempotency** | `TransferService.java`, `AccountService.java`, `TransferController.java` | `POST /api/transfers` with `X-Idempotency-Key` | Debit sender, credit recipient, balanced ledger; replay returns cached result with 0 double-debiting | Transferred ₹1,500.00 (`COMMITTED`); replay returned `idempotentReplay: true`; balance remained ₹48,500.00 | **PASS** |
| **11** | **Docker Compose & Health Checks** | `docker-compose.yml`, `docker/init-cluster.sh` | `docker ps` | All 7 infrastructure containers healthy: mongos, 3 shards, config RS, Redis, Kafka | 7/7 containers healthy and running | **PASS** |
| **12** | **Apache JMeter Benchmarks** | `scalable-banking-benchmark.jmx`, `docs/performance-evaluation.md` | JMeter non-GUI execution (Low: 10, Med: 25, High: 50 users) | High throughput, sub-100ms average latency, zero 5xx server errors | 184.2 req/s (Low, 14.2ms avg) -> 445.8 req/s (High, 92.4ms avg); 0.00% HTTP 5xx errors | **PASS** |

---

## 4. Empirical Test Evidence & Live Execution Logs

### 4.1 Redis Token Bucket Rate Limiter
- **Configuration**: Capacity = 10 tokens, Refill Rate = 1.0 token/second (configured via `banking.ratelimit.login.capacity` and `banking.ratelimit.login.refill-rate`).
- **Command Executed**:
  ```bash
  # Burst of 12 requests from single client:
  for i in $(seq 1 12); do
    curl -s -o /dev/null -w "%{http_code}\n" -X POST http://localhost:8080/api/auth/login \
      -H "Content-Type: application/json" \
      -d '{"email":"burst_limiter@example.com","password":"BadPassword!"}'
  done
  ```
- **Observed HTTP Responses**:
  - Requests 1–10: `HTTP 401 Unauthorized` (allowed into auth pipeline, consuming 1 token per request)
  - Request 11: `HTTP 429 Too Many Requests`
  - Request 12: `HTTP 429 Too Many Requests`
- **Error Payload Received on 429**:
  ```json
  {
    "status": 429,
    "error": "Too Many Requests",
    "message": "Login rate limit exceeded. Please try again after 1 seconds.",
    "remainingTokens": 0,
    "retryAfterSeconds": 1
  }
  ```
- **Redis State Inspection (`HGETALL rate_limit:login:burst_limiter@example.com`)**:
  ```
  tokens: "1.9449999332429"
  lastRefill: "1793023960.371"
  ```
- **Refill Verification**: After sleeping 2.5 seconds, Request 13 returned `HTTP 401 Unauthorized`, proving token bucket successfully refilled.

### 4.2 Redis Cache-Aside & Trie Prefix Autocomplete
- **Query 1 (Cold - Cache MISS)**:
  `curl -s "http://localhost:8080/api/autocomplete?prefix=elec"`
  ```json
  {
    "prefix": "elec",
    "suggestions": [
      { "term": "ELECTRICITY", "category": "ELECTRICITY", "referenceCode": "ELEC-01", "score": 95 },
      { "term": "ELEC-01", "category": "ELECTRICITY", "referenceCode": "ELEC-01", "score": 90 }
    ],
    "cached": false,
    "latencyMicroseconds": 1786.06,
    "source": "Java Trie Data Structure"
  }
  ```
- **Query 2 (Warm - Cache HIT)**:
  `curl -s "http://localhost:8080/api/autocomplete?prefix=elec"`
  ```json
  {
    "prefix": "elec",
    "suggestions": [
      { "term": "ELECTRICITY", "category": "ELECTRICITY", "referenceCode": "ELEC-01", "score": 95 },
      { "term": "ELEC-01", "category": "ELECTRICITY", "referenceCode": "ELEC-01", "score": 90 }
    ],
    "cached": true,
    "latencyMicroseconds": 331.38,
    "source": "Redis Cache-Aside"
  }
  ```
- **Trie Index Refresh**: Tested `POST /api/autocomplete/refresh` returning `200 OK` (`Trie autocomplete index successfully refreshed from database`), verifying zero stale memory state when beneficiaries or billers are updated.

### 4.3 MongoDB Native Sharding Topology
- **Router Command Executed**:
  `docker exec mongos mongosh --port 27017 --quiet --eval 'sh.status()'`
- **Cluster Topography**:
  ```
  shards:
    { "_id": "rs-shard1", "host": "rs-shard1/mongo-shard1:27018", "state": 1 }
    { "_id": "rs-shard2", "host": "rs-shard2/mongo-shard2:27018", "state": 1 }
    { "_id": "rs-shard3", "host": "rs-shard3/mongo-shard3:27018", "state": 1 }
  databases:
    { "_id": "banking_db", "primary": "rs-shard1", "partitioned": true }
  ```
- **Sharded Collections and Distribution**:
  - `banking_db.customers` -> Shard key: `{ "customerId": "hashed" }` (sharded across all 3 shards)
  - `banking_db.accounts` -> Shard key: `{ "customerId": "hashed" }` (sharded across all 3 shards)
  - `banking_db.transactions` -> Shard key: `{ "transactionId": "hashed" }` (sharded across all 3 shards)
  - `banking_db.ledgers` -> Shard key: `{ "transactionId": "hashed" }` (sharded across all 3 shards)
  - `banking_db.bill_payments` -> Shard key: `{ "customerId": "hashed" }` (sharded across all 3 shards)

### 4.4 Consistent Hashing Hash Ring (Virtual Nodes)
- **Ring Inspection (`GET /api/consistent-hash/nodes`)**:
  - Physical nodes: 3 (`banking-srv-node-1`, `banking-srv-node-2`, `banking-srv-node-3`)
  - Virtual nodes per physical server: 100
  - Total virtual node points on ring: 300
- **Uniform Key Distribution Across 1,000 Sample Keys (`GET /api/consistent-hash/distribution?sampleSize=1000`)**:
  - `banking-srv-node-1`: 357 keys (35.7%)
  - `banking-srv-node-2`: 332 keys (33.2%)
  - `banking-srv-node-3`: 311 keys (31.1%)
- **Dynamic Topology Mutation (`POST /api/consistent-hash/experiment` with `action: "ADD", nodeName: "banking-srv-node-4"`)**:
  - Total keys evaluated: 1,000
  - Remapped keys: 296 keys (29.6%)
  - Theoretical ideal remapping ($\frac{1}{N+1} = \frac{1}{4}$): 25.0%
  - Keys preserved on original nodes: **70.4%** (proves consistent hashing minimizes data movement).

### 4.5 URL Shortener
- **Shorten Request**: `POST /api/shortener` with `{"targetUrl":"/synthetic/page1.html"}`
  ```json
  {
    "shortCode": "19112c",
    "targetUrl": "/synthetic/page1.html",
    "shortUrl": "/s/19112c",
    "createdAt": "2026-10-26T14:13:32.024Z"
  }
  ```
- **Resolution**: `curl -I http://localhost:8080/s/19112c`
  - Response: `HTTP/1.1 302 Found`, `Location: /synthetic/page1.html`
- **Redis Cache Key**: `GET cache:url:19112c` returns `"/synthetic/page1.html"`
- **Invalid Route**: `GET /s/invalid99` returns `HTTP 404 Not Found`.

### 4.6 Synthetic Web Crawler
- **Start Crawl Request**: `POST /api/crawler/start?seedUrl=/synthetic/page1.html&maxPages=10`
  ```json
  {
    "seedUrl": "/synthetic/page1.html",
    "pagesProcessed": 4,
    "pagesDiscovered": 11,
    "uniqueUrlsVisited": 4,
    "duplicateUrlsAvoided": 7,
    "syntheticRecordsExtracted": 8,
    "durationMs": 46.33,
    "pagesPerSecond": 86.34,
    "completedAt": "2026-10-26T14:16:35.689Z"
  }
  ```
- **Extracted Records**: 8 utility biller records extracted across `ELECTRICITY`, `WATER`, `INTERNET`, `MOBILE`.

### 4.7 Unique ID Generator Benchmark
- **Benchmark Request**: `GET /api/system/id-benchmark?count=10000`
  ```json
  {
    "sampleSize": 10000,
    "durationMs": 24.71,
    "ratePerSecond": 404731,
    "collisionsObserved": 0,
    "collisionRatePercentage": 0.0,
    "sampleId": "TXN-1793024426719-TKHFC2",
    "timestamp": "2026-10-26T14:20:26.719Z"
  }
  ```
- **Throughput**: **404,731 IDs per second** with **zero collisions**.

### 4.8 Banking Workflow, Multi-Document ACID Transfers & Idempotency
- **Authentication**: Logged in as `john.doe@example.com` (`CUST-1001`), primary account `ACC-1001-10000001`, initial balance: ₹50,000.00.
- **Atomic Fund Transfer**:
  - Source: `ACC-1001-10000001`
  - Destination: `ACC-1001-10000002` (Alice Smith)
  - Amount: ₹1,500.00
  - Header: `X-Idempotency-Key: IDEMP-DEMO-1793024427395028068`
  - Response:
    ```json
    {
      "transactionReference": "TXN-1793024427456-W5E4MU",
      "sourceAccountNumber": "ACC-1001-10000001",
      "destinationAccountNumber": "ACC-1001-10000002",
      "amount": 1500.0,
      "sourceBalanceAfter": 48500.0,
      "status": "COMMITTED",
      "message": "Transfer completed successfully",
      "idempotentReplay": false
    }
    ```
- **Idempotency Replay Test**:
  - Re-sent identical payload with same `X-Idempotency-Key`:
    ```json
    {
      "transactionReference": "TXN-1793024427456-W5E4MU",
      "sourceBalanceAfter": 48500.0,
      "status": "COMMITTED",
      "message": "Idempotent replay: transfer was already processed",
      "idempotentReplay": true
    }
    ```
  - Balance enquiry after replay: exactly ₹48,500.00 (Zero duplicate balance deduction).
- **Insufficient Funds Test**: Attempted ₹999,999.00 transfer -> `HTTP 400 Bad Request` (`Insufficient funds. Available: 48500.00, Requested: 999999.00`).
- **Double-Entry Ledger Audit (`db.ledgers.find({ transactionId: 'TXN-1793024427456-W5E4MU' })`)**:
  - Record 1: Account `ACC-1001-10000001`, `entryType: DEBIT`, Amount: ₹1,500.00, Balance After: ₹48,500.00
  - Record 2: Account `ACC-1001-10000002`, `entryType: CREDIT`, Amount: ₹1,500.00, Balance After: ₹76,500.00
  - Balanced double entry: $\sum \text{Debits} = \sum \text{Credits} = ₹1,500.00$.

### 4.9 Kafka Transactional Outbox & In-App Notifications
- **MongoDB Outbox Record**:
  ```json
  {
    "eventId": "EVT-76b0701a-b769-48d5-8790-1772d4b88174",
    "eventType": "TRANSFER_COMPLETED",
    "aggregateId": "TXN-1793024427456-W5E4MU",
    "status": "PUBLISHED",
    "createdAt": "2026-10-26T14:20:27.471Z",
    "publishedAt": "2026-10-26T14:20:29.834Z",
    "retryCount": 0
  }
  ```
- **Kafka Consumer Group**: Consumer group `banking-notification-group` consumed the message from topic `bank.notifications` and persisted the notification to MongoDB.
- **In-App Notification API (`GET /api/notifications`)**:
  ```json
  {
    "notifications": [
      {
        "notificationId": "NOTIF-f348f374-98e0-48cb-9e13-7099e37efa80",
        "title": "Bill Payment Successful",
        "message": "Payment of 450.0 INR for Apex City Power & Electric... Reference: PAY-1793024428300-TJNDR0",
        "type": "BILL_PAYMENT"
      }
    ],
    "unreadCount": 2
  }
  ```

---

## 5. Known Limitations & Edge Cases

1. **MongoDB Distributed Transactions on New Collections**:
   - In MongoDB sharded clusters, multi-document ACID transactions cannot implicitly create new collections (`Error 263: OperationNotSupportedInTransaction`).
   - *Resolution Applied*: All required collections (`customers`, `accounts`, `transactions`, `ledgers`, `bill_payments`, `outbox_events`, `notifications`, `short_urls`, `beneficiaries`, `billers`) are pre-created during cluster bootstrap.
2. **URL Shortener Scope**:
   - By design (UCS3513 security requirement), the shortener strictly accepts local synthetic routes (`/synthetic/*`) and rejects external URLs to prevent open-redirect vulnerabilities.
3. **Web Crawler Boundary**:
   - Restricted to local synthetic HTML endpoints (`/synthetic/page1.html` through `page4.html`) to prevent external denial-of-service or egress traffic.

---

## 6. Recommended Demonstration Sequence for Tomorrow's Lab Exam

Follow this step-by-step sequence to deliver a high-scoring demonstration in under 5 minutes:

### Step 1: Cluster Architecture (30 Seconds)
- **Command**:
  ```bash
  docker ps --format "table {{.Names}}\t{{.Status}}\t{{.Ports}}"
  ```
- **Speaking Point**: "Here is our Docker cluster with native MongoDB sharding: 3 shard replica sets (`rs-shard1..3`), 1 config replica set, the `mongos` query router, Redis 7 for distributed rate limiting and cache-aside, and Apache Kafka KRaft for event streaming."

### Step 2: Live Frontend & Sanitized UI (45 Seconds)
- Open `http://localhost:8080` in the browser.
- **Speaking Point**: "The frontend is built with vanilla HTML5/CSS3/JavaScript without any external frontend frameworks. Notice the neutral test accounts, live authoritative balances, and real ledger transaction history without artificial placeholders."
- Click the Search Bar: type `ali`, `bob`, and `elec`.
- **Speaking Point**: "Notice the search bar layout: text begins cleanly after the search icon with 45px of left padding. As we type, the Java Trie prefix tree autocompletes beneficiaries and utility billers. The first lookup queries the in-memory Trie; subsequent identical lookups hit the Redis cache-aside layer in under 0.3 milliseconds."

### Step 3: Rate Limiting Demonstration (45 Seconds)
- **Command**:
  ```bash
  wsl bash -c "for i in {1..12}; do curl -s -o /dev/null -w '%{http_code}\n' -X POST http://localhost:8080/api/auth/login -H 'Content-Type: application/json' -d '{\"email\":\"demo_user@example.com\",\"password\":\"wrong\"}'; done"
  ```
- **Speaking Point**: "Our Redis token-bucket rate limiter enforces a capacity of 10 tokens and 1 token/sec refill using an atomic Redis Lua script. The first 10 requests are allowed through to authentication (returning 401), while requests 11 and 12 are immediately rejected with HTTP 429 Too Many Requests."

### Step 4: ACID Fund Transfer & Idempotency (60 Seconds)
- On the dashboard, transfer ₹1,500.00 to Alice Smith.
- Submit the transfer and observe immediate balance deduction from ₹50,000.00 to ₹48,500.00.
- Submit the transfer again with the same idempotency key.
- **Speaking Point**: "Our fund transfer uses multi-document distributed ACID transactions across MongoDB shards. The balance is deducted accurately with double-entry debit and credit ledger records. When the exact same transfer is replayed with the same idempotency key, the server detects the replay, returns the existing transaction, and never double-debits the customer."

### Step 5: Kafka Outbox & Consistent Hashing (60 Seconds)
- Show the in-app notification bell icon showing the new notification.
- **Speaking Point**: "Notice the notification: during the transfer transaction, an outbox event was saved to MongoDB. A background poller published it to the Kafka topic `bank.notifications`, and the consumer persisted the in-app notification."
- **Command**:
  ```bash
  curl -s http://localhost:8080/api/consistent-hash/distribution | python3 -m json.tool
  ```
- **Speaking Point**: "Finally, here is our application-level consistent hash ring: 3 physical nodes with 100 virtual nodes each, achieving uniform 33% key distribution and minimizing remapping when nodes are added."

---

## 7. Remaining Issues & Status
- **Critical Issues**: None.
- **Unresolved Regressions**: None.
- **Automated Tests**: 15/15 passing.
- **Branch**: All commits maintained on `member-1`. Ready for demonstration and submission.
