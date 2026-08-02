-- ============================================================
-- 付款单表 + 银行账户乐观锁
-- 创建日期：2026-06-25
-- 说明：应付账款付款功能重构，支持四账联动（应付+银行+流水+凭证）
-- ============================================================

-- 1. 付款单表
CREATE TABLE IF NOT EXISTS payment (
    payment_id        BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    payment_no        VARCHAR(32) NOT NULL,
    payable_id        BIGINT NOT NULL,
    payable_no        VARCHAR(32) NOT NULL,
    supplier_id       BIGINT,
    supplier_name     VARCHAR(100) NOT NULL,
    payment_amount    BIGINT NOT NULL,
    payment_method    VARCHAR(20) NOT NULL DEFAULT 'bank_transfer',
    bank_account_id   BIGINT NOT NULL,
    bank_account_name VARCHAR(100),
    payment_date      DATE NOT NULL,
    fund_flow_id      BIGINT,
    voucher_id        BIGINT,
    status            INTEGER NOT NULL DEFAULT 1,
    remark            VARCHAR(500),
    create_user_id    BIGINT,
    create_user_name  VARCHAR(50),
    create_time       TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time       TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted           INTEGER NOT NULL DEFAULT 0
);

-- 唯一索引：付款单号
CREATE UNIQUE INDEX IF NOT EXISTS uk_payment_payment_no ON payment(payment_no);

-- 普通索引：应付单ID、供应商、付款日期、状态
CREATE INDEX IF NOT EXISTS idx_payment_payable_id ON payment(payable_id);
CREATE INDEX IF NOT EXISTS idx_payment_supplier_id ON payment(supplier_id);
CREATE INDEX IF NOT EXISTS idx_payment_payment_date ON payment(payment_date);
CREATE INDEX IF NOT EXISTS idx_payment_status ON payment(status);

-- 2. 银行账户表（首次迁移时表不存在则创建）
-- 修复说明（2026-07-23）：与 BankAccount 实体对齐，使用 account_id / account_number / account_type 等字段
CREATE TABLE IF NOT EXISTS bank_accounts (
    account_id          BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    account_name        VARCHAR(100) NOT NULL,
    bank_name           VARCHAR(100),
    account_number      VARCHAR(50) NOT NULL,
    account_number_masked VARCHAR(20),
    account_type        INTEGER NOT NULL,
    balance             BIGINT DEFAULT 0,
    currency            VARCHAR(10) DEFAULT 'CNY',
    status              INTEGER DEFAULT 1,
    open_date           DATE,
    remark              TEXT,
    created_by          VARCHAR(100),
    updated_by          VARCHAR(100),
    version             INTEGER DEFAULT 0,
    create_time         TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time         TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted             INTEGER NOT NULL DEFAULT 0
);

CREATE INDEX IF NOT EXISTS idx_bank_accounts_type ON bank_accounts(account_type) WHERE deleted = 0;
CREATE INDEX IF NOT EXISTS idx_bank_accounts_status ON bank_accounts(status) WHERE deleted = 0;

COMMENT ON TABLE bank_accounts IS '银行账户表';
COMMENT ON COLUMN bank_accounts.account_id IS '账户ID';
COMMENT ON COLUMN bank_accounts.account_name IS '账户名称';
COMMENT ON COLUMN bank_accounts.bank_name IS '开户银行';
COMMENT ON COLUMN bank_accounts.account_number IS '账号';
COMMENT ON COLUMN bank_accounts.account_type IS '账户类型：1-基本户 2-一般户 3-备用金 4-其他';
COMMENT ON COLUMN bank_accounts.status IS '状态：1=启用 0=停用';
COMMENT ON COLUMN bank_accounts.version IS '乐观锁版本号';
