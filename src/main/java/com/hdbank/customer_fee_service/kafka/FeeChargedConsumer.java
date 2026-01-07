package com.hdbank.customer_fee_service.kafka;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.hdbank.customer_fee_service.config.KafkaConfig;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

@Component
@RequiredArgsConstructor
@Slf4j
public class FeeChargedConsumer {

    private final ObjectMapper objectMapper;
    private final KafkaTemplate<String, String> kafkaTemplate;

    // In-memory cache to track retry counts per event
    private final Map<String, Integer> retryCountMap = new HashMap<>();
    private static final int MAX_RETRY_ATTEMPTS = 3;

    @KafkaListener(
            topics = KafkaConfig.TOPIC_FEE_CHARGED,
            groupId = "${spring.kafka.consumer.group-id}",
            containerFactory = "kafkaListenerContainerFactory"
    )
    @Retryable(
            retryFor = {Exception.class},
            maxAttempts = 3,
            backoff = @Backoff(delay = 1000, multiplier = 2.0)  // 1s, 2s, 4s
    )
    public void consumeFeeChargedEvent(
            String eventJson,
            @Header(value = KafkaHeaders.RECEIVED_KEY, required = false) String key
    ) throws JsonProcessingException {
        log.info("Received message from topic: {}", KafkaConfig.TOPIC_FEE_CHARGED);

        try {
            // Deserialize JSON string to FeeChargedEvent object
            FeeChargedEvent event = objectMapper.readValue(eventJson, FeeChargedEvent.class);

            log.info("Processing FeeChargedEvent: eventId={}, customerId={}, amount={} {}",
                    event.getEventId(),
                    event.getCustomerId(),
                    event.getChargedAmount(),
                    event.getCurrency());

            // Check if this event has been retried too many times
            int retryCount = retryCountMap.getOrDefault(event.getEventId(), 0);

            if (retryCount >= MAX_RETRY_ATTEMPTS) {
                log.error("Event {} exceeded max retry attempts, sending to DLQ", event.getEventId());
                sendToDLQ(eventJson, event.getEventId());
                retryCountMap.remove(event.getEventId());
                return;
            }

            processEvent(event);

            retryCountMap.remove(event.getEventId());

            log.info("FeeChargedEvent processed successfully: {}", event.getEventId());

        } catch (Exception e) {
            log.error("Error processing FeeChargedEvent from JSON: {}", eventJson, e);

            String eventId = extractEventId(eventJson);

            if (eventId != null) {
                int currentRetry = retryCountMap.getOrDefault(eventId, 0);
                retryCountMap.put(eventId, currentRetry + 1);

                if (currentRetry + 1 >= MAX_RETRY_ATTEMPTS) {
                    log.error("Max retries reached for event {}, sending to DLQ", eventId);
                    sendToDLQ(eventJson, eventId);
                    retryCountMap.remove(eventId);
                } else {
                    log.info("Sending event {} to retry topic (attempt {}/{})",
                            eventId, currentRetry + 1, MAX_RETRY_ATTEMPTS);
                    sendToRetryTopic(eventJson, eventId);
                }
            } else {
                // Can't extract eventId, send directly to DLQ
                log.error("Cannot extract eventId, sending to DLQ");
                sendToDLQ(eventJson, "unknown");
            }

            throw e; // Re-throw to trigger @Retryable
        }
    }

    private void processEvent(FeeChargedEvent event) {
        // TODO: Implement business logic
        log.info("Processing fee charged event for customer: {}", event.getCustomerId());
        log.info("Fee amount: {} {}", event.getChargedAmount(), event.getCurrency());
        log.info("Billing month: {}", event.getBillingMonth());

        // Example: Send email notification, update analytics dashboard, etc.

        // Simulate processing (remove in production)
        if (event.getChargedAmount() == null) {
            throw new IllegalArgumentException("Charged amount cannot be null");
        }
    }

    private void sendToRetryTopic(String eventJson, String eventId) {
        try {
            kafkaTemplate.send(KafkaConfig.TOPIC_FEE_CHARGED_RETRY, eventId, eventJson);
            log.info("Event {} sent to retry topic", eventId);
        } catch (Exception e) {
            log.error("Error sending event to retry topic", e);
            sendToDLQ(eventJson, eventId);
        }
    }

    private void sendToDLQ(String eventJson, String eventId) {
        try {
            kafkaTemplate.send(KafkaConfig.TOPIC_FEE_CHARGED_DLQ, eventId, eventJson);
            log.info("Event {} sent to DLQ", eventId);
        } catch (Exception e) {
            log.error("CRITICAL: Failed to send event to DLQ", e);
            // TODO: Alert ops team
        }
    }

    private String extractEventId(String eventJson) {
        try {
            FeeChargedEvent event = objectMapper.readValue(eventJson, FeeChargedEvent.class);
            return event.getEventId();
        } catch (Exception e) {
            log.warn("Cannot extract eventId from JSON", e);
            return null;
        }
    }
}