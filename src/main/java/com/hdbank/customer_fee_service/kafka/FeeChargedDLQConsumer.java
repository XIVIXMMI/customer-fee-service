package com.hdbank.customer_fee_service.kafka;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.hdbank.customer_fee_service.config.KafkaConfig;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

/**
 * Dead Letter Queue Consumer
 * This consumer only LOGS failed events for manual investigation
 * DO NOT process or retry events from DLQ automatically
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class FeeChargedDLQConsumer {

    private final ObjectMapper objectMapper;

    @KafkaListener(
            topics = KafkaConfig.TOPIC_FEE_CHARGED_DLQ,
            groupId = "${spring.kafka.consumer.group-id}-dlq",
            containerFactory = "kafkaListenerContainerFactory"
    )
    public void consumeDLQEvent(String eventJson) {
        log.error("DLQ: Received failed event from topic: {}", KafkaConfig.TOPIC_FEE_CHARGED_DLQ);
        log.error("DLQ: Raw JSON: {}", eventJson);

        try {
            FeeChargedEvent event = objectMapper.readValue(eventJson, FeeChargedEvent.class);

            log.error("DLQ: Failed event details:");
            log.error("  - Event ID: {}", event.getEventId());
            log.error("  - Customer ID: {}", event.getCustomerId());
            log.error("  - Job ID: {}", event.getJobId());
            log.error("  - Amount: {} {}", event.getChargedAmount(), event.getCurrency());
            log.error("  - Billing Month: {}", event.getBillingMonth());
            log.error("  - Event Time: {}", event.getEventTime());

        } catch (Exception e) {
            log.error("DLQ: Cannot parse event JSON", e);
        }

        log.error("DLQ: This event requires MANUAL investigation and resolution");

        // TODO: Integrate with monitoring/alerting system
        // - Send alert to ops team (email, Slack, PagerDuty)
        // - Store in separate DLQ database table for investigation
        // - Create JIRA ticket automatically
    }
}