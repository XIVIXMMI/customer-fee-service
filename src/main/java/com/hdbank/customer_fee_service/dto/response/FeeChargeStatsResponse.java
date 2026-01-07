package com.hdbank.customer_fee_service.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FeeChargeStatsResponse {
    private String billingMonth;
    private Long totalJobs;
    private Long jobsDone;
    private Long jobsFailed;
    private Long jobsPending;
    private Long totalAttempts;
    private Long attemptsSuccess;
    private Long attemptsFailed;
}