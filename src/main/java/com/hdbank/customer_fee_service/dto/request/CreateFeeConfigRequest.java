package com.hdbank.customer_fee_service.dto.request;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Map;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CreateFeeConfigRequest {

    @NotNull(message = "Customer ID is required")
    @JsonProperty("customer_id")
    private Long customerId;

    @NotNull(message = "Fee type ID is required")
    @JsonProperty("fee_type_id")
    private Long feeTypeId;

    @DecimalMin(value = "0.0", inclusive = false, message = "Monthly fee amount must be greater than 0")
    @JsonProperty("monthly_fee_amount")
    private BigDecimal monthlyFeeAmount;

    @NotBlank(message = "Currency is required")
    @Pattern(regexp = "^[A-Z]{3}$", message = "Currency must be a 3-letter code")
    private String currency;

    @NotNull(message = "Effective from date is required")
    @JsonProperty("effective_from")
    private LocalDate effectiveFrom;

    @JsonProperty("effective_to")
    private LocalDate effectiveTo;

    @JsonProperty("calculation_params")
    private Map<String, Object> calculationParams;
}