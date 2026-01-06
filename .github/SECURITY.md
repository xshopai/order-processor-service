# Security Policy

## Overview

The Order Processor Service is a Java Spring Boot microservice implementing choreography-based saga patterns for complex order processing workflows. It coordinates order fulfillment, inventory management, payment processing, and shipping across multiple services within the xshopai platform.

## Supported Versions

We provide security updates for the following versions:

| Version | Supported          |
| ------- | ------------------ |
| 1.0.x   | :white_check_mark: |
| < 1.0   | :x:                |

## Security Features

### Spring Security Framework

- **Spring Security**: Comprehensive security framework for enterprise applications
- **JWT Authentication**: Secure token-based authentication with JJWT library
- **Method-level Security**: Fine-grained authorization controls
- **CSRF Protection**: Cross-site request forgery prevention

### Microservice Security

- **Service-to-Service Authentication**: Secure inter-service communication
- **Message Queue Security**: Authenticated AMQP communication
- **Distributed Transaction Security**: Secure saga pattern implementation
- **Circuit Breaker Security**: Resilient service communication patterns

### Data Protection

- **JPA Security**: Secure database operations with Hibernate
- **PostgreSQL Security**: Encrypted database connections
- **Flyway Migration Security**: Secure database schema management
- **Data Validation**: Comprehensive input validation with Bean Validation

### Enterprise Security

- **Actuator Security**: Secure health checks and metrics endpoints
- **Micrometer Security**: Secure metrics collection with Prometheus
- **OpenTelemetry**: Distributed tracing with security context
- **Testcontainers**: Secure integration testing with containers

### Monitoring & Observability

- **Logback Security**: Structured logging with sensitive data protection
- **Health Check Security**: Secure service health monitoring
- **Metrics Security**: Authenticated metrics collection
- **Audit Trail**: Comprehensive order processing audit logging

## Security Best Practices

### For Developers

1. **Application Security Configuration**: Secure Spring Boot setup

   ```yaml
   # application-production.yml
   spring:
     datasource:
       url: jdbc:postgresql://localhost:5432/orderprocessor
       username: ${DB_USERNAME}
       password: ${DB_PASSWORD}
       hikari:
         maximum-pool-size: 20
         connection-timeout: 30000
         idle-timeout: 600000
         max-lifetime: 1800000

     security:
       jwt:
         secret: ${JWT_SECRET}
         expiration: 3600000

     rabbitmq:
       host: ${RABBITMQ_HOST}
       port: 5672
       username: ${RABBITMQ_USERNAME}
       password: ${RABBITMQ_PASSWORD}
       ssl:
         enabled: true

   management:
     endpoints:
       web:
         exposure:
           include: health,metrics,prometheus
         base-path: /actuator
     endpoint:
       health:
         show-details: when-authorized
   ```

2. **Security Configuration**: Comprehensive Spring Security setup

   ```java
   @Configuration
   @EnableWebSecurity
   @EnableMethodSecurity(prePostEnabled = true)
   public class SecurityConfig {

       @Bean
       public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
           http
               .csrf(csrf -> csrf.disable()) // API service
               .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
               .authorizeHttpRequests(auth -> auth
                   .requestMatchers("/actuator/health").permitAll()
                   .requestMatchers("/actuator/**").hasRole("ADMIN")
                   .requestMatchers("/api/orders/**").hasAnyRole("USER", "ADMIN")
                   .requestMatchers("/api/admin/**").hasRole("ADMIN")
                   .anyRequest().authenticated()
               )
               .oauth2ResourceServer(oauth2 -> oauth2.jwt(Customizer.withDefaults()));

           return http.build();
       }

       @Bean
       public JwtDecoder jwtDecoder() {
           return NimbusJwtDecoder.withSecretKey(getSecretKey())
               .macAlgorithm(MacAlgorithm.HS256)
               .build();
       }
   }
   ```

3. **Input Validation**: Comprehensive validation with Bean Validation

   ```java
   @Entity
   @Table(name = "order_processing_events")
   public class OrderProcessingEvent {

       @Id
       @GeneratedValue(strategy = GenerationType.UUID)
       private UUID id;

       @NotNull(message = "Order ID is required")
       @Column(name = "order_id", nullable = false)
       private UUID orderId;

       @NotBlank(message = "Event type is required")
       @Size(max = 100, message = "Event type must not exceed 100 characters")
       @Column(name = "event_type", nullable = false, length = 100)
       private String eventType;

       @NotNull(message = "Event status is required")
       @Enumerated(EnumType.STRING)
       @Column(name = "status", nullable = false)
       private EventStatus status;

       @Valid
       @JsonProperty("event_data")
       @Column(name = "event_data", columnDefinition = "jsonb")
       private Map<String, Object> eventData;
   }

   // Service-level validation
   @Service
   @Validated
   public class OrderProcessorService {

       public void processOrder(@Valid @NotNull OrderProcessingRequest request) {
           // Validate business rules
           if (!isValidOrderState(request.getOrderId())) {
               throw new InvalidOrderStateException("Order is not in a valid state for processing");
           }

           // Process with security context
           processOrderSecurely(request);
       }
   }
   ```

