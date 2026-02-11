package com.xshopai.orderprocessor.model.events;

import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/**
 * Event published when a return status changes (approved, rejected, completed, refund processed)
 */
@Data
public class ReturnStatusChangedEvent {
    private UUID returnId;
    private String returnNumber;
    private UUID orderId;
    private String customerId;
    private String status; // ReturnStatus enum value as string
    private BigDecimal totalRefund;
    private String currency;
    private List<ReturnItemEvent> items;
    private LocalDateTime updatedAt;
    private String correlationId;
}
