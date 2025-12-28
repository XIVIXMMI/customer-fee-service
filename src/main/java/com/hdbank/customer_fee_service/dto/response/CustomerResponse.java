package com.hdbank.customer_fee_service.dto.response;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.hdbank.customer_fee_service.entity.Customer;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class CustomerResponse {

    private Long id;

    @JsonProperty("full_name")
    private String fullName;

    private String email;

    @JsonProperty("phone_number")
    private String phoneNumber;

    private String status; // ACTIVE, INACTIVE

    @JsonProperty("created_at")
    private String createdAt;

    @JsonProperty("updated_at")
    private String updatedAt;

    @JsonProperty("version")
    private Long version;

    public static CustomerResponse from(Customer customer){
        return CustomerResponse.builder()
                .id(customer.getId())
                .fullName(customer.getFullName())
                .email(customer.getEmail())
                .phoneNumber(customer.getPhoneNumber())
                .status(customer.getStatus())
                .createdAt(customer.getCreatedAt() != null ? customer.getCreatedAt().toString() : null)
                .updatedAt(customer.getUpdatedAt() != null ? customer.getUpdatedAt().toString() : null)
                .version(customer.getVersion())
                .build();
    }
}
