package com.systemdesign.banking.ledger;

import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface LedgerRepository extends MongoRepository<LedgerEntry, String> {
    List<LedgerEntry> findByTransactionId(String transactionId);
    List<LedgerEntry> findByAccountNumberOrderByTimestampDesc(String accountNumber);
    List<LedgerEntry> findByTransactionReference(String transactionReference);
}
