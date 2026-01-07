package com.hdbank.customer_fee_service.dto.response;

import com.hdbank.customer_fee_service.entity.AttemptStatus;
import com.hdbank.customer_fee_service.entity.FeeChargeAttempt;
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
public class FeeChargeAttemptResponse {
    private Long id;
    private Long jobId;
    private Long customerId;
    private String billingMonth;
    private BigDecimal amount;
    private Integer attemptNo;
    private AttemptStatus status;
    private String errorCode;
    private String errorMessage;
    private String externalTxnId;
    private Instant createdAt;

    public static FeeChargeAttemptResponse from(FeeChargeAttempt attempt) {
        return FeeChargeAttemptResponse.builder()
                .id(attempt.getId())
                .jobId(attempt.getJobId())
                .customerId(attempt.getCustomerId())
                .billingMonth(attempt.getBillingMonth())
                .amount(attempt.getAmount())
                .attemptNo(attempt.getAttemptNo())
                .status(attempt.getStatus())
                .errorCode(attempt.getErrorCode())
                .errorMessage(attempt.getErrorMessage())
                .externalTxnId(attempt.getExternalTxnId())
                .createdAt(attempt.getCreatedAt())
                .build();
    }
}