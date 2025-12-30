package com.hdbank.customer_fee_service.kafka;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FeeChargedEvent {

    @JsonProperty("event_id")
    private String eventId;  // UUID

    @JsonProperty("event_time")
    private String eventTime;  // ISO-8601

    @JsonProperty("job_id")
    private Long jobId;

    @JsonProperty("customer_id")
    private Long customerId;

    @JsonProperty("fee_config_id")
    private Long feeConfigId;

    @JsonProperty("charged_amount")
    private BigDecimal chargedAmount;

    private String currency;

    @JsonProperty("billing_month")
    private String billingMonth;

    @Builder.Default
    @JsonProperty("event_type")
    private String eventType = "FEE_CHARGED";
}