package com.xshopai.orderprocessor.model.events;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * Event fired when an order is deleted
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class OrderDeletedEvent implements Serializable {
    
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
     * When the order was deleted
     */
    private LocalDateTime deletedAt;
    
    /**
     * Who deleted the order (user ID or system)
     */
    private String deletedBy;
    
    /**
     * Optional reason for deletion
     */
    private String reason;
    
    /**
     * Correlation ID for tracing
     */
    private String correlationId;
}
