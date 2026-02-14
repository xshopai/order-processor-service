package com.xshopai.orderprocessor.messaging;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.rabbitmq.client.*;
import com.xshopai.orderprocessor.model.events.*;
import com.xshopai.orderprocessor.service.SagaOrchestratorService;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 * RabbitMQ Consumer for Order Processor Service
 * 
 * This consumer is enabled when MESSAGING_PROVIDER=rabbitmq (local development without Dapr).
 * It listens to RabbitMQ queues for events and dispatches them to the appropriate handlers.
 * 
 * When using Dapr (MESSAGING_PROVIDER=dapr), event delivery is handled by Dapr's HTTP push
 * to the /dapr/events/* endpoints in the consumer controllers.
 */
@Component
@ConditionalOnProperty(name = "messaging.provider", havingValue = "rabbitmq")
@Slf4j
public class RabbitMQConsumer {

    private static final String EXCHANGE_NAME = "xshopai.events";
    private static final String QUEUE_NAME = "order-processor-service";

    /**
     * Topics this service subscribes to (must match subscriptions.yaml)
     */
    private static final List<String> SUBSCRIBED_TOPICS = Arrays.asList(
            "order.placed",
            "payment.processed",
            "payment.failed",
            "inventory.reserved",
            "inventory.failed",
            "shipping.prepared",
            "shipping.failed",
            "order.cancelled",
            "return.approved",
            "return.completed"
    );

    @Value("${rabbitmq.host:localhost}")
    private String host;

    @Value("${rabbitmq.port:5672}")
    private int port;

    @Value("${rabbitmq.username:guest}")
    private String username;

    @Value("${rabbitmq.password:guest}")
    private String password;

    @Autowired
    private SagaOrchestratorService sagaOrchestratorService;

    @Autowired
    private ObjectMapper objectMapper;

    private Connection connection;
    private Channel channel;
    private ExecutorService executorService;
    private volatile boolean running = false;

    @PostConstruct
    public void start() {
        log.info("Starting RabbitMQ consumer for order-processor-service (MESSAGING_PROVIDER=rabbitmq)");
        
        executorService = Executors.newSingleThreadExecutor(r -> {
            Thread t = new Thread(r, "rabbitmq-consumer");
            t.setDaemon(true);
            return t;
        });

        executorService.submit(this::startConsumer);
    }

    private void startConsumer() {
        int retryCount = 0;
        int maxRetries = 10;
        int retryDelaySeconds = 5;

        while (!running && retryCount < maxRetries) {
            try {
                ConnectionFactory factory = new ConnectionFactory();
                factory.setHost(host);
                factory.setPort(port);
                factory.setUsername(username);
                factory.setPassword(password);
                factory.setAutomaticRecoveryEnabled(true);
                factory.setNetworkRecoveryInterval(5000);

                connection = factory.newConnection("order-processor-consumer");
                channel = connection.createChannel();

                // Declare exchange if not exists
                channel.exchangeDeclare(EXCHANGE_NAME, "topic", true);

                // Declare durable queue
                Map<String, Object> args = new HashMap<>();
                args.put("x-dead-letter-exchange", EXCHANGE_NAME + ".dlx");
                channel.queueDeclare(QUEUE_NAME, true, false, false, args);

                // Bind queue to all subscribed topics
                for (String topic : SUBSCRIBED_TOPICS) {
                    channel.queueBind(QUEUE_NAME, EXCHANGE_NAME, topic);
                    log.info("Bound queue {} to topic: {}", QUEUE_NAME, topic);
                }

                // Set prefetch count for fair dispatch
                channel.basicQos(10);

                running = true;
                log.info("✅ RabbitMQ consumer connected - listening on queue: {}", QUEUE_NAME);
                log.info("   Subscribed topics: {}", SUBSCRIBED_TOPICS);

                // Start consuming
                channel.basicConsume(QUEUE_NAME, false, new DefaultConsumer(channel) {
                    @Override
                    public void handleDelivery(String consumerTag, Envelope envelope,
                                               AMQP.BasicProperties properties, byte[] body) {
                        String routingKey = envelope.getRoutingKey();
                        String message = new String(body, StandardCharsets.UTF_8);
                        String correlationId = properties.getCorrelationId();

                        try {
                            log.debug("Received event on topic: {} correlationId: {}", routingKey, correlationId);
                            handleEvent(routingKey, message, correlationId);
                            channel.basicAck(envelope.getDeliveryTag(), false);
                        } catch (Exception e) {
                            log.error("Error processing event on topic: {} - {}", routingKey, e.getMessage(), e);
                            try {
                                // Requeue failed messages (up to broker retry limits)
                                channel.basicNack(envelope.getDeliveryTag(), false, true);
                            } catch (IOException ioException) {
                                log.error("Failed to nack message", ioException);
                            }
                        }
                    }
                });

            } catch (Exception e) {
                retryCount++;
                log.warn("Failed to connect to RabbitMQ (attempt {}/{}): {}",
                        retryCount, maxRetries, e.getMessage());
                
                if (retryCount < maxRetries) {
                    try {
                        Thread.sleep(retryDelaySeconds * 1000L);
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                        break;
                    }
                }
            }
        }

        if (!running) {
            log.error("❌ Failed to start RabbitMQ consumer after {} attempts", maxRetries);
        }
    }

