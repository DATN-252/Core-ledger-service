ALTER TABLE IF EXISTS loan_accounts
ADD COLUMN IF NOT EXISTS statement_interest_rate_monthly DOUBLE PRECISION NOT NULL DEFAULT 2.5;

ALTER TABLE IF EXISTS credit_card_statements
ADD COLUMN IF NOT EXISTS interest_rate_monthly DOUBLE PRECISION NOT NULL DEFAULT 2.5;

ALTER TABLE IF EXISTS credit_card_statements
ADD COLUMN IF NOT EXISTS interest_charged DOUBLE PRECISION NOT NULL DEFAULT 0.0;

ALTER TABLE IF EXISTS credit_card_statements
ADD COLUMN IF NOT EXISTS interest_applied_at TIMESTAMP;

UPDATE loan_accounts
SET statement_interest_rate_monthly = 2.5
WHERE statement_interest_rate_monthly IS NULL
   OR statement_interest_rate_monthly = 0;

UPDATE credit_card_statements
SET interest_rate_monthly = 2.5
WHERE interest_rate_monthly IS NULL
   OR interest_rate_monthly = 0;

UPDATE credit_card_statements
SET interest_charged = 0.0
WHERE interest_charged IS NULL;
