package com.hdbank.customer_fee_service.dto.request;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Pattern;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Map;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class UpdateFeeConfigRequest {

    @DecimalMin(value = "0.0", inclusive = false, message = "Monthly fee amount must be greater than 0")
    @JsonProperty("monthly_fee_amount")
    private BigDecimal monthlyFeeAmount;

    @Pattern(regexp = "^[A-Z]{3}$", message = "Currency must be a 3-letter code")
    private String currency;

    @JsonProperty("effective_from")
    private LocalDate effectiveFrom;

    @JsonProperty("effective_to")
    private LocalDate effectiveTo;

    @JsonProperty("calculation_params")
    private Map<String, Object> calculationParams;
}