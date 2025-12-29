package com.hdbank.customer_fee_service.dto.response;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.hdbank.customer_fee_service.entity.FeeType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FeeTypeResponse {

    private Long id;

    private String code;

    private String name;

    private String description;

    @JsonProperty("calculation_type")
    private String calculationType;

    @JsonProperty("is_active")
    private boolean isActive;

    @JsonProperty("created_at")
    private String createdAt;

    @JsonProperty("updated_at")
    private String updatedAt;

    public static FeeTypeResponse from(FeeType feeType){
        return FeeTypeResponse.builder()
                .id(feeType.getId())
                .code(feeType.getCode())
                .name(feeType.getName())
                .description(feeType.getDescription())
                .calculationType(feeType.getCalculationType())
                .isActive(feeType.getIsActive())
                .createdAt(feeType.getCreatedAt() != null ? feeType.getCreatedAt().toString() : null)
                .updatedAt(feeType.getUpdatedAt() != null ? feeType.getUpdatedAt().toString() : null)
                .build();
    }
}
