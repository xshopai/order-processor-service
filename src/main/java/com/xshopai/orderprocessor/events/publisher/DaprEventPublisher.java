package com.xshopai.orderprocessor.events.publisher;

import io.dapr.client.DaprClient;
import io.dapr.client.domain.CloudEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import jakarta.annotation.PostConstruct;
import java.util.HashMap;
import java.util.Map;

/**
 * Dapr Event Publisher Service
 * Handles publishing events to Dapr pub/sub component
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class DaprEventPublisher {

    @Value("${dapr.pubsub-name:order-pubsub}")
    private String pubsubName;

    private final DaprClient daprClient;

    @PostConstruct
    public void init() {
        log.info("Dapr Event Publisher initialized with pubsub: {}", pubsubName);
    }

    /**
     * Publish an event to a specific topic
     */
    public void publishEvent(String topic, Object event) {
        publishEvent(topic, event, null);
    }

    /**
     * Publish an event with metadata
     */
    public void publishEvent(String topic, Object event, Map<String, String> metadata) {
        try {
            log.debug("Publishing event to topic: {}", topic);
            
            if (metadata == null) {
                metadata = new HashMap<>();
            }
            
            daprClient.publishEvent(pubsubName, topic, event, metadata).block();
            
            log.info("Event published successfully to topic: {}", topic);
        } catch (Exception e) {
            log.error("Failed to publish event to topic: {}", topic, e);
            throw new RuntimeException("Failed to publish event", e);
        }
    }

    /**
     * Publish event with correlation ID
     */
    public void publishEventWithCorrelationId(String topic, Object event, String correlationId) {
        Map<String, String> metadata = new HashMap<>();
        metadata.put("correlationId", correlationId);
        metadata.put("X-Correlation-Id", correlationId);
        publishEvent(topic, event, metadata);
    }

    // Order Events
    public void publishOrderCreated(Object event) {
        publishEvent("order.created", event);
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
