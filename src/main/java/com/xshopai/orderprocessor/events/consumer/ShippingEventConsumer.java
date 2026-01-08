package com.xshopai.orderprocessor.events.consumer;

import io.dapr.client.domain.CloudEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import com.xshopai.orderprocessor.model.events.ShippingPreparedEvent;
import com.xshopai.orderprocessor.model.events.ShippingFailedEvent;
import com.xshopai.orderprocessor.service.SagaOrchestratorService;

/**
 * Shipping Event Consumer
 * Handles shipping-related events from Dapr pub/sub
 */
@RestController
@RequestMapping("/dapr/events")
@RequiredArgsConstructor
@Slf4j
public class ShippingEventConsumer {

    private final SagaOrchestratorService sagaOrchestratorService;

    /**
     * Handle shipping.prepared event
     */
    @PostMapping("/shipping-prepared")
    public ResponseEntity<Void> handleShippingPrepared(@RequestBody CloudEvent<ShippingPreparedEvent> cloudEvent) {
        try {
            log.info("Received shipping.prepared event: {}", cloudEvent.getId());
            ShippingPreparedEvent event = cloudEvent.getData();
            sagaOrchestratorService.handleShippingPrepared(event);
            return ResponseEntity.ok().build();
        } catch (Exception e) {
            log.error("Error handling shipping.prepared event", e);
            return ResponseEntity.status(500).build();
        }
    }

    /**
     * Handle shipping.failed event
     */
    @PostMapping("/shipping-failed")
    public ResponseEntity<Void> handleShippingFailed(@RequestBody CloudEvent<ShippingFailedEvent> cloudEvent) {
        try {
            log.info("Received shipping.failed event: {}", cloudEvent.getId());
            ShippingFailedEvent event = cloudEvent.getData();
            sagaOrchestratorService.handleShippingFailed(event);
            return ResponseEntity.ok().build();
        } catch (Exception e) {
            log.error("Error handling shipping.failed event", e);
            return ResponseEntity.status(500).build();
        }
    }
}
