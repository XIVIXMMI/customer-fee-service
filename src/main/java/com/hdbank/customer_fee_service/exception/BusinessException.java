package com.hdbank.customer_fee_service.exception;

import lombok.Getter;

/**
 * HTTP code 400
 * Response code: customs
 */
@Getter
public class BusinessException extends RuntimeException {

    private final String errorCode;

    public BusinessException(String errorCode,String message) {
        super(message);
        this.errorCode = errorCode;
    }

    public BusinessException(String errorCode,String message, Throwable cause) {
        super(message,cause);
        this.errorCode = errorCode;
    }
}
