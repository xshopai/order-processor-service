package com.xshopai.orderprocessor.service;

import com.xshopai.orderprocessor.events.publisher.DaprEventPublisher;
import com.xshopai.orderprocessor.model.entity.OrderProcessingSaga;
import com.xshopai.orderprocessor.model.events.*;
import com.xshopai.orderprocessor.model.events.InventoryReservationEvent.InventoryItem;
import com.xshopai.orderprocessor.repository.OrderProcessingSagaRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Choreography-based Saga Orchestrator Service
 * Manages the state and flow of order processing saga using Dapr
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class SagaOrchestratorService {

    private final OrderProcessingSagaRepository sagaRepository;
    private final DaprEventPublisher daprEventPublisher;
    private final SagaMetricsService metricsService;
    private final ObjectMapper objectMapper;

    @Value("${saga.retry.max-attempts:3}")
    private int maxRetryAttempts;

    /**
     * Start a new saga for order processing
     * Saga starts in PENDING_PAYMENT_CONFIRMATION status
     * Admin must manually confirm payment to proceed
     */
    @Transactional
    public void startOrderProcessingSaga(OrderCreatedEvent orderCreatedEvent) {
        log.info("Starting order processing saga for order: {}", orderCreatedEvent.getOrderId());

        // Check if saga already exists
        if (sagaRepository.existsByOrderId(orderCreatedEvent.getOrderId())) {
            log.warn("Saga already exists for order: {}", orderCreatedEvent.getOrderId());
            return;
        }

        // Create new saga in PENDING state - no automatic processing
        OrderProcessingSaga saga = new OrderProcessingSaga();
        saga.setOrderId(orderCreatedEvent.getOrderId());
        saga.setCustomerId(orderCreatedEvent.getCustomerId());
        saga.setOrderNumber(orderCreatedEvent.getOrderNumber());
        saga.setTotalAmount(orderCreatedEvent.getTotalAmount());
        saga.setCurrency(orderCreatedEvent.getCurrency());
        saga.setStatus(OrderProcessingSaga.SagaStatus.PENDING_PAYMENT_CONFIRMATION);
        saga.setCurrentStep(OrderProcessingSaga.ProcessingStep.AWAITING_PAYMENT);

        // Store order items and addresses from event (event-driven architecture)
        // This eliminates the need for HTTP calls to Order Service
        try {
            if (orderCreatedEvent.getItems() != null && !orderCreatedEvent.getItems().isEmpty()) {
                saga.setOrderItems(objectMapper.writeValueAsString(orderCreatedEvent.getItems()));
            }
            if (orderCreatedEvent.getShippingAddress() != null) {
                saga.setShippingAddress(objectMapper.writeValueAsString(orderCreatedEvent.getShippingAddress()));
            }
            if (orderCreatedEvent.getBillingAddress() != null) {
                saga.setBillingAddress(objectMapper.writeValueAsString(orderCreatedEvent.getBillingAddress()));
            }
        } catch (JsonProcessingException e) {
            log.error("Failed to serialize order data for saga", e);
            throw new RuntimeException("Failed to store order data in saga", e);
        }

        saga = sagaRepository.save(saga);
        log.info("Created saga {} for order: {} - Status: PENDING_PAYMENT_CONFIRMATION", 
                saga.getId(), orderCreatedEvent.getOrderId());

        // Record metrics
        metricsService.recordSagaStarted(orderCreatedEvent.getOrderNumber());
        
        // NO AUTOMATIC PROCESSING - Admin must confirm payment via Admin UI
        log.info("Saga awaiting admin action: Payment confirmation required for order: {}", 
                orderCreatedEvent.getOrderNumber());
    }

    /**
     * Handle payment processed event (triggered by admin action via Admin UI)
     * Admin has confirmed payment received - move saga to next pending state
     */
    @Transactional
    public void handlePaymentProcessed(PaymentProcessedEvent paymentProcessedEvent) {
        log.info("Admin confirmed payment for order: {}", paymentProcessedEvent.getOrderId());

        Optional<OrderProcessingSaga> sagaOpt = sagaRepository.findByOrderId(paymentProcessedEvent.getOrderId());
        if (sagaOpt.isEmpty()) {
            log.warn("No saga found for order: {}", paymentProcessedEvent.getOrderId());
            return;
        }

        OrderProcessingSaga saga = sagaOpt.get();
        saga.setPaymentId(paymentProcessedEvent.getPaymentId());
        saga.markPaymentConfirmed(); // Sets status to PAYMENT_CONFIRMED, step to AWAITING_SHIPMENT
        saga.setStatus(OrderProcessingSaga.SagaStatus.PENDING_SHIPPING_PREPARATION);
        
        saga = sagaRepository.save(saga);
        log.info("Updated saga {} - Payment confirmed, awaiting admin shipment preparation", saga.getId());

        // NO AUTOMATIC PROCESSING - Admin must prepare shipment via Admin UI
        log.info("Saga awaiting admin action: Shipment preparation required for order: {}", 
                saga.getOrderNumber());
    }

    /**
     * Handle payment failed event (admin marks payment as failed/rejected)
     */
    @Transactional
    public void handlePaymentFailed(PaymentFailedEvent paymentFailedEvent) {
        log.info("Admin marked payment as failed for order: {}", paymentFailedEvent.getOrderId());

        Optional<OrderProcessingSaga> sagaOpt = sagaRepository.findByOrderId(paymentFailedEvent.getOrderId());
        if (sagaOpt.isEmpty()) {
            log.warn("No saga found for order: {}", paymentFailedEvent.getOrderId());
            return;
        }

        OrderProcessingSaga saga = sagaOpt.get();
        
        // No automatic retries - admin must decide to retry manually or cancel order
        log.warn("Payment failed for order: {} - Reason: {}", 
                paymentFailedEvent.getOrderId(), paymentFailedEvent.getReason());
        handleSagaFailure(saga, "Payment failed: " + paymentFailedEvent.getReason());
    }

    /**
     * Handle inventory reserved event (admin confirmed stock availability)
     * NOTE: In admin-driven workflow, inventory check should be part of shipment preparation
     * This handler exists for compatibility but may not be needed
     */
    @Transactional
    public void handleInventoryReserved(InventoryReservedEvent inventoryReservedEvent) {
        log.info("Inventory confirmed for order: {}", inventoryReservedEvent.getOrderId());

        Optional<OrderProcessingSaga> sagaOpt = sagaRepository.findByOrderId(inventoryReservedEvent.getOrderId());
        if (sagaOpt.isEmpty()) {
            log.warn("No saga found for order: {}", inventoryReservedEvent.getOrderId());
            return;
        }

        OrderProcessingSaga saga = sagaOpt.get();
        saga.setInventoryReservationId(inventoryReservedEvent.getReservationId());
        // Note: This handler is for future use if inventory reservation becomes part of workflow
        // Currently admin workflow doesn't include explicit inventory reservation step
        
        saga = sagaRepository.save(saga);
        log.info("Inventory reservation recorded for saga {}", saga.getId());

        // REMOVED automatic shipping preparation - admin must manually prepare shipment
        // Admin will trigger shipping via Admin UI which publishes shipping.prepared event
        
        // Placeholder for potential error handling
        try {
            // Future: Could notify admin of inventory status if needed
        } catch (Exception e) {
            log.error("Failed to start shipping preparation for saga: {}", saga.getId(), e);
            handleSagaFailure(saga, "Failed to start shipping preparation: " + e.getMessage());
        }
    }

    /**
     * Handle inventory reservation failed event
     */
    @Transactional
    public void handleInventoryFailed(InventoryFailedEvent inventoryFailedEvent) {
        log.info("Handling inventory failure for order: {}", inventoryFailedEvent.getOrderId());

        Optional<OrderProcessingSaga> sagaOpt = sagaRepository.findByOrderId(inventoryFailedEvent.getOrderId());
        if (sagaOpt.isEmpty()) {
            log.warn("No saga found for order: {}", inventoryFailedEvent.getOrderId());
            return;
        }

        OrderProcessingSaga saga = sagaOpt.get();
        
        if (saga.canRetry()) {
            log.info("Retrying inventory reservation for saga: {} (attempt {})", saga.getId(), saga.getRetryCount() + 1);
            saga.incrementRetry();
            sagaRepository.save(saga);
            
            try {
                Thread.sleep(1000 * saga.getRetryCount()); // Simple backoff
                reserveInventory(saga);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                handleSagaFailure(saga, "Inventory retry interrupted");
            } catch (Exception e) {
                log.error("Failed to retry inventory reservation for saga: {}", saga.getId(), e);
                handleSagaFailure(saga, "Inventory retry failed: " + e.getMessage());
            }
        } else {
            log.error("Inventory reservation failed for saga: {} after {} attempts", saga.getId(), saga.getRetryCount());
            handleSagaFailure(saga, "Inventory reservation failed: " + inventoryFailedEvent.getReason());
        }
    }

    /**
     * Handle shipping prepared event
     */
    /**
     * Handle shipping prepared event (triggered by admin action via Admin UI)
     * Admin has prepared the shipment - complete the saga
     */
    @Transactional
    public void handleShippingPrepared(ShippingPreparedEvent shippingPreparedEvent) {
        log.info("Admin confirmed shipment prepared for order: {}", shippingPreparedEvent.getOrderId());

        Optional<OrderProcessingSaga> sagaOpt = sagaRepository.findByOrderId(shippingPreparedEvent.getOrderId());
        if (sagaOpt.isEmpty()) {
            log.warn("No saga found for order: {}", shippingPreparedEvent.getOrderId());
            return;
        }

        OrderProcessingSaga saga = sagaOpt.get();
        saga.setShippingId(shippingPreparedEvent.getShippingId());
        saga.markShippingPrepared(); // Sets status to SHIPPING_PREPARED
        saga.setStatus(OrderProcessingSaga.SagaStatus.COMPLETED);
        saga.setCurrentStep(OrderProcessingSaga.ProcessingStep.COMPLETED);
        saga.markCompleted();
        
        sagaRepository.save(saga);
        log.info("Successfully completed saga {} for order: {} - All admin actions completed", 
                saga.getId(), shippingPreparedEvent.getOrderId());
        
        // Saga is complete - order fully processed
    }

    /**
     * Handle shipping failed event
     */
    @Transactional
    public void handleShippingFailed(ShippingFailedEvent shippingFailedEvent) {
        log.info("Handling shipping failure for order: {}", shippingFailedEvent.getOrderId());

        Optional<OrderProcessingSaga> sagaOpt = sagaRepository.findByOrderId(shippingFailedEvent.getOrderId());
        if (sagaOpt.isEmpty()) {
            log.warn("No saga found for order: {}", shippingFailedEvent.getOrderId());
            return;
        }

        OrderProcessingSaga saga = sagaOpt.get();
        
        if (saga.canRetry()) {
            log.info("Retrying shipping preparation for saga: {} (attempt {})", saga.getId(), saga.getRetryCount() + 1);
            saga.incrementRetry();
            sagaRepository.save(saga);
            
            try {
                Thread.sleep(1000 * saga.getRetryCount()); // Simple backoff
                prepareShipping(saga);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                handleSagaFailure(saga, "Shipping retry interrupted");
            } catch (Exception e) {
                log.error("Failed to retry shipping preparation for saga: {}", saga.getId(), e);
                handleSagaFailure(saga, "Shipping retry failed: " + e.getMessage());
            }
        } else {
            log.error("Shipping preparation failed for saga: {} after {} attempts", saga.getId(), saga.getRetryCount());
            handleSagaFailure(saga, "Shipping preparation failed: " + shippingFailedEvent.getReason());
        }
    }
    @Transactional
    public void completeSaga(UUID orderId) {
        log.info("Completing saga for order: {}", orderId);

        Optional<OrderProcessingSaga> sagaOpt = sagaRepository.findByOrderId(orderId);
        if (sagaOpt.isEmpty()) {
            log.warn("No saga found for order: {}", orderId);
            return;
        }

        OrderProcessingSaga saga = sagaOpt.get();
        saga.markCompleted();
        sagaRepository.save(saga);
        
        log.info("Successfully completed saga {} for order: {}", saga.getId(), orderId);
        
        // Publish order completed status via OrderStatusChangedEvent
        daprEventPublisher.publishOrderCompletedStatus(
            saga.getOrderId(),
            saga.getOrderNumber(),
            saga.getCustomerId(),
            saga.getId().toString() // Use saga ID as correlation ID
        );
    }

    /**
     * Handle saga failure and initiate compensation
     */
    @Transactional
    public void handleSagaFailure(OrderProcessingSaga saga, String errorMessage) {
        log.error("Handling saga failure for saga: {} - {}", saga.getId(), errorMessage);

        saga.markFailed(errorMessage);
        saga.setStatus(OrderProcessingSaga.SagaStatus.COMPENSATING);
        sagaRepository.save(saga);

        // Start compensation process
        try {
            compensateSaga(saga);
        } catch (Exception e) {
            log.error("Failed to compensate saga: {}", saga.getId(), e);
            saga.setStatus(OrderProcessingSaga.SagaStatus.CANCELLED);
            sagaRepository.save(saga);
        }
    }

    /**
     * Process payment for the order
     */
    private void processPayment(OrderProcessingSaga saga, OrderCreatedEvent orderEvent) {
        log.info("Processing payment for saga: {}", saga.getId());

        PaymentProcessingEvent paymentEvent = new PaymentProcessingEvent(
                saga.getOrderId(),
                saga.getCustomerId(),
                saga.getTotalAmount(),
                saga.getCurrency(),
                "default", // payment method
                OffsetDateTime.now()
        );

        daprEventPublisher.publishPaymentProcessing(paymentEvent);
    }

    /**
     * Retry payment processing
     */
    private void processPaymentRetry(OrderProcessingSaga saga) {
        log.info("Retrying payment for saga: {}", saga.getId());
        
        PaymentProcessingEvent paymentEvent = new PaymentProcessingEvent(
                saga.getOrderId(),
                saga.getCustomerId(),
                saga.getTotalAmount(),
                saga.getCurrency(),
                "default",
                OffsetDateTime.now()
        );

        daprEventPublisher.publishPaymentProcessing(paymentEvent);
    }

    /**
     * Reserve inventory for the order
     * Note: Order items should be included in the OrderCreatedEvent
     * This maintains event-driven architecture without direct service calls
     */
    private void reserveInventory(OrderProcessingSaga saga) {
        log.info("Reserving inventory for saga: {}", saga.getId());

        // Deserialize order items from saga (stored from OrderCreatedEvent)
        // This maintains event-driven architecture without HTTP calls
        List<InventoryItem> items;
        try {
            if (saga.getOrderItems() != null) {
                items = objectMapper.readValue(
                    saga.getOrderItems(), 
                    objectMapper.getTypeFactory().constructCollectionType(List.class, InventoryItem.class)
                );
            } else {
                log.warn("No order items found in saga {}, using empty list", saga.getId());
                items = java.util.Collections.emptyList();
            }
        } catch (JsonProcessingException e) {
            log.error("Failed to deserialize order items from saga", e);
            throw new RuntimeException("Failed to read order items from saga", e);
        }

        InventoryReservationEvent inventoryEvent = new InventoryReservationEvent(
            saga.getOrderId(),
            items,
            LocalDateTime.now()
        );

        daprEventPublisher.publishInventoryReservation(inventoryEvent);
    }

    /**
     * Prepare shipping for the order
     */
    private void prepareShipping(OrderProcessingSaga saga) {
        log.info("Preparing shipping for saga: {}", saga.getId());

        // In a real implementation, you'd fetch shipping details and prepare shipping
        daprEventPublisher.publishShippingPreparation(saga.getOrderId(), saga.getCustomerId());
    }

    /**
     * Compensate the saga by reversing completed actions
     */
    private void compensateSaga(OrderProcessingSaga saga) {
        log.info("Compensating saga: {}", saga.getId());

        // Reverse actions in reverse order
        if (saga.getShippingId() != null) {
            daprEventPublisher.publishShippingCancellation(saga.getOrderId(), saga.getShippingId());
        }

        if (saga.getInventoryReservationId() != null) {
            daprEventPublisher.publishInventoryRelease(saga.getOrderId(), saga.getInventoryReservationId());
        }

        if (saga.getPaymentId() != null) {
            daprEventPublisher.publishPaymentRefund(saga.getOrderId(), saga.getPaymentId());
        }

        saga.setStatus(OrderProcessingSaga.SagaStatus.COMPENSATED);
        sagaRepository.save(saga);
        
        // Notify Order Service of order failure via OrderStatusChangedEvent
        String failureStep = determineFailureStep(saga);
        daprEventPublisher.publishOrderFailedStatus(
            saga.getOrderId(),
            saga.getOrderNumber(),
            saga.getCustomerId(),
            String.format("Saga compensation completed. Failure at %s: %s", failureStep, saga.getErrorMessage()),
            saga.getId().toString() // Use saga ID as correlation ID
        );
        
        log.info("Completed compensation for saga: {}", saga.getId());
    }

    /**
     * Determine which step failed based on saga state
     */
    private String determineFailureStep(OrderProcessingSaga saga) {
        if (saga.getPaymentId() == null) {
            return "payment";
        } else if (saga.getInventoryReservationId() == null) {
            return "inventory";
        } else if (saga.getShippingId() == null) {
            return "shipping";
        } else {
            return "unknown";
        }
    }

    /**
     * Find and process stuck sagas
     */
    @Transactional
    public List<OrderProcessingSaga> processStuckSagas() {
        log.info("Processing stuck sagas");

        LocalDateTime cutoffTime = LocalDateTime.now().minusMinutes(30);
        // In admin-driven workflow, "stuck" sagas are those awaiting admin action for too long
        // These are not failures, just alerts that admin attention is needed
        List<OrderProcessingSaga.SagaStatus> awaitingStatuses = List.of(
                OrderProcessingSaga.SagaStatus.PENDING_PAYMENT_CONFIRMATION,
                OrderProcessingSaga.SagaStatus.PENDING_SHIPPING_PREPARATION
        );

        List<OrderProcessingSaga> stuckSagas = sagaRepository.findStuckSagas(awaitingStatuses, cutoffTime);
        
        for (OrderProcessingSaga saga : stuckSagas) {
            log.warn("Found saga awaiting admin action for extended period: {} in status: {}", 
                saga.getId(), saga.getStatus());
            
            // In admin workflow, we DON'T retry automatically
            // Just log for monitoring/alerting purposes
            // Admin must take manual action
            
            // Could send notification to admin team here
            // metricsService.recordSagaRequiresAttention(saga);
        }
        
        return stuckSagas;
    }

    /**
     * Retry the current step for stuck saga (DEPRECATED in admin-driven workflow)
     * Kept for potential future use or emergency recovery
     */
    @Deprecated
    private void retryCurrentStep(OrderProcessingSaga saga) {
        log.info("Retry mechanism disabled in admin-driven workflow for saga: {}", saga.getId());
        // Admin must manually intervene - no automatic retries
    }

    /**
     * Handle order status changed event from Order Service
     */
    @Transactional
    public void handleOrderStatusChanged(OrderStatusChangedEvent event) {
        log.info("Handling order status change for order: {} from {} to {}", 
                event.getOrderId(), event.getPreviousStatus(), event.getNewStatus());

        Optional<OrderProcessingSaga> sagaOpt = sagaRepository.findByOrderId(UUID.fromString(event.getOrderId()));
        if (sagaOpt.isEmpty()) {
            log.info("No saga found for order: {}, status change may be external", event.getOrderId());
            return;
        }

        OrderProcessingSaga saga = sagaOpt.get();
        
        // Log status change for audit trail
        log.info("Saga {} - Order status changed: {} -> {} by {} (reason: {})", 
                saga.getId(), event.getPreviousStatus(), event.getNewStatus(), 
                event.getUpdatedBy(), event.getReason());
        
        // Update saga based on new status
        switch (event.getNewStatus().toLowerCase()) {
            case "cancelled":
                handleOrderCancelledFromStatus(saga, event);
                break;
            case "shipped":
                handleOrderShippedFromStatus(saga, event);
                break;
            case "delivered":
                handleOrderDeliveredFromStatus(saga, event);
                break;
            default:
                log.debug("Status change to {} does not require saga update", event.getNewStatus());
        }
    }

    /**
     * Handle order cancelled event from Order Service
     * Triggers compensation/rollback of saga
     */
    @Transactional
    public void handleOrderCancelled(OrderStatusChangedEvent event) {
        log.info("Handling order cancellation for order: {}", event.getOrderId());

        Optional<OrderProcessingSaga> sagaOpt = sagaRepository.findByOrderId(UUID.fromString(event.getOrderId()));
        if (sagaOpt.isEmpty()) {
            log.warn("No saga found for cancelled order: {}", event.getOrderId());
            return;
        }

        OrderProcessingSaga saga = sagaOpt.get();
        
        // Check if saga is already compensating or compensated
        if (saga.getStatus() == OrderProcessingSaga.SagaStatus.COMPENSATING ||
            saga.getStatus() == OrderProcessingSaga.SagaStatus.COMPENSATED) {
            log.info("Saga {} is already being compensated", saga.getId());
            return;
        }

        log.info("Initiating compensation for cancelled order: {}", event.getOrderId());
        saga.setStatus(OrderProcessingSaga.SagaStatus.COMPENSATING);
        saga.setErrorMessage("Order cancelled: " + (event.getReason() != null ? event.getReason() : "User requested"));
        sagaRepository.save(saga);

        // Start compensation process
        try {
            compensateSaga(saga);
            metricsService.recordSagaCancelled(event.getOrderNumber());
        } catch (Exception e) {
            log.error("Failed to compensate saga {} for cancelled order: {}", saga.getId(), event.getOrderId(), e);
            saga.setStatus(OrderProcessingSaga.SagaStatus.CANCELLED);
            saga.setErrorMessage("Compensation failed: " + e.getMessage());
            sagaRepository.save(saga);
        }
    }

    /**
     * Handle order shipped event from Order Service
     * Updates saga to reflect shipping has been completed
     */
    @Transactional
    public void handleOrderShipped(OrderStatusChangedEvent event) {
        log.info("Handling order shipped for order: {}", event.getOrderId());

        Optional<OrderProcessingSaga> sagaOpt = sagaRepository.findByOrderId(UUID.fromString(event.getOrderId()));
        if (sagaOpt.isEmpty()) {
            log.info("No saga found for shipped order: {}, may have been completed already", event.getOrderId());
            return;
        }

        OrderProcessingSaga saga = sagaOpt.get();
        
        // Update saga to reflect shipping is complete
        if (saga.getStatus() != OrderProcessingSaga.SagaStatus.COMPLETED) {
            saga.setStatus(OrderProcessingSaga.SagaStatus.COMPLETED);
            saga.setCurrentStep(OrderProcessingSaga.ProcessingStep.COMPLETED);
            sagaRepository.save(saga);
            
            log.info("Updated saga {} to COMPLETED due to order shipment", saga.getId());
            metricsService.recordSagaCompleted(event.getOrderNumber());
        }
    }

    /**
     * Handle order delivered event from Order Service
     * Marks saga as fully completed and ready for archival
     */
    @Transactional
    public void handleOrderDelivered(OrderStatusChangedEvent event) {
        log.info("Handling order delivered for order: {}", event.getOrderId());

        Optional<OrderProcessingSaga> sagaOpt = sagaRepository.findByOrderId(UUID.fromString(event.getOrderId()));
        if (sagaOpt.isEmpty()) {
            log.info("No saga found for delivered order: {}, may have been completed already", event.getOrderId());
            return;
        }

        OrderProcessingSaga saga = sagaOpt.get();
        
        // Mark saga as completed if not already
        if (saga.getStatus() != OrderProcessingSaga.SagaStatus.COMPLETED) {
            saga.setStatus(OrderProcessingSaga.SagaStatus.COMPLETED);
            saga.setCurrentStep(OrderProcessingSaga.ProcessingStep.COMPLETED);
            saga.markCompleted();
            sagaRepository.save(saga);
            
            log.info("Marked saga {} as COMPLETED due to order delivery", saga.getId());
            metricsService.recordSagaCompleted(event.getOrderNumber());
        }
        
        // Saga can now be archived or cleaned up
        log.info("Saga {} for delivered order {} is complete and can be archived", 
                saga.getId(), event.getOrderId());
    }

    /**
     * Handle order deleted event from Order Service
     * Cleans up saga record if it exists
     */
    @Transactional
    public void handleOrderDeleted(OrderDeletedEvent event) {
        log.info("Handling order deletion for order: {}", event.getOrderId());

        Optional<OrderProcessingSaga> sagaOpt = sagaRepository.findByOrderId(UUID.fromString(event.getOrderId()));
        if (sagaOpt.isEmpty()) {
            log.info("No saga found for deleted order: {}", event.getOrderId());
            return;
        }

        OrderProcessingSaga saga = sagaOpt.get();
        
        // If saga is in progress, trigger compensation first
        if (saga.getStatus() == OrderProcessingSaga.SagaStatus.PENDING_PAYMENT_CONFIRMATION ||
            saga.getStatus() == OrderProcessingSaga.SagaStatus.PAYMENT_CONFIRMED ||
            saga.getStatus() == OrderProcessingSaga.SagaStatus.PENDING_SHIPPING_PREPARATION) {
            
            log.warn("Saga {} is in progress, compensating before deletion", saga.getId());
            saga.setStatus(OrderProcessingSaga.SagaStatus.COMPENSATING);
            saga.setErrorMessage("Order deleted: " + (event.getReason() != null ? event.getReason() : "User requested"));
            sagaRepository.save(saga);
            
            try {
                compensateSaga(saga);
            } catch (Exception e) {
                log.error("Failed to compensate saga {} before deletion", saga.getId(), e);
            }
        }
        
        // Archive or delete saga record
        log.info("Deleting saga {} for deleted order {}", saga.getId(), event.getOrderId());
        sagaRepository.delete(saga);
        
        metricsService.recordSagaDeleted(event.getOrderNumber());
    }

    /**
     * Helper method to handle status change to cancelled
     */
    private void handleOrderCancelledFromStatus(OrderProcessingSaga saga, OrderStatusChangedEvent event) {
        if (saga.getStatus() != OrderProcessingSaga.SagaStatus.COMPENSATING &&
            saga.getStatus() != OrderProcessingSaga.SagaStatus.COMPENSATED) {
            
            saga.setStatus(OrderProcessingSaga.SagaStatus.COMPENSATING);
            saga.setErrorMessage("Order cancelled via status change: " + event.getReason());
            sagaRepository.save(saga);
            
            try {
                compensateSaga(saga);
            } catch (Exception e) {
                log.error("Failed to compensate saga {} after status change to cancelled", saga.getId(), e);
            }
        }
    }

    /**
     * Helper method to handle status change to shipped
     */
    private void handleOrderShippedFromStatus(OrderProcessingSaga saga, OrderStatusChangedEvent event) {
        if (saga.getStatus() != OrderProcessingSaga.SagaStatus.COMPLETED) {
            saga.setStatus(OrderProcessingSaga.SagaStatus.COMPLETED);
            saga.setCurrentStep(OrderProcessingSaga.ProcessingStep.COMPLETED);
            sagaRepository.save(saga);
        }
    }

    /**
     * Helper method to handle status change to delivered
     */
    private void handleOrderDeliveredFromStatus(OrderProcessingSaga saga, OrderStatusChangedEvent event) {
        if (saga.getStatus() != OrderProcessingSaga.SagaStatus.COMPLETED) {
            saga.setStatus(OrderProcessingSaga.SagaStatus.COMPLETED);
            saga.setCurrentStep(OrderProcessingSaga.ProcessingStep.COMPLETED);
            saga.markCompleted();
            sagaRepository.save(saga);
        }
    }
}
