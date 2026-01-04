package com.xshopai.orderprocessor.controller;

import com.xshopai.orderprocessor.model.entity.OrderProcessingSaga;
import com.xshopai.orderprocessor.repository.OrderProcessingSagaRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;

import static org.hamcrest.Matchers.hasSize;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(controllers = AdminController.class, excludeAutoConfiguration = SecurityAutoConfiguration.class)
class AdminControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private OrderProcessingSagaRepository sagaRepository;

    @Test
    void getAllSagas_ShouldReturnPagedSagas() throws Exception {
        // Arrange
        UUID sagaId = UUID.randomUUID();
        UUID orderId = UUID.randomUUID();
        
        OrderProcessingSaga saga = new OrderProcessingSaga();
        saga.setId(sagaId);
        saga.setOrderId(orderId);
        saga.setCustomerId(UUID.randomUUID().toString());
        saga.setOrderNumber("ORD-001");
        saga.setStatus(OrderProcessingSaga.SagaStatus.CREATED);
        saga.setTotalAmount(new BigDecimal("99.99"));
        saga.setCreatedAt(LocalDateTime.now());
        saga.setUpdatedAt(LocalDateTime.now());

        List<OrderProcessingSaga> sagas = Arrays.asList(saga);
        Page<OrderProcessingSaga> page = new PageImpl<>(sagas, PageRequest.of(0, 10), 1);

        when(sagaRepository.findAll(any(Pageable.class))).thenReturn(page);

        // Act & Assert
        mockMvc.perform(get("/api/v1/admin/sagas")
                .param("page", "0")
                .param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.content", hasSize(1)))
                .andExpect(jsonPath("$.content[0].id").value(sagaId.toString()))
                .andExpect(jsonPath("$.content[0].orderId").value(orderId.toString()))
                .andExpect(jsonPath("$.content[0].status").value("CREATED"));
    }

    @Test
    void getSagaById_WhenSagaExists_ShouldReturnSaga() throws Exception {
        // Arrange
        UUID sagaId = UUID.randomUUID();
        UUID orderId = UUID.randomUUID();
        
        OrderProcessingSaga saga = new OrderProcessingSaga();
        saga.setId(sagaId);
        saga.setOrderId(orderId);
        saga.setCustomerId(UUID.randomUUID().toString());
        saga.setOrderNumber("ORD-002");
        saga.setStatus(OrderProcessingSaga.SagaStatus.COMPLETED);
        saga.setTotalAmount(new BigDecimal("149.99"));
        saga.setCreatedAt(LocalDateTime.now());
        saga.setUpdatedAt(LocalDateTime.now());

        when(sagaRepository.findById(sagaId)).thenReturn(Optional.of(saga));

        // Act & Assert
        mockMvc.perform(get("/api/v1/admin/sagas/{id}", sagaId))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.id").value(sagaId.toString()))
                .andExpect(jsonPath("$.orderId").value(orderId.toString()))
                .andExpect(jsonPath("$.status").value("COMPLETED"));
    }

    @Test
    void getSagaById_WhenSagaNotExists_ShouldReturn404() throws Exception {
        // Arrange
        UUID sagaId = UUID.randomUUID();
        when(sagaRepository.findById(sagaId)).thenReturn(Optional.empty());

        // Act & Assert
        mockMvc.perform(get("/api/v1/admin/sagas/{id}", sagaId))
                .andExpect(status().isNotFound());
    }

    @Test
    void getSagaByOrderId_WhenSagaExists_ShouldReturnSaga() throws Exception {
        // Arrange
        UUID sagaId = UUID.randomUUID();
        UUID orderId = UUID.randomUUID();
        
        OrderProcessingSaga saga = new OrderProcessingSaga();
        saga.setId(sagaId);
        saga.setOrderId(orderId);
        saga.setCustomerId(UUID.randomUUID().toString());
        saga.setOrderNumber("ORD-003");
        saga.setStatus(OrderProcessingSaga.SagaStatus.PENDING_PAYMENT_CONFIRMATION);
        saga.setTotalAmount(new BigDecimal("75.50"));
        saga.setCreatedAt(LocalDateTime.now());
        saga.setUpdatedAt(LocalDateTime.now());

        when(sagaRepository.findByOrderId(orderId)).thenReturn(Optional.of(saga));

        // Act & Assert
        mockMvc.perform(get("/api/v1/admin/sagas/order/{orderId}", orderId))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.id").value(sagaId.toString()))
                .andExpect(jsonPath("$.orderId").value(orderId.toString()))
                .andExpect(jsonPath("$.status").value("PENDING_PAYMENT_CONFIRMATION"));
    }

    @Test
    void getSagaByOrderId_WhenSagaNotExists_ShouldReturn404() throws Exception {
        // Arrange
        UUID orderId = UUID.randomUUID();
        when(sagaRepository.findByOrderId(orderId)).thenReturn(Optional.empty());

        // Act & Assert
        mockMvc.perform(get("/api/v1/admin/sagas/order/{orderId}", orderId))
                .andExpect(status().isNotFound());
    }

    @Test
    void getSagaStats_ShouldReturnStatusCounts() throws Exception {
        // Arrange
        when(sagaRepository.countByStatus(OrderProcessingSaga.SagaStatus.CREATED)).thenReturn(5L);
        when(sagaRepository.countByStatus(OrderProcessingSaga.SagaStatus.PENDING_PAYMENT_CONFIRMATION)).thenReturn(3L);
        when(sagaRepository.countByStatus(OrderProcessingSaga.SagaStatus.PAYMENT_CONFIRMED)).thenReturn(2L);
        when(sagaRepository.countByStatus(OrderProcessingSaga.SagaStatus.PENDING_SHIPPING_PREPARATION)).thenReturn(1L);
        when(sagaRepository.countByStatus(OrderProcessingSaga.SagaStatus.COMPLETED)).thenReturn(15L);
        when(sagaRepository.countByStatus(OrderProcessingSaga.SagaStatus.CANCELLED)).thenReturn(2L);
        when(sagaRepository.countByStatus(OrderProcessingSaga.SagaStatus.COMPENSATING)).thenReturn(1L);
        when(sagaRepository.countByStatus(OrderProcessingSaga.SagaStatus.COMPENSATED)).thenReturn(1L);

        // Act & Assert
        mockMvc.perform(get("/api/v1/admin/sagas/stats"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.STARTED").value(5))
                .andExpect(jsonPath("$.PAYMENT_PROCESSING").value(3))
                .andExpect(jsonPath("$.INVENTORY_PROCESSING").value(2))
                .andExpect(jsonPath("$.SHIPPING_PROCESSING").value(1))
                .andExpect(jsonPath("$.COMPLETED").value(15))
                .andExpect(jsonPath("$.FAILED").value(2))
                .andExpect(jsonPath("$.COMPENSATING").value(1))
                .andExpect(jsonPath("$.COMPENSATED").value(1));
    }
}
