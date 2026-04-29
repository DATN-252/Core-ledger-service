ALTER TABLE IF EXISTS loan_accounts
ADD COLUMN IF NOT EXISTS statement_late_fee_fixed DOUBLE PRECISION NOT NULL DEFAULT 15.0;

ALTER TABLE IF EXISTS credit_card_statements
ADD COLUMN IF NOT EXISTS late_fee_fixed DOUBLE PRECISION NOT NULL DEFAULT 15.0;

ALTER TABLE IF EXISTS credit_card_statements
ADD COLUMN IF NOT EXISTS late_fee_charged DOUBLE PRECISION NOT NULL DEFAULT 0.0;

ALTER TABLE IF EXISTS credit_card_statements
ADD COLUMN IF NOT EXISTS late_fee_applied_at TIMESTAMP;

UPDATE loan_accounts
SET statement_late_fee_fixed = 15.0
WHERE statement_late_fee_fixed IS NULL
   OR statement_late_fee_fixed = 0;

UPDATE credit_card_statements
SET late_fee_fixed = 15.0
WHERE late_fee_fixed IS NULL
   OR late_fee_fixed = 0;

UPDATE credit_card_statements
SET late_fee_charged = 0.0
WHERE late_fee_charged IS NULL;
