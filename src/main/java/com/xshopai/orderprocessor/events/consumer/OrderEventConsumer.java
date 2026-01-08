package com.xshopai.orderprocessor.events.consumer;

import io.dapr.client.domain.CloudEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import com.xshopai.orderprocessor.model.events.OrderCreatedEvent;
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
     * Handle order.created event
     */
    @PostMapping("/order-created")
    public ResponseEntity<Void> handleOrderCreated(@RequestBody CloudEvent<OrderCreatedEvent> cloudEvent) {
        try {
            log.info("Received order.created event: {}", cloudEvent.getId());
            OrderCreatedEvent event = cloudEvent.getData();
            sagaOrchestratorService.startOrderProcessingSaga(event);
            return ResponseEntity.ok().build();
        } catch (Exception e) {
            log.error("Error handling order.created event", e);
            return ResponseEntity.status(500).build();
        }
    }
}
