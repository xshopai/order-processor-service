<div align="center">

# 🔄 Order Processor Service

**Saga orchestration microservice for distributed order processing in the xshopai platform**

[![Java](https://img.shields.io/badge/Java-17+-ED8B00?style=for-the-badge&logo=openjdk&logoColor=white)](https://adoptium.net)
[![Spring Boot](https://img.shields.io/badge/Spring_Boot-3.3-6DB33F?style=for-the-badge&logo=springboot&logoColor=white)](https://spring.io/projects/spring-boot)
[![PostgreSQL](https://img.shields.io/badge/PostgreSQL-12+-4169E1?style=for-the-badge&logo=postgresql&logoColor=white)](https://postgresql.org)
[![Dapr](https://img.shields.io/badge/Dapr-Enabled-0D597F?style=for-the-badge&logo=dapr&logoColor=white)](https://dapr.io)
[![License](https://img.shields.io/badge/License-MIT-green?style=for-the-badge)](LICENSE)

[Getting Started](#-getting-started) •
[Documentation](#-documentation) •
[Architecture](#-architecture) •
[Contributing](#-contributing)

</div>

---

## 🎯 Overview

The **Order Processor Service** implements a choreography-based saga pattern for distributed order processing. It coordinates transactions across payment, inventory, and shipping services, handling compensations on failure to maintain eventual consistency. Built with Spring Boot 3 and Java 17, it uses Flyway for database migrations and integrates with the Dapr service mesh.

---

## ✨ Key Features

<table>
<tr>
<td width="50%">

### 🔄 Saga Orchestration

- Choreography-based saga pattern
- Distributed transaction coordination
- Automatic compensation on failure
- Eventually consistent transactions

</td>
<td width="50%">

### 📡 Event-Driven Processing

- Event sourcing for order processing
- Dapr pub/sub integration (RabbitMQ)
- Idempotency and retry mechanisms
- Comprehensive event logging

</td>
</tr>
<tr>
<td width="50%">

### 🗄️ Data Management

- PostgreSQL with Spring Data JPA
- Flyway database migrations
- Spring Actuator health checks
- Structured event storage

</td>
<td width="50%">

### 🛡️ Enterprise Security

- JWT authentication (Spring Security)
- Service-to-service token validation
- OpenTelemetry distributed tracing
- Spring Boot Actuator monitoring

</td>
</tr>
</table>

---

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

---

## 🚀 Getting Started

### Prerequisites

- Java JDK 17+
- Maven 3.9+
- PostgreSQL 12+
- Docker & Docker Compose (optional)
- Dapr CLI (for production-like setup)

### Quick Start with Docker Compose

```bash
# Clone the repository
git clone https://github.com/xshopai/order-processor-service.git
cd order-processor-service

# Start PostgreSQL + service
docker-compose up -d

# Verify the service is healthy
curl http://localhost:8007/actuator/health
```

### Local Development Setup

<details>
<summary><b>🔧 Without Dapr (Simple Setup)</b></summary>

```bash
# Start PostgreSQL
docker-compose -f docker-compose.db.yml up -d

# Build the project
mvn clean install

# Run with Spring profile
mvn spring-boot:run -Dspring-boot.run.profiles=direct
```

📖 See [Local Development Guide](docs/LOCAL_DEVELOPMENT.md) for detailed instructions.

</details>

<details>
<summary><b>⚡ With Dapr (Production-like)</b></summary>

```bash
# Ensure Dapr is initialized
dapr init

# Start with Dapr sidecar
./run.sh       # Linux/Mac
.\run.ps1      # Windows

# Or manually
dapr run \
  --app-id order-processor-service \
  --app-port 8007 \
  --dapr-http-port 3500 \
  --resources-path .dapr/components \
  --config .dapr/config.yaml \
  -- mvn spring-boot:run -Dspring-boot.run.profiles=dapr
```

> **Note:** All services now use the standard Dapr ports (3500 for HTTP, 50001 for gRPC).

</details>

---

## 📚 Documentation

| Document                                          | Description                                        |
| :------------------------------------------------ | :------------------------------------------------- |
| 📘 [Local Development](docs/LOCAL_DEVELOPMENT.md) | Step-by-step local setup and development workflows |
| ☁️ [Azure Container Apps](docs/ACA_DEPLOYMENT.md) | Deploy to serverless containers with built-in Dapr |

---

## 🧪 Testing

```bash
# Run all tests
mvn test

# Build without tests
mvn clean install -DskipTests

# Run with specific profile
mvn test -Dspring.profiles.active=test

# Package for deployment
mvn clean package
```

### Test Coverage

| Metric        | Status              |
| :------------ | :------------------ |
| Unit Tests    | ✅ JUnit 5          |
| Integration   | ✅ Spring Boot Test |
| Security Scan | ✅ Spring Security  |

---

## 🏗️ Project Structure

```
order-processor-service/
├── 📁 src/
│   ├── 📁 main/
│   │   ├── 📁 java/com/xshopai/orderprocessor/
│   │   │   ├── 📁 config/         # Spring configuration
│   │   │   ├── 📁 controller/     # REST controllers
│   │   │   ├── 📁 model/          # JPA entities
│   │   │   ├── 📁 repository/     # Spring Data JPA repos
│   │   │   ├── 📁 service/        # Business logic (saga)
│   │   │   ├── 📁 event/          # Event handling
│   │   │   └── 📁 security/       # JWT + Spring Security
│   │   └── 📁 resources/
│   │       ├── 📄 application.yml  # Default config
│   │       ├── 📄 application-dapr.yml
│   │       └── 📄 application-direct.yml
│   └── 📁 test/                    # Test suite
├── 📁 docs/                        # Documentation
├── 📁 scripts/                     # Utility scripts
├── 📁 .dapr/                       # Dapr configuration
│   ├── 📁 components/              # Pub/sub, state store configs
│   └── 📄 config.yaml              # Dapr runtime configuration
├── 📄 docker-compose.yml           # Full service stack
├── 📄 docker-compose.db.yml        # PostgreSQL only
├── 📄 Dockerfile                   # Production container image
└── 📄 pom.xml                      # Maven dependencies
```

---

## 🔧 Technology Stack

| Category          | Technology                                       |
| :---------------- | :----------------------------------------------- |
| ☕ Runtime        | Java 17+ (JDK 21 in Docker)                      |
| 🌐 Framework      | Spring Boot 3.3 with Spring Security             |
| 🗄️ Database       | PostgreSQL 12+ with Spring Data JPA + Flyway     |
| 📨 Messaging      | Dapr Pub/Sub (RabbitMQ) + Dapr SDK               |
| 🔐 Authentication | JWT Tokens + Spring Security                     |
| 🧪 Testing        | JUnit 5 + Spring Boot Test                       |
| 📊 Observability  | Spring Actuator + OpenTelemetry + Lombok logging |

---

## ⚡ Quick Reference

```bash
# 🐳 Docker Compose
docker-compose up -d              # Start all services
docker-compose down               # Stop all services
docker-compose -f docker-compose.db.yml up -d  # PostgreSQL only

# ☕ Local Development
mvn spring-boot:run               # Run (default profile)
mvn spring-boot:run -Dspring-boot.run.profiles=dev  # Dev profile

# ⚡ Dapr Development
./run.sh                          # Linux/Mac
.\run.ps1                         # Windows

# 🧪 Testing
mvn test                          # Run all tests
mvn clean package                 # Build JAR

# 🔍 Health Check
curl http://localhost:8007/actuator/health
```

---

## 🤝 Contributing

We welcome contributions! Please follow these steps:

1. **Fork** the repository
2. **Create** a feature branch
   ```bash
   git checkout -b feature/amazing-feature
   ```
3. **Write** tests for your changes
4. **Run** the test suite
   ```bash
   mvn test
   ```
5. **Commit** your changes
   ```bash
   git commit -m 'feat: add amazing feature'
   ```
6. **Push** to your branch
   ```bash
   git push origin feature/amazing-feature
   ```
7. **Open** a Pull Request

Please ensure your PR:

- ✅ Passes all existing tests
- ✅ Includes tests for new functionality
- ✅ Follows the existing code style
- ✅ Updates documentation as needed

---

## 🆘 Support

| Resource         | Link                                                                                 |
| :--------------- | :----------------------------------------------------------------------------------- |
| 🐛 Bug Reports   | [GitHub Issues](https://github.com/xshopai/order-processor-service/issues)           |
| 📖 Documentation | [docs/](docs/)                                                                       |
| 💬 Discussions   | [GitHub Discussions](https://github.com/xshopai/order-processor-service/discussions) |

---

## 📄 License

This project is part of the **xshopai** e-commerce platform.
Licensed under the MIT License - see [LICENSE](LICENSE) for details.

---

<div align="center">

**[⬆ Back to Top](#-order-processor-service)**

Made with ❤️ by the xshopai team

</div>
