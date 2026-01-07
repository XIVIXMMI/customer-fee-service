package com.hdbank.customer_fee_service.controller;

import com.hdbank.customer_fee_service.dto.response.ApiDataResponse;
import com.hdbank.customer_fee_service.dto.response.FeeTypeResponse;
import com.hdbank.customer_fee_service.service.FeeTypeService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import java.util.List;

@RestController
@RequestMapping("/api/v1/fee-types")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Fee Type Management", description = "APIs for managing fee types (Admin)")
public class FeeTypeController {

    private final FeeTypeService feeTypeService;

    @GetMapping
    @Operation(
            summary = "Get all active fee types",
            description = "Retrieve all active fee type configurations"
    )
    public ResponseEntity<ApiDataResponse<List<FeeTypeResponse>>> getAllFeeTypes() {
        log.info("GET /api/v1/fee-types - Getting all fee types");
        List<FeeTypeResponse> responses = feeTypeService.getAllFeeTypeActive();
        return ResponseEntity.ok(ApiDataResponse.success(responses));
    }

    @GetMapping("/{id}")
    @Operation(
            summary = "Get fee type by ID",
            description = "Retrieve fee type details by ID"
    )
    public ResponseEntity<ApiDataResponse<FeeTypeResponse>> getFeeTypeById(
            @PathVariable Long id
    ) {
        log.info("GET /api/v1/fee-types/{} - Getting fee type by ID", id);
        FeeTypeResponse response = feeTypeService.getFeeTypeById(id);
        return ResponseEntity.ok(ApiDataResponse.success(response));
    }
}