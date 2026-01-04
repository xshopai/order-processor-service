package com.xshopai.orderprocessor.config;

// Imports commented out as RestTemplate bean is currently disabled
// import org.springframework.boot.web.client.RestTemplateBuilder;
// import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
// import org.springframework.web.client.RestTemplate;
// import java.time.Duration;

/**
 * Configuration for RestTemplate
 * Currently commented out as services use mock implementations
 * TODO: Uncomment when implementing actual external service calls
 */
@Configuration
public class RestConfig {

    /**
     * RestTemplate with timeout configuration
     * Currently commented out as it's unused in mock implementations
     * 
     * TODO: Uncomment this bean when implementing real service calls:
     * 
     * @Bean
     * public RestTemplate restTemplate(RestTemplateBuilder builder) {
     *     return builder
     *             .setConnectTimeout(Duration.ofSeconds(10))
     *             .setReadTimeout(Duration.ofSeconds(30))
     *             .build();
     * }
     */
}
