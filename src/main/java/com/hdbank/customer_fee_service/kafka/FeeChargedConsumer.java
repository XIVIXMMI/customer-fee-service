package com.hdbank.customer_fee_service.kafka;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.hdbank.customer_fee_service.config.KafkaConfig;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class FeeChargedConsumer {

    private final ObjectMapper objectMapper;

    @KafkaListener(
            topics = KafkaConfig.TOPIC_FEE_CHARGED,
            groupId = "${spring.kafka.consumer.group-id}",
            containerFactory = "kafkaListenerContainerFactory"
    )
    public void consumeFeeChargedEvent(String eventJson) {
        log.info("Received message from topic: {}", KafkaConfig.TOPIC_FEE_CHARGED);

        try {
            // Deserialize JSON string to FeeChargedEvent object
            FeeChargedEvent event = objectMapper.readValue(eventJson, FeeChargedEvent.class);

            log.info("Received FeeChargedEvent: eventId={}, customerId={}, amount={} {}",
                    event.getEventId(),
                    event.getCustomerId(),
                    event.getChargedAmount(),
                    event.getCurrency());

            // Process event - example: send notification, update analytics, etc.
            processEvent(event);

            log.info("FeeChargedEvent processed successfully: {}", event.getEventId());

        } catch (Exception e) {
            log.error("Error processing FeeChargedEvent from JSON: {}", eventJson, e);
            // send to DLQ or retry topic
        }
    }

    private void processEvent(FeeChargedEvent event) {
        // TODO: Implement business logic
        log.info("Processing fee charged event for customer: {}", event.getCustomerId());
        log.info("Fee amount: {} {}", event.getChargedAmount(), event.getCurrency());
        log.info("Billing month: {}", event.getBillingMonth());

    }
}