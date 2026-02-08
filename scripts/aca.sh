#!/bin/bash
# ============================================================================
# Azure Container Apps Deployment Script for Order Processor Service
# ============================================================================
# PREREQUISITE: Run infrastructure deployment first:
#   cd infrastructure/azure/aca/scripts && ./deploy.sh
# ============================================================================

set -e

# ============================================================================
# CONFIGURATION
# ============================================================================

# Service Configuration
SERVICE_NAME="order-processor-service"
APP_PORT=8007
PROJECT_NAME="xshopai"

# Database Configuration
DB_NAME="order_processor_db"
POSTGRES_USERNAME="xshopaiadmin"

# Container Resources
CPU="1.0"
MEMORY="2.0Gi"
MIN_REPLICAS=2
MAX_REPLICAS=10

# Dapr Configuration (fixed for Azure Container Apps)
DAPR_HTTP_PORT=3500
DAPR_GRPC_PORT=50001

# Java 21 Configuration (required for Spring Boot 3.x)
# Override system Java if needed
if [ -d "/c/Program Files/Java/jdk-21.0.10/bin" ]; then
    export JAVA_HOME="C:\\Program Files\\Java\\jdk-21.0.10"
    export PATH="/c/Program Files/Java/jdk-21.0.10/bin:$PATH"
elif [ -d "/c/Program Files/Java/jdk-21/bin" ]; then
    export JAVA_HOME="C:\\Program Files\\Java\\jdk-21"
    export PATH="/c/Program Files/Java/jdk-21/bin:$PATH"
elif [ -d "$HOME/.sdkman/candidates/java/21-open/bin" ]; then
    export JAVA_HOME="$HOME/.sdkman/candidates/java/21-open"
    export PATH="$JAVA_HOME/bin:$PATH"
fi

# ============================================================================
# COLORS & HELPER FUNCTIONS
# ============================================================================
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
CYAN='\033[0;36m'
NC='\033[0m'

print_header() { echo -e "\n${BLUE}=== $1 ===${NC}\n"; }
print_success() { echo -e "${GREEN}✓ $1${NC}"; }
print_warning() { echo -e "${YELLOW}⚠ $1${NC}"; }
print_error() { echo -e "${RED}✗ $1${NC}"; }
print_info() { echo -e "${CYAN}ℹ $1${NC}"; }

# ============================================================================
# PREREQUISITES CHECK
# ============================================================================
print_header "Checking Prerequisites"

command -v az &>/dev/null || { print_error "Azure CLI not installed"; exit 1; }
print_success "Azure CLI installed"

command -v docker &>/dev/null || { print_error "Docker not installed"; exit 1; }
print_success "Docker installed"

command -v mvn &>/dev/null || { print_error "Maven not installed"; exit 1; }
print_success "Maven installed"

az account show &>/dev/null || az login
print_success "Logged into Azure"

# Get script and service directories
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
SERVICE_DIR="$(dirname "$SCRIPT_DIR")"

# ============================================================================
# USER INPUT - Environment & Suffix
# ============================================================================
print_header "Environment Selection"

echo "Available environments: dev, prod"
read -p "Enter environment [dev]: " ENVIRONMENT
ENVIRONMENT="${ENVIRONMENT:-dev}"

[[ "$ENVIRONMENT" =~ ^(dev|prod)$ ]] || { print_error "Invalid environment (dev/prod only)"; exit 1; }
print_success "Environment: $ENVIRONMENT"

echo ""
echo "Find your suffix by running:"
echo -e "  ${BLUE}az group list --query \"[?starts_with(name, 'rg-xshopai-$ENVIRONMENT')].name\" -o tsv${NC}"
echo ""
read -p "Enter infrastructure suffix: " SUFFIX

[[ "$SUFFIX" =~ ^[a-z0-9]{3,6}$ ]] || { print_error "Invalid suffix (3-6 lowercase alphanumeric)"; exit 1; }
print_success "Suffix: $SUFFIX"

# ============================================================================
# DERIVED RESOURCE NAMES (must match infrastructure deployment)
# ============================================================================
RESOURCE_GROUP="rg-${PROJECT_NAME}-${ENVIRONMENT}-${SUFFIX}"
ACR_NAME="${PROJECT_NAME}${ENVIRONMENT}${SUFFIX}"
CONTAINER_ENV="cae-${PROJECT_NAME}-${ENVIRONMENT}-${SUFFIX}"
# Container app name limit is 32 chars, use short name: ca-orderproc-{env}-{suffix}
CONTAINER_APP_NAME="ca-orderproc-${ENVIRONMENT}-${SUFFIX}"
POSTGRES_SERVER="psql-${PROJECT_NAME}-${ENVIRONMENT}-${SUFFIX}"
KEY_VAULT="kv-${PROJECT_NAME}-${ENVIRONMENT}-${SUFFIX}"
MANAGED_IDENTITY="id-${PROJECT_NAME}-${ENVIRONMENT}-${SUFFIX}"

