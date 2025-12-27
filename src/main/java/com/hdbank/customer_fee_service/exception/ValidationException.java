package com.hdbank.customer_fee_service.exception;

/**
 * HTTP code 400
 * Response code "01": VALIDATION_ERROR
 */
public class ValidationException extends RuntimeException {
    public ValidationException(String message) {
        super(message);
    }
    public ValidationException(String message, Throwable cause) {
        super(message, cause);
    }
}
