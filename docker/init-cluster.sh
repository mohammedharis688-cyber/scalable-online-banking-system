#!/bin/bash
set -e

echo "=== Initializing MongoDB Sharded Cluster ==="

# Wait for all nodes to be accessible
wait_for_mongo() {
    host=$1
    port=$2
    echo "Waiting for ${host}:${port}..."
    until mongosh --host "${host}" --port "${port}" --eval "db.adminCommand('ping')" > /dev/null 2>&1; do
        sleep 2
    done
    echo "${host}:${port} is up."
}

wait_for_mongo "mongo-configsvr" 27019
wait_for_mongo "mongo-shard1" 27018
wait_for_mongo "mongo-shard2" 27018
wait_for_mongo "mongo-shard3" 27018

# 1. Initiate Config Server Replica Set
echo "Configuring Config Server Replica Set (rs-cfg)..."
mongosh --host "mongo-configsvr" --port 27019 --eval '
try {
    rs.status();
} catch (e) {
    rs.initiate({
        _id: "rs-cfg",
        configsvr: true,
        members: [{ _id: 0, host: "mongo-configsvr:27019" }]
    });
}
'

# 2. Initiate Shard 1 Replica Set
echo "Configuring Shard 1 Replica Set (rs-shard1)..."
mongosh --host "mongo-shard1" --port 27018 --eval '
try {
    rs.status();
} catch (e) {
    rs.initiate({
        _id: "rs-shard1",
        members: [{ _id: 0, host: "mongo-shard1:27018" }]
    });
}
'

# 3. Initiate Shard 2 Replica Set
echo "Configuring Shard 2 Replica Set (rs-shard2)..."
mongosh --host "mongo-shard2" --port 27018 --eval '
try {
    rs.status();
} catch (e) {
    rs.initiate({
        _id: "rs-shard2",
        members: [{ _id: 0, host: "mongo-shard2:27018" }]
    });
}
'

# 4. Initiate Shard 3 Replica Set
echo "Configuring Shard 3 Replica Set (rs-shard3)..."
mongosh --host "mongo-shard3" --port 27018 --eval '
try {
    rs.status();
} catch (e) {
    rs.initiate({
        _id: "rs-shard3",
        members: [{ _id: 0, host: "mongo-shard3:27018" }]
    });
}
'

# Wait for mongos to be reachable
wait_for_mongo "mongos" 27017

# 5. Add Shards to Mongos Router
echo "Adding shards to mongos..."
mongosh --host "mongos" --port 27017 --eval '
sh.addShard("rs-shard1/mongo-shard1:27018");
sh.addShard("rs-shard2/mongo-shard2:27018");
sh.addShard("rs-shard3/mongo-shard3:27018");

print("Current Shards in Cluster:");
printjson(sh.status());
'

# 6. Enable Sharding on database and shard target collections
echo "Configuring collection sharding for banking_db..."
mongosh --host "mongos" --port 27017 --eval '
sh.enableSharding("banking_db");

// Shard customers by hashed customerId for uniform customer lookup distribution
sh.shardCollection("banking_db.customers", { "customerId": "hashed" });

// Shard accounts by hashed accountNumber for balanced balance lookups
sh.shardCollection("banking_db.accounts", { "accountNumber": "hashed" });

// Shard transactions by hashed transactionId for write throughput
sh.shardCollection("banking_db.transactions", { "transactionId": "hashed" });

// Shard ledgers by hashed transactionId to collocate ledger entries with transactions
sh.shardCollection("banking_db.ledgers", { "transactionId": "hashed" });

// Shard bill_payments by hashed paymentReference for write scalability
db.getSiblingDB("banking_db").bill_payments.createIndex({ "paymentReference": "hashed" });
sh.shardCollection("banking_db.bill_payments", { "paymentReference": "hashed" });

print("=== MongoDB Sharded Cluster Configuration Complete! ===");
printjson(sh.status());
'
