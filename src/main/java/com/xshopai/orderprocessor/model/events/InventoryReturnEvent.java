package com.xshopai.orderprocessor.model.events;

import lombok.Data;
import java.util.List;
import java.util.UUID;

/**
 * Event published when items need to be returned to inventory
 */
@Data
public class InventoryReturnEvent {
    private UUID returnId;
    private UUID orderId;
    private String returnNumber;
    private List<ReturnItemEvent> items;
    private String correlationId;
}
