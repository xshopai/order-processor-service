package com.xshopai.orderprocessor.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import io.dapr.client.DaprClient;
import io.dapr.client.DaprClientBuilder;
import io.dapr.serializer.DaprObjectSerializer;
import io.dapr.utils.TypeRef;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

/**
 * Dapr Configuration
 * Configures ObjectMapper with proper Jackson support for Java 8 date/time types
 */
@Configuration
public class DaprConfig {

    /**
     * Configure ObjectMapper with JSR-310 support for Java 8 date/time types
     * This is needed for serializing OffsetDateTime, LocalDateTime, etc.
     */
    @Bean
    @Primary
    public ObjectMapper objectMapper() {
        ObjectMapper mapper = new ObjectMapper();
        mapper.registerModule(new JavaTimeModule());
        mapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        return mapper;
    }

    /**
     * Create custom DaprObjectSerializer that uses our configured ObjectMapper
     * TEMPORARILY DISABLED - has compilation issues with Dapr SDK version
     * TODO: Fix serializer implementation for Java 8 date/time types
     */
    /*
    @Bean
    public DaprObjectSerializer daprObjectSerializer(ObjectMapper objectMapper) {
        return new DaprObjectSerializer() {
            @Override
            public byte[] serialize(Object state) {
                try {
                    return objectMapper.writeValueAsBytes(state);
                } catch (Exception e) {
                    throw new RuntimeException("Error serializing object", e);
                }
            }

            @Override
            public <T> T deserialize(byte[] content, Class<T> clazz) {
                try {
                    if (content == null || content.length == 0) {
                        return null;
                    }
                    return objectMapper.readValue(content, clazz);
                } catch (Exception e) {
                    throw new RuntimeException("Error deserializing object", e);
                }
            }

            @Override
            public <T> T deserialize(byte[] content, TypeRef<T> type) {
                try {
                    if (content == null || content.length == 0) {
                        return null;
                    }
                    return objectMapper.readValue(content, objectMapper.constructType(type.getType()));
                } catch (Exception e) {
                    throw new RuntimeException("Error deserializing object with TypeRef", e);
                }
            }

            @Override
            public String getContentType() {
                return "application/json";
            }
        };
    }
    */

    /**
     * Create DaprClient bean with default serializer
     * Once custom serializer is fixed, update to use it
     */
    @Bean
    public DaprClient daprClient() {
        return new DaprClientBuilder()
                // .withObjectSerializer(serializer) // TODO: Re-enable when serializer fixed
                .build();
    }
}
