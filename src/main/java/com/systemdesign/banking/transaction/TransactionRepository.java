package com.systemdesign.banking.transaction;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface TransactionRepository extends MongoRepository<Transaction, String> {

    Optional<Transaction> findByIdempotencyKey(String idempotencyKey);

    Optional<Transaction> findByTransactionReference(String transactionReference);

    Page<Transaction> findBySourceAccountNumberOrDestinationAccountNumberOrderByTimestampDesc(
            String sourceAccount, String destinationAccount, Pageable pageable);

    Page<Transaction> findBySourceAccountNumberOrDestinationAccountNumberAndTypeOrderByTimestampDesc(
            String sourceAccount, String destinationAccount, String type, Pageable pageable);
}
