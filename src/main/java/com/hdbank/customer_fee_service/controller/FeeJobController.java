package com.hdbank.customer_fee_service.controller;

import com.hdbank.customer_fee_service.dto.response.ApiDataResponse;
import com.hdbank.customer_fee_service.dto.response.FeeChargeAttemptResponse;
import com.hdbank.customer_fee_service.dto.response.FeeJobResponse;
import com.hdbank.customer_fee_service.entity.CustomerFeeJob;
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
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.PathVariable;
import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/v1/fee-jobs")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Fee Job Management", description = "APIs for managing fee jobs and charge attempts")
public class FeeJobController {

    private final CustomerFeeJobService feeJobService;
    private final FeeChargeAttemptService attemptService;

    @GetMapping
    @Operation(
            summary = "Get fee jobs",
            description = "Get fee jobs with optional filters (customerId, billingMonth, status)"
    )
    public ResponseEntity<ApiDataResponse<List<FeeJobResponse>>> getFeeJobs(
            @RequestParam(required = false) Long customerId,
            @RequestParam(required = false) String billingMonth,
            @RequestParam(required = false) String status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        log.info("GET /api/v1/fee-jobs - customerId: {}, billingMonth: {}, status: {}",
                customerId, billingMonth, status);

        List<CustomerFeeJob> jobs;

        if (customerId != null && billingMonth != null) {
            jobs = feeJobService.getJobsByCustomerIdAndMonth(customerId, billingMonth);
        } else if (customerId != null) {
            jobs = feeJobService.getJobsByCustomerId(customerId);
        } else if (billingMonth != null) {
            jobs = feeJobService.getJobsByBillingMonth(billingMonth);
        } else if (status != null) {
            jobs = feeJobService.getJobsByStatus(status, page, size);
        } else {
            // Return empty list or throw exception
            jobs = List.of();
        }

        List<FeeJobResponse> responses = jobs.stream()
                .map(FeeJobResponse::from)
                .collect(Collectors.toList());

        return ResponseEntity.ok(ApiDataResponse.success(responses));
    }

    @GetMapping("/{jobId}")
    @Operation(
            summary = "Get fee job by ID",
            description = "Retrieve fee job details by job ID"
    )
    public ResponseEntity<ApiDataResponse<FeeJobResponse>> getFeeJobById(
            @PathVariable Long jobId
    ) {
        log.info("GET /api/v1/fee-jobs/{} - Getting fee job by ID", jobId);
        CustomerFeeJob job = feeJobService.getJobById(jobId);
        return ResponseEntity.ok(ApiDataResponse.success(FeeJobResponse.from(job)));
    }

    @GetMapping("/{jobId}/attempts")
    @Operation(
            summary = "Get charge attempts for a job",
            description = "Retrieve all charge attempts for a specific fee job"
    )
    public ResponseEntity<ApiDataResponse<List<FeeChargeAttemptResponse>>> getJobAttempts(
            @PathVariable Long jobId
    ) {
        log.info("GET /api/v1/fee-jobs/{}/attempts - Getting charge attempts", jobId);
        List<FeeChargeAttempt> attempts = attemptService.getAttemptsByJobId(jobId);

        List<FeeChargeAttemptResponse> responses = attempts.stream()
                .map(FeeChargeAttemptResponse::from)
                .collect(Collectors.toList());

        return ResponseEntity.ok(ApiDataResponse.success(responses));
    }
}