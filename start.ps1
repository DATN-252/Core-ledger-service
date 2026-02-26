# Quick Start - Core Ledger Service
# Run this script to start the service

Write-Host "🚀 Starting Core Ledger Service..." -ForegroundColor Cyan
Write-Host ""

# Check if Docker is running
Write-Host "1️⃣ Checking Docker... Container" -ForegroundColor Yellow
$dockerRunning = docker ps 2>$null
if ($LASTEXITCODE -ne 0) {
    Write-Host "❌ Docker is not running. Please start Docker Desktop first." -ForegroundColor Red
    exit 1
}

# Check database
Write-Host "2️⃣ Checking database..." -ForegroundColor Yellow
$dbContainer = docker ps --filter "name=ledger-postgres" --format "{{.Names}}"
if ($dbContainer -eq "ledger-postgres") {
    Write-Host "✅ Database is running" -ForegroundColor Green
} else {
    Write-Host "⚠️  Database not running. Starting..." -ForegroundColor Yellow
    docker-compose up -d
    Start-Sleep -Seconds 3
    Write-Host "✅ Database started" -ForegroundColor Green
}

Write-Host ""
Write-Host "3️⃣ Building and starting Core Ledger Service..." -ForegroundColor Yellow
Write-Host "   This may take a minute on first run..." -ForegroundColor Gray
Write-Host ""

# Run the service
.\gradlew.bat bootRun
