package com.xshopai.orderprocessor.controller;

import com.xshopai.orderprocessor.model.entity.OrderProcessingSaga;
import com.xshopai.orderprocessor.repository.OrderProcessingSagaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.UUID;

/**
 * Admin API endpoints for the Order Processor Service
 */
@RestController
@RequestMapping("/api/v1/admin/sagas")
@RequiredArgsConstructor
public class AdminController {

    private final OrderProcessingSagaRepository sagaRepository;

    /**
     * Get all sagas with pagination
     */
    @GetMapping
    public ResponseEntity<Page<OrderProcessingSaga>> getAllSagas(Pageable pageable) {
        return ResponseEntity.ok(sagaRepository.findAll(pageable));
    }

    /**
     * Get saga by ID
     */
    @GetMapping("/{id}")
    public ResponseEntity<OrderProcessingSaga> getSagaById(@PathVariable UUID id) {
        return sagaRepository.findById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    /**
     * Get saga by order ID
     */
    @GetMapping("/order/{orderId}")
    public ResponseEntity<OrderProcessingSaga> getSagaByOrderId(@PathVariable UUID orderId) {
        return sagaRepository.findByOrderId(orderId)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    /**
     * Get saga counts by status
     */
    @GetMapping("/stats")
    public ResponseEntity<Map<String, Long>> getSagaStats() {
        Map<String, Long> stats = Map.of(
            "CREATED", sagaRepository.countByStatus(OrderProcessingSaga.SagaStatus.CREATED),
            "PENDING_PAYMENT_CONFIRMATION", sagaRepository.countByStatus(OrderProcessingSaga.SagaStatus.PENDING_PAYMENT_CONFIRMATION),
            "PAYMENT_CONFIRMED", sagaRepository.countByStatus(OrderProcessingSaga.SagaStatus.PAYMENT_CONFIRMED),
            "PENDING_SHIPPING_PREPARATION", sagaRepository.countByStatus(OrderProcessingSaga.SagaStatus.PENDING_SHIPPING_PREPARATION),
            "SHIPPING_PREPARED", sagaRepository.countByStatus(OrderProcessingSaga.SagaStatus.SHIPPING_PREPARED),
            "COMPLETED", sagaRepository.countByStatus(OrderProcessingSaga.SagaStatus.COMPLETED),
            "CANCELLED", sagaRepository.countByStatus(OrderProcessingSaga.SagaStatus.CANCELLED),
            "COMPENSATING", sagaRepository.countByStatus(OrderProcessingSaga.SagaStatus.COMPENSATING),
            "COMPENSATED", sagaRepository.countByStatus(OrderProcessingSaga.SagaStatus.COMPENSATED)
        );
        
        return ResponseEntity.ok(stats);
    }
    /**
     * Get Dapr info
     */
    @GetMapping("/dapr-info")
    public ResponseEntity<Map<String, Object>> getDaprInfo() {
        Map<String, Object> info = Map.of(
            "provider", "Dapr",
            "pubsubComponent", "order-processor-pubsub",
            "healthy", true
        );
        
        return ResponseEntity.ok(info);
    }
}
