#!/bin/bash

# Order Processor Service - Run with Dapr Pub/Sub

echo "Starting Order Processor Service (Dapr Pub/Sub)..."
echo "Service will be available at: http://localhost:8007"
echo "Dapr HTTP endpoint: http://localhost:3507"
echo "Dapr gRPC endpoint: localhost:50007"
echo ""

# Kill any processes using required ports (prevents "address already in use" errors)
for PORT in 8007 3507 50007; do
    for pid in $(netstat -ano 2>/dev/null | grep ":$PORT" | grep LISTENING | awk '{print $5}' | sort -u); do
        echo "Killing process $pid on port $PORT..."
        taskkill //F //PID $pid 2>/dev/null
    done
done

dapr run \
  --app-id order-processor-service \
  --app-port 8007 \
  --dapr-http-port 3507 \
  --dapr-grpc-port 50007 \
  --log-level info \
  --config ./.dapr/config.yaml \
  --resources-path ./.dapr/components \
  -- mvn spring-boot:run -Dmaven.test.skip=true -Dspring-boot.run.profiles=dapr

