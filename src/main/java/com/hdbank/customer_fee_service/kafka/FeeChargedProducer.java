package com.hdbank.customer_fee_service.kafka;

import com.hdbank.customer_fee_service.config.KafkaConfig;
import com.hdbank.customer_fee_service.dto.response.FeeChargeResult;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.UUID;

@Component
@RequiredArgsConstructor
@Slf4j
public class FeeChargedProducer {

    private final KafkaTemplate<String, FeeChargedEvent> kafkaTemplate;

    public void publishFeeChargedEvent(FeeChargeResult result) {
        try {
            FeeChargedEvent event = FeeChargedEvent.builder()
                    .eventId(UUID.randomUUID().toString())
                    .eventTime(Instant.now().toString())
                    .jobId(result.getJobId())
                    .customerId(result.getCustomerId())
                    .feeConfigId(result.getFeeConfigId())
                    .chargedAmount(result.getChargedAmount())
                    .currency(result.getCurrency())
                    .billingMonth(result.getBillingMonth())
                    .eventType("FEE_CHARGED")
                    .build();

            log.info("Publishing FeeChargedEvent for customer: {}, amount: {}",
                    result.getCustomerId(), result.getChargedAmount());

            kafkaTemplate.send(KafkaConfig.TOPIC_FEE_CHARGED,
                    result.getCustomerId().toString(),
                    event);

            log.info("FeeChargedEvent published successfully");

        } catch (Exception e) {
            log.error("Error publishing FeeChargedEvent", e);
            // Don't throw - fee already charged, just log error
        }
    }
}