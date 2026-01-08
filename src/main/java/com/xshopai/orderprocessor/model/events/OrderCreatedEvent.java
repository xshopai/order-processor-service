package com.xshopai.orderprocessor.model.events;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

/**
 * Event published when an order is created
 * Matches the schema from the .NET Order Service
 */
@Data
public class OrderCreatedEvent {
    private UUID orderId;
    private String correlationId;
    private String customerId;
    private String orderNumber;
    private BigDecimal totalAmount;
    private String currency;
    
    private OffsetDateTime createdAt;
    
    private List<OrderItemEvent> items;
    private AddressEvent shippingAddress;
    private AddressEvent billingAddress;
}