# ============================================================================
# VERIFY INFRASTRUCTURE EXISTS
# ============================================================================
print_header "Verifying Infrastructure"

az group show --name "$RESOURCE_GROUP" &>/dev/null || { print_error "Resource group not found: $RESOURCE_GROUP"; exit 1; }
print_success "Resource Group: $RESOURCE_GROUP"

ACR_LOGIN_SERVER=$(az acr show --name "$ACR_NAME" --query loginServer -o tsv 2>/dev/null) || { print_error "ACR not found: $ACR_NAME"; exit 1; }
print_success "Container Registry: $ACR_LOGIN_SERVER"

az containerapp env show --name "$CONTAINER_ENV" --resource-group "$RESOURCE_GROUP" &>/dev/null || { print_error "Container Env not found: $CONTAINER_ENV"; exit 1; }
print_success "Container Environment: $CONTAINER_ENV"

POSTGRES_HOST=$(az postgres flexible-server show --name "$POSTGRES_SERVER" --resource-group "$RESOURCE_GROUP" --query fullyQualifiedDomainName -o tsv 2>/dev/null) || { print_error "PostgreSQL not found: $POSTGRES_SERVER"; exit 1; }
print_success "PostgreSQL Server: $POSTGRES_HOST"

# Check PostgreSQL server state - start if stopped
POSTGRES_STATE=$(az postgres flexible-server show --name "$POSTGRES_SERVER" --resource-group "$RESOURCE_GROUP" --query state -o tsv 2>/dev/null)
if [ "$POSTGRES_STATE" = "Stopped" ]; then
    print_warning "PostgreSQL server is stopped. Starting it..."
    az postgres flexible-server start --resource-group "$RESOURCE_GROUP" --name "$POSTGRES_SERVER" --output none
    print_info "Waiting for PostgreSQL to be ready..."
    sleep 30
    print_success "PostgreSQL server started"
elif [ "$POSTGRES_STATE" = "Ready" ]; then
    print_success "PostgreSQL server is running"
else
    print_warning "PostgreSQL server state: $POSTGRES_STATE"
fi

# Get Managed Identity (optional)
IDENTITY_ID=$(MSYS_NO_PATHCONV=1 az identity show --name "$MANAGED_IDENTITY" --resource-group "$RESOURCE_GROUP" --query id -o tsv 2>/dev/null || echo "")
[ -n "$IDENTITY_ID" ] && print_success "Managed Identity: $MANAGED_IDENTITY" || print_warning "Managed Identity not found (optional)"

# ============================================================================
# DATABASE SETUP
# ============================================================================
print_header "Database Configuration"

# Create database if not exists
if az postgres flexible-server db show --resource-group "$RESOURCE_GROUP" --server-name "$POSTGRES_SERVER" --database-name "$DB_NAME" &>/dev/null; then
    print_success "Database exists: $DB_NAME"
else
    print_info "Creating database: $DB_NAME"
    az postgres flexible-server db create --resource-group "$RESOURCE_GROUP" --server-name "$POSTGRES_SERVER" --database-name "$DB_NAME" --output none
    print_success "Database created: $DB_NAME"
fi

# ============================================================================
# NETWORK CONFIGURATION
# ============================================================================
print_header "Network Configuration"

# Check/create firewall rule for Azure services
if az postgres flexible-server firewall-rule show --resource-group "$RESOURCE_GROUP" --name "$POSTGRES_SERVER" --rule-name AllowAzureServices &>/dev/null; then
    print_success "Firewall rule 'AllowAzureServices' exists"
else
    print_info "Creating firewall rule for Azure services..."
    az postgres flexible-server firewall-rule create \
        --resource-group "$RESOURCE_GROUP" \
        --name "$POSTGRES_SERVER" \
        --rule-name AllowAzureServices \
        --start-ip-address "0.0.0.0" \
        --end-ip-address "0.0.0.0" \
        --output none
    print_success "Firewall rule 'AllowAzureServices' created"
fi

# ============================================================================
# CONFIRMATION
# ============================================================================
print_header "Deployment Summary"

