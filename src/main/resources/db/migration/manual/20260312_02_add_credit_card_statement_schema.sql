BEGIN;

ALTER TABLE loan_accounts
    ADD COLUMN IF NOT EXISTS billing_day_of_month INTEGER,
    ADD COLUMN IF NOT EXISTS payment_due_days INTEGER,
    ADD COLUMN IF NOT EXISTS minimum_payment_rate DOUBLE PRECISION,
    ADD COLUMN IF NOT EXISTS minimum_payment_floor DOUBLE PRECISION;

UPDATE loan_accounts
SET billing_day_of_month = 25
WHERE billing_day_of_month IS NULL;

UPDATE loan_accounts
SET payment_due_days = 20
WHERE payment_due_days IS NULL;

UPDATE loan_accounts
SET minimum_payment_rate = 5.0
WHERE minimum_payment_rate IS NULL;

UPDATE loan_accounts
SET minimum_payment_floor = 10.0
WHERE minimum_payment_floor IS NULL;

ALTER TABLE loan_accounts
    ALTER COLUMN billing_day_of_month SET NOT NULL,
    ALTER COLUMN payment_due_days SET NOT NULL,
    ALTER COLUMN minimum_payment_rate SET NOT NULL,
    ALTER COLUMN minimum_payment_floor SET NOT NULL;

CREATE TABLE IF NOT EXISTS credit_card_statements (
    id BIGSERIAL PRIMARY KEY,
    account_number VARCHAR(50) NOT NULL,
    statement_period_start DATE NOT NULL,
    statement_period_end DATE NOT NULL,
    billing_date DATE NOT NULL,
    due_date DATE NOT NULL,
    previous_balance DOUBLE PRECISION NOT NULL,
    total_charges DOUBLE PRECISION NOT NULL,
    total_payments DOUBLE PRECISION NOT NULL,
    minimum_due DOUBLE PRECISION NOT NULL,
    new_balance DOUBLE PRECISION NOT NULL,
    available_credit_at_billing DOUBLE PRECISION NOT NULL,
    transaction_count INTEGER NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP
);

DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1
        FROM pg_constraint
        WHERE conname = 'uk_credit_card_statements_account_billing'
    ) THEN
        ALTER TABLE credit_card_statements
            ADD CONSTRAINT uk_credit_card_statements_account_billing
            UNIQUE (account_number, billing_date);
    END IF;
END $$;

CREATE INDEX IF NOT EXISTS idx_credit_card_statements_account_billing
    ON credit_card_statements (account_number, billing_date DESC);

COMMIT;
