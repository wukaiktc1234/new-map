-- ============================================================
-- 收款单表
-- 创建日期：2026-06-25
-- 说明：应收账款收款功能，支持四账联动（应收+银行+流水+凭证）
-- 与付款单（payment表）对称：付款借应付/贷银行，收款借银行/贷应收
-- ============================================================

-- 收款单表
CREATE TABLE IF NOT EXISTS receipt (
    receipt_id         BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    receipt_no         VARCHAR(32) NOT NULL,
    receivable_id      BIGINT NOT NULL,
    receivable_no      VARCHAR(32) NOT NULL,
    customer_id        BIGINT,
    customer_name      VARCHAR(100) NOT NULL,
    receipt_amount     BIGINT NOT NULL,
    receipt_method     VARCHAR(20) NOT NULL DEFAULT 'bank_transfer',
    bank_account_id    BIGINT NOT NULL,
    bank_account_name  VARCHAR(100),
    receipt_date       DATE NOT NULL,
    fund_flow_id       BIGINT,
    voucher_id         BIGINT,
    status             INTEGER NOT NULL DEFAULT 1,
    remark             VARCHAR(500),
    create_user_id     BIGINT,
    create_user_name   VARCHAR(50),
    create_time        TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time        TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted            INTEGER NOT NULL DEFAULT 0
);

-- 唯一索引：收款单号
CREATE UNIQUE INDEX IF NOT EXISTS uk_receipt_receipt_no ON receipt(receipt_no);

-- 普通索引：应收单ID、客户、收款日期、状态
CREATE INDEX IF NOT EXISTS idx_receipt_receivable_id ON receipt(receivable_id);
CREATE INDEX IF NOT EXISTS idx_receipt_customer_id ON receipt(customer_id);
CREATE INDEX IF NOT EXISTS idx_receipt_receipt_date ON receipt(receipt_date);
CREATE INDEX IF NOT EXISTS idx_receipt_status ON receipt(status);
