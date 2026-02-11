#!/bin/bash

# Order Processor Service - Run with Dapr

echo "Starting Order Processor Service with Dapr..."
echo "Service will be available at: http://localhost:8007"
echo "Dapr HTTP endpoint: http://localhost:3507"
echo "Dapr gRPC endpoint: localhost:50007"
echo ""

dapr run \
  --app-id order-processor-service \
  --app-port 8007 \
  --dapr-http-port 3507 \
  --dapr-grpc-port 50007 \
  --log-level info \
  --config ./.dapr/config.yaml \
  --resources-path ./.dapr/components \
  -- mvn spring-boot:run -Dmaven.test.skip=true -Dspring-boot.run.profiles=dapr

