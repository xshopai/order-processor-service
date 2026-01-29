package com.xshopai.orderprocessor.messaging;

import com.azure.messaging.servicebus.ServiceBusClientBuilder;
import com.azure.messaging.servicebus.ServiceBusMessage;
import com.azure.messaging.servicebus.ServiceBusSenderClient;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Azure Service Bus Messaging Provider Implementation
 * Uses Azure Service Bus Topics for event publishing
 */
@Slf4j
public class ServiceBusMessagingProvider implements MessagingProvider {

    private final String connectionString;
    private final ObjectMapper objectMapper;
    private final Map<String, ServiceBusSenderClient> senderClients;
    private volatile boolean closed = false;

    /**
     * Create a new Service Bus messaging provider
     * 
     * @param connectionString Azure Service Bus connection string
     * @param objectMapper JSON serializer
     */
    public ServiceBusMessagingProvider(String connectionString, ObjectMapper objectMapper) {
        this.connectionString = connectionString;
        this.objectMapper = objectMapper;
        this.senderClients = new ConcurrentHashMap<>();
        log.info("ServiceBusMessagingProvider initialized");
    }

    /**
     * Get or create a sender client for the specified topic
     */
    private ServiceBusSenderClient getSenderClient(String topic) {
        return senderClients.computeIfAbsent(topic, t -> {
            try {
                return new ServiceBusClientBuilder()
                        .connectionString(connectionString)
                        .sender()
                        .topicName(t)
                        .buildClient();
            } catch (Exception e) {
                log.error("Failed to create Service Bus sender for topic: {}", t, e);
                throw new RuntimeException("Failed to create Service Bus sender", e);
            }
        });
    }

    @Override
    public boolean publishEvent(String topic, Object eventData, String correlationId) {
        if (closed) {
            log.warn("Attempted to publish to closed ServiceBusMessagingProvider");
            return false;
        }

        try {
            String messageJson = objectMapper.writeValueAsString(eventData);
            
            ServiceBusMessage message = new ServiceBusMessage(messageJson);
            message.setContentType("application/json");
            message.setMessageId(UUID.randomUUID().toString());
            
            if (correlationId != null && !correlationId.isEmpty()) {
                message.setCorrelationId(correlationId);
                message.getApplicationProperties().put("correlationId", correlationId);
                message.getApplicationProperties().put("X-Correlation-Id", correlationId);
            }
            message.getApplicationProperties().put("source", "order-processor-service");
            message.getApplicationProperties().put("eventType", topic);

            ServiceBusSenderClient senderClient = getSenderClient(topic);
            senderClient.sendMessage(message);
            
            log.debug("Event published to Service Bus topic: {} with correlationId: {}", topic, correlationId);
            return true;
        } catch (Exception e) {
            log.error("Failed to publish event to Service Bus topic: {} - Error: {}", topic, e.getMessage(), e);
            return false;
        }
    }

    @Override
    public String getProviderName() {
        return "servicebus";
    }

    @Override
    public boolean isReady() {
        if (closed) {
            return false;
        }
        try {
            // Check if connection string is configured
            return connectionString != null && !connectionString.isEmpty();
        } catch (Exception e) {
            log.warn("Service Bus readiness check failed: {}", e.getMessage());
            return false;
        }
    }

    @Override
    public void close() {
        if (!closed) {
            closed = true;
            try {
                for (ServiceBusSenderClient client : senderClients.values()) {
                    try {
                        client.close();
                    } catch (Exception e) {
                        log.warn("Error closing Service Bus sender client: {}", e.getMessage());
                    }
                }
                senderClients.clear();
                log.info("ServiceBusMessagingProvider closed");
            } catch (Exception e) {
                log.warn("Error closing Service Bus clients: {}", e.getMessage());
            }
        }
    }
}
