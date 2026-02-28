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

# Copy profile config to application.yml:
#   application-dev.yml  — written by Codespace setup.sh with Docker hostnames
#   application-direct.yml — present in repo, used for local dev (localhost ports)
if [ -f "src/main/resources/application-dev.yml" ]; then
    cp "src/main/resources/application-dev.yml" "src/main/resources/application.yml"
    echo "✅ Copied application-dev.yml → application.yml (Codespace)"
elif [ -f "src/main/resources/application-direct.yml" ]; then
    cp "src/main/resources/application-direct.yml" "src/main/resources/application.yml"
    echo "✅ Copied application-direct.yml → application.yml (local dev)"
fi

# Run with Spring Boot (skip tests for faster startup)
mvn spring-boot:run -Dmaven.test.skip=true
