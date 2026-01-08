package com.xshopai.orderprocessor.client;

import io.dapr.client.DaprClient;
import io.dapr.client.domain.HttpExtension;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import jakarta.annotation.PostConstruct;

/**
 * Dapr Service Client
 * Handles service-to-service invocation using Dapr
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class DaprServiceClient {

    private final DaprClient daprClient;

    @PostConstruct
    public void init() {
        log.info("Dapr Service Client initialized");
    }

    /**
     * Invoke a service method via Dapr
     */
    public <T> T invokeService(String appId, String method, Object request, Class<T> responseType) {
        try {
            log.debug("Invoking service: {} method: {}", appId, method);
            
            T response = daprClient.invokeMethod(
                appId,
                method,
                request,
                HttpExtension.POST,
                responseType
            ).block();
            
            log.debug("Service invocation successful: {} method: {}", appId, method);
            return response;
        } catch (Exception e) {
            log.error("Failed to invoke service: {} method: {}", appId, method, e);
            throw new RuntimeException("Service invocation failed", e);
        }
    }

    /**
     * Invoke GET method on a service
     */
    public <T> T invokeGet(String appId, String method, Class<T> responseType) {
        try {
            log.debug("Invoking GET service: {} method: {}", appId, method);
            
            T response = daprClient.invokeMethod(
                appId,
                method,
                null,
                HttpExtension.GET,
                responseType
            ).block();
            
            log.debug("GET invocation successful: {} method: {}", appId, method);
            return response;
        } catch (Exception e) {
            log.error("Failed to invoke GET service: {} method: {}", appId, method, e);
            throw new RuntimeException("Service invocation failed", e);
        }
    }

    /**
     * Invoke POST method on a service
     */
    public <T> T invokePost(String appId, String method, Object request, Class<T> responseType) {
        return invokeService(appId, method, request, responseType);
    }

    /**
     * Invoke PUT method on a service
     */
    public <T> T invokePut(String appId, String method, Object request, Class<T> responseType) {
        try {
            log.debug("Invoking PUT service: {} method: {}", appId, method);
            
            T response = daprClient.invokeMethod(
                appId,
                method,
                request,
                HttpExtension.PUT,
                responseType
            ).block();
            
            log.debug("PUT invocation successful: {} method: {}", appId, method);
            return response;
        } catch (Exception e) {
            log.error("Failed to invoke PUT service: {} method: {}", appId, method, e);
            throw new RuntimeException("Service invocation failed", e);
        }
    }

    /**
     * Invoke DELETE method on a service
     */
    public <T> T invokeDelete(String appId, String method, Class<T> responseType) {
        try {
            log.debug("Invoking DELETE service: {} method: {}", appId, method);
            
            T response = daprClient.invokeMethod(
                appId,
                method,
                null,
                HttpExtension.DELETE,
                responseType
            ).block();
            
            log.debug("DELETE invocation successful: {} method: {}", appId, method);
            return response;
        } catch (Exception e) {
            log.error("Failed to invoke DELETE service: {} method: {}", appId, method, e);
            throw new RuntimeException("Service invocation failed", e);
        }
    }
}
