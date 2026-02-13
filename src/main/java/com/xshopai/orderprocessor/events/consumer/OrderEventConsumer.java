package com.xshopai.orderprocessor.events.consumer;

import io.dapr.client.domain.CloudEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import com.xshopai.orderprocessor.model.events.OrderCreatedEvent;
import com.xshopai.orderprocessor.model.events.OrderCancelledEvent;
import com.xshopai.orderprocessor.model.events.ReturnStatusChangedEvent;
import com.xshopai.orderprocessor.service.SagaOrchestratorService;

/**
 * Order Event Consumer
 * Handles order-related events from Dapr pub/sub
 */
@RestController
@RequestMapping("/dapr/events")
@RequiredArgsConstructor
@Slf4j
public class OrderEventConsumer {

    private final SagaOrchestratorService sagaOrchestratorService;

    /**
     * Handle order.placed event
     */
    @PostMapping("/order-created")
    public ResponseEntity<Void> handleOrderCreated(@RequestBody CloudEvent<OrderCreatedEvent> cloudEvent) {
        try {
            log.info("Received order.placed event: {}", cloudEvent.getId());
            OrderCreatedEvent event = cloudEvent.getData();
            sagaOrchestratorService.startOrderProcessingSaga(event);
            return ResponseEntity.ok().build();
        } catch (Exception e) {
            log.error("Error handling order.placed event", e);
            return ResponseEntity.status(500).build();
        }
    }

    /**
     * Handle order.cancelled event
     */
    @PostMapping("/order-cancelled")
    public ResponseEntity<Void> handleOrderCancelled(@RequestBody CloudEvent<OrderCancelledEvent> cloudEvent) {
        try {
            log.info("Received order.cancelled event: {}", cloudEvent.getId());
            OrderCancelledEvent event = cloudEvent.getData();
            sagaOrchestratorService.handleOrderCancelled(event);
            return ResponseEntity.ok().build();
        } catch (Exception e) {
            log.error("Error handling order.cancelled event", e);
            return ResponseEntity.status(500).build();
        }
    }

    /**
     * Handle return.approved event
     */
    @PostMapping("/return-approved")
    public ResponseEntity<Void> handleReturnApproved(@RequestBody CloudEvent<ReturnStatusChangedEvent> cloudEvent) {
        try {
            log.info("Received return.approved event: {}", cloudEvent.getId());
            ReturnStatusChangedEvent event = cloudEvent.getData();
            sagaOrchestratorService.handleReturnApproved(event);
            return ResponseEntity.ok().build();
        } catch (Exception e) {
            log.error("Error handling return.approved event", e);
            return ResponseEntity.status(500).build();
        }
    }

    /**
     * Handle return.completed event
     */
    @PostMapping("/return-completed")
    public ResponseEntity<Void> handleReturnCompleted(@RequestBody CloudEvent<ReturnStatusChangedEvent> cloudEvent) {
        try {
            log.info("Received return.completed event: {}", cloudEvent.getId());
            ReturnStatusChangedEvent event = cloudEvent.getData();
            sagaOrchestratorService.handleReturnCompleted(event);
            return ResponseEntity.ok().build();
        } catch (Exception e) {
            log.error("Error handling return.completed event", e);
            return ResponseEntity.status(500).build();
        }
    }
}
