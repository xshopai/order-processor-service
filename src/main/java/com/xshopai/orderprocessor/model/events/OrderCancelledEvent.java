package com.xshopai.orderprocessor.model.events;

import lombok.Data;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Event published when an order is cancelled
 * Maps to OrderService.Core.Models.Events.OrderCancelledEvent
 */
@Data
public class OrderCancelledEvent {
    private String orderId;
    private String correlationId;
    private String customerId;
    private String orderNumber;
    private String cancellationReason;
    private String cancelledBy;
    private LocalDateTime cancelledAt;
    
    // Financial information for refund processing
    private Double totalAmount;
    private String currency;
    private String paymentTransactionId;
    private String paymentProvider;
    
    // Items to return to inventory
    private List<OrderItemEvent> items;
    
    // Saga compensation tracking
    private Boolean requiresPaymentRefund;
    private Boolean requiresInventoryRelease;
}
