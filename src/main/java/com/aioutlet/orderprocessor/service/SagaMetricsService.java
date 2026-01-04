package com.xshopai.orderprocessor.service;

import com.xshopai.orderprocessor.model.entity.OrderProcessingSaga;
import com.xshopai.orderprocessor.repository.OrderProcessingSagaRepository;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Service for collecting and exposing metrics about saga processing
 */
@Service
@Slf4j
public class SagaMetricsService {

    private final OrderProcessingSagaRepository sagaRepository;
    private final MeterRegistry meterRegistry;
    
    private final AtomicLong activeSagasGauge = new AtomicLong(0);
    private final AtomicLong completedSagasGauge = new AtomicLong(0);
    private final AtomicLong failedSagasGauge = new AtomicLong(0);
    private final AtomicLong stuckSagasGauge = new AtomicLong(0);

    public SagaMetricsService(OrderProcessingSagaRepository sagaRepository, MeterRegistry meterRegistry) {
        this.sagaRepository = sagaRepository;
        this.meterRegistry = meterRegistry;
        
        // Register gauges
        Gauge.builder("saga.active.count", activeSagasGauge, AtomicLong::get)
            .description("Number of active sagas")
            .register(meterRegistry);
            
        Gauge.builder("saga.completed.count", completedSagasGauge, AtomicLong::get)
            .description("Number of completed sagas")
            .register(meterRegistry);
            
        Gauge.builder("saga.failed.count", failedSagasGauge, AtomicLong::get)
            .description("Number of failed sagas")
            .register(meterRegistry);
            
        Gauge.builder("saga.stuck.count", stuckSagasGauge, AtomicLong::get)
            .description("Number of stuck sagas")
            .register(meterRegistry);
    }

    /**
     * Record saga started event
     */
    public void recordSagaStarted(String orderNumber) {
        Counter.builder("saga.started.total")
            .description("Total number of sagas started")
            .tag("order_number", orderNumber)
            .register(meterRegistry)
            .increment();
        
        updateGauges();
    }

    /**
     * Record saga completed event
     */
    public void recordSagaCompleted(String orderNumber, Duration processingTime) {
        Counter.builder("saga.completed.total")
            .description("Total number of sagas completed")
            .tag("order_number", orderNumber)
            .register(meterRegistry)
            .increment();
            
        Timer.builder("saga.processing.duration")
            .description("Time taken to process saga")
            .tag("outcome", "completed")
            .register(meterRegistry)
            .record(processingTime);
        
        updateGauges();
    }

    /**
     * Record saga failed event
     */
    public void recordSagaFailed(String orderNumber, String failureReason, Duration processingTime) {
        Counter.builder("saga.failed.total")
            .description("Total number of sagas failed")
            .tag("order_number", orderNumber)
            .tag("failure_reason", failureReason)
            .register(meterRegistry)
            .increment();
            
        Timer.builder("saga.processing.duration")
            .description("Time taken to process saga")
            .tag("outcome", "failed")
            .register(meterRegistry)
            .record(processingTime);
        
        updateGauges();
    }

    /**
     * Record payment processing event
     */
    public void recordPaymentProcessing(String orderNumber) {
        Counter.builder("saga.payment.processing.total")
            .description("Total number of payment processing events")
            .tag("order_number", orderNumber)
            .register(meterRegistry)
            .increment();
    }

    /**
     * Record inventory processing event
     */
    public void recordInventoryProcessing(String orderNumber) {
        Counter.builder("saga.inventory.processing.total")
            .description("Total number of inventory processing events")
            .tag("order_number", orderNumber)
            .register(meterRegistry)
            .increment();
    }

    /**
     * Record shipping processing event
     */
    public void recordShippingProcessing(String orderNumber) {
        Counter.builder("saga.shipping.processing.total")
            .description("Total number of shipping processing events")
            .tag("order_number", orderNumber)
            .register(meterRegistry)
            .increment();
    }

    /**
     * Record retry event
     */
    public void recordRetry(String orderNumber, String step, int retryCount) {
        Counter.builder("saga.retry.total")
            .description("Total number of saga retries")
            .tag("order_number", orderNumber)
            .tag("step", step)
            .tag("retry_count", String.valueOf(retryCount))
            .register(meterRegistry)
            .increment();
    }

    /**
     * Record compensation event
     */
    public void recordCompensation(String orderNumber, String step) {
        Counter.builder("saga.compensation.total")
            .description("Total number of compensation actions")
            .tag("order_number", orderNumber)
            .tag("step", step)
            .register(meterRegistry)
            .increment();
    }

    /**
     * Record saga completed event (without processing time)
     */
    public void recordSagaCompleted(String orderNumber) {
        Counter.builder("saga.completed.total")
            .description("Total number of sagas completed")
            .tag("order_number", orderNumber)
            .register(meterRegistry)
            .increment();
        
        updateGauges();
    }

    /**
     * Record saga cancelled event
     */
    public void recordSagaCancelled(String orderNumber) {
        Counter.builder("saga.cancelled.total")
            .description("Total number of sagas cancelled")
            .tag("order_number", orderNumber)
            .register(meterRegistry)
            .increment();
        
        updateGauges();
    }

    /**
     * Record saga deleted event
     */
    public void recordSagaDeleted(String orderNumber) {
        Counter.builder("saga.deleted.total")
            .description("Total number of sagas deleted")
            .tag("order_number", orderNumber)
            .register(meterRegistry)
            .increment();
        
        updateGauges();
    }

    /**
     * Update gauge metrics
     */
    public void updateGauges() {
        try {
            // In admin-driven workflow, active means awaiting admin action
            List<OrderProcessingSaga.SagaStatus> activeStatuses = List.of(
                OrderProcessingSaga.SagaStatus.PENDING_PAYMENT_CONFIRMATION,
                OrderProcessingSaga.SagaStatus.PAYMENT_CONFIRMED,
                OrderProcessingSaga.SagaStatus.PENDING_SHIPPING_PREPARATION
            );
            
            long activeSagas = sagaRepository.countByStatusIn(activeStatuses);
            long completedSagas = sagaRepository.countByStatus(OrderProcessingSaga.SagaStatus.COMPLETED);
            long cancelledSagas = sagaRepository.countByStatus(OrderProcessingSaga.SagaStatus.CANCELLED);
            
            LocalDateTime cutoffTime = LocalDateTime.now().minusMinutes(30);
            long stuckSagas = sagaRepository.countStuckSagas(activeStatuses, cutoffTime);
            
            activeSagasGauge.set(activeSagas);
            completedSagasGauge.set(completedSagas);
            failedSagasGauge.set(cancelledSagas); // Using existing gauge name, but tracking cancelled
            stuckSagasGauge.set(stuckSagas);
            
        } catch (Exception e) {
            log.error("Failed to update saga metrics", e);
        }
    }
}
