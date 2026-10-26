#!/usr/bin/env bash
set -e

BASE_URL="http://localhost:8080"

# Portable JSON helper functions using Python 3
pj() {
  python3 -m json.tool
}

extract_json() {
  python3 -c "import sys, json; data=json.load(sys.stdin); print(data.get('$1', ''))"
}

echo "================================================================="
echo " UCS3513 SYSTEM DESIGN LABORATORY - AUTOMATED VALIDATION SUITE"
echo " Target: Scalable Online Banking System (Spring Boot + Mongo + Redis + Kafka)"
echo "================================================================="

echo ""
echo "-----------------------------------------------------------------"
echo ">>> TEST 1: REDIS TOKEN BUCKET RATE LIMITER"
echo "-----------------------------------------------------------------"
curl -s -X POST "${BASE_URL}/api/system/ratelimiter/reset"
echo "Rate limiter telemetry reset."

echo "Testing burst of 12 requests (Capacity: 10, Refill: 1.0/s)..."
for i in $(seq 1 12); do
  status=$(curl -s -o /tmp/resp.json -w "%{http_code}" -X POST "${BASE_URL}/api/auth/login" \
    -H "Content-Type: application/json" \
    -d '{"email":"burst_limiter@example.com","password":"BadPassword!"}')
  msg=$(grep -o '"message":"[^"]*"' /tmp/resp.json || echo "No message")
  echo "Request $i: HTTP $status ($msg)"
done

echo "Waiting 2.5 seconds for bucket refill..."
sleep 2.5
status=$(curl -s -o /tmp/resp.json -w "%{http_code}" -X POST "${BASE_URL}/api/auth/login" \
  -H "Content-Type: application/json" \
  -d '{"email":"burst_limiter@example.com","password":"BadPassword!"}')
msg=$(grep -o '"message":"[^"]*"' /tmp/resp.json || echo "No message")
echo "After refill Request 13: HTTP $status ($msg)"

echo "Redis key inspection:"
docker exec banking-redis redis-cli HGETALL "rate_limit:login:burst_limiter@example.com"
echo "Rate Limiter Telemetry:"
curl -s "${BASE_URL}/api/system/ratelimiter/metrics" | pj
echo ""

echo ""
echo "-----------------------------------------------------------------"
echo ">>> TEST 2 & 5: REDIS CACHE-ASIDE & TRIE AUTOCOMPLETE"
echo "-----------------------------------------------------------------"
curl -s -X POST "${BASE_URL}/api/system/cache/reset"
echo "Cache telemetry reset."

echo "Query 1 (Prefix: 'elec') - Expect Cache MISS -> Java Trie:"
curl -s "${BASE_URL}/api/autocomplete?prefix=elec" | pj
echo ""

echo "Query 2 (Prefix: 'elec') - Expect Cache HIT -> Redis Cache-Aside:"
curl -s "${BASE_URL}/api/autocomplete?prefix=elec" | pj
echo ""

echo "Query 3 (Prefix: 'ali') - Beneficiary search (Alice Smith):"
curl -s "${BASE_URL}/api/autocomplete?prefix=ali" | pj
echo ""

echo "Query 4 (Prefix: 'bob') - Beneficiary search (Bob Jones):"
curl -s "${BASE_URL}/api/autocomplete?prefix=bob" | pj
echo ""

echo "Cache Metrics Telemetry:"
curl -s "${BASE_URL}/api/system/cache/metrics" | pj
echo ""

echo ""
echo "-----------------------------------------------------------------"
echo ">>> TEST 3: MONGODB NATIVE SHARDING TOPOLOGY & CHUNK DISTRIBUTION"
echo "-----------------------------------------------------------------"
echo "Spring Boot Sharding Status API:"
curl -s "${BASE_URL}/api/system/sharding/status" | pj
echo ""

echo "Native mongos sh.status() Summary:"
docker exec mongos mongosh --port 27017 --quiet --eval '
  print("=== SHARDS IN CLUSTER ===");
  db.adminCommand({ listShards: 1 }).shards.forEach(s => print(s._id + " -> " + s.host));
  print("\n=== SHARDED COLLECTIONS IN banking_db ===");
  const collections = ["customers", "accounts", "transactions", "ledgers", "bill_payments"];
  collections.forEach(c => {
    const collStats = db.getSiblingDB("banking_db").getCollection(c).stats();
    print(c + " sharded: " + collStats.sharded + ", total docs: " + collStats.count);
    if (collStats.shards) {
      for (const shard in collStats.shards) {
        print("   " + shard + ": " + collStats.shards[shard].count + " docs");
      }
    }
  });
