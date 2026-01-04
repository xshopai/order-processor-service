package com.xshopai.orderprocessor.model.events;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/**
 * Event published when inventory reservation is requested
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class InventoryReservationEvent {
    private UUID orderId;
    private List<InventoryItem> items;
    
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'")
    private LocalDateTime requestedAt;
    
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class InventoryItem {
        private String productId;
        private Integer quantity;
    }
}
