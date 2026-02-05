package com.xshopai.orderprocessor.client;

import io.dapr.client.DaprClient;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Service;

import jakarta.annotation.PostConstruct;
import java.util.Map;

/**
 * Dapr Secret Manager
 * Handles retrieving secrets from Dapr secret store with multi-format support.
 * 
 * Supports multiple secret key formats for different environments:
 * - Local development: colon separator (database:host) via local file store
 * - Azure Key Vault: dash separator (database-host) via Key Vault
 * - Environment variables: underscore separator (database_host) as fallback
 */
@Service
@Slf4j
public class DaprSecretManager {

    private static final String SECRET_STORE_NAME = "secretstore";

    private final DaprClient daprClient;
    private final Environment environment;
    private final boolean daprEnabled;

    public DaprSecretManager(
            @Value("${messaging.provider:${MESSAGING_PROVIDER:dapr}}") String messagingProvider,
            @Autowired(required = false) DaprClient daprClient,
            Environment environment) {
        this.daprClient = daprClient;
        this.environment = environment;
        this.daprEnabled = "dapr".equalsIgnoreCase(messagingProvider) && daprClient != null;
    }

    @PostConstruct
    public void init() {
        if (daprEnabled) {
            log.info("Dapr Secret Manager initialized with store: {} (Dapr enabled)", SECRET_STORE_NAME);
        } else {
            log.info("Dapr Secret Manager initialized (Dapr disabled - using env vars only)");
        }
    }

    /**
     * Get a specific secret by key with Dapr secret store priority.
     * 
     * Priority:
     * 1. Dapr secret store (.dapr/secrets.json when running with Dapr)
     * 2. Environment variable (UPPER_SNAKE_CASE from .env file when running without Dapr)
     * 3. Spring Environment property (fallback)
     * 
     * Note: When running with Dapr, secrets come from .dapr/secrets.json.
     * When running without Dapr, secrets come from .env file or environment variables.
     */
    public String getSecret(String key) {
        String envKey = key.replace(":", "_").replace("-", "_").toUpperCase();
        
        // 1. Try Dapr secret store FIRST (only if enabled)
        if (daprEnabled && daprClient != null) {
            String value = tryGetDaprSecret(key);
            if (value != null && !value.isEmpty()) {
                log.debug("Found secret from Dapr secret store: {}", key);
                return value;
            }
        }
        
        // 2. Fallback to environment variable (from .env file)
        String value = System.getenv(envKey);
        if (value != null && !value.isEmpty()) {
            log.debug("Found secret from environment variable: {}", envKey);
            return value;
        }
        
        // 3. Fallback to Spring Environment property
        value = environment.getProperty(envKey);
        if (value != null && !value.isEmpty()) {
            log.debug("Found secret from Spring environment: {}", envKey);
            return value;
        }
        
        // Also try lowercase underscore format
        String lowerEnvKey = key.replace(":", "_").replace("-", "_");
        value = environment.getProperty(lowerEnvKey);
        if (value != null && !value.isEmpty()) {
            log.debug("Found secret from Spring environment: {}", lowerEnvKey);
            return value;
        }
        
        log.warn("Secret not found: {} (tried Dapr, env: {}, and Spring)", key, envKey);
        return null;
    }
    
    /**
     * Try to get a secret from Dapr, returning null on failure
     */
    private String tryGetDaprSecret(String key) {
        try {
            log.debug("Attempting to retrieve secret from Dapr: {}", key);
            
            Map<String, String> secret = daprClient.getSecret(SECRET_STORE_NAME, key).block();
            
            if (secret == null || secret.isEmpty()) {
                return null;
            }
            
            String value = secret.values().stream().findFirst().orElse(null);
            if (value != null) {
                log.debug("Successfully retrieved secret: {}", key);
            }
            return value;
        } catch (Exception e) {
            log.debug("Could not retrieve secret '{}' from Dapr: {}", key, e.getMessage());
            return null;
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
