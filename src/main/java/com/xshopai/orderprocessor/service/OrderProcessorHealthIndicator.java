package com.xshopai.orderprocessor.service;

import com.xshopai.orderprocessor.model.entity.OrderProcessingSaga;
import com.xshopai.orderprocessor.repository.OrderProcessingSagaRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Custom health check service for order processor
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class OrderProcessorHealthIndicator {

    private final OrderProcessingSagaRepository sagaRepository;

    /**
     * Get health status of the order processor
     */
    public Map<String, Object> getHealthStatus() {
        try {
            Map<String, Object> details = new HashMap<>();
            
            // Check saga statistics
            long totalSagas = sagaRepository.count();
            // In admin-driven workflow, active means awaiting admin action
            long activeSagas = sagaRepository.countByStatusIn(List.of(
                OrderProcessingSaga.SagaStatus.PENDING_PAYMENT_CONFIRMATION,
                OrderProcessingSaga.SagaStatus.PAYMENT_CONFIRMED,
                OrderProcessingSaga.SagaStatus.PENDING_SHIPPING_PREPARATION
            ));
            long cancelledSagas = sagaRepository.countByStatus(OrderProcessingSaga.SagaStatus.CANCELLED);
            long completedSagas = sagaRepository.countByStatus(OrderProcessingSaga.SagaStatus.COMPLETED);
            
            details.put("totalSagas", totalSagas);
            details.put("activeSagas", activeSagas);
            details.put("cancelledSagas", cancelledSagas);
            details.put("completedSagas", completedSagas);
            
            // Check for sagas awaiting admin action for extended period
            LocalDateTime cutoffTime = LocalDateTime.now().minusMinutes(30);
            List<OrderProcessingSaga.SagaStatus> awaitingStatuses = List.of(
                OrderProcessingSaga.SagaStatus.PENDING_PAYMENT_CONFIRMATION,
                OrderProcessingSaga.SagaStatus.PENDING_SHIPPING_PREPARATION
            );
            
            long stuckSagas = sagaRepository.countStuckSagas(awaitingStatuses, cutoffTime);
            details.put("stuckSagas", stuckSagas);
            
            // Determine health status
            if (stuckSagas > 10) {
                details.put("status", "DOWN");
                details.put("reason", "Too many stuck sagas");
            } else if (stuckSagas > 5) {
                details.put("status", "DEGRADED");
                details.put("reason", "Some sagas are stuck");
            } else {
                details.put("status", "UP");
                details.put("reason", "All systems operational");
            }
            
            return details;
            
        } catch (Exception e) {
            log.error("Health check failed", e);
            Map<String, Object> errorDetails = new HashMap<>();
            errorDetails.put("status", "DOWN");
            errorDetails.put("reason", "Health check failed: " + e.getMessage());
            return errorDetails;
        }
    }
}
