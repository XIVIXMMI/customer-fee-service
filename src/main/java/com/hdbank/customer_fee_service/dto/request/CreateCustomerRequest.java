package com.hdbank.customer_fee_service.dto.request;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Customer creation request")
public class CreateCustomerRequest {

    @NotBlank(message = "Full name is required")
    @Size(max = 100, message = "Full name must not exceed 100 characters")
    @JsonProperty("full_name")
    @Schema(description = "Customer full name", example = "Nguyen Van A")
    private String fullName;

    @NotBlank(message = "Email is required")
    @Email(message = "Email should be valid")
    @Size(max = 100, message = "Email must not exceed 100 characters")
    @Schema(description = "Customer email", example = "nguyenvana@example.com")
    private String email;

    @NotBlank( message = "Phone number is required")
    @Pattern(regexp = "^[0-9]{10,15}$", message = "Phone number must be 10-15 digits")
    @JsonProperty("phone_number")
    @Schema(description = "Customer phone number", example = "0907774311")
    private String phoneNumber;

    @NotBlank(message = "Status is required")
    @Pattern(regexp = "^(ACTIVE|INACTIVE)$", message = "Status must be ACTIVE or INACTIVE")
    @Schema(description = "Customer status", example = "ACTIVE")
    private String status; // ACTIVE, INACTIVE
}
