package com.xshopai.orderprocessor.messaging;

import io.dapr.client.DaprClient;
import lombok.extern.slf4j.Slf4j;

import java.util.HashMap;
import java.util.Map;

/**
 * Dapr Messaging Provider Implementation
 * Uses Dapr pub/sub component for event publishing
 */
@Slf4j
public class DaprMessagingProvider implements MessagingProvider {

    private final DaprClient daprClient;
    private final String pubsubName;
    private volatile boolean closed = false;

    /**
     * Create a new Dapr messaging provider
     * 
     * @param daprClient The Dapr client instance
     * @param pubsubName The Dapr pub/sub component name (e.g., "pubsub")
     */
    public DaprMessagingProvider(DaprClient daprClient, String pubsubName) {
        this.daprClient = daprClient;
        this.pubsubName = pubsubName;
        log.info("DaprMessagingProvider initialized with pubsub: {}", pubsubName);
    }

    @Override
    public boolean publishEvent(String topic, Object eventData, String correlationId) {
        if (closed) {
            log.warn("Attempted to publish to closed DaprMessagingProvider");
            return false;
        }

        try {
            Map<String, String> metadata = new HashMap<>();
            if (correlationId != null && !correlationId.isEmpty()) {
                metadata.put("correlationId", correlationId);
                metadata.put("X-Correlation-Id", correlationId);
            }

            daprClient.publishEvent(pubsubName, topic, eventData, metadata).block();
            
            log.debug("Event published to Dapr topic: {} with correlationId: {}", topic, correlationId);
            return true;
        } catch (Exception e) {
            log.error("Failed to publish event to Dapr topic: {} - Error: {}", topic, e.getMessage(), e);
            return false;
        }
    }

    @Override
    public String getProviderName() {
        return "dapr";
    }

    @Override
    public boolean isReady() {
        if (closed) {
            return false;
        }
        try {
            // Simple check - Dapr client exists and is not null
            return daprClient != null;
        } catch (Exception e) {
            log.warn("Dapr readiness check failed: {}", e.getMessage());
            return false;
        }
    }

    @Override
    public void close() {
        if (!closed) {
            closed = true;
            try {
                if (daprClient != null) {
                    daprClient.close();
                }
                log.info("DaprMessagingProvider closed");
            } catch (Exception e) {
                log.warn("Error closing Dapr client: {}", e.getMessage());
            }
        }
    }
}
