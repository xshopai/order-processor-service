package com.xshopai.orderprocessor.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.time.Instant;
import java.util.Map;

/**
 * Wrapper class for messages from message-broker-service
 * This matches the Message structure from the Go service
 */
@Data
public class MessageWrapper {
    private String id;
    private String topic;
    private Map<String, Object> data;  // The actual event payload
    private MessageMetadata metadata;
    private Instant timestamp;
    
    @JsonProperty("correlationId")
    private String correlationId;
    
    @Data
    public static class MessageMetadata {
        private String source;
        private String contentType;
        private Integer priority;
        private Map<String, String> headers;
    }
}
