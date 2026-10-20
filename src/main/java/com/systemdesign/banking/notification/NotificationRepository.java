package com.systemdesign.banking.notification;

import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface NotificationRepository extends MongoRepository<Notification, String> {
    List<Notification> findByCustomerIdOrderByTimestampDesc(String customerId);
    Optional<Notification> findByNotificationIdAndCustomerId(String notificationId, String customerId);
    long countByCustomerIdAndIsReadFalse(String customerId);
}
