package com.xshopai.orderprocessor.model.events;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Event published during compensation when shipping is cancelled
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ShippingCancellationEvent {
    private UUID orderId;
    private String shippingId;
    private String reason;
    
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'")
    private LocalDateTime cancelledAt;
}
