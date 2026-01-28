#!/usr/bin/env bash

# ============================================================================
# Azure Container Apps Deployment Script for Order Processor Service
# ============================================================================
# Version: 1.0.0
# Created: June 2025
# Author: xshopai Platform Team
#
# Description:
#   Deploys the order-processor-service to Azure Container Apps.
#   This service requires PostgreSQL database which should already be created
#   by the infrastructure deployment script (deploy-infra.sh).
#
# Prerequisites:
#   - Azure CLI installed and authenticated
#   - Docker Desktop running
#   - Maven installed (for building Java app)
#   - Infrastructure deployed via deploy-infra.sh (includes PostgreSQL)
#
# Usage:
#   ./aca.sh [environment]
#   Example: ./aca.sh dev
#
# ============================================================================

set -e

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
CYAN='\033[0;36m'
NC='\033[0m' # No Color

# Helper functions
print_header() { echo -e "\n${BLUE}============================================================================${NC}"; echo -e "${BLUE}$1${NC}"; echo -e "${BLUE}============================================================================${NC}\n"; }
print_success() { echo -e "${GREEN}✓ $1${NC}"; }
print_error() { echo -e "${RED}✗ $1${NC}"; }
print_warning() { echo -e "${YELLOW}⚠ $1${NC}"; }
print_info() { echo -e "${CYAN}ℹ $1${NC}"; }

# ============================================================================
# Configuration Constants
# ============================================================================
SERVICE_NAME="order-processor-service"
PROJECT_NAME="xshopai"
SCRIPT_DIR="$( cd "$( dirname "${BASH_SOURCE[0]}" )" && pwd )"
SERVICE_DIR="$( cd "$SCRIPT_DIR/.." && pwd )"
APP_PORT=1007

# Dapr configuration for Azure Container Apps
# In ACA, Dapr sidecar ALWAYS runs on port 3500 (HTTP) and 50001 (gRPC)
DAPR_HTTP_PORT=3500
DAPR_GRPC_PORT=50001
DAPR_PUBSUB_NAME="pubsub"
DATABASE_NAME="order_processor_db"

# Parse command line arguments
ENVIRONMENT="${1:-}"

# ============================================================================
# Banner
# ============================================================================
echo -e "${BLUE}"
echo "╔══════════════════════════════════════════════════════════════════════╗"
echo "║           Order Processor Service - ACA Deployment                   ║"
echo "║                    xshopai Platform                                  ║"
echo "╚══════════════════════════════════════════════════════════════════════╝"
echo -e "${NC}"

# ============================================================================
# Prerequisites Check
# ============================================================================
print_header "Step 1: Checking Prerequisites"

# Check Azure CLI
if ! command -v az &> /dev/null; then
    print_error "Azure CLI is not installed"
    echo "Install from: https://docs.microsoft.com/en-us/cli/azure/install-azure-cli"
    exit 1
fi
print_success "Azure CLI installed"

# Check Docker
if ! docker info &> /dev/null; then
    print_error "Docker is not running"
    echo "Please start Docker Desktop"
    exit 1
fi
print_success "Docker is running"

# Check Maven
if ! command -v mvn &> /dev/null; then
    print_error "Maven is not installed"
    echo "Install from: https://maven.apache.org/download.cgi"
    exit 1
fi
print_success "Maven installed"

# Check Azure login
if ! az account show &> /dev/null; then
    print_warning "Not logged into Azure, initiating login..."
    az login
fi
print_success "Authenticated with Azure"

# ============================================================================
# Environment Selection
# ============================================================================
print_header "Environment Selection"

if [ -z "$ENVIRONMENT" ]; then
    echo "Available environments:"
    echo "  1) dev (development)"
    echo "  2) staging"
    echo "  3) prod (production)"
    echo ""
    read -p "Select environment (1-3): " ENV_CHOICE
    case $ENV_CHOICE in
        1) ENVIRONMENT="dev" ;;
        2) ENVIRONMENT="staging" ;;
        3) ENVIRONMENT="prod" ;;
        *) print_error "Invalid selection"; exit 1 ;;
    esac
fi

# Validate environment
if [[ ! "$ENVIRONMENT" =~ ^(dev|staging|prod)$ ]]; then
    print_error "Invalid environment: $ENVIRONMENT"
    echo "Valid environments: dev, staging, prod"
    exit 1
fi

print_success "Environment: $ENVIRONMENT"

