package com.hdbank.customer_fee_service.controller;

import com.hdbank.customer_fee_service.dto.request.CreateFeeConfigRequest;
import com.hdbank.customer_fee_service.dto.request.UpdateFeeConfigRequest;
import com.hdbank.customer_fee_service.dto.response.ApiResponse;
import com.hdbank.customer_fee_service.dto.response.FeeConfigResponse;
import com.hdbank.customer_fee_service.service.FeeConfigService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/fee-configs")
@RequiredArgsConstructor
@Slf4j
public class CustomerFeeConfigController {

    private final FeeConfigService feeConfigService;

    @PostMapping
    public ResponseEntity<ApiResponse<FeeConfigResponse>> createFeeConfig(
            @Valid @RequestBody CreateFeeConfigRequest request
            ) {
        log.info("POST /api/v1/fee-configs - Creating Fee config");

        FeeConfigResponse response = feeConfigService.createFeeConfig(request);
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(ApiResponse.success(response, "Fee config created successfully"));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<FeeConfigResponse>> getFeeConfigById(
            @PathVariable Long id
    ) {
        log.info("GET /api/v1/fee-configs/{} - Getting Fee config by id", id);

        FeeConfigResponse response = feeConfigService.getFeeConfigById(id);
        return ResponseEntity
                .ok(ApiResponse.success(response, "Fee config retrieved successfully"));
    }

    @GetMapping("/customer/{customerId}/active")
    public ResponseEntity<ApiResponse<FeeConfigResponse>> getActiveFeeConfigByCustomerId(
            @PathVariable Long customerId
    ) {
        log.info("GET /api/v1/fee-configs/customer/{}/active - Getting active Fee config for customer", customerId);
        FeeConfigResponse response = feeConfigService.getActiveFeeConfigByCustomerId(customerId);
        return ResponseEntity
                .ok(ApiResponse.success(response, "Active Fee config retrieved successfully"));
    }

    @GetMapping("/customer/{customerId}")
    public ResponseEntity<ApiResponse<List<FeeConfigResponse>>> getAllFeeConfigsByCustomerId(
            @PathVariable Long customerId
    ) {
        log.info("GET /api/v1/fee-configs/customer/{} - Getting all Fee configs for customer, include expired", customerId);
        List<FeeConfigResponse> response = feeConfigService.getAllFeeConfigsByCustomerIdIncludeExpired(customerId);
        return ResponseEntity
                .ok(ApiResponse.success(response));
    }

    @PutMapping("/{id}" )
    public ResponseEntity<ApiResponse<FeeConfigResponse>> updateFeeConfig(
            @PathVariable Long id,
            @Valid @RequestBody UpdateFeeConfigRequest request
    ) {
        log.info("PUT /api/v1/fee-configs/{} - Updating Fee config", id);
        FeeConfigResponse response = feeConfigService.updateFeeConfig(id, request);
        return ResponseEntity
                .ok(ApiResponse.success(response, "Fee config updated successfully"));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteFeeConfig(
            @PathVariable Long id
    ) {
        log.info("DELETE /api/v1/fee-configs/{} - Deleting Fee config", id);
        feeConfigService.deleteFeeConfig(id);
        return ResponseEntity
                .ok(ApiResponse.success(null, "Fee config deleted successfully"));
    }
}
