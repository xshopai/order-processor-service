package com.xshopai.orderprocessor.exception;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

/**
 * Global exception handler for the Order Processor Service
 * Provides environment-specific error handling with proper production security
 */
@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler {

    @Value("${app.environment:development}")
    private String environment;
    
    private boolean isDevelopment() {
        return "development".equals(environment) || "local".equals(environment);
    }

    @ExceptionHandler(SagaNotFoundException.class)
    public ResponseEntity<Map<String, Object>> handleSagaNotFoundException(
            SagaNotFoundException ex, WebRequest request) {
        
        String correlationId = request.getHeader("x-correlation-id");
        
        if (isDevelopment()) {
            log.warn("Saga not found: {} | CorrelationId: {}", ex.getMessage(), correlationId);
        } else {
            log.warn("Saga not found | CorrelationId: {}", correlationId);
        }
        
        Map<String, Object> body = new HashMap<>();
        body.put("timestamp", LocalDateTime.now());
        body.put("status", HttpStatus.NOT_FOUND.value());
        body.put("error", "Saga Not Found");
        body.put("message", isDevelopment() ? ex.getMessage() : "The requested saga was not found");
        body.put("path", request.getDescription(false));
        if (correlationId != null) {
            body.put("correlationId", correlationId);
        }
        
        return new ResponseEntity<>(body, HttpStatus.NOT_FOUND);
    }

    @ExceptionHandler(SagaProcessingException.class)
    public ResponseEntity<Map<String, Object>> handleSagaProcessingException(
            SagaProcessingException ex, WebRequest request) {
        
        String correlationId = request.getHeader("x-correlation-id");
        
        if (isDevelopment()) {
            log.error("Saga processing error: {} | CorrelationId: {}", ex.getMessage(), correlationId, ex);
        } else {
            log.error("Saga processing error | CorrelationId: {} | Environment: {}", correlationId, environment);
        }
        
        Map<String, Object> body = new HashMap<>();
        body.put("timestamp", LocalDateTime.now());
        body.put("status", HttpStatus.INTERNAL_SERVER_ERROR.value());
        body.put("error", "Saga Processing Error");
        body.put("message", isDevelopment() ? ex.getMessage() : "An error occurred during saga processing");
        body.put("path", request.getDescription(false));
        if (correlationId != null) {
            body.put("correlationId", correlationId);
        }
        
        return new ResponseEntity<>(body, HttpStatus.INTERNAL_SERVER_ERROR);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, Object>> handleGenericException(
            Exception ex, WebRequest request) {
        
        String correlationId = request.getHeader("x-correlation-id");
        
        if (isDevelopment()) {
            log.error("Unexpected error: {} | CorrelationId: {}", ex.getMessage(), correlationId, ex);
        } else {
            log.error("Unexpected error | CorrelationId: {} | Environment: {}", correlationId, environment);
        }
        
        Map<String, Object> body = new HashMap<>();
        body.put("timestamp", LocalDateTime.now());
        body.put("status", HttpStatus.INTERNAL_SERVER_ERROR.value());
        body.put("error", "Internal Server Error");
        body.put("message", isDevelopment() ? ex.getMessage() : "An unexpected error occurred");
        body.put("path", request.getDescription(false));
        if (correlationId != null) {
            body.put("correlationId", correlationId);
        }
        
        return new ResponseEntity<>(body, HttpStatus.INTERNAL_SERVER_ERROR);
    }
}
