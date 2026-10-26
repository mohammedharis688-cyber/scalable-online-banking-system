package com.systemdesign.banking.notification;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;

@Document(collection = "notifications")
public class Notification {

    @Id
    private String id;

    @Indexed(unique = true)
    private String notificationId;

    @Indexed
    private String customerId;

    private String title;
    private String message;
    private String type; // TRANSFER_OUT, TRANSFER_IN, BILL_PAYMENT, SYSTEM
    private boolean isRead = false;
    private Instant timestamp = Instant.now();

    public Notification() {
    }

    public Notification(String notificationId, String customerId, String title, String message, String type) {
        this.notificationId = notificationId;
        this.customerId = customerId;
        this.title = title;
        this.message = message;
        this.type = type;
        this.isRead = false;
        this.timestamp = Instant.now();
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getNotificationId() {
        return notificationId;
    }

    public void setNotificationId(String notificationId) {
        this.notificationId = notificationId;
    }

    public String getCustomerId() {
        return customerId;
    }

    public void setCustomerId(String customerId) {
        this.customerId = customerId;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public boolean isRead() {
        return isRead;
    }

    public void setRead(boolean read) {
        isRead = read;
    }

    public Instant getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(Instant timestamp) {
        this.timestamp = timestamp;
    }
}
