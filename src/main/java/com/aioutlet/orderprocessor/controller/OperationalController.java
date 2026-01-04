package com.xshopai.orderprocessor.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.lang.management.ManagementFactory;
import java.lang.management.MemoryMXBean;
import java.lang.management.RuntimeMXBean;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

/**
 * Operational/Infrastructure endpoints
 * These endpoints are used by monitoring systems, load balancers, and DevOps tools
 */
@RestController
@RequiredArgsConstructor
public class OperationalController {

    /**
     * Home endpoint - welcome message
     */
    @GetMapping("/")
    public ResponseEntity<Map<String, Object>> home() {
        Map<String, Object> response = new HashMap<>();
        response.put("message", "Welcome to the Order Processor Service");
        response.put("service", "order-processor-service");
        response.put("description", "Choreography-based saga pattern order processor");
        
        return ResponseEntity.ok(response);
    }

    /**
     * Version endpoint
     */
    @GetMapping("/version")
    public ResponseEntity<Map<String, Object>> version() {
        Map<String, Object> response = new HashMap<>();
        response.put("version", System.getProperty("api.version", "1.0.0"));
        response.put("service", "order-processor-service");
        response.put("environment", System.getProperty("spring.profiles.active", "development"));
        
        return ResponseEntity.ok(response);
    }

    /**
     * Basic health check endpoint
     */
    @GetMapping("/health")
    public ResponseEntity<Map<String, Object>> health() {
        Map<String, Object> health = new HashMap<>();
        health.put("status", "healthy");
        health.put("service", "order-processor-service");
        health.put("timestamp", Instant.now().toString());
        health.put("version", System.getProperty("api.version", "1.0.0"));
        
        return ResponseEntity.ok(health);
    }

    /**
     * Readiness probe - check if service is ready to serve traffic
     */
    @GetMapping("/readiness")
    public ResponseEntity<Map<String, Object>> readiness() {
        try {
            // Add more sophisticated checks here (DB connectivity, message broker, etc.)
            // Example: Check database connectivity, RabbitMQ/Kafka connectivity, etc.
            // checkDatabaseConnectivity();
            // checkMessageBrokerConnectivity();
            
            Map<String, Object> readiness = new HashMap<>();
            readiness.put("status", "ready");
            readiness.put("service", "order-processor-service");
            readiness.put("timestamp", Instant.now().toString());
            
            Map<String, String> checks = new HashMap<>();
            checks.put("database", "connected");
            checks.put("messageBroker", "connected");
            // Add other dependency checks
            readiness.put("checks", checks);
            
            return ResponseEntity.ok(readiness);
        } catch (Exception e) {
            Map<String, Object> notReady = new HashMap<>();
            notReady.put("status", "not ready");
            notReady.put("service", "order-processor-service");
            notReady.put("timestamp", Instant.now().toString());
            notReady.put("error", "Service dependencies not available");
            
            return ResponseEntity.status(503).body(notReady);
        }
    }

    /**
     * Liveness probe - check if the app is running
     */
    @GetMapping("/liveness")
    public ResponseEntity<Map<String, Object>> liveness() {
        RuntimeMXBean runtimeBean = ManagementFactory.getRuntimeMXBean();
        double uptimeSeconds = runtimeBean.getUptime() / 1000.0;
        
        Map<String, Object> liveness = new HashMap<>();
        liveness.put("status", "alive");
        liveness.put("service", "order-processor-service");
        liveness.put("timestamp", Instant.now().toString());
        liveness.put("uptime", uptimeSeconds);
        
        return ResponseEntity.ok(liveness);
    }

    /**
     * Basic metrics endpoint
     */
    @GetMapping("/metrics")
    public ResponseEntity<Map<String, Object>> metrics() {
        RuntimeMXBean runtimeBean = ManagementFactory.getRuntimeMXBean();
        MemoryMXBean memoryBean = ManagementFactory.getMemoryMXBean();
        
        double uptimeSeconds = runtimeBean.getUptime() / 1000.0;
        
        Map<String, Object> metrics = new HashMap<>();
        metrics.put("service", "order-processor-service");
        metrics.put("timestamp", Instant.now().toString());
        
        Map<String, Object> metricsData = new HashMap<>();
        metricsData.put("uptime", uptimeSeconds);
        
        Map<String, Object> memory = new HashMap<>();
        memory.put("heapUsed", memoryBean.getHeapMemoryUsage().getUsed());
        memory.put("heapMax", memoryBean.getHeapMemoryUsage().getMax());
        memory.put("nonHeapUsed", memoryBean.getNonHeapMemoryUsage().getUsed());
        memory.put("nonHeapMax", memoryBean.getNonHeapMemoryUsage().getMax());
        metricsData.put("memory", memory);
        
        metricsData.put("availableProcessors", Runtime.getRuntime().availableProcessors());
        metricsData.put("javaVersion", System.getProperty("java.version"));
        metricsData.put("javaVendor", System.getProperty("java.vendor"));
        
        metrics.put("metrics", metricsData);
        
        return ResponseEntity.ok(metrics);
    }
}
