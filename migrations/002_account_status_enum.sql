-- Migration: Add Account Status Lifecycle Management
-- Run before deploying state machine feature

-- Ensure all EXISTING accounts remain ACTIVE (they're already in use)
-- New accounts will be created as PENDING and require activation
UPDATE savings_accounts
SET
    status = 'ACTIVE'
WHERE
    status IS NULL
    OR status = ''
    OR status = 'PENDING';

UPDATE loan_accounts
SET
    status = 'ACTIVE'
WHERE
    status IS NULL
    OR status = ''
    OR status = 'PENDING';

-- Add lock_reason column to track why accounts are locked
ALTER TABLE savings_accounts
ADD COLUMN IF NOT EXISTS lock_reason VARCHAR(500);

ALTER TABLE loan_accounts
ADD COLUMN IF NOT EXISTS lock_reason VARCHAR(500);

-- Add closed_date column to track when accounts were closed
ALTER TABLE savings_accounts
ADD COLUMN IF NOT EXISTS closed_date TIMESTAMP;

ALTER TABLE loan_accounts
ADD COLUMN IF NOT EXISTS closed_date TIMESTAMP;

-- Create index on status for better query performance
CREATE INDEX IF NOT EXISTS idx_savings_accounts_status ON savings_accounts (status);

CREATE INDEX IF NOT EXISTS idx_loan_accounts_status ON loan_accounts (status);