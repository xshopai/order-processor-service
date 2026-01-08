package com.xshopai.orderprocessor.model.events;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * Event fired when an order's status changes
 * Used for order lifecycle events: updated, cancelled, shipped, delivered
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class OrderStatusChangedEvent implements Serializable {
    
    private static final long serialVersionUID = 1L;
    
    /**
     * Order unique identifier
     */
    private String orderId;
    
    /**
     * Order number (user-friendly identifier)
     */
    private String orderNumber;
    
    /**
     * Customer unique identifier
     */
    private String customerId;
    
    /**
     * Previous order status
     */
    private String previousStatus;
    
    /**
     * New order status
     */
    private String newStatus;
    
    /**
     * When the status was changed
     */
    private LocalDateTime updatedAt;
    
    /**
     * Who updated the status (user ID or system)
     */
    private String updatedBy;
    
    /**
     * Optional reason or notes for the status change
     */
    private String reason;
    
    /**
     * Correlation ID for tracing
     */
    private String correlationId;
}
