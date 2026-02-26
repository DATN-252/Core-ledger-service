# Check Database Status
# This script connects to PostgreSQL and checks the current database state

Write-Host "========================================" -ForegroundColor Cyan
Write-Host "Database Status Check" -ForegroundColor Cyan
Write-Host "========================================" -ForegroundColor Cyan
Write-Host ""

$dbHost = "localhost"
$dbPort = "5432"
$dbName = "ledger_db"
$dbUser = "postgres"
$dbPassword = "postgres"

# Set PGPASSWORD environment variable
$env:PGPASSWORD = $dbPassword

Write-Host "Connecting to: $dbName@$dbHost:$dbPort" -ForegroundColor Yellow
Write-Host ""

# Check if psql is available
try {
    $psqlVersion = psql --version 2>&1
    Write-Host "✓ PostgreSQL client found: $psqlVersion" -ForegroundColor Green
} catch {
    Write-Host "✗ psql not found in PATH" -ForegroundColor Red
    Write-Host "Please install PostgreSQL client tools or use pgAdmin" -ForegroundColor Yellow
    exit 1
}

Write-Host ""
Write-Host "========================================" -ForegroundColor Cyan
Write-Host "1. Checking Tables" -ForegroundColor Cyan
Write-Host "========================================" -ForegroundColor Cyan

$checkTablesSQL = @"
SELECT 
    schemaname as schema,
    tablename as table_name,
    pg_size_pretty(pg_total_relation_size(schemaname||'.'||tablename)) as size
FROM pg_tables
WHERE schemaname = 'public'
ORDER BY tablename;
"@

psql -h $dbHost -p $dbPort -U $dbUser -d $dbName -c $checkTablesSQL

Write-Host ""
Write-Host "========================================" -ForegroundColor Cyan
Write-Host "2. Checking Clients Table" -ForegroundColor Cyan
Write-Host "========================================" -ForegroundColor Cyan

$checkClientsSQL = @"
SELECT 
    COUNT(*) as total_clients,
    COUNT(DISTINCT status) as distinct_statuses
FROM clients;
"@

try {
    psql -h $dbHost -p $dbPort -U $dbUser -d $dbName -c $checkClientsSQL
    
    Write-Host ""
    Write-Host "Sample clients:" -ForegroundColor Yellow
    $sampleClientsSQL = "SELECT client_id, full_name, email, status FROM clients LIMIT 5;"
    psql -h $dbHost -p $dbPort -U $dbUser -d $dbName -c $sampleClientsSQL
} catch {
    Write-Host "✗ Clients table does not exist or is empty" -ForegroundColor Red
}

Write-Host ""
Write-Host "========================================" -ForegroundColor Cyan
Write-Host "3. Checking Savings Accounts" -ForegroundColor Cyan
Write-Host "========================================" -ForegroundColor Cyan

$checkSavingsSQL = @"
SELECT 
    COUNT(*) as total_accounts,
    COUNT(client_id) as accounts_with_client,
    COUNT(client_name) as accounts_with_name,
    SUM(balance) as total_balance
FROM savings_accounts;
"@

try {
    psql -h $dbHost -p $dbPort -U $dbUser -d $dbName -c $checkSavingsSQL
} catch {
    Write-Host "✗ Savings_accounts table does not exist" -ForegroundColor Red
}

Write-Host ""
Write-Host "========================================" -ForegroundColor Cyan
Write-Host "4. Checking Loan Accounts" -ForegroundColor Cyan
Write-Host "========================================" -ForegroundColor Cyan

$checkLoansSQL = @"
SELECT 
    COUNT(*) as total_accounts,
    COUNT(client_id) as accounts_with_client,
    COUNT(client_name) as accounts_with_name,
    SUM(principal) as total_credit_limit,
    SUM(principal_outstanding) as total_outstanding
FROM loan_accounts;
"@

try {
    psql -h $dbHost -p $dbPort -U $dbUser -d $dbName -c $checkLoansSQL
} catch {
    Write-Host "✗ Loan_accounts table does not exist" -ForegroundColor Red
}

Write-Host ""
Write-Host "========================================" -ForegroundColor Cyan
Write-Host "5. Checking Foreign Keys" -ForegroundColor Cyan
Write-Host "========================================" -ForegroundColor Cyan

$checkFKSQL = @"
SELECT
    tc.table_name, 
    kcu.column_name, 
    ccu.table_name AS foreign_table_name,
    ccu.column_name AS foreign_column_name,
    tc.constraint_name
FROM information_schema.table_constraints AS tc 
JOIN information_schema.key_column_usage AS kcu
    ON tc.constraint_name = kcu.constraint_name
    AND tc.table_schema = kcu.table_schema
