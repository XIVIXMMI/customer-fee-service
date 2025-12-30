package com.hdbank.customer_fee_service.dto.request;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
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
@Schema(description = "Fee Configuration request")
public class CreateFeeConfigRequest {

    @NotNull(message = "Customer ID is required")
    @JsonProperty("customer_id")
    @Schema(description = "Customer ID", example = "1")
    private Long customerId;

    @NotNull(message = "Fee type ID is required")
    @JsonProperty("fee_type_id")
    @Schema(description = "Fee Type Id", example = "1")
    private Long feeTypeId;

    @DecimalMin(value = "0.0", inclusive = false, message = "Monthly fee amount must be greater than 0")
    @JsonProperty("monthly_fee_amount")
    @Schema(description = "Monthly Fee Amout", example = "30000")
    private BigDecimal monthlyFeeAmount;

    @NotBlank(message = "Currency is required")
    @Pattern(regexp = "^[A-Z]{3}$", message = "Currency must be a 3-letter code")
    @Schema(description = "Currency of fee", example = "VND")
    private String currency;

    @NotNull(message = "Effective from date is required")
    @JsonProperty("effective_from")
    @Schema(description = "Day effective begin", example = "2025-01-01")
    private LocalDate effectiveFrom;

    @JsonProperty("effective_to")
    @Schema(description = "Day effective end", example = "2035-01-01")
    private LocalDate effectiveTo;

    @JsonProperty("calculation_params")
    @Schema(description = "Calculation params of strategies", example = "{\n" +
            "  \"tiers\": [\n" +
            "    {\n" +
            "      \"to\": 50000000,\n" +
            "      \"fee\": 10000,\n" +
            "      \"from\": 0\n" +
            "    },\n" +
            "    {\n" +
            "      \"to\": 100000000,\n" +
            "      \"fee\": 20000,\n" +
            "      \"from\": 50000001\n" +
            "    },\n" +
            "    {\n" +
            "      \"to\": null,\n" +
            "      \"fee\": 30000,\n" +
            "      \"from\": 100000001\n" +
            "    }\n" +
            "  ],\n" +
            "  \"balance\": 80000000\n" +
            "}")
    private Map<String, Object> calculationParams;
}