4. **Secure Message Processing**: Safe event handling

   ```java
   @Component
   @RabbitListener(queues = "order.processing.queue")
   public class OrderEventProcessor {

       @Autowired
       private OrderProcessorService orderService;

       @Autowired
       private AuditService auditService;

       @RabbitHandler
       @Transactional
       public void processOrderEvent(@Valid @Payload OrderEvent event,
                                   @Header Map<String, Object> headers) {

           try {
               // Validate event source and authenticity
               validateEventSource(event, headers);

               // Log processing attempt
               auditService.logOrderProcessingAttempt(event);

               // Process event securely
               ProcessingResult result = orderService.processEvent(event);

               // Log processing result
               auditService.logOrderProcessingResult(event, result);

           } catch (SecurityException e) {
               log.error("Security violation in order processing: {}", e.getMessage());
               auditService.logSecurityViolation(event, e);
               throw e;
           } catch (Exception e) {
               log.error("Error processing order event: {}", e.getMessage());
               auditService.logProcessingError(event, e);
               throw new OrderProcessingException("Failed to process order event", e);
           }
       }

       private void validateEventSource(OrderEvent event, Map<String, Object> headers) {
           String signature = (String) headers.get("X-Event-Signature");
           if (!eventSignatureValidator.isValidSignature(event, signature)) {
               throw new SecurityException("Invalid event signature");
           }
       }
   }
   ```

### For Deployment

1. **Java Security**:

   - Use Java 17+ with latest security updates
   - Configure JVM security properties
   - Enable Java security manager if required
   - Regular JDK security updates

2. **Database Security**:

   - Enable PostgreSQL SSL connections
   - Use connection pooling with proper limits
   - Implement database migration security
   - Regular database security patches

3. **Container Security**:
   - Use minimal base images
   - Scan container images for vulnerabilities
   - Implement runtime security monitoring
   - Secure container orchestration

## Data Handling

### Sensitive Data Categories

1. **Order Processing Data**:

   - Order lifecycle events and state transitions
   - Customer order information
   - Payment processing status
   - Inventory allocation data

2. **Coordination Data**:

   - Saga transaction coordination data
   - Service communication metadata
   - Processing state and checkpoints
   - Compensation action data

3. **Business Logic Data**:
   - Order fulfillment rules and policies
   - Service integration configurations
   - Processing workflow definitions
   - Error handling and retry policies

### Data Protection Measures

- **Database Encryption**: PostgreSQL encryption at rest
- **Transport Security**: TLS for all service communications
- **Event Encryption**: Encrypted message queue communications
- **Audit Logging**: Comprehensive processing audit trail

### Data Retention

- Order processing events: 5 years (business analysis)
- Saga coordination data: 1 year (operational analysis)
- Error logs: 90 days (troubleshooting)
- Audit trails: 7 years (compliance requirements)

## Vulnerability Reporting

### Reporting Security Issues

Order processor vulnerabilities can affect entire order workflows:

1. **Do NOT** open a public issue
2. **Do NOT** attempt to manipulate order processing
3. **Email** our security team at: <security@xshopai.com>

### Critical Security Areas

- Order workflow manipulation
- Saga transaction tampering
- Message queue security violations
- Service authentication bypass
- Data integrity violations

### Response Timeline

- **6 hours**: Critical order processing vulnerabilities
- **12 hours**: High severity workflow manipulation
- **24 hours**: Medium severity security issues
- **72 hours**: Low severity issues

### Severity Classification

| Severity | Description                                      | Examples                             |
| -------- | ------------------------------------------------ | ------------------------------------ |
| Critical | Order workflow corruption, data integrity issues | Saga tampering, state corruption     |
| High     | Authentication bypass, unauthorized processing   | Service bypass, privilege escalation |
| Medium   | Information disclosure, processing delays        | Data exposure, performance issues    |
| Low      | Minor processing issues, logging problems        | Event formatting, metrics accuracy   |

## Security Testing

### Order Processing Testing

Regular security assessments should include:

- Order workflow security validation
- Saga pattern security testing
- Message queue security verification
- Service authentication testing
- Database security validation

### Automated Security Testing

- Unit tests for secure order processing logic
- Integration tests with Testcontainers
- Load testing for high-volume order processing
- Security tests for authentication and authorization

