package com.xshopai.orderprocessor.model.events;

import lombok.Data;
import java.math.BigDecimal;
import java.util.UUID;

/**
 * Return item information for events
 */
@Data
public class ReturnItemEvent {
    private UUID productId;
    private String productName;
    private int quantityToReturn;
    private BigDecimal refundAmount;
}
