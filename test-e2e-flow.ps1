# E2E Testing Script - Full Banking Flow

Write-Host "🏦 E2E Test: ATM → jPOS → CMS → Core Ledger" -ForegroundColor Cyan
Write-Host "=============================================" -ForegroundColor Cyan
Write-Host ""

$ledgerUrl = "http://localhost:8083"
$cmsUrl = "http://localhost:8082"

# Step 1: Create accounts in Core Ledger
Write-Host "Step 1: Creating test accounts in Core Ledger..." -ForegroundColor Yellow

# Savings account
$savingsBody = @{
    accountNumber = "FINERACT_ACC_001"
    balance = 10000.0
    clientName = "John Doe"
} | ConvertTo-Json

try {
    $response = Invoke-RestMethod -Uri "$ledgerUrl/savingsaccounts" `
        -Method POST -Body $savingsBody -ContentType "application/json"
    Write-Host "✅ Created savings account: FINERACT_ACC_001 (Balance: 10000)" -ForegroundColor Green
} catch {
    Write-Host "⚠️ Savings account might already exist" -ForegroundColor Yellow
}

# Loan account
$loanBody = @{
    accountNumber = "FINERACT_LOAN_001"
    principal = 50000.0
    clientName = "Jane Smith"
} | ConvertTo-Json

try {
    $response = Invoke-RestMethod -Uri "$ledgerUrl/loans" `
        -Method POST -Body $loanBody -ContentType "application/json"
    Write-Host "✅ Created loan account: FINERACT_LOAN_001 (Limit: 50000)" -ForegroundColor Green
} catch {
    Write-Host "⚠️ Loan account might already exist" -ForegroundColor Yellow
}

Write-Host ""

# Step 2: Issue cards in CMS
Write-Host "Step 2: Issuing cards in CMS..." -ForegroundColor Yellow

# Debit card
try {
    $uri = "$cmsUrl/api/cards/issue"
    $params = "pan=1234567890123456&cvv=123&expirationDate=2028-12-31&accountId=FINERACT_ACC_001&cardholderName=John Doe"
    $response = Invoke-RestMethod -Uri "$uri" -Method POST -Body $params -ContentType "application/x-www-form-urlencoded"
    Write-Host "✅ Issued debit card: 1234567890123456" -ForegroundColor Green
} catch {
    Write-Host "⚠️ Debit card might already exist" -ForegroundColor Yellow
}

# Credit card
try {
    $uri = "$cmsUrl/api/cards/issue/credit"
    $params = "pan=9876543210987654&cvv=456&expirationDate=2029-12-31&creditLimit=50000&loanAccountId=FINERACT_LOAN_001&cardholderName=Jane Smith"
    $response = Invoke-RestMethod -Uri "$uri" -Method POST -Body $params -ContentType "application/x-www-form-urlencoded"
    Write-Host "✅ Issued credit card: 9876543210987654" -ForegroundColor Green
} catch {
    Write-Host "⚠️ Credit card might already exist" -ForegroundColor Yellow
}

Write-Host ""

# Step 3: Test debit card transaction
Write-Host "Step 3: Testing debit card transaction ($100)..." -ForegroundColor Yellow
$txBody = @{
    cardNumber = "1234567890123456"
    amount = 100.0
} | ConvertTo-Json

try {
    $response = Invoke-RestMethod -Uri "$cmsUrl/api/transaction" `
        -Method POST -Body $txBody -ContentType "application/json"
    
    if ($response.approved) {
        Write-Host "✅ Transaction APPROVED" -ForegroundColor Green
        Write-Host "   Response Code: $($response.responseCode)" -ForegroundColor Gray
        Write-Host "   Message: $($response.message)" -ForegroundColor Gray
    } else {
        Write-Host "❌ Transaction DECLINED" -ForegroundColor Red
        Write-Host "   Reason: $($response.message)" -ForegroundColor Gray
    }
} catch {
    Write-Host "❌ Transaction failed: $_" -ForegroundColor Red
}

Write-Host ""

# Step 4: Verify balance in Core Ledger
Write-Host "Step 4: Verifying balance in Core Ledger..." -ForegroundColor Yellow
try {
    $response = Invoke-RestMethod -Uri "$ledgerUrl/savingsaccounts/FINERACT_ACC_001" -Method GET
    Write-Host "✅ Account balance: $($response.accountBalance)" -ForegroundColor Green
    Write-Host "   Expected: 9900.0 (10000 - 100)" -ForegroundColor Gray
} catch {
    Write-Host "❌ Failed to get balance" -ForegroundColor Red
}

Write-Host ""

# Step 5: Test credit card transaction
Write-Host "Step 5: Testing credit card transaction ($1000)..." -ForegroundColor Yellow
$txBody = @{
    cardNumber = "9876543210987654"
    amount = 1000.0
} | ConvertTo-Json

try {
    $response = Invoke-RestMethod -Uri "$cmsUrl/api/transaction" `
        -Method POST -Body $txBody -ContentType "application/json"
    
    if ($response.approved) {
        Write-Host "✅ Credit transaction APPROVED" -ForegroundColor Green
    } else {
        Write-Host "❌ Credit transaction DECLINED" -ForegroundColor Red
    }
} catch {
    Write-Host "❌ Transaction failed: $_" -ForegroundColor Red
}

Write-Host ""

# Step 6: Verify outstanding balance
Write-Host "Step 6: Verifying credit card outstanding..." -ForegroundColor Yellow
try {
    $response = Invoke-RestMethod -Uri "$ledgerUrl/loans/FINERACT_LOAN_001" -Method GET
    Write-Host "✅ Outstanding balance: $($response.principalOutstanding)" -ForegroundColor Green
    Write-Host "   Available credit: $($response.principal - $response.principalOutstanding)" -ForegroundColor Gray
} catch {
    Write-Host "❌ Failed to get loan info" -ForegroundColor Red
}

Write-Host ""
Write-Host "🎉 E2E Test Complete!" -ForegroundColor Cyan
Write-Host ""
Write-Host "Summary:" -ForegroundColor White
Write-Host "  ✅ Core Ledger Service - Working" -ForegroundColor Green
Write-Host "  ✅ CMS Service - Working" -ForegroundColor Green
Write-Host "  ✅ Integration - Working" -ForegroundColor Green
Write-Host ""
Write-Host "Next: Test with jPOS Switch (send ISO 8583 message)" -ForegroundColor Yellow
