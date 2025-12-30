package com.hdbank.customer_fee_service.exception;

import com.hdbank.customer_fee_service.dto.response.ApiDataResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;

import java.util.ArrayList;
import java.util.List;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(EntityNotFoundException.class)
    public ResponseEntity<ApiDataResponse<Void>> handleEntityNotFoundException(
            EntityNotFoundException ex,
            WebRequest request) {
        log.error("Entity not found exception: {}", ex.getMessage());
        log.debug("Request details: {}", request.getDescription(false));
        ApiDataResponse<Void> response = ApiDataResponse.entityNotFound(ex.getMessage());
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(response);
    }

    @ExceptionHandler(ValidationException.class)
    public ResponseEntity<ApiDataResponse<Void>> handleValidationException(
            ValidationException ex,
            WebRequest request) {
        log.error("Validation exception: {}", ex.getMessage());
        log.debug("Request details: {}", request.getDescription(false));
        ApiDataResponse<Void> response = ApiDataResponse.error("01", ex.getMessage());
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
    }

    /**
     * handle Bean Validation errors
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiDataResponse<Void>> handleMethodArgumentNotValidException(
            MethodArgumentNotValidException ex,
            WebRequest request) {
        log.error("Method argument not valid exception: {}", ex.getMessage());
        log.debug("Request details: {}", request.getDescription(false));

        List<ApiDataResponse.ErrorDetail> errors = new ArrayList<>();
        for(FieldError fieldError : ex.getBindingResult().getFieldErrors()) {
            ApiDataResponse.ErrorDetail errorDetail = ApiDataResponse.ErrorDetail.builder()
                    .field(fieldError.getField())
                    .message(fieldError.getDefaultMessage())
                    .rejectedValue(fieldError.getRejectedValue())
                    .build();
            errors.add(errorDetail);
        }
        ApiDataResponse<Void> response = ApiDataResponse.validationError(errors);
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
    }

    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<ApiDataResponse<Void>> handleBusinessException(
            BusinessException ex,
            WebRequest request) {
        log.error("Business exception: {}", ex.getMessage());
        log.debug("Request details: {}", request.getDescription(false));
        ApiDataResponse<Void> response = ApiDataResponse.error(ex.getErrorCode(), ex.getMessage());
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiDataResponse<Void>> handleGlobalException(
            Exception ex,
            WebRequest request) {
        log.error("Unhandled exception: {}", ex.getMessage(), ex);
        log.debug("Request details: {}", request.getDescription(false));
        ApiDataResponse<Void> response =
                ApiDataResponse.internalServerError("An unexpected error occurred. Please contact support.");
        response.setResponseMessage(
                String.format(
                        "An unexpected error occurred. Please contact support with response_id: %s",
                        response.getResponseId()
                )
        );
        log.error("Error response_id: {}", response.getResponseId());
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
    }
}
