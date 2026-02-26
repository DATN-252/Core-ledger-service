# Test Client APIs

Write-Host "========================================" -ForegroundColor Cyan
Write-Host "Testing Client Entity APIs" -ForegroundColor Cyan
Write-Host "========================================" -ForegroundColor Cyan
Write-Host ""

$baseUrl = "http://localhost:8083"

# ============================================
# Test 1: Create Client với Minimal Fields
# ============================================
Write-Host "Test 1: Create Client (Minimal)" -ForegroundColor Yellow

$createMinimal = @{
    clientId = "CLI_TEST_001"
    fullName = "Nguyen Van Test"
    dateOfBirth = "1990-01-01"
    gender = "MALE"
    email = "test001@example.com"
    phoneNumber = "+84901234567"
    address = "123 Test Street, Hanoi"
    idNumber = "001234567890"
    idType = "NATIONAL_ID"
} | ConvertTo-Json

try {
    $response = Invoke-RestMethod -Uri "$baseUrl/clients" -Method Post -Body $createMinimal -ContentType "application/json"
    Write-Host "✓ Created minimal client: $($response.clientId)" -ForegroundColor Green
    Write-Host "  Name: $($response.fullName)" -ForegroundColor Gray
} catch {
    Write-Host "✗ Failed: $_" -ForegroundColor Red
}

Write-Host ""

# ============================================
# Test 2: Create Client với Full Fields
# ============================================
Write-Host "Test 2: Create Client (Full)" -ForegroundColor Yellow

$createFull = @{
    clientId = "CLI_TEST_002"
    fullName = "Tran Thi Full"
    dateOfBirth = "1992-05-15"
    gender = "FEMALE"
    email = "test002@example.com"
    phoneNumber = "+84987654321"
    address = "456 Full Street"
    city = "Ho Chi Minh"
    country = "Vietnam"
    idNumber = "002345678901"
    idType = "NATIONAL_ID"
    idIssueDate = "2018-01-01"
    idExpiryDate = "2033-01-01"
    occupation = "Software Engineer"
    employerName = "FPT Software"
    employerAddress = "FPT Tower, Hanoi"
    employmentType = "FULL_TIME"
    monthlyIncome = 30000000.0
    yearsAtCurrentJob = 4
} | ConvertTo-Json

try {
    $response = Invoke-RestMethod -Uri "$baseUrl/clients" -Method Post -Body $createFull -ContentType "application/json"
    Write-Host "✓ Created full client: $($response.clientId)" -ForegroundColor Green
} catch {
    Write-Host "✗ Failed: $_" -ForegroundColor Red
}

Write-Host ""

# ============================================
# Test 3: Get Client Details
# ============================================
Write-Host "Test 3: Get Client Details" -ForegroundColor Yellow

try {
    $client = Invoke-RestMethod -Uri "$baseUrl/clients/CLI_TEST_002" -Method Get
    Write-Host "✓ Retrieved client: $($client.fullName)" -ForegroundColor Green
    Write-Host "  Email: $($client.email)" -ForegroundColor Gray
    Write-Host "  Occupation: $($client.occupation)" -ForegroundColor Gray
    Write-Host "  Monthly Income: $($client.monthlyIncome) VND" -ForegroundColor Gray
    Write-Host "  Suggested Credit Limit: $($client.suggestedCreditLimit) VND" -ForegroundColor Cyan
    Write-Host "  ID Expired: $($client.idExpired)" -ForegroundColor Gray
    Write-Host "  ID Expiring Soon: $($client.idExpiringSoon)" -ForegroundColor Gray
} catch {
    Write-Host "✗ Failed: $_" -ForegroundColor Red
}

Write-Host ""

# ============================================
# Test 4: Update Client (Add Employment Info)
# ============================================
Write-Host "Test 4: Update Client Employment" -ForegroundColor Yellow

$update = @{
    occupation = "Teacher"
    employerName = "Hanoi University"
    monthlyIncome = 15000000.0
    employmentType = "FULL_TIME"
    yearsAtCurrentJob = 2
} | ConvertTo-Json

try {
    $response = Invoke-RestMethod -Uri "$baseUrl/clients/CLI_TEST_001" -Method Put -Body $update -ContentType "application/json"
    Write-Host "✓ Updated client employment info" -ForegroundColor Green
    
    # Verify update
    $updatedClient = Invoke-RestMethod -Uri "$baseUrl/clients/CLI_TEST_001" -Method Get
    Write-Host "  New occupation: $($updatedClient.occupation)" -ForegroundColor Gray
    Write-Host "  New suggested credit: $($updatedClient.suggestedCreditLimit) VND" -ForegroundColor Cyan
} catch {
    Write-Host "✗ Failed: $_" -ForegroundColor Red
}

