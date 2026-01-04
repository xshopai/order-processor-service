# 🔄 Order Processor Service

Saga orchestration microservice for xshop.ai - implements choreography-based saga pattern for distributed order processing transactions across payment, inventory, and shipping services.

## 🚀 Quick Start

### Prerequisites

- **Java** 21+ ([Download](https://adoptium.net/))
- **Maven** 3.9+ ([Install Guide](https://maven.apache.org/install.html))
- **PostgreSQL** 12+ ([Download](https://www.postgresql.org/download/))
- **Dapr CLI** 1.16+ ([Install Guide](https://docs.dapr.io/getting-started/install-dapr-cli/))

### Setup

**1. Start PostgreSQL**
```bash
# Using Docker (recommended)
docker run -d --name order-processor-postgres -p 5432:5432 \
  -e POSTGRES_PASSWORD=postgres \
  -e POSTGRES_DB=order_processor_db \
  postgres:12

# Or install PostgreSQL locally
```

**2. Clone & Build**
```bash
git clone https://github.com/xshopai/order-processor-service.git
cd order-processor-service
mvn clean install
```

**3. Configure Environment**
```bash
# Edit application.properties or create application-dev.properties
# spring.datasource.url=jdbc:postgresql://localhost:5432/order_processor_db
# spring.datasource.username=postgres
# spring.datasource.password=postgres
```

**4. Initialize Dapr**
```bash
# First time only
dapr init
```

**5. Run Service**
```bash
# Start with Dapr (recommended)
./run.sh       # Linux/Mac
.\run.ps1      # Windows

# Or run directly
mvn spring-boot:run
```

**6. Verify**
```bash
# Check health
curl http://localhost:8080/actuator/health

# Should return: {"status":"UP"...}
```

### Common Commands

```bash
# Run tests
mvn test

# Build package
mvn clean package

# Run with profile
mvn spring-boot:run -Dspring-boot.run.profiles=dev

# Skip tests
mvn clean install -DskipTests
```

## 📚 Documentation

| Document | Description |
|----------|-------------|
| [📖 Developer Guide](docs/DEVELOPER_GUIDE.md) | Local setup, debugging, daily workflows |
| [📘 Technical Reference](docs/TECHNICAL.md) | Architecture, security, monitoring |
| [🤝 Contributing](docs/CONTRIBUTING.md) | Contribution guidelines and workflow |

**API Documentation**: See `.dapr/README.md` for Dapr configuration and `src/main/java/com/xshopai/orderprocessor/` for endpoint definitions.

## ⚙️ Configuration

### Required Environment Variables

```bash
# Service
SPRING_PROFILES_ACTIVE=development
SERVER_PORT=8080

# Database
SPRING_DATASOURCE_URL=jdbc:postgresql://localhost:5432/order_processor_db
SPRING_DATASOURCE_USERNAME=postgres
SPRING_DATASOURCE_PASSWORD=postgres

# JWT
JWT_SECRET=your-secret-key-min-32-characters

# Dapr
DAPR_HTTP_PORT=3510
DAPR_GRPC_PORT=50010
DAPR_APP_ID=order-processor-service
```

See [application.properties](src/main/resources/application.properties) for complete configuration options.

## ✨ Key Features

- Choreography-based saga pattern
- Distributed transaction coordination
- Event sourcing for order processing
- Compensation logic for failed transactions
- Integration with payment, inventory, and shipping services
- Idempotency and retry mechanisms
- Comprehensive event logging
- Spring Boot 3.x with Java 21

## 🏗️ Architecture

**Saga Orchestration Pattern:**
```
Order Created → Payment → Inventory → Shipping → Order Completed
                  ↓          ↓           ↓
            Compensate  Compensate  Compensate (on failure)
```

- Implements choreography pattern (event-driven)
- Each service publishes events, processor orchestrates
- Automatic compensation on failure
- Eventually consistent transactions

## 🔗 Related Services

- [order-service](https://github.com/xshopai/order-service) - Order management
- [payment-service](https://github.com/xshopai/payment-service) - Payment processing
- [inventory-service](https://github.com/xshopai/inventory-service) - Inventory management

## 📄 License

MIT License - see [LICENSE](LICENSE)

## 📞 Support

- **Issues**: [GitHub Issues](https://github.com/xshopai/order-processor-service/issues)
- **Discussions**: [GitHub Discussions](https://github.com/xshopai/order-processor-service/discussions)
- **Documentation**: [docs/](docs/)