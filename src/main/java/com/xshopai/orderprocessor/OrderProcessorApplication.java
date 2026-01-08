package com.xshopai.orderprocessor;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.transaction.annotation.EnableTransactionManagement;

/**
 * Order Processor Service Application
 * 
 * Handles order processing workflows using choreography-based saga pattern.
 * Processes order events and coordinates with external services (payment, inventory, shipping)
 * to complete order fulfillment with distributed transaction support.
 */
@SpringBootApplication
@EnableAsync
@EnableTransactionManagement
public class OrderProcessorApplication {

    public static void main(String[] args) {
        SpringApplication.run(OrderProcessorApplication.class, args);
    }
}
