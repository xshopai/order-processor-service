package com.xshopai.orderprocessor.events.publisher;

import com.xshopai.orderprocessor.messaging.MessagingProvider;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import jakarta.annotation.PostConstruct;
import java.util.HashMap;
import java.util.Map;

/**
 * Event Publisher Service
 * Handles publishing events using the configured MessagingProvider
 * 
 * This class provides a high-level API for publishing domain events.
 * The underlying messaging implementation (Dapr, RabbitMQ, Service Bus) 
 * is abstracted by the MessagingProvider interface.
 * 
 * Note: Class name kept as DaprEventPublisher for backward compatibility,
 * but now uses the MessagingProvider abstraction internally.
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class DaprEventPublisher {

    private final MessagingProvider messagingProvider;

    @PostConstruct
    public void init() {
        log.info("Event Publisher initialized with provider: {}", messagingProvider.getProviderName());
    }

    /**
     * Publish an event to a specific topic
     */
    public void publishEvent(String topic, Object event) {
        publishEvent(topic, event, (Map<String, String>) null);
    }

    /**
     * Publish an event with metadata
     * Note: Metadata is included in the event payload for non-Dapr providers
     */
    public void publishEvent(String topic, Object event, Map<String, String> metadata) {
        try {
            log.debug("Publishing event to topic: {}", topic);
            
            String correlationId = null;
            if (metadata != null) {
                correlationId = metadata.get("correlationId");
                if (correlationId == null) {
                    correlationId = metadata.get("X-Correlation-Id");
                }
            }
            
            boolean success = messagingProvider.publishEvent(topic, event, correlationId);
            
            if (success) {
                log.info("Event published successfully to topic: {}", topic);
            } else {
                log.warn("Event publishing returned false for topic: {}", topic);
            }
        } catch (Exception e) {
            log.error("Failed to publish event to topic: {}", topic, e);
            throw new RuntimeException("Failed to publish event", e);
        }
    }

    /**
     * Publish event with correlation ID
     */
    public void publishEventWithCorrelationId(String topic, Object event, String correlationId) {
        try {
            log.debug("Publishing event to topic: {} with correlationId: {}", topic, correlationId);
            
            boolean success = messagingProvider.publishEvent(topic, event, correlationId);
            
            if (success) {
                log.info("Event published successfully to topic: {} with correlationId: {}", topic, correlationId);
            } else {
                log.warn("Event publishing returned false for topic: {} with correlationId: {}", topic, correlationId);
            }
        } catch (Exception e) {
            log.error("Failed to publish event to topic: {} with correlationId: {}", topic, correlationId, e);
            throw new RuntimeException("Failed to publish event", e);
        }
    }

    // Order Events
    public void publishOrderCreated(Object event) {
        publishEvent("order.placed", event);
    }

    public void publishOrderStatusChanged(Object event) {
        publishEvent("order.status.changed", event);
    }

    public void publishOrderCompleted(Object event) {
        publishEvent("order.completed", event);
    }

    public void publishOrderFailed(Object event) {
        publishEvent("order.failed", event);
    }

    // Payment Events
    public void publishPaymentProcessing(Object event) {
        publishEvent("payment.processing", event);
    }

    public void publishPaymentProcessed(Object event) {
        publishEvent("payment.processed", event);
    }

    public void publishPaymentFailed(Object event) {
        publishEvent("payment.failed", event);
    }

    public void publishPaymentRefund(Object event) {
        publishEvent("payment.refund", event);
    }

    // Inventory Events
    public void publishInventoryReservation(Object event) {
        publishEvent("inventory.reservation", event);
    }

    public void publishInventoryReserved(Object event) {
        publishEvent("inventory.reserved", event);
    }

    public void publishInventoryFailed(Object event) {
        publishEvent("inventory.failed", event);
    }

    public void publishInventoryRelease(Object event) {
        publishEvent("inventory.release", event);
    }

    // Shipping Events
    public void publishShippingPreparation(Object event) {
        publishEvent("shipping.preparation", event);
    }

    public void publishShippingPrepared(Object event) {
        publishEvent("shipping.prepared", event);
    }

    public void publishShippingFailed(Object event) {
        publishEvent("shipping.failed", event);
    }

    public void publishShippingCancellation(Object event) {
        publishEvent("shipping.cancellation", event);
    }

    // Additional convenience methods with multiple parameters
    public void publishPaymentProcessedStatus(Object orderId, String status, String paymentId, String transactionId) {
        publishOrderStatusChanged(Map.of(
            "orderId", orderId,
            "status", status,
            "paymentId", paymentId,
            "transactionId", transactionId,
            "step", "payment"
        ));
    }

    public void publishInventoryReservedStatus(Object orderId, String status, String reservationId, String note) {
        publishOrderStatusChanged(Map.of(
            "orderId", orderId,
            "status", status,
            "reservationId", reservationId,
            "note", note,
            "step", "inventory"
        ));
    }

    public void publishShippingPreparedStatus(Object orderId, String status, String shippingId, String carrier) {
        publishOrderStatusChanged(Map.of(
            "orderId", orderId,
            "status", status,
            "shippingId", shippingId,
            "carrier", carrier,
            "step", "shipping"
        ));
    }

    public void publishOrderCompletedStatus(Object orderId, String status, String note) {
        publishOrderStatusChanged(Map.of(
            "orderId", orderId,
            "status", status,
            "note", note
        ));
    }

    // Overloaded for orderId, orderNumber, customerId, correlationId
    public void publishOrderCompletedStatus(Object orderId, String orderNumber, String customerId, String correlationId) {
        publishOrderStatusChanged(Map.of(
            "orderId", orderId,
            "orderNumber", orderNumber,
            "customerId", customerId,
            "correlationId", correlationId,
            "status", "COMPLETED"
        ));
    }

    public void publishOrderFailedStatus(Object orderId, String status, String errorMessage, String failedStep) {
        publishOrderFailed(Map.of(
            "orderId", orderId,
            "status", status,
            "errorMessage", errorMessage,
            "failedStep", failedStep
        ));
    }

    // Overloaded for orderId, orderNumber, customerId, errorMessage, correlationId
    public void publishOrderFailedStatus(Object orderId, String orderNumber, String customerId, String errorMessage, String correlationId) {
        publishOrderFailed(Map.of(
            "orderId", orderId,
            "orderNumber", orderNumber,
            "customerId", customerId,
            "errorMessage", errorMessage,
            "correlationId", correlationId,
            "status", "FAILED"
        ));
    }

    public void publishShippingPreparation(Object orderId, Object customerId) {
        publishShippingPreparation(Map.of(
            "orderId", orderId,
            "customerId", customerId
        ));
    }

    public void publishPaymentRefund(Object orderId, String paymentId) {
        publishPaymentRefund(Map.of(
            "orderId", orderId,
            "paymentId", paymentId
        ));
    }

    public void publishInventoryRelease(Object orderId, String reservationId) {
        publishInventoryRelease(Map.of(
            "orderId", orderId,
            "reservationId", reservationId
        ));
    }

    public void publishShippingCancellation(Object orderId, String shippingId) {
        publishShippingCancellation(Map.of(
            "orderId", orderId,
            "shippingId", shippingId
        ));
    }
}
