# Order Processor Service - Local Development Guide

## Prerequisites

- Java 21 (JDK)
- Maven 3.9+
- PostgreSQL 14+ (local or Docker)
- Dapr CLI (for event processing)

## Quick Start

### 1. Start PostgreSQL

```bash
docker run -d \
  --name postgres-order-processor \
  -e POSTGRES_USER=orderadmin \
  -e POSTGRES_PASSWORD=orderpass \
  -e POSTGRES_DB=order_processor_db \
  -p 5433:5432 \
  postgres:14
```

### 2. Configure Application

Create `src/main/resources/application-local.yml`:

```yaml
spring:
  datasource:
    url: jdbc:postgresql://localhost:5433/order_processor_db
    username: orderadmin
    password: orderpass
  jpa:
    hibernate:
      ddl-auto: update

server:
  port: 1007

dapr:
  http-port: 3507
```

### 3. Build the Project

```bash
./mvnw clean package -DskipTests
```

### 4. Run the Service

Without Dapr:

```bash
./mvnw spring-boot:run -Dspring-boot.run.profiles=local
```

With Dapr:

```bash
./run.sh
```

## Event Subscriptions

Order Processor subscribes to:

| Event                | Action                                   |
| -------------------- | ---------------------------------------- |
| `order.created`      | Start order processing workflow          |
| `payment.completed`  | Update order status, trigger fulfillment |
| `payment.failed`     | Cancel order, notify customer            |
| `inventory.reserved` | Confirm stock allocation                 |

## API Endpoints

| Method | Endpoint                  | Description             |
| ------ | ------------------------- | ----------------------- |
| GET    | `/actuator/health`        | Health check            |
| GET    | `/api/orders/{id}/status` | Get processing status   |
| POST   | `/api/orders/{id}/retry`  | Retry failed processing |

## Project Structure

```
order-processor-service/
├── src/main/java/
│   └── com/xshopai/orderprocessor/
│       ├── config/          # Spring configuration
│       ├── controller/      # REST controllers
│       ├── service/         # Business logic
│       ├── workflow/        # Order processing workflows
│       ├── event/           # Event handlers
│       └── repository/      # Data access
├── src/main/resources/
│   └── application.yml
├── pom.xml
└── Dockerfile
```

## Testing

```bash
# Run all tests
./mvnw test

# Run integration tests
./mvnw verify -Pintegration-test
```

## Troubleshooting

### Build Failures

- Ensure Java 21 is installed: `java -version`
- Clear Maven cache: `./mvnw dependency:purge-local-repository`

### Database Connection Issues

- Verify PostgreSQL is running on port 5433
- Check connection string in application-local.yml
