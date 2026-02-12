-- Active: 1770868476303@@127.0.0.1@5434@ledgerdb
-- Migration script to add Client entity and update account relationships
-- Run this EITHER before first service start OR to migrate existing data

-- ============================================
-- PART 1: Create base tables (if not exist)
-- ============================================

-- Create savings_accounts table (if not exists)
CREATE TABLE IF NOT EXISTS savings_accounts (
    id BIGSERIAL PRIMARY KEY,
    account_number VARCHAR(255) UNIQUE NOT NULL,
    balance DOUBLE PRECISION NOT NULL DEFAULT 0.0,
    currency VARCHAR(255) NOT NULL DEFAULT 'USD',
    status VARCHAR(255) NOT NULL DEFAULT 'ACTIVE',
    client_name VARCHAR(255),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP
);

-- Create loan_accounts table (if not exists)
CREATE TABLE IF NOT EXISTS loan_accounts (
    id BIGSERIAL PRIMARY KEY,
    account_number VARCHAR(255) UNIQUE NOT NULL,
    principal DOUBLE PRECISION NOT NULL,
    principal_outstanding DOUBLE PRECISION NOT NULL DEFAULT 0.0,
    currency VARCHAR(255) NOT NULL DEFAULT 'USD',
    status VARCHAR(255) NOT NULL DEFAULT 'ACTIVE',
    client_name VARCHAR(255),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP
);

-- Create transactions table (if not exists)
CREATE TABLE IF NOT EXISTS transactions (
    id BIGSERIAL PRIMARY KEY,
    account_number VARCHAR(255) NOT NULL,
    transaction_type VARCHAR(50) NOT NULL,
    amount DOUBLE PRECISION NOT NULL,
    balance_after DOUBLE PRECISION NOT NULL,
    transaction_date TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    description VARCHAR(500)
);

-- ============================================
-- PART 2: Create clients table
-- ============================================
CREATE TABLE IF NOT EXISTS clients (
    id BIGSERIAL PRIMARY KEY,
    client_id VARCHAR(50) UNIQUE NOT NULL,
    -- Core required fields
    full_name VARCHAR(255) NOT NULL,
    date_of_birth DATE NOT NULL,
    gender VARCHAR(10) NOT NULL,
    email VARCHAR(255) UNIQUE NOT NULL,
    phone_number VARCHAR(20) NOT NULL,
    address VARCHAR(500) NOT NULL,
    id_number VARCHAR(50) UNIQUE NOT NULL,
    id_type VARCHAR(20) NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
    -- Optional fields
    city VARCHAR(100),
    country VARCHAR(100),
    id_issue_date DATE,
    id_expiry_date DATE,
    occupation VARCHAR(255),
    employer_name VARCHAR(255),
    employer_address VARCHAR(500),
    employment_type VARCHAR(20),
    monthly_income DOUBLE PRECISION,
    years_at_current_job INTEGER,
    -- Audit fields
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP,
    CONSTRAINT chk_gender CHECK (
        gender IN ('MALE', 'FEMALE', 'OTHER')
    ),
    CONSTRAINT chk_id_type CHECK (
        id_type IN (
            'NATIONAL_ID',
            'PASSPORT',
            'DRIVERS_LICENSE'
        )
    ),
    CONSTRAINT chk_employment_type CHECK (
        employment_type IN (
            'FULL_TIME',
            'PART_TIME',
            'SELF_EMPLOYED',
            'UNEMPLOYED',
            'RETIRED',
            'STUDENT'
        )
    ),
    CONSTRAINT chk_status CHECK (
        status IN (
            'ACTIVE',
            'INACTIVE',
            'BLOCKED'
        )
    )
);

-- Create indexes
CREATE INDEX idx_clients_email ON clients (email);

CREATE INDEX idx_clients_id_number ON clients (id_number);

CREATE INDEX idx_clients_status ON clients (status);

CREATE INDEX idx_clients_full_name ON clients (full_name);

-- Step 2: Add client_id column to savings_accounts (nullable first)
ALTER TABLE savings_accounts
ADD COLUMN IF NOT EXISTS client_id BIGINT;

-- Step 3: Add client_id column to loan_accounts (nullable first)
ALTER TABLE loan_accounts ADD COLUMN IF NOT EXISTS client_id BIGINT;