## Security Configuration

### Required Environment Variables

```bash
# Database Security
DB_URL=jdbc:postgresql://server:5432/orderprocessor?ssl=true&sslmode=require
DB_USERNAME=orderprocessor_user
DB_PASSWORD=secure-database-password

# JWT Security
JWT_SECRET=your-256-bit-jwt-secret-key
JWT_ISSUER=xshopai.OrderProcessor
JWT_AUDIENCE=xshopai.Platform

# Message Queue Security
RABBITMQ_HOST=secure-rabbitmq-server
RABBITMQ_USERNAME=orderprocessor_user
RABBITMQ_PASSWORD=secure-rabbitmq-password
RABBITMQ_SSL_ENABLED=true

# Service Security
SPRING_PROFILES_ACTIVE=production
SERVER_SSL_ENABLED=true
SERVER_SSL_KEY_STORE=classpath:keystore.p12
SERVER_SSL_KEY_STORE_PASSWORD=keystore-password

# Actuator Security
MANAGEMENT_ENDPOINTS_WEB_EXPOSURE_INCLUDE=health,metrics,prometheus
MANAGEMENT_ENDPOINT_HEALTH_SHOW_DETAILS=when-authorized
MANAGEMENT_SECURITY_ENABLED=true

# OpenTelemetry
OTEL_EXPORTER_OTLP_ENDPOINT=http://jaeger:14268/api/traces
OTEL_SERVICE_NAME=order-processor-service
OTEL_RESOURCE_ATTRIBUTES=service.name=order-processor,service.version=1.0.0
```

### Java Security Configuration

```java
// Application security properties
@ConfigurationProperties(prefix = "app.security")
@Component
@Data
public class SecurityProperties {

    private Jwt jwt = new Jwt();
    private MessageQueue messageQueue = new MessageQueue();
    private Audit audit = new Audit();

    @Data
    public static class Jwt {
        private String secret;
        private long expirationMs = 3600000; // 1 hour
        private String issuer;
        private String audience;
    }

    @Data
    public static class MessageQueue {
        private boolean signatureRequired = true;
        private String signingKey;
        private int maxRetries = 3;
        private long retryDelayMs = 1000;
    }

    @Data
    public static class Audit {
        private boolean enabled = true;
        private String auditServiceUrl;
        private boolean encryptAuditData = true;
    }
}

// Method-level security
@Service
@PreAuthorize("hasRole('ORDER_PROCESSOR')")
public class OrderProcessorService {

    @PreAuthorize("hasPermission(#orderId, 'Order', 'PROCESS')")
    public ProcessingResult processOrder(UUID orderId) {
        // Secure order processing logic
    }

    @PreAuthorize("hasRole('ADMIN')")
    public void compensateOrder(UUID orderId) {
        // Secure compensation logic
    }
}
```

## Spring Boot Security Best Practices

### Dependency Security

1. **Maven Security**: Regular dependency updates and vulnerability scanning
2. **OWASP Dependency Check**: Automated vulnerability detection
3. **Snyk Integration**: Continuous security monitoring
4. **Private Repositories**: Use private Maven repositories for internal dependencies

### Configuration Security

1. **Externalized Configuration**: Environment-specific configurations
2. **Secret Management**: Azure Key Vault or Kubernetes secrets
3. **Profile Security**: Secure profile-based configurations
4. **Property Encryption**: Encrypt sensitive configuration values

## Compliance

The Order Processor Service adheres to:

- **SOX Compliance**: Financial order processing controls
- **GDPR**: Customer data protection in order processing
- **Spring Security**: Framework security best practices
- **Java Security**: Platform security guidelines
- **Microservices Security**: Distributed system security patterns

## Performance & Security

### High-Performance Security

- **Connection Pooling**: Secure database connection management
- **Async Processing**: Non-blocking secure operations
- **Circuit Breakers**: Resilient secure service communication
- **Caching Security**: Secure Redis/Hazelcast caching

## Incident Response

### Order Processing Security Incidents

1. **Workflow Corruption**: Immediate processing halt and investigation
2. **Saga Tampering**: Transaction rollback and integrity verification
3. **Service Compromise**: Service isolation and security assessment
4. **Data Integrity**: Comprehensive data validation and restoration

### Recovery Procedures

- Order processing state restoration
- Saga transaction compensation and recovery
- Service configuration security hardening
- Workflow integrity verification

## Contact

For security-related questions or concerns:

- **Email**: <security@xshopai.com>
- **Emergency**: Include "URGENT ORDER PROCESSOR SECURITY" in subject line
- **Business Impact**: Copy <operations@xshopai.com>

---

**Last Updated**: September 8, 2025  
**Next Review**: December 8, 2025  
**Version**: 1.0.0