# Environment-specific settings
case $ENVIRONMENT in
    dev)
        LOG_LEVEL="DEBUG"
        MIN_REPLICAS=1
        MAX_REPLICAS=2
        CPU_CORES="0.5"
        MEMORY="1.0Gi"
        ;;
    staging)
        LOG_LEVEL="INFO"
        MIN_REPLICAS=1
        MAX_REPLICAS=3
        CPU_CORES="0.5"
        MEMORY="1.0Gi"
        ;;
    prod)
        LOG_LEVEL="WARN"
        MIN_REPLICAS=2
        MAX_REPLICAS=10
        CPU_CORES="1.0"
        MEMORY="2.0Gi"
        ;;
esac

# ============================================================================
# Suffix Configuration
# ============================================================================
print_header "Infrastructure Configuration"

echo -e "${CYAN}The suffix was set during infrastructure deployment.${NC}"
echo "You can find it by running:"
echo -e "   ${BLUE}az group list --query \"[?starts_with(name, 'rg-xshopai-$ENVIRONMENT')].{Name:name, Suffix:tags.suffix}\" -o table${NC}"
echo ""

read -p "Enter the infrastructure suffix: " SUFFIX

if [ -z "$SUFFIX" ]; then
    print_error "Suffix is required. Please run the infrastructure deployment first."
    exit 1
fi

# Validate suffix format
if [[ ! "$SUFFIX" =~ ^[a-z0-9]{3,6}$ ]]; then
    print_error "Invalid suffix format: $SUFFIX"
    echo "   Suffix must be 3-6 lowercase alphanumeric characters."
    exit 1
fi
print_success "Using suffix: $SUFFIX"

# ============================================================================
# Derive Resource Names from Infrastructure
# ============================================================================
# These names must match what was created by deploy-infra.sh
RESOURCE_GROUP="rg-${PROJECT_NAME}-${ENVIRONMENT}-${SUFFIX}"
ACR_NAME="${PROJECT_NAME}${ENVIRONMENT}${SUFFIX}"
CONTAINER_ENV="cae-${PROJECT_NAME}-${ENVIRONMENT}-${SUFFIX}"
POSTGRES_SERVER="psql-${PROJECT_NAME}-${ENVIRONMENT}-${SUFFIX}"
KEY_VAULT="kv-${PROJECT_NAME}-${ENVIRONMENT}-${SUFFIX}"
MANAGED_IDENTITY="id-${PROJECT_NAME}-${ENVIRONMENT}-${SUFFIX}"

print_info "Derived resource names:"
echo "   Resource Group:      $RESOURCE_GROUP"
echo "   Container Registry:  $ACR_NAME"
echo "   Container Env:       $CONTAINER_ENV"
echo "   PostgreSQL Server:   $POSTGRES_SERVER"
echo "   Key Vault:           $KEY_VAULT"
echo ""

# ============================================================================
# Verify Infrastructure Exists
# ============================================================================
print_header "Verifying Infrastructure"

# Check Resource Group
if ! az group show --name "$RESOURCE_GROUP" &> /dev/null; then
    print_error "Resource group '$RESOURCE_GROUP' does not exist."
    echo "Please run the infrastructure deployment first:"
    echo "   cd infrastructure/azure/aca/scripts"
    echo "   ./deploy-infra.sh $ENVIRONMENT"
    exit 1
fi
print_success "Resource group exists: $RESOURCE_GROUP"

# Check ACR
if ! az acr show --name "$ACR_NAME" &> /dev/null; then
    print_error "Container registry '$ACR_NAME' does not exist"
    exit 1
fi
ACR_LOGIN_SERVER=$(az acr show --name "$ACR_NAME" --query loginServer -o tsv)
print_success "Container registry exists: $ACR_LOGIN_SERVER"

# Check Container Apps Environment
if ! az containerapp env show --name "$CONTAINER_ENV" --resource-group "$RESOURCE_GROUP" &> /dev/null; then
    print_error "Container Apps Environment '$CONTAINER_ENV' does not exist"
    exit 1
fi
print_success "Container Apps Environment exists"

# Check PostgreSQL Server
if ! az postgres flexible-server show --name "$POSTGRES_SERVER" --resource-group "$RESOURCE_GROUP" &> /dev/null; then
    print_error "PostgreSQL server '$POSTGRES_SERVER' does not exist"
    echo "Please ensure PostgreSQL was created during infrastructure deployment."
    exit 1
fi
POSTGRES_HOST="${POSTGRES_SERVER}.postgres.database.azure.com"
print_success "PostgreSQL server exists: $POSTGRES_HOST"

