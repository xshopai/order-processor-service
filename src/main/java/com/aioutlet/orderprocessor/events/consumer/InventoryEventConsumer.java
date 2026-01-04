package com.xshopai.orderprocessor.events.consumer;

import io.dapr.client.domain.CloudEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import com.xshopai.orderprocessor.model.events.InventoryReservedEvent;
import com.xshopai.orderprocessor.model.events.InventoryFailedEvent;
import com.xshopai.orderprocessor.service.SagaOrchestratorService;

/**
 * Inventory Event Consumer
 * Handles inventory-related events from Dapr pub/sub
 */
@RestController
@RequestMapping("/dapr/events")
@RequiredArgsConstructor
@Slf4j
public class InventoryEventConsumer {

    private final SagaOrchestratorService sagaOrchestratorService;

    /**
     * Handle inventory.reserved event
     */
    @PostMapping("/inventory-reserved")
    public ResponseEntity<Void> handleInventoryReserved(@RequestBody CloudEvent<InventoryReservedEvent> cloudEvent) {
        try {
            log.info("Received inventory.reserved event: {}", cloudEvent.getId());
            InventoryReservedEvent event = cloudEvent.getData();
            sagaOrchestratorService.handleInventoryReserved(event);
            return ResponseEntity.ok().build();
        } catch (Exception e) {
            log.error("Error handling inventory.reserved event", e);
            return ResponseEntity.status(500).build();
        }
    }

    /**
     * Handle inventory.failed event
     */
    @PostMapping("/inventory-failed")
    public ResponseEntity<Void> handleInventoryFailed(@RequestBody CloudEvent<InventoryFailedEvent> cloudEvent) {
        try {
            log.info("Received inventory.failed event: {}", cloudEvent.getId());
            InventoryFailedEvent event = cloudEvent.getData();
            sagaOrchestratorService.handleInventoryFailed(event);
            return ResponseEntity.ok().build();
        } catch (Exception e) {
            log.error("Error handling inventory.failed event", e);
            return ResponseEntity.status(500).build();
        }
    }
}
