package com.xshopai.orderprocessor.model.events;

import lombok.Data;

import java.math.BigDecimal;

/**
 * Order item event data
 * Matches the schema from the .NET Order Service
 */
@Data
public class OrderItemEvent {
    private String productId;
    private String productName;
    private Integer quantity;
    private BigDecimal unitPrice;
    private BigDecimal totalPrice;
}
