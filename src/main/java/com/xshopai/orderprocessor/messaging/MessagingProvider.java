package com.xshopai.orderprocessor.messaging;

/**
 * Messaging Provider Interface
 * Abstracts the underlying messaging implementation (Dapr, RabbitMQ, Azure Service Bus)
 * 
 * Implementations should handle serialization, error handling, and connection management.
 * All methods are designed to be resilient - failures are logged but don't throw exceptions
 * to prevent messaging issues from blocking business operations.
 */
public interface MessagingProvider extends AutoCloseable {

    /**
     * Publish an event to a specific topic
     * 
     * @param topic The topic/subject to publish to
     * @param eventData The event payload (will be serialized to JSON)
     * @param correlationId Optional correlation ID for distributed tracing
     * @return true if published successfully, false otherwise
     */
    boolean publishEvent(String topic, Object eventData, String correlationId);

    /**
     * Publish an event without correlation ID
     * 
     * @param topic The topic/subject to publish to
     * @param eventData The event payload
     * @return true if published successfully, false otherwise
     */
    default boolean publishEvent(String topic, Object eventData) {
        return publishEvent(topic, eventData, null);
    }

    /**
     * Get the name/type of this messaging provider
     * 
     * @return Provider name (e.g., "dapr", "rabbitmq", "servicebus")
     */
    String getProviderName();

    /**
     * Check if the messaging provider is connected and ready
     * 
     * @return true if ready to publish, false otherwise
     */
    boolean isReady();

    /**
     * Close and cleanup resources
     */
    @Override
    void close();
}
