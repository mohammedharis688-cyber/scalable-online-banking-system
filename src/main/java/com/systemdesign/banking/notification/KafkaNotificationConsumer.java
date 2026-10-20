package com.systemdesign.banking.notification;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.systemdesign.banking.config.KafkaConfig;
import com.systemdesign.banking.systemdesign.idgenerator.UniqueIdGenerator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
public class KafkaNotificationConsumer {

    private static final Logger log = LoggerFactory.getLogger(KafkaNotificationConsumer.class);

    private final NotificationRepository notificationRepository;
    private final UniqueIdGenerator idGenerator;
    private final ObjectMapper objectMapper;

    public KafkaNotificationConsumer(
            NotificationRepository notificationRepository,
            UniqueIdGenerator idGenerator,
            ObjectMapper objectMapper) {
        this.notificationRepository = notificationRepository;
        this.idGenerator = idGenerator;
        this.objectMapper = objectMapper;
    }

    @KafkaListener(topics = KafkaConfig.NOTIFICATION_TOPIC, groupId = "banking-notification-group")
    public void consumeNotificationEvent(String message) {
        try {
            log.info("Kafka consumer received notification event: {}", message);
            JsonNode root = objectMapper.readTree(message);

            String customerId = root.path("customerId").asText();
            String title = root.path("title").asText("Banking Notification");
            String body = root.path("message").asText();
            String type = root.path("type").asText("SYSTEM");

            if (customerId != null && !customerId.isBlank()) {
                Notification notification = new Notification(
                        idGenerator.generateNotificationId(),
                        customerId,
                        title,
                        body,
                        type
                );
                notificationRepository.save(notification);
                log.info("Persisted in-app notification [{}] for customer [{}]",
                        notification.getNotificationId(), customerId);
            }
        } catch (Exception ex) {
            log.error("Failed to process Kafka notification message: {}", message, ex);
        }
    }
}
