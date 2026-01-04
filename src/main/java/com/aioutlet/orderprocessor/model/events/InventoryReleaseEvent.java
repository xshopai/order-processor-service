package com.xshopai.orderprocessor.model.events;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * Event published during compensation when inventory is released
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class InventoryReleaseEvent {
    private UUID orderId;
    private String reservationId;
    private String reason;
    
    private OffsetDateTime releasedAt;
}
