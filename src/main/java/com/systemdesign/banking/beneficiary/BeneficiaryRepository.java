package com.systemdesign.banking.beneficiary;

import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface BeneficiaryRepository extends MongoRepository<Beneficiary, String> {
    List<Beneficiary> findByCustomerId(String customerId);
    Optional<Beneficiary> findByIdAndCustomerId(String id, String customerId);
    boolean existsByCustomerIdAndAccountNumber(String customerId, String accountNumber);
}
