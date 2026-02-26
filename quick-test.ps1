# 🧪 Quick Test - Core Ledger APIs

Write-Host "Testing Core Ledger Service APIs..." -ForegroundColor Cyan
Write-Host ""

$baseUrl = "http://localhost:8083"

# Test 1: Health check
Write-Host "1. Health Check" -ForegroundColor Yellow
try {
    $health = Invoke-RestMethod -Uri "$baseUrl/actuator/health"
    Write-Host "   ✅ Service is UP" -ForegroundColor Green
} catch {
    Write-Host "   ❌ Service is not running!" -ForegroundColor Red
    Write-Host "   Make sure service is started with: .\build-and-run.ps1" -ForegroundColor Yellow
    exit 1
}

Write-Host ""

# Test 2: Create savings account
Write-Host "2. Create Savings Account (Debit Card)" -ForegroundColor Yellow
$body = @{
    accountNumber = "TEST_ACC_001"
    balance = 10000.0
    clientName = "John Doe"
} | ConvertTo-Json

try {
    $response = Invoke-RestMethod -Uri "$baseUrl/savingsaccounts" -Method POST -Body $body -ContentType "application/json"
    Write-Host "   ✅ Account created: $($response.accountNumber)" -ForegroundColor Green
} catch {
    Write-Host "   ⚠️  Account might already exist" -ForegroundColor Yellow
}

Write-Host ""

# Test 3: Get account balance
Write-Host "3. Get Account Balance" -ForegroundColor Yellow
try {
    $response = Invoke-RestMethod -Uri "$baseUrl/savingsaccounts/TEST_ACC_001"
    Write-Host "   ✅ Balance: $($response.accountBalance)" -ForegroundColor Green
} catch {
    Write-Host "   ❌ Failed: $_" -ForegroundColor Red
}

Write-Host ""

# Test 4: Create loan account
Write-Host "4. Create Loan Account (Credit Card)" -ForegroundColor Yellow
$body = @{
    accountNumber = "TEST_LOAN_001"
    principal = 50000.0
    clientName = "Jane Smith"
} | ConvertTo-Json

try {
    $response = Invoke-RestMethod -Uri "$baseUrl/loans" -Method POST -Body $body -ContentType "application/json"
    Write-Host "   ✅ Loan created with limit: $($response.principal)" -ForegroundColor Green
} catch {
    Write-Host "   ⚠️  Loan might already exist" -ForegroundColor Yellow
}

Write-Host ""
Write-Host "✨ Basic tests complete!" -ForegroundColor Cyan
Write-Host ""
Write-Host "Next steps:" -ForegroundColor White
Write-Host "  1. Run full API test: .\test-ledger-api.ps1" -ForegroundColor Gray
Write-Host "  2. Update CMS config to use this service" -ForegroundColor Gray
Write-Host "  3. Run E2E test: .\test-e2e-flow.ps1" -ForegroundColor Gray
