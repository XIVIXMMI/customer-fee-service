package com.hdbank.customer_fee_service.dto.response;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.hdbank.customer_fee_service.entity.CustomerFeeConfig;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FeeConfigResponse {

    private Long id;

    @JsonProperty("customer_id")
    private Long customerId;

    @JsonProperty("fee_type_id")
    private Long feeTypeId;

    @JsonProperty("monthly_fee_amount")
    private BigDecimal monthlyFeeAmount;

    private String currency;

    @JsonProperty("effective_from")
    private LocalDate effectiveFrom;

    @JsonProperty("effective_to")
    private LocalDate effectiveTo;

    @JsonProperty("calculation_params")
    private Map<String, Object> calculationParams;

    @JsonProperty("created_at")
    private String createdAt;

    @JsonProperty("updated_at")
    private String updatedAt;

    @JsonProperty("version")
    private Long version;

    public static FeeConfigResponse from(CustomerFeeConfig config) {
        return FeeConfigResponse.builder()
                .id(config.getId())
                .customerId(config.getCustomerId())
                .feeTypeId(config.getFeeTypeId())
                .monthlyFeeAmount(config.getMonthlyFeeAmount())
                .currency(config.getCurrency())
                .effectiveFrom(config.getEffectiveFrom())
                .effectiveTo(config.getEffectiveTo())
                .calculationParams(config.getCalculationParams())
                .createdAt(config.getCreatedAt() != null ? config.getCreatedAt().toString() : null)
                .updatedAt(config.getUpdatedAt() != null ? config.getUpdatedAt().toString() : null)
                .version(config.getVersion())
                .build();
    }
}