JOIN information_schema.constraint_column_usage AS ccu
    ON ccu.constraint_name = tc.constraint_name
    AND ccu.table_schema = tc.table_schema
WHERE tc.constraint_type = 'FOREIGN KEY' 
    AND tc.table_schema = 'public'
    AND (tc.table_name = 'savings_accounts' OR tc.table_name = 'loan_accounts');
"@

psql -h $dbHost -p $dbPort -U $dbUser -d $dbName -c $checkFKSQL

Write-Host ""
Write-Host "========================================" -ForegroundColor Cyan
Write-Host "6. Checking Indexes" -ForegroundColor Cyan
Write-Host "========================================" -ForegroundColor Cyan

$checkIndexesSQL = @"
SELECT
    tablename,
    indexname,
    indexdef
FROM pg_indexes
WHERE schemaname = 'public'
    AND tablename = 'clients'
ORDER BY tablename, indexname;
"@

psql -h $dbHost -p $dbPort -U $dbUser -d $dbName -c $checkIndexesSQL

Write-Host ""
Write-Host "========================================" -ForegroundColor Cyan
Write-Host "7. Migration Status" -ForegroundColor Cyan
Write-Host "========================================" -ForegroundColor Cyan

# Check if migration has been run
$checkMigrationSQL = @"
DO $$ 
BEGIN
    -- Check if clients table exists
    IF EXISTS (SELECT 1 FROM information_schema.tables WHERE table_name = 'clients') THEN
        RAISE NOTICE '✓ Clients table exists';
    ELSE
        RAISE NOTICE '✗ Clients table MISSING - run migration!';
    END IF;
    
    -- Check if client_id column exists in savings_accounts
    IF EXISTS (
        SELECT 1 FROM information_schema.columns 
        WHERE table_name = 'savings_accounts' AND column_name = 'client_id'
    ) THEN
        RAISE NOTICE '✓ savings_accounts.client_id column exists';
    ELSE
        RAISE NOTICE '✗ savings_accounts.client_id MISSING - run migration!';
    END IF;
    
    -- Check if client_id column exists in loan_accounts
    IF EXISTS (
        SELECT 1 FROM information_schema.columns 
        WHERE table_name = 'loan_accounts' AND column_name = 'client_id'
    ) THEN
        RAISE NOTICE '✓ loan_accounts.client_id column exists';
    ELSE
        RAISE NOTICE '✗ loan_accounts.client_id MISSING - run migration!';
    END IF;
    
    -- Check FK constraints
    IF EXISTS (
        SELECT 1 FROM pg_constraint WHERE conname = 'fk_savings_client'
    ) THEN
        RAISE NOTICE '✓ FK constraint fk_savings_client exists';
    ELSE
        RAISE NOTICE '⚠ FK constraint fk_savings_client MISSING';
    END IF;
    
    IF EXISTS (
        SELECT 1 FROM pg_constraint WHERE conname = 'fk_loan_client'
    ) THEN
        RAISE NOTICE '✓ FK constraint fk_loan_client exists';
    ELSE
        RAISE NOTICE '⚠ FK constraint fk_loan_client MISSING';
    END IF;
END $$;
"@

psql -h $dbHost -p $dbPort -U $dbUser -d $dbName -c $checkMigrationSQL

Write-Host ""
Write-Host "========================================" -ForegroundColor Cyan
Write-Host "Summary" -ForegroundColor Cyan
Write-Host "========================================" -ForegroundColor Cyan

$summarySQL = @"
SELECT 
    'Clients' as entity,
    (SELECT COUNT(*) FROM clients) as count
UNION ALL
SELECT 
    'Savings Accounts',
    (SELECT COUNT(*) FROM savings_accounts)
UNION ALL
SELECT 
    'Loan Accounts',
    (SELECT COUNT(*) FROM loan_accounts)
UNION ALL
SELECT 
    'Transactions',
    (SELECT COUNT(*) FROM transactions);
"@

psql -h $dbHost -p $dbPort -U $dbUser -d $dbName -c $summarySQL

Write-Host ""
Write-Host "========================================" -ForegroundColor Green
Write-Host "Database check complete!" -ForegroundColor Green
Write-Host "========================================" -ForegroundColor Green
Write-Host ""
Write-Host "Next steps:" -ForegroundColor Yellow
Write-Host "  1. If migration not run: psql -U postgres -d ledger_db -f migrations/001_add_client_entity.sql" -ForegroundColor Gray
Write-Host "  2. If tables empty: Run test-client-api.ps1 to create sample data" -ForegroundColor Gray
Write-Host "  3. Start service: .\gradlew bootRun" -ForegroundColor Gray
Write-Host ""

# Clean up
$env:PGPASSWORD = $null
