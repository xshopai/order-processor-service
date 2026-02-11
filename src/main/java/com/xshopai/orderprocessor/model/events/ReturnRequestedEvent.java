package com.xshopai.orderprocessor.model.events;

import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/**
 * Event published when a return is requested
 */
@Data
public class ReturnRequestedEvent {
    private UUID returnId;
    private UUID orderId;
    private String orderNumber;
    private String customerId;
    private String customerEmail;
    private String customerName;
    private String returnNumber;
    private String reason; // ReturnReason enum value as string
    private String description;
    private BigDecimal totalRefund;
    private String currency;
    private List<ReturnItemEvent> items;
    private LocalDateTime requestedAt;
    private String correlationId;
}
