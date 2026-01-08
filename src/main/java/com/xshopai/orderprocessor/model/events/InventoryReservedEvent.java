package com.xshopai.orderprocessor.model.events;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/**
 * Event published when inventory reservation succeeds
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class InventoryReservedEvent {
    private UUID orderId;
    private String reservationId;
    private List<String> productIds;
    private String correlationId;
    
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'")
    private LocalDateTime reservedAt;
}
