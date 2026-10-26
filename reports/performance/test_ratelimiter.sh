#!/usr/bin/env bash
set -e

echo "=== 1. REDIS TOKEN BUCKET RATE LIMITER TEST ==="
curl -s -X POST http://localhost:8080/api/system/ratelimiter/reset
echo "Reset metrics completed."

echo "Sending 14 rapid login attempts (Capacity: 10, Refill: 1.0/sec)..."
for i in $(seq 1 14); do
  status=$(curl -s -o /tmp/resp.json -w "%{http_code}" -X POST http://localhost:8080/api/auth/login \
    -H "Content-Type: application/json" \
    -d '{"email":"ratetest@example.com","password":"DummyPassword123!"}')
  msg=$(cat /tmp/resp.json | grep -o '"message":"[^"]*"' || echo "No message")
  echo "Attempt $i -> HTTP $status | $msg"
done

echo ""
echo "Inspecting Redis Hash key 'rate_limit:login:ratetest@example.com':"
docker exec banking-redis redis-cli HGETALL "rate_limit:login:ratetest@example.com"

echo ""
echo "Inspecting System Rate Limiter Metrics:"
curl -s http://localhost:8080/api/system/ratelimiter/metrics
echo ""
