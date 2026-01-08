package com.xshopai.orderprocessor.model.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Order Processing Saga Entity
 * Tracks the state of order processing across multiple services
 */
@Entity
@Table(name = "order_processing_saga")
@Data
@NoArgsConstructor
public class OrderProcessingSaga {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "order_id", nullable = false, unique = true)
    private UUID orderId;

    @Column(name = "customer_id", nullable = false)
    private String customerId;

    @Column(name = "order_number", nullable = false)
    private String orderNumber;

    @Column(name = "total_amount", nullable = false, precision = 10, scale = 2)
    private BigDecimal totalAmount;

    @Column(name = "currency", nullable = false)
    private String currency = "USD";

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private SagaStatus status = SagaStatus.CREATED;

    @Enumerated(EnumType.STRING)
    @Column(name = "current_step", nullable = false)
    private ProcessingStep currentStep = ProcessingStep.AWAITING_PAYMENT;

    @Column(name = "payment_id")
    private String paymentId;

    @Column(name = "inventory_reservation_id")
    private String inventoryReservationId;

    @Column(name = "shipping_id")
    private String shippingId;

    @Column(name = "order_items", columnDefinition = "jsonb")
    @JdbcTypeCode(SqlTypes.JSON)
    private String orderItems;

    @Column(name = "shipping_address", columnDefinition = "jsonb")
    @JdbcTypeCode(SqlTypes.JSON)
    private String shippingAddress;

    @Column(name = "billing_address", columnDefinition = "jsonb")
    @JdbcTypeCode(SqlTypes.JSON)
    private String billingAddress;

    @Column(name = "error_message")
    private String errorMessage;

    @Column(name = "retry_count", nullable = false)
    private Integer retryCount = 0;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt = LocalDateTime.now();

    @Column(name = "completed_at")
    private LocalDateTime completedAt;

    public enum SagaStatus {
        CREATED,                        // Order created, saga initiated
        PENDING_PAYMENT_CONFIRMATION,   // Waiting for admin to confirm payment received
        PAYMENT_CONFIRMED,              // Admin confirmed payment received
        PENDING_SHIPPING_PREPARATION,   // Waiting for admin to prepare shipment
        SHIPPING_PREPARED,              // Admin prepared shipment
        COMPLETED,                      // Order fully processed
        CANCELLED,                      // Order cancelled by admin
        COMPENSATING,                   // Rolling back due to cancellation
        COMPENSATED                     // Rollback completed
    }

    public enum ProcessingStep {
        AWAITING_PAYMENT,               // Admin needs to confirm payment
        AWAITING_SHIPMENT,              // Admin needs to prepare shipment
        COMPLETED                       // All steps done
    }

    @PreUpdate
    public void preUpdate() {
        this.updatedAt = LocalDateTime.now();
    }

    public boolean isCompleted() {
        return status == SagaStatus.COMPLETED;
    }

    public boolean isFailed() {
        return status == SagaStatus.CANCELLED || status == SagaStatus.COMPENSATED;
    }

    public boolean canRetry() {
        // No automatic retries in admin-driven workflow
        return false;
    }

    private boolean isProcessingStep() {
        return status == SagaStatus.PENDING_PAYMENT_CONFIRMATION ||
               status == SagaStatus.PENDING_SHIPPING_PREPARATION;
    }

    public void incrementRetry() {
        this.retryCount++;
    }

    public void markCompleted() {
        this.status = SagaStatus.COMPLETED;
        this.completedAt = LocalDateTime.now();
        this.currentStep = ProcessingStep.COMPLETED;
    }

    public void markFailed(String errorMessage) {
        this.status = SagaStatus.CANCELLED;
        this.errorMessage = errorMessage;
    }

    public void markPaymentConfirmed() {
        this.status = SagaStatus.PAYMENT_CONFIRMED;
        this.currentStep = ProcessingStep.AWAITING_SHIPMENT;
    }

    public void markShippingPrepared() {
        this.status = SagaStatus.SHIPPING_PREPARED;
    }
}