'
echo ""

echo ""
echo "-----------------------------------------------------------------"
echo ">>> TEST 4: APPLICATION-LEVEL CONSISTENT HASHING RING"
echo "-----------------------------------------------------------------"
echo "Current Ring Nodes and Virtual Nodes:"
curl -s "${BASE_URL}/api/consistent-hash/nodes" | pj
echo ""

echo "Key Routing (Key: CUST-1001, ACC-1001-10000001, TXN-999):"
curl -s "${BASE_URL}/api/consistent-hash/route?key=CUST-1001" | pj
curl -s "${BASE_URL}/api/consistent-hash/route?key=ACC-1001-10000001" | pj
curl -s "${BASE_URL}/api/consistent-hash/route?key=TXN-999" | pj
echo ""

echo "Uniform Distribution Analysis (1000 sample keys across 3 nodes):"
curl -s "${BASE_URL}/api/consistent-hash/distribution?sampleSize=1000" | pj
echo ""

echo "Topology Mutation Experiment: Add Node 'banking-srv-node-4':"
curl -s -X POST "${BASE_URL}/api/consistent-hash/experiment" \
  -H "Content-Type: application/json" \
  -d '{"action":"ADD","nodeName":"banking-srv-node-4","sampleSize":1000}' | pj
echo ""

echo ""
echo "-----------------------------------------------------------------"
echo ">>> TEST 6: URL SHORTENER WITH REDIS CACHE-ASIDE"
echo "-----------------------------------------------------------------"
echo "Creating Short URL for '/synthetic/page1.html':"
SHORT_RESP=$(curl -s -X POST "${BASE_URL}/api/shortener" \
  -H "Content-Type: application/json" \
  -d '{"targetUrl":"/synthetic/page1.html"}')
echo "$SHORT_RESP" | pj
CODE=$(echo "$SHORT_RESP" | extract_json shortCode)
echo "Generated Short Code: $CODE"

echo "Resolving Short Code via Redirect (HTTP 302):"
curl -s -I "${BASE_URL}/s/${CODE}" | head -n 5

echo "Checking Redis Cache for key 'cache:url:${CODE}':"
docker exec banking-redis redis-cli GET "cache:url:${CODE}"

echo "Testing 404 for invalid short code 'invalid99':"
curl -s -o /tmp/resp.json -w "%{http_code}\n" "${BASE_URL}/s/invalid99"
echo ""

echo ""
echo "-----------------------------------------------------------------"
echo ">>> TEST 7: SYNTHETIC WEB CRAWLER"
echo "-----------------------------------------------------------------"
echo "Starting BFS Crawl on '/synthetic/page1.html' (maxPages: 10):"
curl -s -X POST "${BASE_URL}/api/crawler/start?seedUrl=/synthetic/page1.html&maxPages=10" | pj
echo ""

echo "Extracted Biller Records from Synthetic HTML Pages:"
curl -s "${BASE_URL}/api/crawler/results" | pj
echo ""

echo ""
echo "-----------------------------------------------------------------"
echo ">>> TEST 8: UNIQUE ID GENERATOR BENCHMARK"
echo "-----------------------------------------------------------------"
echo "Benchmarking UniqueIdGenerator across 10,000 IDs:"
curl -s "${BASE_URL}/api/system/id-benchmark?count=10000" | pj
echo ""

echo ""
echo "-----------------------------------------------------------------"
echo ">>> TEST 9 & 10: BANKING WORKFLOW, ACID TRANSFER, IDEMPOTENCY & KAFKA OUTBOX"
echo "-----------------------------------------------------------------"
echo "1. Authenticating as John Doe (john.doe@example.com)..."
AUTH_RESP=$(curl -s -X POST "${BASE_URL}/api/auth/login" \
  -H "Content-Type: application/json" \
  -d '{"email":"john.doe@example.com","password":"Password123!"}')
TOKEN=$(echo "$AUTH_RESP" | extract_json token)
ACCOUNT_NUM=$(echo "$AUTH_RESP" | extract_json primaryAccountNumber)
CUSTOMER_ID=$(echo "$AUTH_RESP" | extract_json customerId)
echo "Logged in. Account: $ACCOUNT_NUM, Customer: $CUSTOMER_ID"

echo "2. Initial Balance Enquiry:"
curl -s -H "Authorization: Bearer $TOKEN" "${BASE_URL}/api/accounts/${ACCOUNT_NUM}/balance" | pj
echo ""