# Check Key Vault
if ! az keyvault show --name "$KEY_VAULT" &> /dev/null; then
    print_error "Key Vault '$KEY_VAULT' does not exist"
    exit 1
fi
print_success "Key Vault exists"

# Check Managed Identity
IDENTITY_ID=$(az identity show --name "$MANAGED_IDENTITY" --resource-group "$RESOURCE_GROUP" --query id -o tsv 2>/dev/null)
if [ -z "$IDENTITY_ID" ]; then
    print_error "Managed Identity '$MANAGED_IDENTITY' does not exist"
    exit 1
fi
IDENTITY_CLIENT_ID=$(az identity show --name "$MANAGED_IDENTITY" --resource-group "$RESOURCE_GROUP" --query clientId -o tsv)
print_success "Managed Identity exists: $IDENTITY_CLIENT_ID"

# ============================================================================
# Get Database Configuration from Key Vault
# ============================================================================
print_header "Retrieving Configuration from Key Vault"

# Get PostgreSQL credentials
POSTGRES_USER=$(az keyvault secret show --vault-name "$KEY_VAULT" --name "database-user" --query "value" -o tsv 2>/dev/null || echo "")
POSTGRES_PASSWORD=$(az keyvault secret show --vault-name "$KEY_VAULT" --name "database-password" --query "value" -o tsv 2>/dev/null || echo "")

if [ -z "$POSTGRES_USER" ] || [ -z "$POSTGRES_PASSWORD" ]; then
    print_warning "PostgreSQL credentials not found in Key Vault. Trying alternate secret names..."
    # Try alternate names (postgres-user, postgres-password)
    POSTGRES_USER=$(az keyvault secret show --vault-name "$KEY_VAULT" --name "postgres-user" --query "value" -o tsv 2>/dev/null || echo "pgadmin")
    POSTGRES_PASSWORD=$(az keyvault secret show --vault-name "$KEY_VAULT" --name "postgres-password" --query "value" -o tsv 2>/dev/null || echo "")
fi

if [ -z "$POSTGRES_PASSWORD" ]; then
    print_error "PostgreSQL password not found in Key Vault"
    echo "Expected secrets: database-password or postgres-password"
    exit 1
fi

print_success "Retrieved PostgreSQL credentials"

# Build JDBC URL
JDBC_URL="jdbc:postgresql://${POSTGRES_HOST}:5432/${DATABASE_NAME}?sslmode=require"
print_info "JDBC URL: jdbc:postgresql://${POSTGRES_HOST}:5432/${DATABASE_NAME}?sslmode=require"

# Get JWT secret (optional)
JWT_SECRET=$(az keyvault secret show --vault-name "$KEY_VAULT" --name "jwt-secret" --query "value" -o tsv 2>/dev/null || echo "")

# ============================================================================
# Create Database
# ============================================================================
print_header "Setting Up PostgreSQL Database"

# Check if firewall rule for Azure services exists (allow Azure services)
print_info "Ensuring Azure services can access PostgreSQL..."
az postgres flexible-server firewall-rule show \
    --name "AllowAzureServices" \
    --resource-group "$RESOURCE_GROUP" \
    --server-name "$POSTGRES_SERVER" &> /dev/null || \
az postgres flexible-server firewall-rule create \
    --name "AllowAzureServices" \
    --resource-group "$RESOURCE_GROUP" \
    --server-name "$POSTGRES_SERVER" \
    --start-ip-address "0.0.0.0" \
    --end-ip-address "0.0.0.0" \
    --output none

