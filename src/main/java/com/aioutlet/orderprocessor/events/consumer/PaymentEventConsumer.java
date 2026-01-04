package com.xshopai.orderprocessor.events.consumer;

import io.dapr.client.domain.CloudEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import com.xshopai.orderprocessor.model.events.PaymentProcessedEvent;
import com.xshopai.orderprocessor.model.events.PaymentFailedEvent;
import com.xshopai.orderprocessor.service.SagaOrchestratorService;

/**
 * Payment Event Consumer
 * Handles payment-related events from Dapr pub/sub
 */
@RestController
@RequestMapping("/dapr/events")
@RequiredArgsConstructor
@Slf4j
public class PaymentEventConsumer {

    private final SagaOrchestratorService sagaOrchestratorService;

    /**
     * Handle payment.processed event
     */
    @PostMapping("/payment-processed")
    public ResponseEntity<Void> handlePaymentProcessed(@RequestBody CloudEvent<PaymentProcessedEvent> cloudEvent) {
        try {
            log.info("Received payment.processed event: {}", cloudEvent.getId());
            PaymentProcessedEvent event = cloudEvent.getData();
            sagaOrchestratorService.handlePaymentProcessed(event);
            return ResponseEntity.ok().build();
        } catch (Exception e) {
            log.error("Error handling payment.processed event", e);
            return ResponseEntity.status(500).build();
        }
    }

    /**
     * Handle payment.failed event
     */
    @PostMapping("/payment-failed")
    public ResponseEntity<Void> handlePaymentFailed(@RequestBody CloudEvent<PaymentFailedEvent> cloudEvent) {
        try {
            log.info("Received payment.failed event: {}", cloudEvent.getId());
            PaymentFailedEvent event = cloudEvent.getData();
            sagaOrchestratorService.handlePaymentFailed(event);
            return ResponseEntity.ok().build();
        } catch (Exception e) {
            log.error("Error handling payment.failed event", e);
            return ResponseEntity.status(500).build();
        }
    }
}