IDEMP_KEY="IDEMP-DEMO-$(date +%s%N)"
echo "3. Executing Fund Transfer of 1500.00 to Alice Smith (ACC-1001-10000002) with Idempotency-Key '$IDEMP_KEY':"
TRANSFER_RESP=$(curl -s -X POST "${BASE_URL}/api/transfers" \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -H "X-Idempotency-Key: $IDEMP_KEY" \
  -d "{
    \"sourceAccountNumber\": \"${ACCOUNT_NUM}\",
    \"destinationAccountNumber\": \"ACC-1001-10000002\",
    \"amount\": 1500.00,
    \"description\": \"Validation suite transfer verification\",
    \"idempotencyKey\": \"${IDEMP_KEY}\"
  }")
echo "$TRANSFER_RESP" | pj
TXN_REF=$(echo "$TRANSFER_RESP" | extract_json transactionReference)
echo "Generated Transaction Reference: $TXN_REF"
echo ""

echo "4. Idempotency Test: Repeating exact same transfer with Idempotency-Key '$IDEMP_KEY':"
RETRY_RESP=$(curl -s -X POST "${BASE_URL}/api/transfers" \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -H "X-Idempotency-Key: $IDEMP_KEY" \
  -d "{
    \"sourceAccountNumber\": \"${ACCOUNT_NUM}\",
    \"destinationAccountNumber\": \"ACC-1001-10000002\",
    \"amount\": 1500.00,
    \"description\": \"Validation suite transfer verification\",
    \"idempotencyKey\": \"${IDEMP_KEY}\"
  }")
echo "$RETRY_RESP" | pj
echo ""

echo "5. Verifying Balance After Transfer (should be deducted by exactly 1500.00, not 3000.00):"
curl -s -H "Authorization: Bearer $TOKEN" "${BASE_URL}/api/accounts/${ACCOUNT_NUM}/balance" | pj
echo ""

echo "6. Testing Insufficient Balance Transfer (Attempting 999999.00):"
FAIL_RESP=$(curl -s -X POST "${BASE_URL}/api/transfers" \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -H "X-Idempotency-Key: IDEMP-FAIL-$(date +%s)" \
  -d "{
    \"sourceAccountNumber\": \"${ACCOUNT_NUM}\",
    \"destinationAccountNumber\": \"ACC-1001-10000002\",
    \"amount\": 999999.00,
    \"description\": \"Exceeds balance test\",
    \"idempotencyKey\": \"IDEMP-FAIL-$(date +%s)\"
  }")
echo "$FAIL_RESP" | pj
echo ""

echo "7. Executing Bill Payment (Electricity: ELEC-01, Amount: 450.00):"
BILL_RESP=$(curl -s -X POST "${BASE_URL}/api/bills/pay" \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d "{
    \"sourceAccountNumber\": \"${ACCOUNT_NUM}\",
    \"billerCode\": \"ELEC-01\",
    \"consumerReference\": \"MTR-VAL-8821\",
    \"amount\": 450.00
  }")
echo "$BILL_RESP" | pj
echo ""

echo "8. Verifying Kafka Event and In-App Notification (Waiting 1.5s for consumer)..."
sleep 1.5
echo "Notifications for John Doe ($CUSTOMER_ID):"
curl -s -H "Authorization: Bearer $TOKEN" "${BASE_URL}/api/notifications" | pj
echo ""

echo "9. Checking Outbox Status in MongoDB for Transaction Reference '$TXN_REF':"
docker exec mongos mongosh --port 27017 --quiet --eval "
  db.getSiblingDB('banking_db').outbox_events.find({ aggregateId: '$TXN_REF' }).forEach(printjson);
"
echo ""

echo "10. Inspecting Balanced Double-Entry Ledger Entries for '$TXN_REF':"
docker exec mongos mongosh --port 27017 --quiet --eval "
  db.getSiblingDB('banking_db').ledgers.find({ transactionId: '$TXN_REF' }).forEach(printjson);
"
echo ""

echo ""
echo "-----------------------------------------------------------------"
echo ">>> TEST 11: DOCKER COMPOSE CLUSTER HEALTH"
echo "-----------------------------------------------------------------"
docker ps --format "table {{.Names}}\t{{.Status}}\t{{.Ports}}"
echo ""

echo "================================================================="
echo " AUTOMATED VALIDATION SUITE EXECUTION COMPLETED"
echo "================================================================="
