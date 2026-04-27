-- pay_order extensions for recharge and channel compatibility.
-- Snapshot fields are persisted so later package price changes do not affect historical orders.
ALTER TABLE pay_order
    ADD COLUMN IF NOT EXISTS package_code VARCHAR(64) NULL,
    ADD COLUMN IF NOT EXISTS package_name_snapshot VARCHAR(128) NULL,
    ADD COLUMN IF NOT EXISTS credit_amount_snapshot INT NULL,
    ADD COLUMN IF NOT EXISTS price_snapshot DECIMAL(18,2) NULL,
    ADD COLUMN IF NOT EXISTS currency VARCHAR(16) NULL DEFAULT 'CNY',
    ADD COLUMN IF NOT EXISTS channel_trade_no VARCHAR(128) NULL,
    ADD COLUMN IF NOT EXISTS tcc_transaction_id VARCHAR(64) NULL,
    ADD COLUMN IF NOT EXISTS notify_status VARCHAR(32) NULL,
    ADD COLUMN IF NOT EXISTS notify_time DATETIME NULL;

CREATE UNIQUE INDEX uk_pay_order_id ON pay_order (pay_order_id);
CREATE UNIQUE INDEX uk_pay_biz ON pay_order (biz_type, biz_no);

-- trade_order extensions for recharge semantics.
ALTER TABLE trade_order
    ADD COLUMN IF NOT EXISTS biz_scene VARCHAR(64) NULL,
    ADD COLUMN IF NOT EXISTS credit_amount INT NULL;

CREATE UNIQUE INDEX uk_trade_order_id ON trade_order (order_id);
CREATE UNIQUE INDEX uk_trade_identifier ON trade_order (identifier);

-- trade_order_stream extensions for recharge audit snapshots.
ALTER TABLE trade_order_stream
    ADD COLUMN IF NOT EXISTS biz_scene VARCHAR(64) NULL,
    ADD COLUMN IF NOT EXISTS credit_amount INT NULL;

-- user_assets_stream extensions for audit and idempotency.
ALTER TABLE user_assets_stream
    ADD COLUMN IF NOT EXISTS transaction_id VARCHAR(64) NULL,
    ADD COLUMN IF NOT EXISTS pay_order_id VARCHAR(64) NULL,
    ADD COLUMN IF NOT EXISTS package_code VARCHAR(64) NULL,
    ADD COLUMN IF NOT EXISTS channel VARCHAR(32) NULL,
    ADD COLUMN IF NOT EXISTS data_hash VARCHAR(64) NULL;

ALTER TABLE user_assets_stream
    MODIFY COLUMN data_hash VARCHAR(64) NOT NULL DEFAULT '';

CREATE UNIQUE INDEX uk_assets_stream_biz ON user_assets_stream (biz_type, biz_no, user_id);
