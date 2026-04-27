-- 复用 user_assets / user_assets_stream 作为积分账户与流水主表。
-- 下面脚本为 billing 模块补齐 TCC 和补偿所需字段与索引。

ALTER TABLE user_assets_stream
    ADD COLUMN IF NOT EXISTS request_no VARCHAR(64) NULL,
    ADD COLUMN IF NOT EXISTS source_system VARCHAR(64) NULL,
    ADD COLUMN IF NOT EXISTS change_type VARCHAR(64) NULL,
    ADD COLUMN IF NOT EXISTS frozen_before DECIMAL(18,2) NULL,
    ADD COLUMN IF NOT EXISTS frozen_after DECIMAL(18,2) NULL,
    ADD COLUMN IF NOT EXISTS tcc_transaction_no VARCHAR(64) NULL;

CREATE UNIQUE INDEX uk_assets_stream_request_no ON user_assets_stream (request_no);

CREATE TABLE IF NOT EXISTS billing_tcc_record
(
    id                 BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    tcc_transaction_no VARCHAR(64) NOT NULL,
    user_id            VARCHAR(64) NOT NULL,
    biz_no             VARCHAR(64) NOT NULL,
    request_no         VARCHAR(64) NOT NULL,
    amount             DECIMAL(18,2) NOT NULL,
    status             VARCHAR(32) NOT NULL,
    expire_time        DATETIME NULL,
    confirm_request_no VARCHAR(64) NULL,
    cancel_request_no  VARCHAR(64) NULL,
    source_system      VARCHAR(64) NULL,
    remark             VARCHAR(255) NULL,
    retry_count        INT NOT NULL DEFAULT 0,
    last_error_msg     VARCHAR(255) NULL,
    deleted            INT NOT NULL DEFAULT 0,
    lock_version       INT NOT NULL DEFAULT 0,
    create_time        DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time        DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
);

CREATE UNIQUE INDEX uk_billing_tcc_tx_no ON billing_tcc_record (tcc_transaction_no);
CREATE UNIQUE INDEX uk_billing_tcc_biz ON billing_tcc_record (user_id, biz_no);
