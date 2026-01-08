package com.xshopai.orderprocessor.util;

import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.MDC;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.util.Optional;
import java.util.UUID;

/**
 * Utility class for correlation ID operations
 * Provides helper methods for getting, setting, and using correlation IDs
 */
@Component
public class CorrelationIdUtil {

    private static final String CORRELATION_ID_HEADER = "X-Correlation-ID";
    private static final String MDC_CORRELATION_ID_KEY = "correlationId";

    /**
     * Get the current correlation ID from MDC
     * 
     * @return correlation ID or "unknown" if not found
     */
    public static String getCurrentCorrelationId() {
        String correlationId = MDC.get(MDC_CORRELATION_ID_KEY);
        return correlationId != null ? correlationId : "unknown";
    }

    /**
     * Get correlation ID from HTTP request
     * 
     * @param request HTTP servlet request
     * @return correlation ID or generated UUID if not found
     */
    public static String getCorrelationId(HttpServletRequest request) {
        if (request == null) {
            return getCurrentCorrelationId();
        }

        // Try to get from request attribute first (set by filter)
        Object correlationId = request.getAttribute(MDC_CORRELATION_ID_KEY);
        if (correlationId != null) {
            return correlationId.toString();
        }

        // Fall back to header
        String headerValue = request.getHeader(CORRELATION_ID_HEADER);
        if (headerValue != null && !headerValue.trim().isEmpty()) {
            return headerValue;
        }

        // Generate new one if not found
        return UUID.randomUUID().toString();
    }

    /**
     * Get correlation ID from current request context
     * 
     * @return correlation ID from current request context
     */
    public static String getCorrelationIdFromContext() {
        try {
            ServletRequestAttributes requestAttributes = 
                (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
            
            if (requestAttributes != null) {
                HttpServletRequest request = requestAttributes.getRequest();
                return getCorrelationId(request);
            }
        } catch (Exception e) {
            // Ignore and fall back to MDC
        }
        
        return getCurrentCorrelationId();
    }

    /**
     * Set correlation ID in MDC
     * 
     * @param correlationId correlation ID to set
     */
    public static void setCorrelationId(String correlationId) {
        if (correlationId != null && !correlationId.trim().isEmpty()) {
            MDC.put(MDC_CORRELATION_ID_KEY, correlationId);
        }
    }

    /**
     * Clear correlation ID from MDC
     */
    public static void clearCorrelationId() {
        MDC.remove(MDC_CORRELATION_ID_KEY);
    }

    /**
     * Create HTTP headers with correlation ID for outgoing requests
     * 
     * @return HttpHeaders with correlation ID
     */
    public static HttpHeaders createHeadersWithCorrelationId() {
        return createHeadersWithCorrelationId(null);
    }

    /**
     * Create HTTP headers with correlation ID for outgoing requests
     * 
     * @param additionalHeaders additional headers to include
     * @return HttpHeaders with correlation ID and additional headers
     */
    public static HttpHeaders createHeadersWithCorrelationId(HttpHeaders additionalHeaders) {
        HttpHeaders headers = additionalHeaders != null ? new HttpHeaders() : additionalHeaders;
        if (headers == null) {
            headers = new HttpHeaders();
        }
        
        String correlationId = getCorrelationIdFromContext();
        headers.set(CORRELATION_ID_HEADER, correlationId);
        
        // Set default content type if not already set
        if (!headers.containsKey(HttpHeaders.CONTENT_TYPE)) {
            headers.set(HttpHeaders.CONTENT_TYPE, "application/json");
        }
        
        return headers;
    }

    /**
     * Execute a runnable with correlation ID context
     * Useful for async operations where MDC might not be propagated
     * 
     * @param correlationId correlation ID to set
     * @param operation operation to execute
     */
    public static void withCorrelationId(String correlationId, Runnable operation) {
        String originalCorrelationId = getCurrentCorrelationId();
        try {
            setCorrelationId(correlationId);
            operation.run();
        } finally {
            if (!"unknown".equals(originalCorrelationId)) {
                setCorrelationId(originalCorrelationId);
            } else {
                clearCorrelationId();
            }
        }
    }

    /**
     * Get Optional correlation ID from current context
     * 
     * @return Optional containing correlation ID if present
     */
    public static Optional<String> getOptionalCorrelationId() {
        String correlationId = getCurrentCorrelationId();
        return "unknown".equals(correlationId) ? Optional.empty() : Optional.of(correlationId);
    }
}
