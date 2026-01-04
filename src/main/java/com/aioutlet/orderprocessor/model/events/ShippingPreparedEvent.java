package com.xshopai.orderprocessor.model.events;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Event published when shipping is successfully prepared
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ShippingPreparedEvent {
    private UUID orderId;
    private String shippingId;
    private String trackingNumber;
    private String carrierName;
    private String shippingMethod;
    private String correlationId;
    
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'")
    private LocalDateTime preparedAt;
}
