package com.xshopai.orderprocessor.model.events;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * Event published when payment processing is requested
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class PaymentProcessingEvent {
    private UUID orderId;
    private String customerId;
    private BigDecimal amount;
    private String currency;
    private String paymentMethod;
    
    private OffsetDateTime requestedAt;
}
