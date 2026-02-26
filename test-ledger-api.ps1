# Core Ledger Service API Testing Guide

Write-Host "🧪 Core Ledger Service API Tests" -ForegroundColor Cyan
Write-Host "=================================" -ForegroundColor Cyan
Write-Host ""

$baseUrl = "http://localhost:8083"

# Test 1: Create Savings Account
Write-Host "📝 Test 1: Create Savings Account" -ForegroundColor Yellow
$body = @{
    accountNumber = "TEST_ACC_001"
    balance = 10000.0
    clientName = "John Doe"
} | ConvertTo-Json

try {
    $response = Invoke-RestMethod -Uri "$baseUrl/savingsaccounts" `
        -Method POST `
        -Body $body `
        -ContentType "application/json"
    Write-Host "✅ Created savings account: $($response.accountNumber)" -ForegroundColor Green
    Write-Host "   Resource ID: $($response.resourceId)" -ForegroundColor Gray
} catch {
    Write-Host "❌ Failed: $_" -ForegroundColor Red
}

Write-Host ""

# Test 2: Get Account Balance
Write-Host "📊 Test 2: Get Account Balance" -ForegroundColor Yellow
try {
    $response = Invoke-RestMethod -Uri "$baseUrl/savingsaccounts/TEST_ACC_001" -Method GET
    Write-Host "✅ Account Balance: $($response.accountBalance)" -ForegroundColor Green
    Write-Host "   Status: $($response.status.value)" -ForegroundColor Gray
} catch {
    Write-Host "❌ Failed: $_" -ForegroundColor Red
}

Write-Host ""

# Test 3: Withdraw Money
Write-Host "💸 Test 3: Withdraw $100" -ForegroundColor Yellow
$body = @{
    locale = "en"
    dateFormat = "dd MMMM yyyy"
    transactionDate = (Get-Date -Format "dd MMMM yyyy")
    transactionAmount = 100.0
} | ConvertTo-Json

try {
    $response = Invoke-RestMethod -Uri "$baseUrl/savingsaccounts/TEST_ACC_001/transactions?command=withdrawal" `
        -Method POST `
        -Body $body `
        -ContentType "application/json"
    Write-Host "✅ Withdrawal successful" -ForegroundColor Green
    Write-Host "   New Balance: $($response.changes.accountBalance)" -ForegroundColor Gray
} catch {
    Write-Host "❌ Failed: $_" -ForegroundColor Red
}

Write-Host ""

# Test 4: Create Loan Account
Write-Host "💳 Test 4: Create Loan Account (Credit Card)" -ForegroundColor Yellow
$body = @{
    accountNumber = "TEST_LOAN_001"
    principal = 50000.0
    clientName = "Jane Smith"
} | ConvertTo-Json

try {
    $response = Invoke-RestMethod -Uri "$baseUrl/loans" `
        -Method POST `
        -Body $body `
        -ContentType "application/json"
    Write-Host "✅ Created loan account: $($response.accountNumber)" -ForegroundColor Green
    Write-Host "   Credit Limit: $($response.principal)" -ForegroundColor Gray
} catch {
    Write-Host "❌ Failed: $_" -ForegroundColor Red
}

Write-Host ""

# Test 5: Get Loan Info
Write-Host "📈 Test 5: Get Loan Info" -ForegroundColor Yellow
try {
    $response = Invoke-RestMethod -Uri "$baseUrl/loans/TEST_LOAN_001" -Method GET
    Write-Host "✅ Credit Limit: $($response.principal)" -ForegroundColor Green
    Write-Host "   Outstanding: $($response.principalOutstanding)" -ForegroundColor Gray
    Write-Host "   Available: $($response.principal - $response.principalOutstanding)" -ForegroundColor Gray
} catch {
    Write-Host "❌ Failed: $_" -ForegroundColor Red
}

Write-Host ""

# Test 6: Add Charge to Loan
Write-Host "🛒 Test 6: Add Charge of $1000" -ForegroundColor Yellow
$body = @{
    amount = 1000.0
    dueDate = (Get-Date -Format "dd MMMM yyyy")
    locale = "en"
    dateFormat = "dd MMMM yyyy"
} | ConvertTo-Json

try {
    $response = Invoke-RestMethod -Uri "$baseUrl/loans/TEST_LOAN_001/charges" `
        -Method POST `
        -Body $body `
        -ContentType "application/json"
    Write-Host "✅ Charge added successfully" -ForegroundColor Green
    Write-Host "   New Outstanding: $($response.changes.principalOutstanding)" -ForegroundColor Gray
} catch {
    Write-Host "❌ Failed: $_" -ForegroundColor Red
}

Write-Host ""
Write-Host "✨ All tests completed!" -ForegroundColor Cyan
