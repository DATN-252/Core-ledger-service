# Test Account State Machine

Write-Host "========================================" -ForegroundColor Cyan
Write-Host "Testing Account State Machine & Lifecycle" -ForegroundColor Cyan
Write-Host "========================================" -ForegroundColor Cyan
Write-Host ""

$baseUrl = "http://localhost:8083"

# Test 1: Create Client
Write-Host "Test 1: Creating test client..." -ForegroundColor Yellow
$client = @{
    clientId = "CLI_STATE_001"
    fullName = "State Machine Test User"
    dateOfBirth = "1990-01-01"
    gender = "MALE"
    email = "statetest@example.com"
    phoneNumber = "+84900000001"
    address = "Test Address"
    idNumber = "099999999999"
    idType = "NATIONAL_ID"
} | ConvertTo-Json

try {
    $clientRes = Invoke-RestMethod -Uri "$baseUrl/clients" -Method Post -Body $client -ContentType "application/json"
    Write-Host "✓ Client created: $($clientRes.clientId)" -ForegroundColor Green
} catch {
    Write-Host "✗ Client creation failed (may already exist)" -ForegroundColor Yellow
}

Write-Host ""

# Test 2: Create Savings Account
Write-Host "Test 2: Creating savings account..." -ForegroundColor Yellow
$savingsReq = @{
    accountNumber = "SAV_STATE_001"
    balance = 10000.0
    clientId = "CLI_STATE_001"
} | ConvertTo-Json

try {
    $savingsRes = Invoke-RestMethod -Uri "$baseUrl/savingsaccounts" -Method Post -Body $savingsReq -ContentType "application/json"
    Write-Host "✓ Savings account created: $($savingsRes.accountNumber)" -ForegroundColor Green
    Write-Host "  Status: $($savingsRes.status) (PENDING - needs activation)" -ForegroundColor Gray
} catch {
    Write-Host "✗ Account creation failed:" $_.Exception.Message -ForegroundColor Red
}

Write-Host ""

# Test 2.5: Activate the account (PENDING → ACTIVE)
Write-Host "Test 2.5: Activating account (PENDING → ACTIVE)..." -ForegroundColor Yellow
try {
    $activateRes = Invoke-RestMethod -Uri "$baseUrl/savingsaccounts/SAV_STATE_001?command=activate" -Method Post -ContentType "application/json"
    Write-Host "✓ Account activated successfully" -ForegroundColor Green
    Write-Host "  Status: $($activateRes.status)" -ForegroundColor Gray
} catch {
    Write-Host "✗ Activation failed:" $_.Exception.Message -ForegroundColor Red
}

Write-Host ""

# Test 3: Try withdrawal on ACTIVE account
Write-Host "Test 3: Withdrawing from ACTIVE account..." -ForegroundColor Yellow
$withdrawReq = @{
    locale = "en"
    dateFormat = "dd MMMM yyyy"
    transactionDate = "12 February 2026"
    transactionAmount = 1000.0
} | ConvertTo-Json

try {
    $withdrawRes = Invoke-RestMethod -Uri "$baseUrl/savingsaccounts/SAV_STATE_001/transactions?command=withdrawal" -Method Post -Body $withdrawReq -ContentType "application/json"
    Write-Host "✓ Withdrawal successful. New balance:" $withdrawRes.changes.accountBalance -ForegroundColor Green
} catch {
    Write-Host "✗ Withdrawal failed:" $_.Exception.Message -ForegroundColor Red
}

Write-Host ""

# Test 4: Lock account
Write-Host "Test 4: Locking account (suspected fraud)..." -ForegroundColor Yellow
$lockReq = @{
    reason = "Suspected fraudulent activity"
} | ConvertTo-Json

try {
    $lockRes = Invoke-RestMethod -Uri "$baseUrl/savingsaccounts/SAV_STATE_001?command=lock" -Method Post -Body $lockReq -ContentType "application/json"
    Write-Host "✓ Account locked successfully" -ForegroundColor Green
    Write-Host "  Status: $($lockRes.status)" -ForegroundColor Gray
    Write-Host "  Message: $($lockRes.message)" -ForegroundColor Gray
} catch {
    Write-Host "✗ Lock failed:" $_.Exception.Message -ForegroundColor Red
}

Write-Host ""