    /**
     * Route event to appropriate handler based on topic
     */
    private void handleEvent(String topic, String message, String correlationId) throws Exception {
        log.info("Processing {} event, correlationId: {}", topic, correlationId);

        switch (topic) {
            case "order.placed":
                OrderCreatedEvent orderCreatedEvent = objectMapper.readValue(message, OrderCreatedEvent.class);
                sagaOrchestratorService.startOrderProcessingSaga(orderCreatedEvent);
                break;

            case "payment.processed":
                PaymentProcessedEvent paymentProcessedEvent = objectMapper.readValue(message, PaymentProcessedEvent.class);
                sagaOrchestratorService.handlePaymentProcessed(paymentProcessedEvent);
                break;

            case "payment.failed":
                PaymentFailedEvent paymentFailedEvent = objectMapper.readValue(message, PaymentFailedEvent.class);
                sagaOrchestratorService.handlePaymentFailed(paymentFailedEvent);
                break;

            case "inventory.reserved":
                InventoryReservedEvent inventoryReservedEvent = objectMapper.readValue(message, InventoryReservedEvent.class);
                sagaOrchestratorService.handleInventoryReserved(inventoryReservedEvent);
                break;

            case "inventory.failed":
                InventoryFailedEvent inventoryFailedEvent = objectMapper.readValue(message, InventoryFailedEvent.class);
                sagaOrchestratorService.handleInventoryFailed(inventoryFailedEvent);
                break;

            case "shipping.prepared":
                ShippingPreparedEvent shippingPreparedEvent = objectMapper.readValue(message, ShippingPreparedEvent.class);
                sagaOrchestratorService.handleShippingPrepared(shippingPreparedEvent);
                break;

            case "shipping.failed":
                ShippingFailedEvent shippingFailedEvent = objectMapper.readValue(message, ShippingFailedEvent.class);
                sagaOrchestratorService.handleShippingFailed(shippingFailedEvent);
                break;

            case "order.cancelled":
                OrderCancelledEvent orderCancelledEvent = objectMapper.readValue(message, OrderCancelledEvent.class);
                sagaOrchestratorService.handleOrderCancelled(orderCancelledEvent);
                break;

            case "return.approved":
                ReturnStatusChangedEvent returnApprovedEvent = objectMapper.readValue(message, ReturnStatusChangedEvent.class);
                sagaOrchestratorService.handleReturnApproved(returnApprovedEvent);
                break;

            case "return.completed":
                ReturnStatusChangedEvent returnCompletedEvent = objectMapper.readValue(message, ReturnStatusChangedEvent.class);
                sagaOrchestratorService.handleReturnCompleted(returnCompletedEvent);
                break;

            default:
                log.warn("Unknown topic: {}, message ignored", topic);
        }
    }

    @PreDestroy
    public void stop() {
        log.info("Stopping RabbitMQ consumer for order-processor-service");
        running = false;

        try {
            if (channel != null && channel.isOpen()) {
                channel.close();
            }
            if (connection != null && connection.isOpen()) {
                connection.close();
            }
        } catch (IOException | TimeoutException e) {
            log.warn("Error closing RabbitMQ connection: {}", e.getMessage());
        }

        if (executorService != null) {
            executorService.shutdown();
            try {
                if (!executorService.awaitTermination(10, TimeUnit.SECONDS)) {
                    executorService.shutdownNow();
                }
            } catch (InterruptedException e) {
                executorService.shutdownNow();
                Thread.currentThread().interrupt();
            }
        }

        log.info("RabbitMQ consumer stopped");
    }
}
