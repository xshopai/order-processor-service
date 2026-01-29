package com.xshopai.orderprocessor.messaging;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.rabbitmq.client.AMQP;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;
import lombok.extern.slf4j.Slf4j;

import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * RabbitMQ Messaging Provider Implementation
 * Direct RabbitMQ connection for event publishing
 */
@Slf4j
public class RabbitMQMessagingProvider implements MessagingProvider {

    private final ObjectMapper objectMapper;
    private final String exchangeName;
    private Connection connection;
    private Channel channel;
    private volatile boolean closed = false;

    /**
     * Create a new RabbitMQ messaging provider
     * 
     * @param host RabbitMQ host
     * @param port RabbitMQ port
     * @param username RabbitMQ username
     * @param password RabbitMQ password
     * @param exchangeName Exchange name for publishing
     * @param objectMapper JSON serializer
     */
    public RabbitMQMessagingProvider(
            String host,
            int port,
            String username,
            String password,
            String exchangeName,
            ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
        this.exchangeName = exchangeName;

        try {
            ConnectionFactory factory = new ConnectionFactory();
            factory.setHost(host);
            factory.setPort(port);
            factory.setUsername(username);
            factory.setPassword(password);
            factory.setAutomaticRecoveryEnabled(true);
            factory.setNetworkRecoveryInterval(5000);

            this.connection = factory.newConnection("order-processor-service");
            this.channel = connection.createChannel();
            
            // Declare topic exchange
            channel.exchangeDeclare(exchangeName, "topic", true);
            
            log.info("RabbitMQMessagingProvider initialized - host: {}, port: {}, exchange: {}", 
                    host, port, exchangeName);
        } catch (Exception e) {
            log.error("Failed to initialize RabbitMQ connection: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to connect to RabbitMQ", e);
        }
    }

    @Override
    public boolean publishEvent(String topic, Object eventData, String correlationId) {
        if (closed) {
            log.warn("Attempted to publish to closed RabbitMQMessagingProvider");
            return false;
        }

        try {
            String messageJson = objectMapper.writeValueAsString(eventData);
            byte[] messageBytes = messageJson.getBytes(StandardCharsets.UTF_8);

            Map<String, Object> headers = new HashMap<>();
            if (correlationId != null && !correlationId.isEmpty()) {
                headers.put("correlationId", correlationId);
                headers.put("X-Correlation-Id", correlationId);
            }
            headers.put("contentType", "application/json");
            headers.put("source", "order-processor-service");

            AMQP.BasicProperties props = new AMQP.BasicProperties.Builder()
                    .contentType("application/json")
                    .correlationId(correlationId != null ? correlationId : UUID.randomUUID().toString())
                    .messageId(UUID.randomUUID().toString())
                    .headers(headers)
                    .deliveryMode(2) // Persistent
                    .build();

            // Use topic as routing key
            channel.basicPublish(exchangeName, topic, props, messageBytes);
            
            log.debug("Event published to RabbitMQ topic: {} with correlationId: {}", topic, correlationId);
            return true;
        } catch (Exception e) {
            log.error("Failed to publish event to RabbitMQ topic: {} - Error: {}", topic, e.getMessage(), e);
            return false;
        }
    }

    @Override
    public String getProviderName() {
        return "rabbitmq";
    }

    @Override
    public boolean isReady() {
        if (closed) {
            return false;
        }
        try {
            return connection != null && connection.isOpen() && channel != null && channel.isOpen();
        } catch (Exception e) {
            log.warn("RabbitMQ readiness check failed: {}", e.getMessage());
            return false;
        }
    }

    @Override
    public void close() {
        if (!closed) {
            closed = true;
            try {
                if (channel != null && channel.isOpen()) {
                    channel.close();
                }
                if (connection != null && connection.isOpen()) {
                    connection.close();
                }
                log.info("RabbitMQMessagingProvider closed");
            } catch (Exception e) {
                log.warn("Error closing RabbitMQ connection: {}", e.getMessage());
            }
        }
    }
}
