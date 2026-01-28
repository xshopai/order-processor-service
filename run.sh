#!/bin/bash
# Order Processor Service - Bash Run Script with Dapr
# Port: 8007, Dapr HTTP: 3500, Dapr gRPC: 50001

echo ""
echo "============================================"
echo "Starting order-processor-service with Dapr..."
echo "============================================"
echo ""

# Kill any existing processes on ports
echo "Cleaning up existing processes..."

# Kill processes on port 8007 (app port)
lsof -ti:8007 | xargs kill -9 2>/dev/null || true

# Kill processes on port 3500 (Dapr HTTP port)
lsof -ti:3500 | xargs kill -9 2>/dev/null || true

# Kill processes on port 50001 (Dapr gRPC port)
lsof -ti:50001 | xargs kill -9 2>/dev/null || true

sleep 2

echo ""
echo "Starting with Dapr sidecar..."
echo "App ID: order-processor-service"
echo "App Port: 8007"
echo "Dapr HTTP Port: 3500"
echo "Dapr gRPC Port: 50001"
echo ""

dapr run \
  --app-id order-processor-service \
  --app-port 8007 \
  --dapr-http-port 3500 \
  --dapr-grpc-port 50001 \
  --log-level error \
  --resources-path ./.dapr/components \
  --config ./.dapr/config.yaml \
  --enable-app-health-check \
  --app-health-check-path /actuator/health \
  --app-health-probe-interval 5 \
  --app-health-probe-timeout 10 \
  --app-health-threshold 2 \
  -- mvn spring-boot:run "-Dspring-boot.run.arguments=--server.port=8007"

echo ""
echo "============================================"
echo "Service stopped."
echo "============================================"
