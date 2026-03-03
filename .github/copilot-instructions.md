# Copilot Instructions — order-processor-service

## Service Identity

- **Name**: order-processor-service
- **Purpose**: Order processing orchestrator — choreography-based saga pattern for order fulfillment (payment, inventory, shipping)
- **Port**: 8007
- **Language**: Java 17
- **Framework**: Spring Boot 3.3.1
- **Database**: PostgreSQL (port 5435) via Spring Data JPA + Flyway migrations
- **Dapr App ID**: `order-processor-service`

## Architecture

- **Pattern**: Choreography-based saga orchestrator — listens to order events, coordinates payment/inventory/shipping steps
- **API Style**: RESTful (admin/operational endpoints) + Dapr subscription endpoints (`/dapr/events/*`)
- **Authentication**: Spring Security with JWT validation
- **Messaging**: Dapr pub/sub (consumer + publisher) via Dapr Java SDK
- **Event Format**: CloudEvents 1.0 specification

## Project Structure

```
order-processor-service/
├── src/main/java/com/xshopai/orderprocessor/
│   ├── OrderProcessorApplication.java
│   ├── config/              # DaprConfig, SecurityConfig, WebConfig
│   ├── controller/          # REST + Admin endpoints
│   ├── events/
│   │   ├── consumer/        # Dapr subscription handlers (@PostMapping /dapr/events/*)
│   │   └── publisher/       # DaprEventPublisher
│   ├── messaging/           # MessagingProvider abstraction (Dapr vs RabbitMQ)
│   ├── model/
│   │   ├── entity/          # JPA entities (OrderProcessingSaga)
│   │   ├── events/          # Event POJOs (OrderCreatedEvent, PaymentProcessedEvent, etc.)
│   │   └── dto/             # Data transfer objects
│   ├── repository/          # Spring Data JPA repositories
│   └── service/             # SagaOrchestratorService, SagaMetricsService
├── src/main/resources/
│   ├── application.yml      # Default config (local, no Dapr)
│   ├── application-dapr.yml # Dapr profile config
│   └── db/migration/        # Flyway SQL migrations
├── src/test/
├── .dapr/components/
└── pom.xml
```

## Code Conventions

- **Java 17** with Spring Boot 3.3 conventions
- Use **Lombok** annotations: `@RequiredArgsConstructor`, `@Slf4j`, `@Data`, `@Builder`
- Use **Spring Data JPA** repositories (extend `JpaRepository`)
- Use **Jackson** with `JavaTimeModule` for Java 8 date/time serialization
- Use `@Transactional` on service methods that modify saga state
- `@EnableAsync` for async event processing
- Spring profiles: `default` (local/direct), `dapr` (with Dapr sidecar)
- Event consumers are `@RestController` classes with `@PostMapping("/dapr/events/{topic}")`

## Saga Pattern

- **OrderProcessingSaga** entity tracks saga state through lifecycle:
  - `PENDING_PAYMENT_CONFIRMATION` → `PAYMENT_CONFIRMED` → `INVENTORY_RESERVED` → `SHIPPING_INITIATED` → `COMPLETED`
- Admin must manually confirm payment (Amazon admin portal pattern)
- Compensating transactions on failure (release inventory, cancel payment)
- Saga stores serialized order items, shipping/billing addresses as JSON columns
- Max retry attempts configurable (`saga.retry.max-attempts`)
- Metrics tracked via `SagaMetricsService`

## Event Subscriptions

| Event                | Action                                        |
| :------------------- | :-------------------------------------------- |
| `order.created`      | Start new saga (PENDING_PAYMENT_CONFIRMATION) |
| `order.cancelled`    | Cancel saga, compensate                       |
| `payment.processed`  | Advance to inventory reservation              |
| `inventory.reserved` | Advance to shipping                           |
| `return.approved`    | Handle return processing                      |

## Database Patterns

- PostgreSQL via Spring Data JPA
- Flyway migrations in `src/main/resources/db/migration/`
- Entity: `OrderProcessingSaga` with JPA annotations
- JSON columns for order items, addresses (stored as `TEXT`, serialized via Jackson)
- Repository: `OrderProcessingSagaRepository` with custom query methods

## Testing Requirements

- All new service methods MUST have unit tests
- All new event handlers MUST be tested with mock event payloads
- Use **JUnit 5** + **Spring Boot Test** as the test framework
- Use **Mockito** for mocking in unit tests
- Mock Dapr client and messaging provider in unit tests
- Do NOT call real databases or downstream services in unit tests
- Run: `mvn test`
- Integration: `mvn verify`

## Dapr Integration

- **Pub/Sub Consumer**: `@PostMapping("/dapr/events/{topic}")` endpoints receive CloudEvents
- **Pub/Sub Publisher**: `DaprEventPublisher` wraps `DaprClient.publishEvent()`
- **Dapr Java SDK**: `io.dapr:dapr-sdk:1.12.0` + `dapr-sdk-springboot`
- **MessagingProvider**: Abstraction supporting both Dapr pub/sub and direct RabbitMQ
- **Ports**: Dapr HTTP 3500, Dapr gRPC 50001

## Security Rules

- JWT MUST be validated via Spring Security before accessing any admin or operational endpoint
- Dapr subscription endpoints (`/dapr/events/*`) are authenticated by the Dapr sidecar — no additional JWT required
- Never trust client-provided order or saga IDs — validate against database state
- Compensating transactions MUST be idempotent — guard against duplicate saga state transitions
- Never expose internal saga state or database IDs in API responses

## Error Handling Contract

All errors MUST follow this JSON structure:

```json
{
  "error": {
    "code": "STRING_CODE",
    "message": "Human readable message",
    "correlationId": "uuid"
  }
}
```

- Never expose stack traces in production
- Use `@ControllerAdvice` for centralized exception handling

## Logging Rules

- Use structured JSON logging via **Logback** / **SLF4J**
- Use `@Slf4j` (Lombok) on all service and handler classes
- Include:
  - timestamp
  - level
  - serviceName
  - correlationId
  - message
- Never log JWT tokens
- Never log secrets

## Non-Goals

- This service does NOT manage the order record directly — that is handled by order-service
- This service does NOT process payments directly — it reacts to payment events
- This service does NOT expose a public-facing API — operational endpoints are admin-only
- This service does NOT handle authentication or JWT issuance

## Environment Variables

```
PORT=8007
SPRING_PROFILES_ACTIVE=local          # or 'dapr'
SPRING_DATASOURCE_URL=jdbc:postgresql://localhost:5435/order_processor_db
SPRING_DATASOURCE_USERNAME=postgres
SPRING_DATASOURCE_PASSWORD=postgres
DAPR_HTTP_PORT=3500
DAPR_GRPC_PORT=50001
```

## Common Commands

```bash
mvn spring-boot:run                  # Run locally
mvn spring-boot:run -Dspring-boot.run.profiles=dapr  # Run with Dapr profile
mvn test                             # Unit tests
mvn verify                           # Integration tests
mvn clean package -DskipTests        # Build JAR
```
