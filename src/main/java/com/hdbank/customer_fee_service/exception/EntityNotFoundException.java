package com.hdbank.customer_fee_service.exception;

/**
 * HTTP code 404
 * Response code "02": ENTITY_NOT_FOUND
 */
public class EntityNotFoundException extends RuntimeException {
    public EntityNotFoundException(String message) {
        super(message);
    }
    public EntityNotFoundException(String entityName, Long id) {
        super(String.format("%s with id %d not found", entityName, id));
    }
    public EntityNotFoundException(String entityName, String fieldName, Object value) {
        super(String.format("%s with %s: %s not found", entityName, fieldName, value));
    }
}
