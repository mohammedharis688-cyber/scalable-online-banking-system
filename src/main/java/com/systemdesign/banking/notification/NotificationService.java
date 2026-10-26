package com.systemdesign.banking.notification;

import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class NotificationService {

    private final NotificationRepository notificationRepository;

    public NotificationService(NotificationRepository notificationRepository) {
        this.notificationRepository = notificationRepository;
    }

    public List<Notification> getCustomerNotifications(String customerId) {
        return notificationRepository.findByCustomerIdOrderByTimestampDesc(customerId);
    }

    public long getUnreadCount(String customerId) {
        return notificationRepository.countByCustomerIdAndIsReadFalse(customerId);
    }

    public void markAsRead(String notificationId, String customerId) {
        notificationRepository.findByNotificationIdAndCustomerId(notificationId, customerId)
                .ifPresent(notification -> {
                    notification.setRead(true);
                    notificationRepository.save(notification);
                });
    }

    public void markAllAsRead(String customerId) {
        List<Notification> unread = notificationRepository.findByCustomerIdOrderByTimestampDesc(customerId);
        for (Notification n : unread) {
            if (!n.isRead()) {
                n.setRead(true);
                notificationRepository.save(n);
            }
        }
    }
}
