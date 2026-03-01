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

# Fix JAVA_HOME if it points to a non-existent directory (e.g. jdk-25 vs jdk-25.0.2)
if [ -n "$JAVA_HOME" ] && [ ! -d "$JAVA_HOME" ]; then
    JAVA_HOME=$(java -XshowSettings:property -version 2>&1 | grep 'java.home' | awk '{print $NF}')
    export JAVA_HOME
    echo "✅ Fixed JAVA_HOME → $JAVA_HOME"
fi

# Navigate to service root directory
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
SERVICE_DIR="$(dirname "$SCRIPT_DIR")"
cd "$SERVICE_DIR"

# Run with Spring Boot using 'direct' profile (works on both local and Codespace)
echo "✅ Using Spring profile: direct"
mvn spring-boot:run -Dmaven.test.skip=true -Dspring-boot.run.profiles=direct
