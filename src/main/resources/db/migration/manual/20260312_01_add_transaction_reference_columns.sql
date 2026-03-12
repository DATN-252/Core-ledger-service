BEGIN;

ALTER TABLE transactions
    ADD COLUMN IF NOT EXISTS payment_id VARCHAR(64),
    ADD COLUMN IF NOT EXISTS idempotency_key VARCHAR(120),
    ADD COLUMN IF NOT EXISTS original_transaction_id VARCHAR(64),
    ADD COLUMN IF NOT EXISTS channel VARCHAR(20),
    ADD COLUMN IF NOT EXISTS auth_code VARCHAR(20),
    ADD COLUMN IF NOT EXISTS stan VARCHAR(20),
    ADD COLUMN IF NOT EXISTS rrn VARCHAR(30),
    ADD COLUMN IF NOT EXISTS external_reference VARCHAR(100),
    ADD COLUMN IF NOT EXISTS response_code VARCHAR(10),
    ADD COLUMN IF NOT EXISTS response_message VARCHAR(255);

UPDATE transactions
SET payment_id = CONCAT('LEGACY-', id)
WHERE payment_id IS NULL;

UPDATE transactions
SET channel = 'SYSTEM'
WHERE channel IS NULL OR BTRIM(channel) = '';

UPDATE transactions
SET response_code = CASE
    WHEN COALESCE(status, 'SUCCESS') = 'SUCCESS' THEN '00'
    ELSE '96'
END
WHERE response_code IS NULL OR BTRIM(response_code) = '';

UPDATE transactions
SET response_message = CASE
    WHEN COALESCE(status, 'SUCCESS') = 'SUCCESS' THEN 'Approved'
    ELSE COALESCE(description, 'Declined')
END
WHERE response_message IS NULL OR BTRIM(response_message) = '';

ALTER TABLE transactions
    ALTER COLUMN payment_id SET NOT NULL;

DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1
        FROM pg_constraint
        WHERE conname = 'uk_transactions_payment_id'
    ) THEN
        ALTER TABLE transactions
            ADD CONSTRAINT uk_transactions_payment_id UNIQUE (payment_id);
    END IF;
END $$;

CREATE INDEX IF NOT EXISTS idx_transactions_payment_id
    ON transactions (payment_id);

CREATE INDEX IF NOT EXISTS idx_transactions_idempotency_key
    ON transactions (idempotency_key);

CREATE INDEX IF NOT EXISTS idx_transactions_account_type_date
    ON transactions (account_number, account_type, transaction_date);

CREATE INDEX IF NOT EXISTS idx_transactions_merchant_status_date
    ON transactions (merchant_id, status, transaction_date);

COMMIT;
