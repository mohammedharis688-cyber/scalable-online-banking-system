package com.systemdesign.banking.billpayment;

import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface BillPaymentRepository extends MongoRepository<BillPayment, String> {
    List<BillPayment> findByCustomerIdOrderByTimestampDesc(String customerId);
}
