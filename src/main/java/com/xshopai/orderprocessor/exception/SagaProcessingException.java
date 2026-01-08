package com.xshopai.orderprocessor.exception;

/**
 * Exception thrown when saga processing fails
 */
public class SagaProcessingException extends RuntimeException {
    
    public SagaProcessingException(String message) {
        super(message);
    }
    
    public SagaProcessingException(String message, Throwable cause) {
        super(message, cause);
    }
}
