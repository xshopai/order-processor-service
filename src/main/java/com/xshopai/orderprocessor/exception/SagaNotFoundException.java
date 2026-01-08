package com.xshopai.orderprocessor.exception;

/**
 * Exception thrown when a saga is not found
 */
public class SagaNotFoundException extends RuntimeException {
    
    public SagaNotFoundException(String message) {
        super(message);
    }
    
    public SagaNotFoundException(String message, Throwable cause) {
        super(message, cause);
    }
}
