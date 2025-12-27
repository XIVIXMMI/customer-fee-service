package com.hdbank.customer_fee_service.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * Generic API response wrapper.
 * All API responses should be wrapped in this class.
 *
 * @param <T> the type of the response data
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ApiResponse<T> {

    /**
     * "00": SUCCESS
     * "01": VALIDATION_ERROR
     * "02": ENTITY_NOT_FOUND
     * "99": INTERNAL_SERVER_ERROR
     */
    @JsonProperty("response_code")
    private String responseCode;

    @JsonProperty("response_message")
    private String responseMessage;

    @JsonProperty("response_id")
    @Builder.Default
    private String responseId = UUID.randomUUID().toString();

    /**
     * Response time (ISO 8601 format).
     */
    @JsonProperty("response_time")
    @Builder.Default
    private String responseTime = Instant.now().toString();

    /**
     * The actual response data. Retunr null if no data to return.
     */
    @JsonProperty("data")
    private T data;

    /**
     * List of validation errors. Return null if no errors.
     */
    @JsonProperty("errors")
    private List<ErrorDetail> errors;

    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @Data
    public static class ErrorDetail {

        private String field;

        private String message;

        @JsonProperty("rejected_value")
        private Object rejectedValue;
    }

    public static <T> ApiResponse<T> success(T data) {
        return ApiResponse.<T>builder()
                .responseCode("00")
                .responseMessage("SUCCESS")
                .data(data)
                .build();
    }

    public static <T> ApiResponse<T> success(T data, String message) {
        return ApiResponse.<T>builder()
                .responseCode("00")
                .responseMessage(message)
                .data(data)
                .build();
    }

    public static <T> ApiResponse<T> error(String code, String message) {
        return ApiResponse.<T>builder()
                .responseCode(code)
                .responseMessage(message)
                .build();
    }

    public static <T> ApiResponse<T> validationError(List<ErrorDetail> errors) {
        return ApiResponse.<T>builder()
                .responseCode("01")
                .errors(errors)
                .build();
    }

    public static <T> ApiResponse<T> entityNotFound(String message) {
        return ApiResponse.<T>builder()
                .responseCode("02")
                .responseMessage(message)
                .build();
    }

    public static <T> ApiResponse<T> internalServerError(String message) {
        return ApiResponse.<T>builder()
                .responseCode("99")
                .responseMessage(message)
                .build();
    }

}
