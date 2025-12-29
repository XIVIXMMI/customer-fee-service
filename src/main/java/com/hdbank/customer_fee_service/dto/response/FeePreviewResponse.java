package com.hdbank.customer_fee_service.dto.response;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FeePreviewResponse {

    @JsonProperty("customer_id")
    private Long customerId;

    @JsonProperty("fee_type_code")
    private String feeTypeCode;

    @JsonProperty("fee_type_name")
    private String feeTypeName;

    @JsonProperty("calculation_type")
    private String calculationType;

    @JsonProperty("monthly_fee_amount")
    private BigDecimal monthlyFeeAmount;

    @JsonProperty("calculated_fee")
    private BigDecimal calculatedFee;

    @JsonProperty("currency")
    private String currency;

    @JsonProperty("calculation_params")
    private Map<String, Object> calculationParams;
}