-- Step 4: Migrate existing data - create default clients from account data
-- This creates one client per unique clientName (only if data exists)
INSERT INTO
    clients (
        client_id,
        full_name,
        date_of_birth,
        gender,
        email,
        phone_number,
        address,
        id_number,
        id_type,
        status
    )
SELECT
    'CLI_' || ROW_NUMBER() OVER (
        ORDER BY client_name
    ) AS client_id,
    client_name AS full_name,
    '1990-01-01' AS date_of_birth,
    'OTHER' AS gender,
    LOWER(
        REPLACE(client_name, ' ', '.')
    ) || '@migrated.example.com' AS email,
    '+84900000000' AS phone_number,
    'Migrated Address' AS address,
    'MIG' || LPAD(
        ROW_NUMBER() OVER (
            ORDER BY client_name
        )::TEXT,
        10,
        '0'
    ) AS id_number,
    'NATIONAL_ID' AS id_type,
    'ACTIVE' AS status
FROM (
        SELECT DISTINCT
            COALESCE(client_name, 'Unknown Client') AS client_name
        FROM savings_accounts
        WHERE
            client_name IS NOT NULL
            AND client_name != ''
        UNION
        SELECT DISTINCT
            COALESCE(client_name, 'Unknown Client') AS client_name
        FROM loan_accounts
        WHERE
            client_name IS NOT NULL
            AND client_name != ''
    ) AS unique_clients
WHERE
    client_name IS NOT NULL
ON CONFLICT (client_id) DO NOTHING;

-- Step 5: Update savings_accounts to reference clients (only if clients exist)
UPDATE savings_accounts sa
SET
    client_id = c.id
FROM clients c
WHERE
    sa.client_name = c.full_name
    AND sa.client_name IS NOT NULL
    AND sa.client_id IS NULL;

-- Step 6: Update loan_accounts to reference clients (only if clients exist)
UPDATE loan_accounts la
SET
    client_id = c.id
FROM clients c
WHERE
    la.client_name = c.full_name
    AND la.client_name IS NOT NULL
    AND la.client_id IS NULL;

-- Step 7: Add foreign key constraints (if not exists)
DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM pg_constraint WHERE conname = 'fk_savings_client'
    ) THEN
        ALTER TABLE savings_accounts
            ADD CONSTRAINT fk_savings_client
            FOREIGN KEY (client_id) REFERENCES clients(id);
    END IF;

    IF NOT EXISTS (
        SELECT 1 FROM pg_constraint WHERE conname = 'fk_loan_client'
    ) THEN
        ALTER TABLE loan_accounts
            ADD CONSTRAINT fk_loan_client
            FOREIGN KEY (client_id) REFERENCES clients(id);
    END IF;
END $$;

-- Step 8: Make client_id NOT NULL only if there is data and all records have client_id
DO $$
BEGIN
    -- For savings_accounts
    IF (SELECT COUNT(*) FROM savings_accounts WHERE client_id IS NULL) = 0 THEN
        IF (SELECT COUNT(*) FROM savings_accounts) > 0 THEN
            ALTER TABLE savings_accounts ALTER COLUMN client_id SET NOT NULL;
        END IF;
    ELSE
        RAISE NOTICE 'Skipping NOT NULL constraint on savings_accounts.client_id - some records still have NULL values';
    END IF;

    -- For loan_accounts
    IF (SELECT COUNT(*) FROM loan_accounts WHERE client_id IS NULL) = 0 THEN
        IF (SELECT COUNT(*) FROM loan_accounts) > 0 THEN
            ALTER TABLE loan_accounts ALTER COLUMN client_id SET NOT NULL;
        END IF;
    ELSE
        RAISE NOTICE 'Skipping NOT NULL constraint on loan_accounts.client_id - some records still have NULL values';
    END IF;
END $$;

-- Step 9: Drop old client_name columns (optional - can keep for backward compatibility)
-- Uncomment these lines if you want to completely remove the old columns:
-- ALTER TABLE savings_accounts DROP COLUMN client_name;
-- ALTER TABLE loan_accounts DROP COLUMN client_name;

-- Step 10: Verify migration
SELECT 'Migration Summary:' AS info;

SELECT COUNT(*) AS total_clients FROM clients;

SELECT COUNT(*) AS savings_accounts_with_client
FROM savings_accounts
WHERE
    client_id IS NOT NULL;

SELECT COUNT(*) AS loan_accounts_with_client
FROM loan_accounts
WHERE
    client_id IS NOT NULL;

-- Display sample data
SELECT * FROM clients LIMIT 5;