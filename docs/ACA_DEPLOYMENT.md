# Order Processor Service - Azure Container Apps Deployment

## Overview

This guide covers deploying the Order Processor Service (Java/Spring Boot) to Azure Container Apps (ACA) with Dapr integration for event-driven order processing workflows.

## Prerequisites

- Azure CLI installed and authenticated
- Docker installed
- Maven installed
- Azure subscription with appropriate permissions
- Azure Container Registry (ACR) created
- Azure PostgreSQL Flexible Server

## Quick Deployment

### Using the Deployment Script

**PowerShell (Windows):**

```powershell
cd scripts
.\aca.ps1
```

**Bash (macOS/Linux):**

```bash
cd scripts
./aca.sh
```

## Manual Deployment

### 1. Set Variables

```bash
RESOURCE_GROUP="rg-xshopai-aca"
LOCATION="swedencentral"
ACR_NAME="acrxshopaiaca"
ENVIRONMENT_NAME="cae-xshopai-aca"
POSTGRES_SERVER="psql-xshopai-aca"
APP_NAME="order-processor-service"
APP_PORT=1007
DATABASE_NAME="order_processor_db"
```

### 2. Create PostgreSQL Database

```bash
az postgres flexible-server create \
  --name $POSTGRES_SERVER \
  --resource-group $RESOURCE_GROUP \
  --location $LOCATION \
  --admin-user pgadmin \
  --admin-password <password> \
  --sku-name Standard_B1ms \
  --tier Burstable \
  --storage-size 32

az postgres flexible-server db create \
  --server-name $POSTGRES_SERVER \
  --resource-group $RESOURCE_GROUP \
  --database-name $DATABASE_NAME
```

### 3. Build and Push Image

```bash
# Build with Maven
mvn clean package -DskipTests

# Login to ACR
az acr login --name $ACR_NAME

# Build and push Docker image
docker build -t $ACR_NAME.azurecr.io/$APP_NAME:latest .
docker push $ACR_NAME.azurecr.io/$APP_NAME:latest
```

### 4. Deploy Container App

```bash
JDBC_URL="jdbc:postgresql://${POSTGRES_SERVER}.postgres.database.azure.com:5432/${DATABASE_NAME}?sslmode=require"

az containerapp create \
  --name $APP_NAME \
  --resource-group $RESOURCE_GROUP \
  --environment $ENVIRONMENT_NAME \
  --image $ACR_NAME.azurecr.io/$APP_NAME:latest \
  --registry-server $ACR_NAME.azurecr.io \
  --target-port $APP_PORT \
  --ingress internal \
  --min-replicas 1 \
  --max-replicas 5 \
  --cpu 0.5 \
  --memory 1Gi \
  --enable-dapr \
  --dapr-app-id $APP_NAME \
  --dapr-app-port $APP_PORT \
  --secrets "db-password=<password>" \
  --env-vars \
    "SERVER_PORT=$APP_PORT" \
    "SPRING_PROFILES_ACTIVE=prod" \
    "SPRING_DATASOURCE_URL=$JDBC_URL" \
    "SPRING_DATASOURCE_USERNAME=pgadmin" \
    "SPRING_DATASOURCE_PASSWORD=secretref:db-password" \
    "DAPR_HTTP_PORT=3500"
```

## Event Subscriptions

The service subscribes to order events via Dapr pub/sub:

- `order.created` - Start order processing workflow
- `payment.completed` - Continue after payment success
- `payment.failed` - Handle payment failure
- `inventory.reserved` - Continue after inventory reservation

## Configuration

### Environment Variables

| Variable                 | Description       | Default |
| ------------------------ | ----------------- | ------- |
| `SERVER_PORT`            | HTTP server port  | 1007    |
| `SPRING_PROFILES_ACTIVE` | Spring profile    | prod    |
| `SPRING_DATASOURCE_URL`  | JDBC URL          | -       |
| `DAPR_HTTP_PORT`         | Dapr sidecar port | 3500    |

## Monitoring

```bash
az containerapp logs show \
  --name $APP_NAME \
  --resource-group $RESOURCE_GROUP \
  --follow
```

## Troubleshooting

### Database Connection Issues

1. Verify PostgreSQL firewall allows Azure services
2. Check JDBC URL format
3. Ensure SSL mode is configured

### Workflow Issues

1. Check Dapr sidecar logs
2. Verify pub/sub component configuration
3. Review event payload formats
