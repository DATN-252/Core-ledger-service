ALTER TABLE IF EXISTS loan_accounts
ADD COLUMN IF NOT EXISTS statement_late_fee_rate DOUBLE PRECISION NOT NULL DEFAULT 4.0;

ALTER TABLE IF EXISTS credit_card_statements
ADD COLUMN IF NOT EXISTS current_minimum_due DOUBLE PRECISION NOT NULL DEFAULT 0.0;

ALTER TABLE IF EXISTS credit_card_statements
ADD COLUMN IF NOT EXISTS past_due_minimum DOUBLE PRECISION NOT NULL DEFAULT 0.0;

ALTER TABLE IF EXISTS credit_card_statements
ADD COLUMN IF NOT EXISTS total_minimum_due_now DOUBLE PRECISION NOT NULL DEFAULT 0.0;

ALTER TABLE IF EXISTS credit_card_statements
ADD COLUMN IF NOT EXISTS grace_period_eligible BOOLEAN NOT NULL DEFAULT FALSE;

ALTER TABLE IF EXISTS credit_card_statements
ADD COLUMN IF NOT EXISTS late_fee_rate DOUBLE PRECISION NOT NULL DEFAULT 4.0;

UPDATE loan_accounts
SET statement_late_fee_rate = 4.0
WHERE statement_late_fee_rate IS NULL
   OR statement_late_fee_rate = 0;

UPDATE credit_card_statements
SET current_minimum_due = COALESCE(current_minimum_due, minimum_due, 0.0),
    past_due_minimum = COALESCE(past_due_minimum, 0.0),
    total_minimum_due_now = COALESCE(total_minimum_due_now, current_minimum_due, minimum_due, 0.0),
    grace_period_eligible = COALESCE(grace_period_eligible, FALSE),
    late_fee_rate = COALESCE(NULLIF(late_fee_rate, 0.0), 4.0)
WHERE current_minimum_due IS NULL
   OR past_due_minimum IS NULL
   OR total_minimum_due_now IS NULL
   OR grace_period_eligible IS NULL
   OR late_fee_rate IS NULL
   OR late_fee_rate = 0.0;