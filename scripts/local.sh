#!/bin/bash

# Order Processor Service - Run without Dapr (local development)

echo "Starting Order Processor Service (without Dapr)..."
echo "Service will be available at: http://localhost:8007"
echo ""
echo "Note: Event consumption and saga orchestration will fail without Dapr."
echo "This mode is suitable for isolated development and testing."
echo ""

# Make mvnw executable
chmod +x ./mvnw

# Run with Spring Boot
./mvnw spring-boot:run
