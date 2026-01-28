#!/usr/bin/env pwsh
# Order Processor Service - PowerShell Run Script with Dapr
# Port: 8007, Dapr HTTP: 3500, Dapr gRPC: 50001

Write-Host ""
Write-Host "============================================" -ForegroundColor Cyan
#!/usr/bin/env pwsh
# Run Order Processor Service with Dapr sidecar
# Usage: .\run.ps1

$Host.UI.RawUI.WindowTitle = "Order Processor"

Write-Host "Starting Order Processor Service with Dapr..." -ForegroundColor Green
Write-Host "============================================" -ForegroundColor Cyan
Write-Host ""

# Kill any existing processes on ports
Write-Host "Cleaning up existing processes..." -ForegroundColor Yellow

# Kill process on port 8007 (app port)
$process = Get-NetTCPConnection -LocalPort 8007 -ErrorAction SilentlyContinue | Select-Object -ExpandProperty OwningProcess -Unique
if ($process) {
    Write-Host "Killing process on port 8007 (PID: $process)" -ForegroundColor Yellow
    Stop-Process -Id $process -Force -ErrorAction SilentlyContinue
}

# Kill process on port 3500 (Dapr HTTP port)
$process = Get-NetTCPConnection -LocalPort 3500 -ErrorAction SilentlyContinue | Select-Object -ExpandProperty OwningProcess -Unique
if ($process) {
    Write-Host "Killing process on port 3500 (PID: $process)" -ForegroundColor Yellow
    Stop-Process -Id $process -Force -ErrorAction SilentlyContinue
}

# Kill process on port 50001 (Dapr gRPC port)
$process = Get-NetTCPConnection -LocalPort 50001 -ErrorAction SilentlyContinue | Select-Object -ExpandProperty OwningProcess -Unique
if ($process) {
    Write-Host "Killing process on port 50001 (PID: $process)" -ForegroundColor Yellow
    Stop-Process -Id $process -Force -ErrorAction SilentlyContinue
}

Start-Sleep -Seconds 2

Write-Host ""
Write-Host "Starting with Dapr sidecar..." -ForegroundColor Green
Write-Host "App ID: order-processor-service" -ForegroundColor Cyan
Write-Host "App Port: 8007" -ForegroundColor Cyan
Write-Host "Dapr HTTP Port: 3500" -ForegroundColor Cyan
Write-Host "Dapr gRPC Port: 50001" -ForegroundColor Cyan
Write-Host ""

dapr run `
  --app-id order-processor-service `
  --app-port 8007 `
  --dapr-http-port 3500 `
  --dapr-grpc-port 50001 `
  --log-level error `
  --resources-path ./.dapr/components `
  --config ./.dapr/config.yaml `
  --enable-app-health-check `
  --app-health-check-path /actuator/health `
  --app-health-probe-interval 5 `
  --app-health-probe-timeout 10 `
  --app-health-threshold 2 `
  -- mvn spring-boot:run "-Dspring-boot.run.arguments=--server.port=8007"

Write-Host ""
Write-Host "============================================" -ForegroundColor Cyan
Write-Host "Service stopped." -ForegroundColor Cyan
Write-Host "============================================" -ForegroundColor Cyan
