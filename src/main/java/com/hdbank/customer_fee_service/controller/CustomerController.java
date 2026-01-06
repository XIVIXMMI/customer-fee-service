package com.hdbank.customer_fee_service.controller;

import com.hdbank.customer_fee_service.dto.request.CreateCustomerRequest;
import com.hdbank.customer_fee_service.dto.request.UpdateCustomerRequest;
import com.hdbank.customer_fee_service.dto.response.ApiDataResponse;
import com.hdbank.customer_fee_service.dto.response.CustomerResponse;
import com.hdbank.customer_fee_service.service.CustomerService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;

@RestController
@RequestMapping("api/v1/customers")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Customer Management", description = "APIs for managing customers")
public class CustomerController {

    private final CustomerService customerService;

    @PostMapping
    @Operation(
            summary = "Create a new customer",
            description = "Create a new customer with basic information"
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "Customer created successfully",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = CustomerResponse.class),
                            examples = @ExampleObject(
                                    name = "Success",
                                    value = """
                                              {
                                                "id": 1,
                                                "customer_name": "Nguyen Van A",
                                                "account_number": "0123456789",
                                                "account_balance": 1000000.00,
                                                "status": "ACTIVE",
                                                "created_at": "2025-01-15T10:30:00"
                                              }
                                              """
                            )
                    )
            ),
            @ApiResponse(responseCode = "400", description = "Invalid input")
    })
    public ResponseEntity<ApiDataResponse<CustomerResponse>> createCustomer(
            @Valid @RequestBody CreateCustomerRequest request
    ) {
        log.info("POST /api/v1/customers - Creating customer");
        CustomerResponse response = customerService.createCustomer(request);
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(ApiDataResponse.success(response, "Customer created successfully"));
    }

    @GetMapping("/{id}")
    @Operation(
            summary = "Get customer by ID",
            description = "Retrieve customer details by customer ID"
    )
    public ResponseEntity<ApiDataResponse<CustomerResponse>> getCustomer(@PathVariable Long id) {
        log.info("GET /api/v1/customers/{} - Getting customer", id);
        CustomerResponse response = customerService.getCustomerById(id);
        return ResponseEntity.ok(ApiDataResponse.success(response));
    }

    @GetMapping
    @Operation(
            summary = "Get all customers",
            description = "Retrieve all active customers"
    )
    public ResponseEntity<ApiDataResponse<Page<CustomerResponse>>> getAllCustomers(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        log.info("GET /api/v1/customers - Getting customers page: {}, size: {}", page, size);

        Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
        Page<CustomerResponse> responses = customerService.getAllCustomers(pageable);

        return ResponseEntity.ok(ApiDataResponse.success(responses));
    }

    @PutMapping("/{id}")
    @Operation(
            summary = "Update customer",
            description = "Update customer information"
    )
    public ResponseEntity<ApiDataResponse<CustomerResponse>> updateCustomer(
            @PathVariable Long id,
            @Valid @RequestBody UpdateCustomerRequest request) {
        log.info("PUT /api/v1/customers/{} - Updating customer", id);
        CustomerResponse response = customerService.updateCustomer(id, request);
        return ResponseEntity.ok(ApiDataResponse.success(response, "Customer updated successfully"));
    }

    @DeleteMapping("/{id}")
    @Operation(
            summary = "Delete customer",
            description = "Soft delete customer (mark as deleted)"
    )
    @ApiResponse(responseCode = "204", description = "Customer deleted successfully")
    public ResponseEntity<ApiDataResponse<Void>> deleteCustomer(@PathVariable Long id) {
        log.info("DELETE /api/v1/customers/{} - Deleting customer", id);
        customerService.deleteCustomer(id);
        return ResponseEntity.ok(ApiDataResponse.success(null, "Customer deleted successfully"));
    }

}
