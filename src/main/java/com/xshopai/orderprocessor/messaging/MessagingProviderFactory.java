package com.xshopai.orderprocessor.messaging;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.dapr.client.DaprClient;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

import jakarta.annotation.PreDestroy;

/**
 * Messaging Provider Factory Configuration
 * Creates the appropriate MessagingProvider based on configuration
 * 
 * Configuration priority:
 * 1. MESSAGING_PROVIDER environment variable
 * 2. messaging.provider application property
 * 3. Default: 'dapr'
 * 
 * Supported providers: 'dapr', 'rabbitmq', 'servicebus'
 */
@Configuration
@Slf4j
public class MessagingProviderFactory {

    @Value("${messaging.provider:${MESSAGING_PROVIDER:dapr}}")
    private String messagingProvider;

    // Dapr configuration
    @Value("${dapr.pubsub-name:pubsub}")
    private String daprPubsubName;

    // RabbitMQ configuration
    @Value("${rabbitmq.host:localhost}")
    private String rabbitMqHost;

    @Value("${rabbitmq.port:5672}")
    private int rabbitMqPort;

    @Value("${rabbitmq.username:guest}")
    private String rabbitMqUsername;

    @Value("${rabbitmq.password:guest}")
    private String rabbitMqPassword;

    @Value("${rabbitmq.exchange:xshopai.events}")
    private String rabbitMqExchange;

    // Azure Service Bus configuration
    @Value("${azure.servicebus.connection-string:}")
    private String serviceBusConnectionString;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired(required = false)
    private DaprClient daprClient;

    private MessagingProvider messagingProviderInstance;

    /**
     * Create and configure the messaging provider bean
     */
    @Bean
    @Primary
    public MessagingProvider messagingProvider() {
        String provider = messagingProvider.toLowerCase().trim();
        log.info("Initializing messaging provider: {}", provider);

        switch (provider) {
            case "rabbitmq":
                messagingProviderInstance = createRabbitMQProvider();
                break;
            case "servicebus":
                messagingProviderInstance = createServiceBusProvider();
                break;
            case "dapr":
            default:
                if (!"dapr".equals(provider)) {
                    log.warn("Unknown messaging provider '{}', falling back to 'dapr'", provider);
                }
                messagingProviderInstance = createDaprProvider();
                break;
        }

        log.info("Messaging provider initialized: {} ({})", 
                messagingProviderInstance.getProviderName(), 
                messagingProviderInstance.getClass().getSimpleName());
        
        return messagingProviderInstance;
    }

    /**
     * Create Dapr messaging provider
     */
    private MessagingProvider createDaprProvider() {
        if (daprClient == null) {
            throw new IllegalStateException(
                "DaprClient bean not found. Ensure Dapr is configured when using 'dapr' messaging provider.");
        }
        return new DaprMessagingProvider(daprClient, daprPubsubName);
    }

    /**
     * Create RabbitMQ messaging provider
     */
    private MessagingProvider createRabbitMQProvider() {
        log.info("Creating RabbitMQ messaging provider - host: {}, port: {}", rabbitMqHost, rabbitMqPort);
        return new RabbitMQMessagingProvider(
                rabbitMqHost,
                rabbitMqPort,
                rabbitMqUsername,
                rabbitMqPassword,
                rabbitMqExchange,
                objectMapper
        );
    }

    /**
     * Create Azure Service Bus messaging provider
     */
    private MessagingProvider createServiceBusProvider() {
        if (serviceBusConnectionString == null || serviceBusConnectionString.isEmpty()) {
            throw new IllegalStateException(
                "Azure Service Bus connection string not configured. " +
                "Set 'azure.servicebus.connection-string' property or 'AZURE_SERVICEBUS_CONNECTION_STRING' environment variable.");
        }
        return new ServiceBusMessagingProvider(serviceBusConnectionString, objectMapper);
    }

    /**
     * Cleanup messaging provider on shutdown
     */
    @PreDestroy
    public void cleanup() {
        if (messagingProviderInstance != null) {
            log.info("Closing messaging provider: {}", messagingProviderInstance.getProviderName());
            messagingProviderInstance.close();
        }
    }
}
