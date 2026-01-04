package com.xshopai.orderprocessor.service;

import com.xshopai.orderprocessor.events.publisher.DaprEventPublisher;
import com.xshopai.orderprocessor.model.entity.OrderProcessingSaga;
import com.xshopai.orderprocessor.model.events.OrderCreatedEvent;
import com.xshopai.orderprocessor.model.events.PaymentProcessedEvent;
import com.xshopai.orderprocessor.model.events.PaymentFailedEvent;
import com.xshopai.orderprocessor.repository.OrderProcessingSagaRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SagaOrchestratorServiceTest {

    @Mock
    private OrderProcessingSagaRepository sagaRepository;

    @Mock
    private DaprEventPublisher daprEventPublisher;

    @Mock
    private SagaMetricsService metricsService;

    @Mock
    private ObjectMapper objectMapper;

    @InjectMocks
    private SagaOrchestratorService sagaOrchestratorService;

    private OrderCreatedEvent orderCreatedEvent;
    private OrderProcessingSaga testSaga;

    @BeforeEach
    void setUp() {
        UUID orderId = UUID.randomUUID();
        
        orderCreatedEvent = new OrderCreatedEvent();
        orderCreatedEvent.setOrderId(orderId);
        orderCreatedEvent.setCustomerId("customer123");
        orderCreatedEvent.setOrderNumber("ORD-20250811-ABC123");
        orderCreatedEvent.setTotalAmount(BigDecimal.valueOf(99.99));
        orderCreatedEvent.setCurrency("USD");
        
        testSaga = new OrderProcessingSaga();
        testSaga.setId(UUID.randomUUID());
        testSaga.setOrderId(orderId);
        testSaga.setCustomerId("customer123");
        testSaga.setOrderNumber("ORD-20250811-ABC123");
        testSaga.setTotalAmount(BigDecimal.valueOf(99.99));
        testSaga.setCurrency("USD");
        testSaga.setStatus(OrderProcessingSaga.SagaStatus.PENDING_PAYMENT_CONFIRMATION);
    }

    @Test
    void startOrderProcessingSaga_ShouldCreateNewSaga_WhenOrderDoesNotExist() {
        // Arrange
        when(sagaRepository.existsByOrderId(orderCreatedEvent.getOrderId())).thenReturn(false);
        when(sagaRepository.save(any(OrderProcessingSaga.class))).thenReturn(testSaga);

        // Act
        sagaOrchestratorService.startOrderProcessingSaga(orderCreatedEvent);

        // Assert
        verify(sagaRepository).existsByOrderId(orderCreatedEvent.getOrderId());
        verify(sagaRepository).save(any(OrderProcessingSaga.class));
        verify(daprEventPublisher).publishPaymentProcessing(any());
        verify(metricsService).recordSagaStarted(orderCreatedEvent.getOrderNumber());
    }

    @Test
    void startOrderProcessingSaga_ShouldNotCreateSaga_WhenOrderAlreadyExists() {
        // Arrange
        when(sagaRepository.existsByOrderId(orderCreatedEvent.getOrderId())).thenReturn(true);

        // Act
        sagaOrchestratorService.startOrderProcessingSaga(orderCreatedEvent);

        // Assert
        verify(sagaRepository).existsByOrderId(orderCreatedEvent.getOrderId());
        verify(sagaRepository, never()).save(any(OrderProcessingSaga.class));
        verify(daprEventPublisher, never()).publishPaymentProcessing(any());
    }

    @Test
    void handlePaymentProcessed_ShouldUpdateSagaStatus_WhenSagaExists() {
        // Arrange
        PaymentProcessedEvent paymentEvent = new PaymentProcessedEvent();
        paymentEvent.setOrderId(testSaga.getOrderId());
        paymentEvent.setPaymentId("payment123");
        
        when(sagaRepository.findByOrderId(paymentEvent.getOrderId())).thenReturn(Optional.of(testSaga));
        when(sagaRepository.save(any(OrderProcessingSaga.class))).thenReturn(testSaga);

        // Act
        sagaOrchestratorService.handlePaymentProcessed(paymentEvent);

        // Assert
        verify(sagaRepository).findByOrderId(paymentEvent.getOrderId());
        verify(sagaRepository).save(testSaga);
        verify(daprEventPublisher).publishInventoryReservation(any());
        assertEquals(OrderProcessingSaga.SagaStatus.PAYMENT_CONFIRMED, testSaga.getStatus());
        assertEquals("payment123", testSaga.getPaymentId());
    }

    @Test
    void handlePaymentFailed_ShouldRetry_WhenRetryCountBelowLimit() {
        // Arrange
        PaymentFailedEvent paymentFailedEvent = new PaymentFailedEvent();
        paymentFailedEvent.setOrderId(testSaga.getOrderId());
        paymentFailedEvent.setReason("Payment declined");
        
        testSaga.setRetryCount(1); // Below limit
        
        when(sagaRepository.findByOrderId(paymentFailedEvent.getOrderId())).thenReturn(Optional.of(testSaga));
        when(sagaRepository.save(any(OrderProcessingSaga.class))).thenReturn(testSaga);

        // Act
        sagaOrchestratorService.handlePaymentFailed(paymentFailedEvent);

        // Assert
        verify(sagaRepository).findByOrderId(paymentFailedEvent.getOrderId());
        verify(sagaRepository).save(testSaga);
        assertEquals(2, testSaga.getRetryCount());
    }

    @Test
    void handlePaymentFailed_ShouldFailSaga_WhenRetryLimitExceeded() {
        // Arrange
        PaymentFailedEvent paymentFailedEvent = new PaymentFailedEvent();
        paymentFailedEvent.setOrderId(testSaga.getOrderId());
        paymentFailedEvent.setReason("Payment declined");
        
        testSaga.setRetryCount(3); // At limit
        
        when(sagaRepository.findByOrderId(paymentFailedEvent.getOrderId())).thenReturn(Optional.of(testSaga));

        // Act
        sagaOrchestratorService.handlePaymentFailed(paymentFailedEvent);

        // Assert
        verify(sagaRepository).findByOrderId(paymentFailedEvent.getOrderId());
        assertEquals(OrderProcessingSaga.SagaStatus.COMPENSATED, testSaga.getStatus());
    }

    @Test
    void completeSaga_ShouldMarkSagaCompleted_WhenSagaExists() {
        // Arrange
        UUID orderId = testSaga.getOrderId();
        when(sagaRepository.findByOrderId(orderId)).thenReturn(Optional.of(testSaga));
        when(sagaRepository.save(any(OrderProcessingSaga.class))).thenReturn(testSaga);

        // Act
        sagaOrchestratorService.completeSaga(orderId);

        // Assert
        verify(sagaRepository).findByOrderId(orderId);
        verify(sagaRepository).save(testSaga);
        verify(daprEventPublisher).publishOrderStatusChanged(any());
        assertTrue(testSaga.isCompleted());
        assertNotNull(testSaga.getCompletedAt());
    }

    @Test
    void handleSagaFailure_ShouldStartCompensation() {
        // Arrange
        String errorMessage = "Test error";
        testSaga.setPaymentId("payment123");
        testSaga.setInventoryReservationId("reservation123");
        
        when(sagaRepository.save(any(OrderProcessingSaga.class))).thenReturn(testSaga);

        // Act
        sagaOrchestratorService.handleSagaFailure(testSaga, errorMessage);

        // Assert
        verify(sagaRepository, atLeast(1)).save(testSaga);
        assertEquals(OrderProcessingSaga.SagaStatus.COMPENSATED, testSaga.getStatus());
        assertEquals(errorMessage, testSaga.getErrorMessage());
        verify(daprEventPublisher).publishPaymentRefund(testSaga.getOrderId(), "payment123");
        verify(daprEventPublisher).publishInventoryRelease(testSaga.getOrderId(), "reservation123");
    }
}
