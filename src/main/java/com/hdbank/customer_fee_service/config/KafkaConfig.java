package com.hdbank.customer_fee_service.config;

import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.TopicBuilder;

@Configuration
public class KafkaConfig {

    public static final String TOPIC_FEE_CHARGED = "payment.fee.charged.v1";

    @Bean
    @ConditionalOnProperty(name = "kafka.admin.enabled", havingValue = "true", matchIfMissing = false)
    public NewTopic feeChargedTopic() {
        return TopicBuilder.name(TOPIC_FEE_CHARGED)
                .partitions(3)
                .replicas(1)
                .build();
    }
}