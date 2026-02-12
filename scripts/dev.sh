#!/bin/bash

# Order Processor Service - Run with direct RabbitMQ (local development)

echo "Starting Order Processor Service (Direct RabbitMQ)..."
echo "Service will be available at: http://localhost:8007"
echo ""

# Kill any process using port 8007 (prevents "address already in use" errors)
PORT=8007
for pid in $(netstat -ano 2>/dev/null | grep ":$PORT" | grep LISTENING | awk '{print $5}' | sort -u); do
    echo "Killing process $pid on port $PORT..."
    taskkill //F //PID $pid 2>/dev/null
done

# Navigate to service root directory
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
SERVICE_DIR="$(dirname "$SCRIPT_DIR")"
cd "$SERVICE_DIR"

# Run with Spring Boot using dev profile (skip tests for faster startup)
mvn spring-boot:run -Dspring-boot.run.profiles=dev -Dmaven.test.skip=true
