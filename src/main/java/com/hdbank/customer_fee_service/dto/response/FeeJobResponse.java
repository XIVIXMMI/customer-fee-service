package com.hdbank.customer_fee_service.dto.response;

import com.hdbank.customer_fee_service.entity.CustomerFeeJob;
import com.hdbank.customer_fee_service.entity.FeeJobStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.Instant;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FeeJobResponse {
    private Long id;
    private Long customerId;
    private String billingMonth;
    private BigDecimal amount;
    private FeeJobStatus status;
    private String idempotencyKey;
    private Instant createdAt;
    private Instant updatedAt;

    public static FeeJobResponse from(CustomerFeeJob job) {
        return FeeJobResponse.builder()
                .id(job.getId())
                .customerId(job.getCustomerId())
                .billingMonth(job.getBillingMonth())
                .amount(job.getAmount())
                .status(job.getStatus())
                .idempotencyKey(job.getIdempotencyKey())
                .createdAt(job.getCreatedAt())
                .updatedAt(job.getUpdatedAt())
                .build();
    }
}