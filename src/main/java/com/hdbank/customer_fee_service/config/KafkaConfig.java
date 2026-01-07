package com.hdbank.customer_fee_service.config;

import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.TopicBuilder;

@Configuration
public class KafkaConfig {

    // Main topic - fee charged successfully
    public static final String TOPIC_FEE_CHARGED = "payment.fee.charged.v1";

    // Retry topic - for failed message processing
    public static final String TOPIC_FEE_CHARGED_RETRY = "payment.fee.charged.retry.v1";

    // Dead Letter Queue - for messages that failed after retries
    public static final String TOPIC_FEE_CHARGED_DLQ = "payment.fee.charged.dlq.v1";

    @Bean
    @ConditionalOnProperty(name = "kafka.admin.enabled", havingValue = "true", matchIfMissing = false)
    public NewTopic feeChargedTopic() {
        return TopicBuilder.name(TOPIC_FEE_CHARGED)
                .partitions(3)
                .replicas(1)
                .build();
    }

    @Bean
    @ConditionalOnProperty(name = "kafka.admin.enabled", havingValue = "true", matchIfMissing = false)
    public NewTopic feeChargedRetryTopic() {
        return TopicBuilder.name(TOPIC_FEE_CHARGED_RETRY)
                .partitions(3)
                .replicas(1)
                .build();
    }

    @Bean
    @ConditionalOnProperty(name = "kafka.admin.enabled", havingValue = "true", matchIfMissing = false)
    public NewTopic feeChargedDLQTopic() {
        return TopicBuilder.name(TOPIC_FEE_CHARGED_DLQ)
                .partitions(1)  // DLQ usually doesn't need many partitions
                .replicas(1)
                .build();
    }
}