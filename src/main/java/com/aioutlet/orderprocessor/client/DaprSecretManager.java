package com.xshopai.orderprocessor.client;

import io.dapr.client.DaprClient;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import jakarta.annotation.PostConstruct;
import java.util.Map;

/**
 * Dapr Secret Manager
 * Handles retrieving secrets from Dapr secret store
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class DaprSecretManager {

    private static final String SECRET_STORE_NAME = "secret-store";

    private final DaprClient daprClient;

    @PostConstruct
    public void init() {
        log.info("Dapr Secret Manager initialized with store: {}", SECRET_STORE_NAME);
    }

    /**
     * Get a specific secret by key
     * Supports nested keys with colon separator (e.g., "jwt:secret")
     */
    public String getSecret(String key) {
        try {
            log.debug("Retrieving secret: {}", key);
            
            Map<String, String> secret = daprClient.getSecret(SECRET_STORE_NAME, key).block();
            
            if (secret == null || secret.isEmpty()) {
                log.warn("Secret not found: {}", key);
                return null;
            }
            
            // Return the first value (Dapr handles nested separator automatically)
            String value = secret.values().stream().findFirst().orElse(null);
            log.debug("Successfully retrieved secret: {}", key);
            return value;
        } catch (Exception e) {
            log.error("Failed to retrieve secret: {}", key, e);
            throw new RuntimeException("Failed to retrieve secret", e);
        }
    }

    /**
     * Get all secrets for a specific key (returns all metadata)
     */
    public Map<String, String> getSecrets(String key) {
        try {
            log.debug("Retrieving secrets for key: {}", key);
            return daprClient.getSecret(SECRET_STORE_NAME, key).block();
        } catch (Exception e) {
            log.error("Failed to retrieve secrets for key: {}", key, e);
            throw new RuntimeException("Failed to retrieve secrets", e);
        }
    }

    /**
     * Get database configuration from secrets
     * Uses nested structure: database:host, database:port, etc.
     */
    public DatabaseConfig getDatabaseConfig() {
        String host = getSecret("database:host");
        String port = getSecret("database:port");
        String name = getSecret("database:name");
        String user = getSecret("database:user");
        String password = getSecret("database:password");
        
        return new DatabaseConfig(host, port, name, user, password);
    }

    /**
     * Get JWT secret
     * Uses nested structure: jwt:secret
     */
    public String getJwtSecret() {
        return getSecret("jwt:secret");
    }

    /**
     * Get service URLs
     */
    public ServiceUrls getServiceUrls() {
        String orderService = getSecret("ORDER_SERVICE_URL");
        String paymentService = getSecret("PAYMENT_SERVICE_URL");
        String inventoryService = getSecret("INVENTORY_SERVICE_URL");
        String shippingService = getSecret("SHIPPING_SERVICE_URL");
        
        return new ServiceUrls(orderService, paymentService, inventoryService, shippingService);
    }

    // Inner classes for structured configuration
    public record DatabaseConfig(
        String host,
        String port,
        String name,
        String user,
        String password
    ) {
        public String getJdbcUrl() {
            return String.format("jdbc:postgresql://%s:%s/%s", host, port, name);
        }
    }

    public record ServiceUrls(
        String orderService,
        String paymentService,
        String inventoryService,
        String shippingService
    ) {}
}
