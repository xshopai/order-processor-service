package com.xshopai.orderprocessor.controller;

import com.xshopai.orderprocessor.util.CorrelationIdUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import jakarta.servlet.http.HttpServletRequest;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

/**
 * Health check and operational endpoints for the Order Processor Service
 * These endpoints help verify correlation ID functionality
 */
@RestController
@RequestMapping("/api/v1")
public class HealthController {

    private static final Logger logger = LoggerFactory.getLogger(HealthController.class);

    /**
     * Health check endpoint
     * 
     * @param request HTTP request
     * @return health status with correlation ID
     */
    @GetMapping("/health")
    public ResponseEntity<Map<String, Object>> health(HttpServletRequest request) {
        String correlationId = CorrelationIdUtil.getCorrelationId(request);
        
        logger.info("Health check requested with correlation ID: {}", correlationId);
        
        Map<String, Object> response = new HashMap<>();
        response.put("status", "UP");
        response.put("service", "order-processor-service");
        response.put("timestamp", LocalDateTime.now().toString());
        response.put("correlationId", correlationId);
        
        return ResponseEntity.ok(response);
    }

    /**
     * Readiness check endpoint
     * 
     * @param request HTTP request
     * @return readiness status with correlation ID
     */
    @GetMapping("/ready")
    public ResponseEntity<Map<String, Object>> ready(HttpServletRequest request) {
        String correlationId = CorrelationIdUtil.getCorrelationId(request);
        
        logger.info("Readiness check requested with correlation ID: {}", correlationId);
        
        Map<String, Object> response = new HashMap<>();
        response.put("status", "READY");
        response.put("service", "order-processor-service");
        response.put("timestamp", LocalDateTime.now().toString());
        response.put("correlationId", correlationId);
        
        return ResponseEntity.ok(response);
    }

    /**
     * Test endpoint to verify correlation ID propagation
     * 
     * @param request HTTP request
     * @return correlation ID details
     */
    @GetMapping("/correlation-test")
    public ResponseEntity<Map<String, Object>> correlationTest(HttpServletRequest request) {
        String correlationId = CorrelationIdUtil.getCorrelationId(request);
        String mdcCorrelationId = CorrelationIdUtil.getCurrentCorrelationId();
        String contextCorrelationId = CorrelationIdUtil.getCorrelationIdFromContext();
        
        logger.info("Correlation ID test - Request: {}, MDC: {}, Context: {}", 
                    correlationId, mdcCorrelationId, contextCorrelationId);
        
        Map<String, Object> response = new HashMap<>();
        response.put("requestCorrelationId", correlationId);
        response.put("mdcCorrelationId", mdcCorrelationId);
        response.put("contextCorrelationId", contextCorrelationId);
        response.put("service", "order-processor-service");
        response.put("timestamp", LocalDateTime.now().toString());
        
        return ResponseEntity.ok(response);
    }

    /**
     * Echo endpoint that returns the correlation ID
     * 
     * @param message message to echo
     * @param request HTTP request
     * @return echoed message with correlation ID
     */
    @PostMapping("/echo")
    public ResponseEntity<Map<String, Object>> echo(
            @RequestBody(required = false) String message,
            HttpServletRequest request) {
        
        String correlationId = CorrelationIdUtil.getCorrelationId(request);
        
        logger.info("Echo request with message: '{}' and correlation ID: {}", message, correlationId);
        
        Map<String, Object> response = new HashMap<>();
        response.put("message", message != null ? message : "Hello from Order Processor Service!");
        response.put("correlationId", correlationId);
        response.put("service", "order-processor-service");
        response.put("timestamp", LocalDateTime.now().toString());
        
        return ResponseEntity.ok(response);
    }
}
