package com.hdbank.customer_fee_service.dto.response;


import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.Instant;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class FeeChargeResult {

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

    private String status;  // SUCCESS, FAILED

    @JsonProperty("error_message")
    private String errorMessage;

    @JsonProperty("charged_at")
    private Instant chargedAt;
}
