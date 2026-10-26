#!/usr/bin/env bash
docker exec mongos mongosh --port 27017 --quiet --eval '
  const dbName = "banking_db";
  const collections = ["customers", "accounts", "transactions", "ledgers", "bill_payments", "outbox_events", "notifications", "short_urls", "beneficiaries", "billers"];
  collections.forEach(col => {
    try {
      db.getSiblingDB(dbName).createCollection(col);
      print("Created collection: " + col);
    } catch (e) {
      print("Collection " + col + " already exists or error: " + e.message);
    }
  });
'