# Test 5: Try withdrawal on LOCKED account (should fail)
Write-Host "Test 5: Attempting withdrawal on LOCKED account (should fail)..." -ForegroundColor Yellow
try {
    $withdrawRes2 = Invoke-RestMethod -Uri "$baseUrl/savingsaccounts/SAV_STATE_001/transactions?command=withdrawal" -Method Post -Body $withdrawReq -ContentType "application/json"
    Write-Host "✗ UNEXPECTED: Withdrawal succeeded on locked account!" -ForegroundColor Red
} catch {
    Write-Host "✓ Withdrawal correctly blocked: Account is locked" -ForegroundColor Green
}

Write-Host ""

# Test 6: Unlock account
Write-Host "Test 6: Unlocking account..." -ForegroundColor Yellow
try {
    $unlockRes = Invoke-RestMethod -Uri "$baseUrl/savingsaccounts/SAV_STATE_001?command=unlock" -Method Post -ContentType "application/json"
    Write-Host "✓ Account unlocked successfully" -ForegroundColor Green
    Write-Host "  Status: $($unlockRes.status)" -ForegroundColor Gray
} catch {
    Write-Host "✗ Unlock failed:" $_.Exception.Message -ForegroundColor Red
}

Write-Host ""

# Test 7: Try close with balance (should fail)
Write-Host "Test 7: Attempting to close account with balance (should fail)..." -ForegroundColor Yellow
try {
    $closeRes = Invoke-RestMethod -Uri "$baseUrl/savingsaccounts/SAV_STATE_001?command=close" -Method Post -ContentType "application/json"
    Write-Host "✗ UNEXPECTED: Close succeeded with remaining balance!" -ForegroundColor Red
} catch {
    Write-Host "✓ Close correctly blocked: Account has balance" -ForegroundColor Green
}

Write-Host ""

# Test 8: Withdraw all balance
Write-Host "Test 8: Withdrawing all remaining balance..." -ForegroundColor Yellow
$withdrawAllReq = @{
    locale = "en"
    dateFormat = "dd MMMM yyyy"
    transactionDate = "12 February 2026"
    transactionAmount = 9000.0
} | ConvertTo-Json

try {
    $withdrawAllRes = Invoke-RestMethod -Uri "$baseUrl/savingsaccounts/SAV_STATE_001/transactions?command=withdrawal" -Method Post -Body $withdrawAllReq -ContentType "application/json"
    Write-Host "✓ All funds withdrawn. Balance: $($withdrawAllRes.changes.accountBalance)" -ForegroundColor Green
} catch {
    Write-Host "✗ Withdrawal failed:" $_.Exception.Message -ForegroundColor Red
}

Write-Host ""

# Test 9: Close account with zero balance
Write-Host "Test 9: Closing account with zero balance..." -ForegroundColor Yellow
try {
    $closeRes2 = Invoke-RestMethod -Uri "$baseUrl/savingsaccounts/SAV_STATE_001?command=close" -Method Post -ContentType "application/json"
    Write-Host "✓ Account closed successfully" -ForegroundColor Green
    Write-Host "  Status: $($closeRes2.status)" -ForegroundColor Gray
} catch {
    Write-Host "✗ Close failed:" $_.Exception.Message -ForegroundColor Red
}

Write-Host ""

# Test 10: Try to reopen closed account (should fail)
Write-Host "Test 10: Attempting to reopen CLOSED account (should fail)..." -ForegroundColor Yellow
try {
    $activateRes = Invoke-RestMethod -Uri "$baseUrl/savingsaccounts/SAV_STATE_001?command=activate" -Method Post -ContentType "application/json"
    Write-Host "✗ UNEXPECTED: Reopened closed account!" -ForegroundColor Red
} catch {
    Write-Host "✓ Reopen correctly blocked: Cannot activate closed accounts" -ForegroundColor Green
}

Write-Host ""
Write-Host "========================================" -ForegroundColor Cyan
Write-Host "Loan Account State Machine Tests" -ForegroundColor Cyan
Write-Host "========================================" -ForegroundColor Cyan
Write-Host ""

# Test 11: Create Loan Account
Write-Host "Test 11: Creating loan account..." -ForegroundColor Yellow
$loanReq = @{
    accountNumber = "LOAN_STATE_001"
    principal = 50000.0
    clientId = "CLI_STATE_001"
} | ConvertTo-Json