Write-Host ""

# ============================================
# Test 5: Create Savings Account for Client
# ============================================
Write-Host "Test 5: Create Savings Account" -ForegroundColor Yellow

$createSavings = @{
    accountNumber = "SAV_TEST_001"
    balance = 1000000.0
    clientId = "CLI_TEST_001"
} | ConvertTo-Json

try {
    $response = Invoke-RestMethod -Uri "$baseUrl/savingsaccounts" -Method Post -Body $createSavings -ContentType "application/json"
    Write-Host "✓ Created savings account: $($response.accountNumber)" -ForegroundColor Green
    Write-Host "  Client: $($response.clientName)" -ForegroundColor Gray
} catch {
    Write-Host "✗ Failed: $_" -ForegroundColor Red
}

Write-Host ""

# ============================================
# Test 6: Create Loan Account for Client
# ============================================
Write-Host "Test 6: Create Loan Account" -ForegroundColor Yellow

$createLoan = @{
    accountNumber = "LOAN_TEST_001"
    principal = 50000000.0
    clientId = "CLI_TEST_002"
} | ConvertTo-Json

try {
    $response = Invoke-RestMethod -Uri "$baseUrl/loans" -Method Post -Body $createLoan -ContentType "application/json"
    Write-Host "✓ Created loan account: $($response.accountNumber)" -ForegroundColor Green
    Write-Host "  Credit Limit: $($response.principal) VND" -ForegroundColor Gray
    Write-Host "  Client: $($response.clientName)" -ForegroundColor Gray
} catch {
    Write-Host "✗ Failed: $_" -ForegroundColor Red
}

Write-Host ""

# ============================================
# Test 7: Get Client Accounts
# ============================================
Write-Host "Test 7: Get Client Accounts" -ForegroundColor Yellow

try {
    $accounts = Invoke-RestMethod -Uri "$baseUrl/clients/CLI_TEST_001/accounts" -Method Get
    Write-Host "✓ Retrieved accounts for $($accounts.clientName)" -ForegroundColor Green
    Write-Host "  Savings Accounts: $($accounts.totalSavingsAccounts)" -ForegroundColor Gray
    Write-Host "  Loan Accounts: $($accounts.totalLoanAccounts)" -ForegroundColor Gray
} catch {
    Write-Host "✗ Failed: $_" -ForegroundColor Red
}

Write-Host ""

# ============================================
# Test 8: Search Clients
# ============================================
Write-Host "Test 8: Search Clients by Name" -ForegroundColor Yellow

try {
    $searchResult = Invoke-RestMethod -Uri "$baseUrl/clients/search?name=Test" -Method Get
    Write-Host "✓ Found $($searchResult.total) clients matching 'Test'" -ForegroundColor Green
    foreach ($client in $searchResult.clients) {
        Write-Host "  - $($client.fullName) ($($client.clientId))" -ForegroundColor Gray
    }
} catch {
    Write-Host "✗ Failed: $_" -ForegroundColor Red
}

Write-Host ""

# ============================================
# Test 9: Get All Active Clients
# ============================================
Write-Host "Test 9: Get All Active Clients" -ForegroundColor Yellow

try {
    $allClients = Invoke-RestMethod -Uri "$baseUrl/clients" -Method Get
    Write-Host "✓ Retrieved $($allClients.total) active clients" -ForegroundColor Green
} catch {
    Write-Host "✗ Failed: $_" -ForegroundColor Red
}

Write-Host ""

# ============================================
# Summary
# ============================================
Write-Host "========================================" -ForegroundColor Cyan
Write-Host "Client API Testing Complete!" -ForegroundColor Green
Write-Host "========================================" -ForegroundColor Cyan
Write-Host ""
Write-Host "Key Features Tested:" -ForegroundColor Yellow
Write-Host "  ✓ Create client (minimal fields)" -ForegroundColor Green
Write-Host "  ✓ Create client (full fields)" -ForegroundColor Green
Write-Host "  ✓ Get client details (with calculated fields)" -ForegroundColor Green
Write-Host "  ✓ Update client information" -ForegroundColor Green
Write-Host "  ✓ Create accounts with clientId" -ForegroundColor Green
Write-Host "  ✓ Get client accounts (all)" -ForegroundColor Green
Write-Host "  ✓ Search clients by name" -ForegroundColor Green
Write-Host "  ✓ List all active clients" -ForegroundColor Green
Write-Host ""