echo "Environment:        $ENVIRONMENT"
echo "Resource Group:     $RESOURCE_GROUP"
echo "Container App:      $CONTAINER_APP_NAME"
echo "Container Name:     $SERVICE_NAME"
echo "Image:              $ACR_LOGIN_SERVER/$SERVICE_NAME:latest"
echo "PostgreSQL Server:  $POSTGRES_HOST"
echo "Database:           $DB_NAME"
echo "CPU/Memory:         $CPU / $MEMORY"
echo "Replicas:           $MIN_REPLICAS - $MAX_REPLICAS"
echo ""

# ============================================================================
# BUILD & PUSH IMAGE
# ============================================================================
print_header "Building and Pushing Image"

az acr login --name "$ACR_NAME"
cd "$SERVICE_DIR"

# Build Java application
print_info "Building Java application with Maven..."
mvn clean package -DskipTests -q
print_success "Maven build completed"

IMAGE_TAG="$ACR_LOGIN_SERVER/$SERVICE_NAME:latest"
docker build -t "$SERVICE_NAME:latest" .
docker tag "$SERVICE_NAME:latest" "$IMAGE_TAG"
docker push "$IMAGE_TAG"
print_success "Image pushed: $IMAGE_TAG"

# ============================================================================
# DEPLOY CONTAINER APP
# ============================================================================
print_header "Deploying Container App"

ACR_PASSWORD=$(az acr credential show --name "$ACR_NAME" --query "passwords[0].value" -o tsv)

# Map environment to app config (dev->development, prod->production)
APP_CONFIG="development"
[ "$ENVIRONMENT" = "prod" ] && APP_CONFIG="production"

# Retrieve ALL secrets from Key Vault at deployment time
# Secrets are injected as env vars - NO Dapr secretstore access needed at runtime
print_info "Retrieving secrets from Key Vault..."

# Per-service Application Insights (each service has its own App Insights resource)
APP_INSIGHTS_CONN=$(az keyvault secret show --vault-name "$KEY_VAULT" --name "appinsights-order-processor-service" --query "value" -o tsv 2>/dev/null || echo "")
[ -n "$APP_INSIGHTS_CONN" ] && print_success "  appinsights-order-processor-service: retrieved" || print_warning "  appinsights-order-processor-service: not configured"

# JWT secret
JWT_SECRET=$(az keyvault secret show --vault-name "$KEY_VAULT" --name "jwt-secret" --query "value" -o tsv 2>/dev/null || echo "")
[ -n "$JWT_SECRET" ] && print_success "  jwt-secret: retrieved" || print_error "  jwt-secret: NOT FOUND"

# PostgreSQL connection
POSTGRES_SERVER_CONNECTION=$(az keyvault secret show --vault-name "$KEY_VAULT" --name "postgres-server-connection" --query "value" -o tsv 2>/dev/null || echo "")
[ -n "$POSTGRES_SERVER_CONNECTION" ] && print_success "  postgres-server-connection: retrieved" || print_error "  postgres-server-connection: NOT FOUND"

# Parse PostgreSQL credentials from connection string
# Format: jdbc:postgresql://host:5432/?user=xxx&password=xxx&sslmode=require
if [ -n "$POSTGRES_SERVER_CONNECTION" ]; then
    POSTGRES_USER=$(echo "$POSTGRES_SERVER_CONNECTION" | grep -oP 'user=\K[^&]+' || echo "$POSTGRES_USERNAME")
    POSTGRES_PASSWORD=$(echo "$POSTGRES_SERVER_CONNECTION" | grep -oP 'password=\K[^&]+' || echo "")
fi

# Build JDBC URL with database name
JDBC_URL="jdbc:postgresql://${POSTGRES_HOST}:5432/${DB_NAME}?sslmode=require"

# Service tokens
SVC_ORDER_TOKEN=$(az keyvault secret show --vault-name "$KEY_VAULT" --name "service-order-token" --query "value" -o tsv 2>/dev/null || echo "")
SVC_PAYMENT_TOKEN=$(az keyvault secret show --vault-name "$KEY_VAULT" --name "service-payment-token" --query "value" -o tsv 2>/dev/null || echo "")
SVC_INVENTORY_TOKEN=$(az keyvault secret show --vault-name "$KEY_VAULT" --name "service-inventory-token" --query "value" -o tsv 2>/dev/null || echo "")
print_success "  service-*-token: retrieved"

