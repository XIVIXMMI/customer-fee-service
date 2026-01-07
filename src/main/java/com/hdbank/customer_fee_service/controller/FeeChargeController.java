package com.hdbank.customer_fee_service.controller;

import com.hdbank.customer_fee_service.dto.response.ApiDataResponse;
import com.hdbank.customer_fee_service.dto.response.FeeChargeAttemptResponse;
import com.hdbank.customer_fee_service.dto.response.FeeChargeStatsResponse;
import com.hdbank.customer_fee_service.entity.FeeChargeAttempt;
import com.hdbank.customer_fee_service.service.CustomerFeeJobService;
import com.hdbank.customer_fee_service.service.FeeChargeAttemptService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;


import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/v1/fee-charges")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Fee Charge Monitoring", description = "APIs for monitoring fee charges and failures")
public class FeeChargeController {

    private final FeeChargeAttemptService attemptService;
    private final CustomerFeeJobService feeJobService;

    @GetMapping("/failures")
    @Operation(
            summary = "Get failed fee charges",
            description = "Retrieve all failed fee charge attempts, optionally filtered by billing month"
    )
    public ResponseEntity<ApiDataResponse<List<FeeChargeAttemptResponse>>> getFailedCharges(
            @RequestParam(required = false) String billingMonth
    ) {
        log.info("GET /api/v1/fee-charges/failures - billingMonth: {}", billingMonth);

        List<FeeChargeAttempt> attempts;
        if (billingMonth != null) {
            attempts = attemptService.getFailedAttemptsByMonth(billingMonth);
        } else {
            attempts = attemptService.getFailedAttempts();
        }

        List<FeeChargeAttemptResponse> responses = attempts.stream()
                .map(FeeChargeAttemptResponse::from)
                .collect(Collectors.toList());

        return ResponseEntity.ok(ApiDataResponse.success(responses));
    }

    @GetMapping("/stats")
    @Operation(
            summary = "Get fee charge statistics",
            description = "Get aggregated statistics for fee charges by billing month"
    )
    public ResponseEntity<ApiDataResponse<FeeChargeStatsResponse>> getChargeStats(
            @RequestParam String billingMonth
    ) {
        log.info("GET /api/v1/fee-charges/stats - billingMonth: {}", billingMonth);
        FeeChargeStatsResponse stats = feeJobService.getStatsByBillingMonth(billingMonth);
        return ResponseEntity.ok(ApiDataResponse.success(stats));
    }

    @GetMapping("/customer/{customerId}")
    @Operation(
            summary = "Get fee charge history by customer",
            description = "Retrieve all fee charge attempts for a specific customer"
    )
    public ResponseEntity<ApiDataResponse<List<FeeChargeAttemptResponse>>> getCustomerChargeHistory(
            @PathVariable Long customerId
    ) {
        log.info("GET /api/v1/fee-charges/customer/{} - Getting charge history", customerId);
        List<FeeChargeAttempt> attempts = attemptService.getAttemptsByCustomerId(customerId);

        List<FeeChargeAttemptResponse> responses = attempts.stream()
                .map(FeeChargeAttemptResponse::from)
                .collect(Collectors.toList());

        return ResponseEntity.ok(ApiDataResponse.success(responses));
    }
}