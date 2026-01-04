package com.xshopai.orderprocessor.filter;

import jakarta.servlet.*;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.Random;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * W3C Trace Context Filter for Order Processor Service
 * Implements W3C Trace Context specification for distributed tracing
 * Specification: https://www.w3.org/TR/trace-context/
 */
@Component
@Order(1)
public class TraceContextFilter implements Filter {

    private static final Logger logger = LoggerFactory.getLogger(TraceContextFilter.class);
    private static final String TRACEPARENT_HEADER = "traceparent";
    private static final String TRACE_ID_HEADER = "X-Trace-ID";
    private static final String CORRELATION_ID_HEADER = "X-Correlation-ID";
    private static final String MDC_TRACE_ID_KEY = "traceId";
    private static final String MDC_SPAN_ID_KEY = "spanId";
    private static final String MDC_CORRELATION_ID_KEY = "correlationId";
    
    // W3C Trace Context pattern: 00-{32-hex-trace-id}-{16-hex-span-id}-{2-hex-flags}
    private static final Pattern TRACEPARENT_PATTERN = 
        Pattern.compile("^00-([0-9a-f]{32})-([0-9a-f]{16})-[0-9a-f]{2}$");
    
    private static final Random random = new Random();

    @Override
    public void init(FilterConfig filterConfig) throws ServletException {
        logger.info("W3C Trace Context Filter initialized");
    }

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {
        
        HttpServletRequest httpRequest = (HttpServletRequest) request;
        HttpServletResponse httpResponse = (HttpServletResponse) response;
        
        try {
            // Extract or generate W3C Trace Context
            TraceContext traceContext = extractOrGenerateTraceContext(httpRequest);
            
            // Set trace context in MDC for logging
            MDC.put(MDC_TRACE_ID_KEY, traceContext.traceId);
            MDC.put(MDC_SPAN_ID_KEY, traceContext.spanId);
            MDC.put(MDC_CORRELATION_ID_KEY, traceContext.traceId); // Use trace ID as correlation ID
            
            // Add W3C traceparent header to response for propagation
            String traceparent = String.format("00-%s-%s-01", 
                traceContext.traceId, traceContext.spanId);
            httpResponse.setHeader(TRACEPARENT_HEADER, traceparent);
            
            // Add trace ID header for easier debugging
            httpResponse.setHeader(TRACE_ID_HEADER, traceContext.traceId);
            
            // Also support legacy correlation ID header
            httpResponse.setHeader(CORRELATION_ID_HEADER, traceContext.traceId);
            
            // Store in request attributes for use in controllers/services
            httpRequest.setAttribute(MDC_TRACE_ID_KEY, traceContext.traceId);
            httpRequest.setAttribute(MDC_SPAN_ID_KEY, traceContext.spanId);
            httpRequest.setAttribute(MDC_CORRELATION_ID_KEY, traceContext.traceId);
            
            logger.debug("Processing request {} {} with trace ID: {} (first 8 chars)",
                    httpRequest.getMethod(), 
                    httpRequest.getRequestURI(), 
                    traceContext.traceId.substring(0, 8));
            
            // Continue with the filter chain
            chain.doFilter(request, response);
            
        } finally {
            // Clean up MDC to prevent memory leaks
            MDC.remove(MDC_TRACE_ID_KEY);
            MDC.remove(MDC_SPAN_ID_KEY);
            MDC.remove(MDC_CORRELATION_ID_KEY);
        }
    }

    @Override
    public void destroy() {
        logger.info("W3C Trace Context Filter destroyed");
    }

    /**
     * Extract W3C Trace Context from traceparent header or generate new one
     */
    private TraceContext extractOrGenerateTraceContext(HttpServletRequest request) {
        String traceparent = request.getHeader(TRACEPARENT_HEADER);
        
        if (traceparent != null && !traceparent.trim().isEmpty()) {
            TraceContext extracted = parseTraceparent(traceparent);
            if (extracted != null) {
                return extracted;
            }
        }
        
        // Generate new trace context if extraction failed or no header present
        return generateTraceContext();
    }

    /**
     * Parse W3C traceparent header
     * Format: 00-{32-hex-trace-id}-{16-hex-span-id}-{2-hex-flags}
     */
    private TraceContext parseTraceparent(String traceparent) {
        Matcher matcher = TRACEPARENT_PATTERN.matcher(traceparent);
        
        if (!matcher.matches()) {
            logger.debug("Invalid traceparent format: {}", traceparent);
            return null;
        }
        
        String traceId = matcher.group(1);
        String spanId = matcher.group(2);
        
        // Validate trace ID is not all zeros
        if (traceId.equals("00000000000000000000000000000000")) {
            logger.debug("Invalid traceparent: trace ID is all zeros");
            return null;
        }
        
        // Validate span ID is not all zeros
        if (spanId.equals("0000000000000000")) {
            logger.debug("Invalid traceparent: span ID is all zeros");
            return null;
        }
        
        return new TraceContext(traceId, spanId);
    }

    /**
     * Generate new W3C Trace Context
     * Trace ID: 32 hex characters (128 bits)
     * Span ID: 16 hex characters (64 bits)
     */
    private TraceContext generateTraceContext() {
        String traceId = generateHexString(32);
        String spanId = generateHexString(16);
        return new TraceContext(traceId, spanId);
    }

    /**
     * Generate random hex string of specified length
     */
    private String generateHexString(int length) {
        StringBuilder sb = new StringBuilder(length);
        for (int i = 0; i < length; i++) {
            sb.append(Integer.toHexString(random.nextInt(16)));
        }
        return sb.toString();
    }

    /**
     * Simple class to hold trace context
     */
    private static class TraceContext {
        final String traceId;
        final String spanId;

        TraceContext(String traceId, String spanId) {
            this.traceId = traceId;
            this.spanId = spanId;
        }
    }
}
