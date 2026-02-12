-- Quick Database Check Script
-- Run this with: psql -U postgres -d ledger_db -f check-database.sql


\echo '========================================'
\echo 'Database Status Check'
\echo '========================================'
\echo ''

\echo '1. All Tables:'
SELECT 
    schemaname as schema,
    tablename as table_name,
    pg_size_pretty(pg_total_relation_size(schemaname||'.'||tablename)) as size
FROM pg_tables
WHERE schemaname = 'public'
ORDER BY tablename;

\echo ''
\echo '2. Clients Summary:'
SELECT 
    COUNT(*) as total_clients,
    COUNT(CASE WHEN status = 'ACTIVE' THEN 1 END) as active,
    COUNT(CASE WHEN status = 'INACTIVE' THEN 1 END) as inactive,
    COUNT(CASE WHEN occupation IS NOT NULL THEN 1 END) as with_employment_info
FROM clients;

\echo ''
\echo '3. Sample Clients:'
SELECT client_id, full_name, email, status, occupation 
FROM clients 
LIMIT 5;

\echo ''
\echo '4. Savings Accounts Summary:'
SELECT 
    COUNT(*) as total_accounts,
    COUNT(client_id) as with_client_fk,
    COUNT(client_name) as with_client_name,
    ROUND(SUM(balance)::numeric, 2) as total_balance
FROM savings_accounts;

\echo ''
\echo '5. Loan Accounts Summary:'
SELECT 
    COUNT(*) as total_accounts,
    COUNT(client_id) as with_client_fk,
    COUNT(client_name) as with_client_name,
    ROUND(SUM(principal)::numeric, 2) as total_credit_limit,
    ROUND(SUM(principal_outstanding)::numeric, 2) as total_outstanding
FROM loan_accounts;

\echo ''
\echo '6. Foreign Key Constraints:'
SELECT
    tc.table_name, 
    kcu.column_name, 
    ccu.table_name AS foreign_table,
    tc.constraint_name
FROM information_schema.table_constraints AS tc 
JOIN information_schema.key_column_usage AS kcu
    ON tc.constraint_name = kcu.constraint_name
JOIN information_schema.constraint_column_usage AS ccu
    ON ccu.constraint_name = tc.constraint_name
WHERE tc.constraint_type = 'FOREIGN KEY' 
    AND tc.table_schema = 'public'
    AND (tc.table_name = 'savings_accounts' OR tc.table_name = 'loan_accounts');

\echo ''
\echo '7. Client-Account Relationships:'
SELECT 
    c.client_id,
    c.full_name,
    COUNT(DISTINCT sa.id) as savings_count,
    COUNT(DISTINCT la.id) as loan_count
FROM clients c
LEFT JOIN savings_accounts sa ON sa.client_id = c.id
LEFT JOIN loan_accounts la ON la.client_id = c.id
GROUP BY c.client_id, c.full_name
LIMIT 5;

\echo ''
\echo '8. Migration Status Check:'
DO $$ 
BEGIN
    IF EXISTS (SELECT 1 FROM information_schema.tables WHERE table_name = 'clients') THEN
        RAISE NOTICE '✓ Clients table exists';

ELSE RAISE NOTICE '✗ Clients table MISSING';

END IF;

IF EXISTS (
    SELECT 1
    FROM information_schema.columns
    WHERE
        table_name = 'savings_accounts'
        AND column_name = 'client_id'
) THEN RAISE NOTICE '✓ savings_accounts.client_id exists';

ELSE RAISE NOTICE '✗ savings_accounts.client_id MISSING';

END IF;

IF EXISTS (
    SELECT 1
    FROM pg_constraint
    WHERE
        conname = 'fk_savings_client'
) THEN RAISE NOTICE '✓ FK fk_savings_client exists';

ELSE RAISE NOTICE '⚠ FK fk_savings_client MISSING';

END IF;

IF EXISTS (
    SELECT 1
    FROM pg_constraint
    WHERE
        conname = 'fk_loan_client'
) THEN RAISE NOTICE '✓ FK fk_loan_client exists';

ELSE RAISE NOTICE '⚠ FK fk_loan_client MISSING';

END IF;

END $$;

\echo ''
\echo '========================================'
\echo 'Check complete!'
\echo '========================================'