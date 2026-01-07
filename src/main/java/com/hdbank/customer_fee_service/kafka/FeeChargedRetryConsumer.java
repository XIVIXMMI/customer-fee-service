package com.hdbank.customer_fee_service.kafka;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.hdbank.customer_fee_service.config.KafkaConfig;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class FeeChargedRetryConsumer {

    private final ObjectMapper objectMapper;
    private final KafkaTemplate<String, String> kafkaTemplate;

    @KafkaListener(
            topics = KafkaConfig.TOPIC_FEE_CHARGED_RETRY,
            groupId = "${spring.kafka.consumer.group-id}-retry",
            containerFactory = "kafkaListenerContainerFactory"
    )
    public void consumeRetryEvent(String eventJson) {
        log.info("Received message from RETRY topic: {}", KafkaConfig.TOPIC_FEE_CHARGED_RETRY);

        try {
            FeeChargedEvent event = objectMapper.readValue(eventJson, FeeChargedEvent.class);

            log.info("Retry processing FeeChargedEvent: eventId={}, customerId={}",
                    event.getEventId(), event.getCustomerId());

            // Re-process the event
            processRetryEvent(event);

            log.info("Retry event processed successfully: {}", event.getEventId());

        } catch (Exception e) {
            log.error("Retry processing failed, sending to DLQ: {}", eventJson, e);
            sendToDLQ(eventJson);
        }
    }

    private void processRetryEvent(FeeChargedEvent event) {
        // Same business logic as main consumer
        log.info("Retry - Processing fee charged event for customer: {}", event.getCustomerId());
        log.info("Retry - Fee amount: {} {}", event.getChargedAmount(), event.getCurrency());

        // TODO: Implement actual retry business logic
        // This should be the SAME as main consumer's processEvent()

        // Example: If this is a transient error (DB connection, external API), it might succeed now
        if (event.getChargedAmount() == null) {
            throw new IllegalArgumentException("Charged amount cannot be null");
        }
    }

    private void sendToDLQ(String eventJson) {
        try {
            kafkaTemplate.send(KafkaConfig.TOPIC_FEE_CHARGED_DLQ, eventJson);
            log.info("Retry event sent to DLQ");
        } catch (Exception e) {
            log.error("CRITICAL: Failed to send retry event to DLQ", e);
        }
    }
}