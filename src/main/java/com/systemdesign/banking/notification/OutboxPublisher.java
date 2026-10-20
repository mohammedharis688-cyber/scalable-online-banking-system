package com.systemdesign.banking.notification;

import com.systemdesign.banking.config.KafkaConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.List;

@Component
public class OutboxPublisher {

    private static final Logger log = LoggerFactory.getLogger(OutboxPublisher.class);

    private final OutboxRepository outboxRepository;
    private final KafkaTemplate<String, String> kafkaTemplate;

    public OutboxPublisher(OutboxRepository outboxRepository, KafkaTemplate<String, String> kafkaTemplate) {
        this.outboxRepository = outboxRepository;
        this.kafkaTemplate = kafkaTemplate;
    }

    /**
     * Polls pending transactional outbox events and publishes them to Apache Kafka.
     * Prevents dual-write vulnerabilities and guarantees at-least-once notification delivery.
     */
    @Scheduled(fixedDelay = 2000)
    public void publishPendingEvents() {
        List<OutboxEvent> pending = outboxRepository.findTop50ByStatusOrderByCreatedAtAsc("PENDING");
        if (pending.isEmpty()) {
            return;
        }

        for (OutboxEvent event : pending) {
            try {
                kafkaTemplate.send(KafkaConfig.NOTIFICATION_TOPIC, event.getAggregateId(), event.getPayload())
                        .whenComplete((result, ex) -> {
                            if (ex == null) {
                                event.setStatus("PUBLISHED");
                                event.setPublishedAt(Instant.now());
                                outboxRepository.save(event);
                                log.debug("Published outbox event [{}] to Kafka topic [{}]",
                                        event.getEventId(), KafkaConfig.NOTIFICATION_TOPIC);
                            } else {
                                log.warn("Kafka broker temporarily unavailable for event [{}]: {}",
                                        event.getEventId(), ex.getMessage());
                                event.setRetryCount(event.getRetryCount() + 1);
                                if (event.getRetryCount() > 10) {
                                    event.setStatus("FAILED");
                                }
                                outboxRepository.save(event);
                            }
                        });
            } catch (Exception e) {
                log.warn("Error sending outbox event [{}] to Kafka: {}", event.getEventId(), e.getMessage());
                event.setRetryCount(event.getRetryCount() + 1);
                outboxRepository.save(event);
            }
        }
    }
}