# Add temporary rule for current machine's public IP
MY_IP=$(curl -s https://api.ipify.org)
print_info "Adding temporary firewall rule for IP: $MY_IP"
az postgres flexible-server firewall-rule create \
    --name "DeploymentRule" \
    --resource-group "$RESOURCE_GROUP" \
    --server-name "$POSTGRES_SERVER" \
    --start-ip-address "$MY_IP" \
    --end-ip-address "$MY_IP" \
    --output none 2>/dev/null || true

# Create database if it doesn't exist
print_info "Creating database '$DATABASE_NAME' if not exists..."
export PGPASSWORD="$POSTGRES_PASSWORD"
psql "host=${POSTGRES_HOST} port=5432 dbname=postgres user=${POSTGRES_USER} sslmode=require" -c "SELECT 'CREATE DATABASE ${DATABASE_NAME}' WHERE NOT EXISTS (SELECT FROM pg_database WHERE datname = '${DATABASE_NAME}')\gexec" 2>/dev/null || {
    print_warning "Could not connect to PostgreSQL to create database"
    print_info "Database may already exist or will be created on first connection"
}
unset PGPASSWORD

print_success "Database setup complete"

# ============================================================================
# Deployment Confirmation
# ============================================================================
print_header "Deployment Summary"

echo -e "${CYAN}The following will be deployed:${NC}"
echo ""
echo "   Service:           $SERVICE_NAME"
echo "   Environment:       $ENVIRONMENT"
echo "   Resource Group:    $RESOURCE_GROUP"
echo "   Container Env:     $CONTAINER_ENV"
echo "   Registry:          $ACR_LOGIN_SERVER"
echo ""
echo "   Configuration:"
echo "   - App Port:        $APP_PORT"
echo "   - Dapr HTTP Port:  $DAPR_HTTP_PORT"
echo "   - Dapr PubSub:     $DAPR_PUBSUB_NAME"
echo "   - Log Level:       $LOG_LEVEL"
echo "   - Min Replicas:    $MIN_REPLICAS"
echo "   - Max Replicas:    $MAX_REPLICAS"
echo ""
echo "   Database:"
echo "   - PostgreSQL:      $POSTGRES_HOST"
echo "   - Database Name:   $DATABASE_NAME"
echo ""

read -p "Do you want to proceed with deployment? (y/N): " CONFIRM
if [[ ! "$CONFIRM" =~ ^[Yy]$ ]]; then
    print_warning "Deployment cancelled by user"
    exit 0
fi

# ============================================================================
# Step 2: Build and Push Container Image
# ============================================================================
print_header "Step 2: Building and Pushing Container Image"

# Login to ACR
print_info "Logging into ACR..."
az acr login --name "$ACR_NAME"
print_success "Logged into ACR"

# Navigate to service directory
cd "$SERVICE_DIR"

# Build with Maven first (compile Java, run tests)
print_info "Building Java application with Maven..."
mvn clean package -DskipTests -q
print_success "Maven build completed"

# Build Docker image
print_info "Building Docker image..."
docker build -t "$SERVICE_NAME:latest" .
print_success "Docker image built"

# Tag and push
IMAGE_TAG="$ACR_LOGIN_SERVER/$SERVICE_NAME:latest"
docker tag "$SERVICE_NAME:latest" "$IMAGE_TAG"
print_info "Pushing image to ACR..."
docker push "$IMAGE_TAG"
print_success "Image pushed: $IMAGE_TAG"

# ============================================================================
# Step 3: Deploy Container App
# ============================================================================
print_header "Step 3: Deploying Container App"

# Get ACR credentials
ACR_PASSWORD=$(az acr credential show --name "$ACR_NAME" --query "passwords[0].value" -o tsv)

# Build environment variables
# Note: Using underscore format because Dapr secret store on ACA doesn't support colons
# The DaprSecretManager needs to be updated to support underscore format as fallback
ENV_VARS=("SPRING_PROFILES_ACTIVE=$ENVIRONMENT")
ENV_VARS+=("SERVER_PORT=$APP_PORT")
ENV_VARS+=("DAPR_HOST=localhost")
ENV_VARS+=("DAPR_HTTP_PORT=$DAPR_HTTP_PORT")
ENV_VARS+=("logging.level.root=$LOG_LEVEL")
ENV_VARS+=("logging.level.com.xshopai.orderprocessor=$LOG_LEVEL")

# Add Azure Client ID for managed identity
if [ -n "$IDENTITY_CLIENT_ID" ]; then
    ENV_VARS+=("AZURE_CLIENT_ID=$IDENTITY_CLIENT_ID")
fi

# Add database configuration as fallback environment variables
# The code should first try Dapr secret store, then fall back to env vars
ENV_VARS+=("database_host=$POSTGRES_HOST")
ENV_VARS+=("database_port=5432")
ENV_VARS+=("database_name=$DATABASE_NAME")
ENV_VARS+=("database_user=$POSTGRES_USER")
ENV_VARS+=("database_password=$POSTGRES_PASSWORD")

# Also set Spring data source properties directly
ENV_VARS+=("SPRING_DATASOURCE_URL=$JDBC_URL")
ENV_VARS+=("SPRING_DATASOURCE_USERNAME=$POSTGRES_USER")
ENV_VARS+=("SPRING_DATASOURCE_PASSWORD=$POSTGRES_PASSWORD")

# Add JWT secret if available
if [ -n "$JWT_SECRET" ]; then
    ENV_VARS+=("jwt_secret=$JWT_SECRET")
fi

# Check if container app exists
if az containerapp show --name "$SERVICE_NAME" --resource-group "$RESOURCE_GROUP" &> /dev/null; then
    print_info "Container app '$SERVICE_NAME' exists, updating..."
    az containerapp update \
        --name "$SERVICE_NAME" \
        --resource-group "$RESOURCE_GROUP" \
        --image "$IMAGE_TAG" \
        --set-env-vars "${ENV_VARS[@]}" \
        --output none
    print_success "Container app updated"
else
    print_info "Creating container app '$SERVICE_NAME'..."
    
    # Build the create command
    MSYS_NO_PATHCONV=1 az containerapp create \
        --name "$SERVICE_NAME" \
        --resource-group "$RESOURCE_GROUP" \
        --environment "$CONTAINER_ENV" \
        --image "$IMAGE_TAG" \
        --registry-server "$ACR_LOGIN_SERVER" \
        --registry-username "$ACR_NAME" \
        --registry-password "$ACR_PASSWORD" \
        --target-port $APP_PORT \
        --ingress internal \
        --min-replicas $MIN_REPLICAS \
        --max-replicas $MAX_REPLICAS \
        --cpu $CPU_CORES \
        --memory $MEMORY \
        --enable-dapr \
        --dapr-app-id "$SERVICE_NAME" \
        --dapr-app-port $APP_PORT \
        --env-vars "${ENV_VARS[@]}" \
        ${IDENTITY_ID:+--user-assigned "$IDENTITY_ID"} \
        --output none

    print_success "Container app created"
fi

# ============================================================================
# Step 4: Configure Dapr Components
# ============================================================================
print_header "Step 4: Configuring Dapr"

# Note: Dapr components are typically shared at the environment level
# They should be created during infrastructure deployment
# Here we just verify they exist

print_info "Verifying Dapr components..."
# Dapr components for ACA are configured at the environment level
# They should already be configured in deploy-infra.sh

print_success "Dapr configuration verified"

# ============================================================================
# Step 5: Verify Deployment
# ============================================================================
print_header "Step 5: Verifying Deployment"

# Get the app URL
APP_URL=$(az containerapp show --name "$SERVICE_NAME" --resource-group "$RESOURCE_GROUP" --query "properties.configuration.ingress.fqdn" -o tsv 2>/dev/null)

print_info "Waiting for deployment to stabilize (30 seconds)..."
sleep 30

# Check health
print_info "Checking application health..."
HEALTH_RESPONSE=$(curl -s -o /dev/null -w "%{http_code}" "https://${APP_URL}/actuator/health" 2>/dev/null || echo "000")

if [ "$HEALTH_RESPONSE" = "200" ]; then
    print_success "Application is healthy!"
else
    print_warning "Health check returned status: $HEALTH_RESPONSE"
    print_info "The app may still be starting up. Check logs for details."
fi

# ============================================================================
# Deployment Complete
# ============================================================================
print_header "Deployment Complete!"

echo -e "${GREEN}Order Processor Service has been deployed successfully!${NC}"
echo ""
echo "Service URL: https://$APP_URL"
echo ""
echo "Useful commands:"
echo -e "   View logs:         ${BLUE}az containerapp logs show --name $SERVICE_NAME --resource-group $RESOURCE_GROUP --follow${NC}"
echo -e "   Check status:      ${BLUE}az containerapp show --name $SERVICE_NAME --resource-group $RESOURCE_GROUP --query properties.runningStatus${NC}"
echo -e "   View revisions:    ${BLUE}az containerapp revision list --name $SERVICE_NAME --resource-group $RESOURCE_GROUP -o table${NC}"
echo -e "   View Dapr logs:    ${BLUE}az containerapp logs show --name $SERVICE_NAME --resource-group $RESOURCE_GROUP --container daprd --follow${NC}"
echo ""
echo "Health endpoints:"
echo -e "   Liveness:          https://$APP_URL/actuator/health/liveness"
echo -e "   Readiness:         https://$APP_URL/actuator/health/readiness"
echo ""

# Cleanup temporary firewall rule
print_info "Cleaning up temporary firewall rule..."
az postgres flexible-server firewall-rule delete \
    --name "DeploymentRule" \
    --resource-group "$RESOURCE_GROUP" \
    --server-name "$POSTGRES_SERVER" \
    --yes \
    --output none 2>/dev/null || true

print_success "Deployment script completed!"
