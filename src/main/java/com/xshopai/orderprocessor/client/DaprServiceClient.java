package com.xshopai.orderprocessor.client;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.dapr.client.DaprClient;
import io.dapr.client.domain.HttpExtension;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import jakarta.annotation.PostConstruct;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.Map;

/**
 * Service Client
 * Handles service-to-service invocation
 * - When MESSAGING_PROVIDER=dapr: Uses Dapr service invocation
 * - Otherwise: Uses direct HTTP calls
 */
@Service
@Slf4j
public class DaprServiceClient {

    private final DaprClient daprClient;
    private final ObjectMapper objectMapper;
    private HttpClient httpClient;
    
    @Value("${messaging.provider:rabbitmq}")
    private String messagingProvider;
    
    // Service URLs for direct HTTP calls
    @Value("${services.order.url:http://xshopai-order-service:8006}")
    private String orderServiceUrl;
    
    @Value("${services.inventory.url:http://xshopai-inventory-service:8005}")
    private String inventoryServiceUrl;
    
    @Value("${services.payment.url:http://xshopai-payment-service:8009}")
    private String paymentServiceUrl;
    
    @Value("${services.notification.url:http://xshopai-notification-service:8011}")
    private String notificationServiceUrl;
    
    private boolean useDapr;

    @Autowired
    public DaprServiceClient(DaprClient daprClient, ObjectMapper objectMapper) {
        this.daprClient = daprClient;
        this.objectMapper = objectMapper;
    }

    @PostConstruct
    public void init() {
        this.useDapr = "dapr".equalsIgnoreCase(messagingProvider);
        this.httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(10))
            .build();
        
        if (useDapr) {
            log.info("Service Client initialized - Using Dapr service invocation");
        } else {
            log.info("Service Client initialized - Using direct HTTP calls");
        }
    }
    
    /**
     * Get the direct HTTP URL for a service
     */
    private String getServiceUrl(String appId) {
        return switch (appId) {
            case "order-service" -> orderServiceUrl;
            case "inventory-service" -> inventoryServiceUrl;
            case "payment-service" -> paymentServiceUrl;
            case "notification-service" -> notificationServiceUrl;
            default -> "http://xshopai-" + appId + ":8000";
        };
    }

    /**
     * Invoke a service method via Dapr or direct HTTP
     */
    public <T> T invokeService(String appId, String method, Object request, Class<T> responseType) {
        if (useDapr) {
            return invokeViaDapr(appId, method, request, HttpExtension.POST, responseType);
        } else {
            return invokeViaHttp(appId, method, "POST", request, responseType);
        }
    }

    /**
     * Invoke GET method on a service
     */
    public <T> T invokeGet(String appId, String method, Class<T> responseType) {
        if (useDapr) {
            return invokeViaDapr(appId, method, null, HttpExtension.GET, responseType);
        } else {
            return invokeViaHttp(appId, method, "GET", null, responseType);
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
        if (useDapr) {
            return invokeViaDapr(appId, method, request, HttpExtension.PUT, responseType);
        } else {
            return invokeViaHttp(appId, method, "PUT", request, responseType);
        }
    }

    /**
     * Invoke DELETE method on a service
     */
    public <T> T invokeDelete(String appId, String method, Class<T> responseType) {
        if (useDapr) {
            return invokeViaDapr(appId, method, null, HttpExtension.DELETE, responseType);
        } else {
            return invokeViaHttp(appId, method, "DELETE", null, responseType);
        }
    }
    
    /**
     * Internal: Invoke via Dapr sidecar
     */
    private <T> T invokeViaDapr(String appId, String method, Object request, HttpExtension httpExtension, Class<T> responseType) {
        try {
            log.debug("Invoking service via Dapr: {} method: {}", appId, method);
            
            T response = daprClient.invokeMethod(
                appId,
                method,
                request,
                httpExtension,
                responseType
            ).block();
            
            log.debug("Dapr invocation successful: {} method: {}", appId, method);
            return response;
        } catch (Exception e) {
            log.error("Failed to invoke via Dapr: {} method: {}", appId, method, e);
            throw new RuntimeException("Service invocation failed", e);
        }
    }
    
    /**
     * Internal: Invoke via direct HTTP
     */
    private <T> T invokeViaHttp(String appId, String method, String httpMethod, Object request, Class<T> responseType) {
        try {
            String baseUrl = getServiceUrl(appId);
            String cleanMethod = method.startsWith("/") ? method.substring(1) : method;
            String url = baseUrl + "/" + cleanMethod;
            
            log.debug("Invoking service via HTTP: {} url: {}", appId, url);
            
            HttpRequest.Builder requestBuilder = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .header("Content-Type", "application/json")
                .timeout(Duration.ofSeconds(30));
            
            if ("GET".equals(httpMethod)) {
                requestBuilder.GET();
            } else if ("DELETE".equals(httpMethod)) {
                requestBuilder.DELETE();
            } else {
                String body = request != null ? objectMapper.writeValueAsString(request) : "";
                requestBuilder.method(httpMethod, HttpRequest.BodyPublishers.ofString(body));
            }
            
            HttpResponse<String> response = httpClient.send(requestBuilder.build(), HttpResponse.BodyHandlers.ofString());
            
            if (response.statusCode() >= 200 && response.statusCode() < 300) {
                log.debug("HTTP invocation successful: {} url: {}", appId, url);
                if (responseType == Void.class || response.body().isEmpty()) {
                    return null;
                }
                return objectMapper.readValue(response.body(), responseType);
            } else {
                log.error("HTTP invocation failed: {} url: {} status: {}", appId, url, response.statusCode());
                throw new RuntimeException("Service invocation failed: HTTP " + response.statusCode());
            }
        } catch (Exception e) {
            log.error("Failed to invoke via HTTP: {} method: {}", appId, method, e);
            throw new RuntimeException("Service invocation failed", e);
        }
    }
}
