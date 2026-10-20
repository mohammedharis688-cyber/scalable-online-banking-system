package com.systemdesign.banking.billpayment;

import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface BillerRepository extends MongoRepository<Biller, String> {
    Optional<Biller> findByBillerCode(String billerCode);
    List<Biller> findByCategoryIgnoreCase(String category);
}
