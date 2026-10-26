# Step-by-Step Demonstration Guide
## Scalable Online Banking System — UCS3513 System Design Laboratory

This guide provides an end-to-end walkthrough for demonstrating all banking workflows and system design laboratory experiments to evaluators.

---

## Preparation & Environment Launch

1. Open a terminal in the project root:
   ```bash
   docker compose up --build
   ```
2. Wait until the cluster initializer displays:
   `=== MongoDB Sharded Cluster Configuration Complete! ===`
3. Open your browser and navigate to:
   [http://localhost:8080](http://localhost:8080)

---

## Demonstration Script

### Step 1: Customer Authentication & Registration
1. On the welcome screen, review the pre-seeded demo users.
2. Click **John Doe (₹50,000 INR)** to automatically fill the credentials (`john.doe@example.com` / `Password123!`).
3. Click **Sign In to Account**.
4. Observe the modern fintech dashboard showing authoritative balance, primary account number (`ACC-1001-10000001`), and customer ID (`CUST-1001`).

### Step 2: Authoritative Balance & Account Ownership
1. Click **Accounts** in the top navigation.
2. Notice the two accounts owned by John Doe (Savings and Checking).
3. Click **Verify Live Shard Balance** on any account. Observe the toast notification confirming the authoritative value retrieved directly from MongoDB without stale caching.
4. Click **+ Open Additional Account**, select `CHECKING`, enter `1000`, and confirm. Observe the newly created account partitioned across the MongoDB shards.

### Step 3: Beneficiary Management
1. Click **Beneficiaries**.
2. Notice pre-registered beneficiaries: Alice Smith (`ACC-1001-10000002`) and Bob Jones (`ACC-1001-10000003`).
3. Click **+ Add New Beneficiary**.
4. Test validation: try entering a non-existent account number (e.g., `ACC-9999-99999999`). Notice the system rejects it because destinations must be active internal accounts.

### Step 4: Fund Transfers & Idempotency
1. Click **Transfers**.
2. Source account: `ACC-1001-10000001` (John Doe).
3. Click **Select Beneficiary** and choose **Alice Smith**.
4. Enter Amount: `1500.00`.
5. Notice the auto-generated **Idempotency Key** (`IDEMP-...`).
6. Click **Authorize & Execute Transfer**.
7. Observe the green confirmation card displaying:
   - Unique Transaction Reference
   - Debit and credit details
   - Updated source balance
8. **Demonstrate Idempotency**: Immediately click **Authorize & Execute Transfer** a second time without changing the idempotency key. Notice the response returns `Idempotent replay: transfer was already processed` and does NOT deduct funds again!

### Step 5: Double-Entry Ledger Mathematical Audit
1. Click **History** in the top navigation.
2. Find the transfer just completed.
3. Click the **Audit Ledger** button on that row.
4. The audit modal pops up, displaying the mathematical proof:
   - Entry 1: DEBIT of ₹1500.00 on John's account.
   - Entry 2: CREDIT of ₹1500.00 on Alice's account.
   - Difference: `₹0.00`
   - Status: `✓ PERFECTLY BALANCED (Sum Debits == Sum Credits)`.

### Step 6: Utility Bill Payment with Trie Autocomplete
1. Click **Bill Payments**.
2. In the search box, start typing `elec`. Notice the Trie autocomplete dropdown instantly suggesting `Apex City Power & Electric (ELEC-01)`.
3. Click the suggestion to populate the biller.
4. Enter a consumer reference (e.g., `METER-5544`) and amount (e.g., `850.00`).
5. Click **Confirm & Pay Utility Bill**.
6. Notice the payment record added to the history and the balance updated.

### Step 7: In-App Notifications via Apache Kafka
1. Click the notification bell icon 🔔 in the top-right navbar.
2. View the notifications dispatched through the Kafka transactional outbox pipeline (e.g., "Bill Payment Successful").
3. Click **Mark Read** or **Mark All as Read**.

### Step 8: System Insights & Laboratory Experiments
Navigate to **System Insights** to demonstrate each laboratory module:

1. **Token Bucket Rate Limiter**:
   - Click **Trigger 15 Burst Requests**.
   - Watch the live log show the first 10 requests allowed and requests 11–15 rejected with **HTTP 429 Too Many Requests**.
2. **MongoDB Native Sharding**:
   - Click **Refresh Cluster Metadata** to view the live JSON output confirming connection to `mongos` router (`isMongosRouter: true`).
3. **Consistent Hashing**:
   - Click **+ Add Node 4** to execute the topology mutation.
   - Show evaluators that across 1,000 keys, exactly $\approx 25\%$ of keys were remapped, proving the minimal remapping property.
4. **Unique ID Benchmark**:
   - Click **Run 50,000 IDs**.
   - Show the live generation throughput ($> 80,000$ IDs/sec) and verified **0 collisions**.
5. **URL Shortener**:
   - Select `/synthetic/page1.html` and click **Create Short Link**.
   - Click the generated short URL (`/s/{code}`) to demonstrate HTTP 302 redirection with Redis cache-aside.
6. **Trie Autocomplete**:
   - Type a prefix and click **Execute Search**.
   - Compare the cold Java Trie latency vs. warm Redis cache-aside lookup latency in microseconds.
7. **Synthetic Web Crawler**:
   - Click **Start Local Crawl**.
   - Review the metrics showing pages processed, duplicate URLs avoided, and the table of extracted utility biller records.
8. **Dataset Scaling**:
   - Click **Generate 50 Customers + 250 Transfers** to dynamically stress-test MongoDB collection sharding.
