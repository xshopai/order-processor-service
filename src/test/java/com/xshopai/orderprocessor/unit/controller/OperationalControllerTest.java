package com.xshopai.orderprocessor.controller;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.notNullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(controllers = OperationalController.class, excludeAutoConfiguration = SecurityAutoConfiguration.class)
class OperationalControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void health_ShouldReturnHealthyStatus() throws Exception {
        // Act & Assert
        mockMvc.perform(get("/health"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.status").value("healthy"))
                .andExpect(jsonPath("$.service").value("order-processor-service"))
                .andExpect(jsonPath("$.timestamp", notNullValue()))
                .andExpect(jsonPath("$.version", notNullValue()));
    }

    @Test
    void readiness_ShouldReturnReadyStatus() throws Exception {
        // Act & Assert
        mockMvc.perform(get("/health/ready"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.status").value("ready"))
                .andExpect(jsonPath("$.service").value("order-processor-service"))
                .andExpect(jsonPath("$.timestamp", notNullValue()))
                .andExpect(jsonPath("$.checks.database").value("connected"))
                .andExpect(jsonPath("$.checks.messageBroker").value("connected"));
    }

    @Test
    void liveness_ShouldReturnAliveStatus() throws Exception {
        // Act & Assert
        mockMvc.perform(get("/health/live"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.status").value("alive"))
                .andExpect(jsonPath("$.service").value("order-processor-service"))
                .andExpect(jsonPath("$.timestamp", notNullValue()))
                .andExpect(jsonPath("$.uptime", notNullValue()));
    }

    @Test
    void metrics_ShouldReturnBasicMetrics() throws Exception {
        // Act & Assert
        mockMvc.perform(get("/metrics"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.service").value("order-processor-service"))
                .andExpect(jsonPath("$.timestamp", notNullValue()))
                .andExpect(jsonPath("$.metrics.uptime", notNullValue()))
                .andExpect(jsonPath("$.metrics.memory.heapUsed", notNullValue()))
                .andExpect(jsonPath("$.metrics.memory.heapMax", notNullValue()))
                .andExpect(jsonPath("$.metrics.availableProcessors", notNullValue()))
                .andExpect(jsonPath("$.metrics.javaVersion", notNullValue()))
                .andExpect(jsonPath("$.metrics.javaVendor", notNullValue()));
    }
}
