ALTER TABLE merchant_settlement_batches
    ADD COLUMN IF NOT EXISTS adjustment_count integer NOT NULL DEFAULT 0;

ALTER TABLE merchant_settlement_batches
    ADD COLUMN IF NOT EXISTS adjustment_amount double precision NOT NULL DEFAULT 0.0;

CREATE TABLE IF NOT EXISTS merchant_settlement_adjustments (
    id bigserial PRIMARY KEY,
    merchant_id varchar(50) NOT NULL,
    merchant_name varchar(255) NOT NULL,
    original_transaction_id bigint NOT NULL,
    original_payment_id varchar(64),
    adjustment_transaction_id bigint NOT NULL,
    original_batch_id bigint NOT NULL,
    adjustment_type varchar(40) NOT NULL,
    amount double precision NOT NULL,
    currency varchar(10) NOT NULL,
    status varchar(20) NOT NULL DEFAULT 'PENDING',
    reason varchar(255),
    reserved_batch_id bigint,
    applied_batch_id bigint,
    created_at timestamp(6) NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at timestamp(6)
);

CREATE INDEX IF NOT EXISTS idx_settlement_adjustments_merchant_status
    ON merchant_settlement_adjustments (merchant_id, status);

CREATE INDEX IF NOT EXISTS idx_settlement_adjustments_adjustment_txn
    ON merchant_settlement_adjustments (adjustment_transaction_id);

CREATE INDEX IF NOT EXISTS idx_settlement_adjustments_original_txn
    ON merchant_settlement_adjustments (original_transaction_id);