# Environment variables for the container
# All secrets are set as env vars - no Dapr secretstore access needed at runtime
ENV_VARS=(
    "APPLICATIONINSIGHTS_CONNECTION_STRING=$APP_INSIGHTS_CONN"
    "DAPR_GRPC_PORT=$DAPR_GRPC_PORT"
    "DAPR_HTTP_PORT=$DAPR_HTTP_PORT"
    "JWT_SECRET=$JWT_SECRET"
    "MESSAGING_PROVIDER=dapr"
    "OTEL_RESOURCE_ATTRIBUTES=service.version=1.0.0"
    "OTEL_SERVICE_NAME=$SERVICE_NAME"
    "POSTGRES_SERVER_CONNECTION=$POSTGRES_SERVER_CONNECTION"
    "SERVER_PORT=$APP_PORT"
    "SERVICE_INVENTORY_TOKEN=$SVC_INVENTORY_TOKEN"
    "SERVICE_NAME=$SERVICE_NAME"
    "SERVICE_ORDER_TOKEN=$SVC_ORDER_TOKEN"
    "SERVICE_PAYMENT_TOKEN=$SVC_PAYMENT_TOKEN"
    "SPRING_DATASOURCE_PASSWORD=$POSTGRES_PASSWORD"
    "SPRING_DATASOURCE_URL=$JDBC_URL"
    "SPRING_DATASOURCE_USERNAME=$POSTGRES_USER"
    "SPRING_PROFILES_ACTIVE=$APP_CONFIG"
)

if az containerapp show --name "$CONTAINER_APP_NAME" --resource-group "$RESOURCE_GROUP" &>/dev/null; then
    print_info "Updating existing container app..."
    az containerapp update \
        --name "$CONTAINER_APP_NAME" \
        --resource-group "$RESOURCE_GROUP" \
        --image "$IMAGE_TAG" \
        --set-env-vars "${ENV_VARS[@]}" \
        --output none
else
    print_info "Creating new container app..."
    MSYS_NO_PATHCONV=1 az containerapp create \
        --name "$CONTAINER_APP_NAME" \
        --container-name "$SERVICE_NAME" \
        --resource-group "$RESOURCE_GROUP" \
        --environment "$CONTAINER_ENV" \
        --image "$IMAGE_TAG" \
        --registry-server "$ACR_LOGIN_SERVER" \
        --registry-username "$ACR_NAME" \
        --registry-password "$ACR_PASSWORD" \
        --target-port "$APP_PORT" \
        --ingress external \
        --min-replicas "$MIN_REPLICAS" \
        --max-replicas "$MAX_REPLICAS" \
        --cpu "$CPU" \
        --memory "$MEMORY" \
        --enable-dapr \
        --dapr-app-id "$SERVICE_NAME" \
        --dapr-app-port "$APP_PORT" \
        --env-vars "${ENV_VARS[@]}" \
        ${IDENTITY_ID:+--user-assigned "$IDENTITY_ID"} \
        --tags "project=$PROJECT_NAME" "environment=$ENVIRONMENT" "suffix=$SUFFIX" "service=$SERVICE_NAME" \
        --output none
fi
print_success "Container app deployed"

# ============================================================================
# VERIFY DEPLOYMENT
# ============================================================================
print_header "Verifying Deployment"

APP_URL=$(az containerapp show --name "$CONTAINER_APP_NAME" --resource-group "$RESOURCE_GROUP" --query properties.configuration.ingress.fqdn -o tsv)

echo ""
echo -e "${GREEN}✅ DEPLOYMENT SUCCESSFUL${NC}"
echo ""
echo "Application URL:  https://$APP_URL"
echo "Health Check:     https://$APP_URL/actuator/health"
echo ""
echo "Useful commands:"
echo -e "  Logs:      ${BLUE}az containerapp logs show --name $CONTAINER_APP_NAME --resource-group $RESOURCE_GROUP --follow${NC}"
echo -e "  Dapr logs: ${BLUE}az containerapp logs show --name $CONTAINER_APP_NAME --resource-group $RESOURCE_GROUP --container daprd --follow${NC}"
echo -e "  Delete:    ${BLUE}az containerapp delete --name $CONTAINER_APP_NAME --resource-group $RESOURCE_GROUP --yes${NC}"
echo ""

# Optional: Test health endpoint
print_info "Waiting 20s for app to start..."
sleep 20
HTTP_STATUS=$(curl -s -o /dev/null -w "%{http_code}" --max-time 30 "https://$APP_URL/actuator/health" 2>/dev/null || echo "000")
[ "$HTTP_STATUS" = "200" ] && print_success "Health check passed!" || print_warning "Health check returned HTTP $HTTP_STATUS (app may still be starting)"
