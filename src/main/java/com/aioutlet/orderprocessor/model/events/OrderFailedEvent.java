package com.xshopai.orderprocessor.model.events;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Event published when the entire order processing saga fails
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class OrderFailedEvent {
    private UUID orderId;
    private String reason;
    private String failureStep; // payment, inventory, shipping
    private String errorCode;
    
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'")
    private LocalDateTime failedAt;
}
