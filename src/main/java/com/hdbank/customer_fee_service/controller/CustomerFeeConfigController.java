package com.hdbank.customer_fee_service.controller;

import com.hdbank.customer_fee_service.dto.request.CreateFeeConfigRequest;
import com.hdbank.customer_fee_service.dto.request.FeePreviewRequest;
import com.hdbank.customer_fee_service.dto.request.UpdateFeeConfigRequest;
import com.hdbank.customer_fee_service.dto.response.ApiDataResponse;
import com.hdbank.customer_fee_service.dto.response.FeeConfigResponse;
import com.hdbank.customer_fee_service.dto.response.FeePreviewResponse;
import com.hdbank.customer_fee_service.service.FeeConfigService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
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
@Tag(name = "Fee Configuration", description = "APIs for managing customer fee configurations")
public class CustomerFeeConfigController {

    private final FeeConfigService feeConfigService;

    @PostMapping
    @Operation(
            summary = "Create fee configuration",
            description = "Create a new fee configuration for a customer"
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "Fee config created successfully",
                    content = @Content(
                            examples = @ExampleObject(
                                    name = "Fixed Monthly Fee",
                                    value = """
                                              {
                                                "id": 1,
                                                "customer_id": 1,
                                                "fee_type_id": 1,
                                                "monthly_fee_amount": 50000.00,
                                                "currency": "VND",
                                                "effective_from": "2025-01-01",
                                                "effective_to": null,
                                                "calculation_params": null
                                              }
                                              """
                            )
                    )
            )
    })
    public ResponseEntity<ApiDataResponse<FeeConfigResponse>> createFeeConfig(
            @Valid
            @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    description = "Fee configuration request",
                    required = true,
                    content = @Content(
                            examples = {
                                    @ExampleObject(
                                            name = "Fixed Monthly Fee",
                                            value = """
                                                      {
                                                        "customer_id": 1,
                                                        "fee_type_id": 1,
                                                        "monthly_fee_amount": 50000.00,
                                                        "currency": "VND",
                                                        "effective_from": "2025-01-01"
                                                      }
                                                      """
                                    ),
                                    @ExampleObject(
                                            name = "Tiered Balance Fee",
                                            value = """
                                                      {
                                                        "customer_id": 2,
                                                        "fee_type_id": 2,
                                                        "monthly_fee_amount": 0,
                                                        "currency": "VND",
                                                        "effective_from": "2025-01-01",
                                                        "calculation_params": {
                                                          "tiers": [
                                                            {"min_balance": 0, "max_balance": 10000000, "fee": 50000},
                                                            {"min_balance": 10000000, "max_balance": 50000000, "fee": 30000},
                                                            {"min_balance": 50000000, "max_balance": null, "fee": 0}
                                                          ]
                                                        }
                                                      }
                                                      """
                                    ),
                                    @ExampleObject(
                                            name = "Percentage of Balance",
                                            value = """
                                                      {
                                                        "customer_id": 3,
                                                        "fee_type_id": 3,
                                                        "monthly_fee_amount": 0,
                                                        "currency": "VND",
                                                        "effective_from": "2025-01-01",
                                                        "calculation_params": {
                                                          "percentage": 0.001,
                                                          "min_fee": 10000,
                                                          "max_fee": 100000
                                                        }
                                                      }
                                                      """
                                    )
                            }
                    )
            )
            @RequestBody CreateFeeConfigRequest request
            ) {
        log.info("POST /api/v1/fee-configs - Creating Fee config");

        FeeConfigResponse response = feeConfigService.createFeeConfig(request);
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(ApiDataResponse.success(response, "Fee config created successfully"));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get fee config by ID")
    public ResponseEntity<ApiDataResponse<FeeConfigResponse>> getFeeConfigById(
            @PathVariable Long id
    ) {
        log.info("GET /api/v1/fee-configs/{} - Getting Fee config by id", id);

        FeeConfigResponse response = feeConfigService.getFeeConfigById(id);
        return ResponseEntity
                .ok(ApiDataResponse.success(response, "Fee config retrieved successfully"));
    }

    @GetMapping("/customer/{customerId}/active")
    @Operation(
            summary = "Get active fee config for customer",
            description = "Get the currently active fee configuration for a customer"
    )
    public ResponseEntity<ApiDataResponse<FeeConfigResponse>> getActiveFeeConfigByCustomerId(
            @PathVariable Long customerId
    ) {
        log.info("GET /api/v1/fee-configs/customer/{}/active - Getting active Fee config for customer", customerId);
        FeeConfigResponse response = feeConfigService.getActiveFeeConfigByCustomerId(customerId);
        return ResponseEntity
                .ok(ApiDataResponse.success(response, "Active Fee config retrieved successfully"));
    }

    @GetMapping("/customer/{customerId}/all")
    @Operation(
            summary = "Get all active fee config for customer",
            description = "Get all the currently active fee configuration for a customer, include expired"
    )
    public ResponseEntity<ApiDataResponse<List<FeeConfigResponse>>> getAllFeeConfigsByCustomerId(
            @PathVariable Long customerId
    ) {
        log.info("GET /api/v1/fee-configs/customer/{} - Getting all Fee configs for customer, include expired", customerId);
        List<FeeConfigResponse> response = feeConfigService.getAllFeeConfigsByCustomerIdIncludeExpired(customerId);
        return ResponseEntity
                .ok(ApiDataResponse.success(response));
    }

    @PutMapping("/{id}" )
    public ResponseEntity<ApiDataResponse<FeeConfigResponse>> updateFeeConfig(
            @PathVariable Long id,
            @Valid @RequestBody UpdateFeeConfigRequest request
    ) {
        log.info("PUT /api/v1/fee-configs/{} - Updating Fee config", id);
        FeeConfigResponse response = feeConfigService.updateFeeConfig(id, request);
        return ResponseEntity
                .ok(ApiDataResponse.success(response, "Fee config updated successfully"));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiDataResponse<Void>> deleteFeeConfig(
            @PathVariable Long id
    ) {
        log.info("DELETE /api/v1/fee-configs/{} - Deleting Fee config", id);
        feeConfigService.deleteFeeConfig(id);
        return ResponseEntity
                .ok(ApiDataResponse.success(null, "Fee config deleted successfully"));
    }

    @PostMapping("/preview")
    public ResponseEntity<ApiDataResponse<FeePreviewResponse>> previewFee(
            @Valid @RequestBody FeePreviewRequest request) {
        log.info("POST /api/v1/fee-configs/preview - Previewing fee");

        FeePreviewResponse response = feeConfigService.feeReview(request);

        return ResponseEntity.ok(ApiDataResponse.success(response));
    }
}