try {
    $loanRes = Invoke-RestMethod -Uri "$baseUrl/loans" -Method Post -Body $loanReq -ContentType "application/json"
    Write-Host "✓ Loan account created: $($loanRes.accountNumber)" -ForegroundColor Green
    Write-Host "  Credit Limit: $($loanRes.principal)" -ForegroundColor Gray
    Write-Host "  Status: PENDING (needs activation)" -ForegroundColor Gray
} catch {
    Write-Host "✗ Loan creation failed:" $_.Exception.Message -ForegroundColor Red
}

Write-Host ""

# Test 11.5: Activate loan account
Write-Host "Test 11.5: Activating loan account..." -ForegroundColor Yellow
try {
    $activateLoanRes = Invoke-RestMethod -Uri "$baseUrl/loans/LOAN_STATE_001?command=activate" -Method Post -ContentType "application/json"
    Write-Host "✓ Loan account activated" -ForegroundColor Green
    Write-Host "  Status: $($activateLoanRes.status)" -ForegroundColor Gray
} catch {
    Write-Host "✗ Activation failed:" $_.Exception.Message -ForegroundColor Red
}

Write-Host ""

# Test 12: Add charge on ACTIVE loan
Write-Host "Test 12: Adding charge to ACTIVE loan..." -ForegroundColor Yellow
$chargeReq = @{
    amount = 5000.0
    dueDate = "12 March 2026"
    locale = "en"
    dateFormat = "dd MMMM yyyy"
} | ConvertTo-Json

try {
    $chargeRes = Invoke-RestMethod -Uri "$baseUrl/loans/LOAN_STATE_001/charges" -Method Post -Body $chargeReq -ContentType "application/json"
    Write-Host "✓ Charge added. Outstanding:" $chargeRes.changes.principalOutstanding -ForegroundColor Green
} catch {
    Write-Host "✗ Charge failed:" $_.Exception.Message -ForegroundColor Red
}

Write-Host ""

# Test 13: Lock loan account
Write-Host "Test 13: Locking loan account..." -ForegroundColor Yellow
$lockLoanReq = @{
    reason = "Payment verification required"
} | ConvertTo-Json

try {
    $lockLoanRes = Invoke-RestMethod -Uri "$baseUrl/loans/LOAN_STATE_001?command=lock" -Method Post -Body $lockLoanReq -ContentType "application/json"
    Write-Host "✓ Loan account locked" -ForegroundColor Green
    Write-Host "  Status: $($lockLoanRes.status)" -ForegroundColor Gray
} catch {
    Write-Host "✗ Lock failed:" $_.Exception.Message -ForegroundColor Red
}

Write-Host ""

# Test 14: Try to add charge on LOCKED loan (should fail)
Write-Host "Test 14: Attempting charge on LOCKED loan (should fail)..." -ForegroundColor Yellow
try {
    $chargeRes2 = Invoke-RestMethod -Uri "$baseUrl/loans/LOAN_STATE_001/charges" -Method Post -Body $chargeReq -ContentType "application/json"
    Write-Host "✗ UNEXPECTED: Charge succeeded on locked account!" -ForegroundColor Red
} catch {
    Write-Host "✓ Charge correctly blocked on LOCKED account" -ForegroundColor Green
}

Write-Host ""

# Test 15: Make payment on LOCKED loan (should succeed)
Write-Host "Test 15: Making payment on LOCKED loan (should succeed)..." -ForegroundColor Yellow
$paymentReq = @{
    amount = 2000.0
} | ConvertTo-Json

try {
    $paymentRes = Invoke-RestMethod -Uri "$baseUrl/loans/LOAN_STATE_001/payments" -Method Post -Body $paymentReq -ContentType "application/json"
    Write-Host "✓ Payment accepted on LOCKED account. Outstanding:" $paymentRes.changes.principalOutstanding -ForegroundColor Green
} catch {
    Write-Host "✗ Payment failed:" $_.Exception.Message -ForegroundColor Red
}

Write-Host ""
Write-Host "========================================" -ForegroundColor Cyan
Write-Host "All State Machine Tests Complete!" -ForegroundColor Cyan
Write-Host "========================================" -ForegroundColor Cyan
