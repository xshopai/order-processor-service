package com.xshopai.orderprocessor.scheduler;

import com.xshopai.orderprocessor.service.SagaOrchestratorService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Scheduled tasks for saga management
 * Note: For production, consider using Dapr Workflows (when available in SDK)
 * or Dapr Actors with reminders for distributed saga timeout handling
 */
@Component
@EnableScheduling
@RequiredArgsConstructor
@Slf4j
public class SagaScheduledTasks {

    private final SagaOrchestratorService sagaOrchestratorService;

    /**
     * Check for and process stuck sagas every 15 minutes
     * In a multi-instance deployment, consider using Dapr distributed lock
     * to ensure only one instance processes timeouts
     */
    @Scheduled(fixedRateString = "${saga.scheduler.stuck-sagas-rate:900000}")
    public void processStuckSagas() {
        log.info("Starting scheduled task: processStuckSagas");
        
        try {
            sagaOrchestratorService.processStuckSagas();
        } catch (Exception e) {
            log.error("Error processing stuck sagas: {}", e.getMessage(), e);
        }
    }
}
