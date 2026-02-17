package com.xshopai.orderprocessor.model.events;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

import java.math.BigDecimal;

/**
 * Order item event data
 * Matches the schema from the .NET Order Service
 */
@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class OrderItemEvent {
    private String productId;
    private String productName;
    
    /**
     * SKU for inventory tracking - maps to ProductSku in Order Service
     */
    private String sku;
    
    private Integer quantity;
    private BigDecimal unitPrice;
    private BigDecimal totalPrice;
}
