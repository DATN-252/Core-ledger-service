ALTER TABLE loan_accounts
ADD COLUMN IF NOT EXISTS statement_interest_rate_annual DOUBLE PRECISION NOT NULL DEFAULT 30.0;

UPDATE loan_accounts
SET statement_interest_rate_annual = CASE
    WHEN statement_interest_rate_monthly IS NOT NULL AND statement_interest_rate_monthly > 0
        THEN ROUND((statement_interest_rate_monthly * 12.0)::NUMERIC, 6)::DOUBLE PRECISION
    ELSE COALESCE(statement_interest_rate_annual, 30.0)
END;

ALTER TABLE credit_card_statements
ADD COLUMN IF NOT EXISTS interest_rate_annual DOUBLE PRECISION NOT NULL DEFAULT 30.0;

UPDATE credit_card_statements
SET interest_rate_annual = CASE
    WHEN interest_rate_monthly IS NOT NULL AND interest_rate_monthly > 0
        THEN ROUND((interest_rate_monthly * 12.0)::NUMERIC, 6)::DOUBLE PRECISION
    ELSE COALESCE(interest_rate_annual, 30.0)
END;