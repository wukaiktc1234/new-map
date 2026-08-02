-- ============================================================
-- 迁移脚本：V20260718_001__ensure_finance_tables_for_migrations.sql
-- 说明：在 V20260719_002/003/005/006 等迁移执行前，确保财务模块核心表已存在。
--       这些表此前由 FinanceDatabaseInitializer 在 Flyway 之后创建，导致空库首次
--       启动时后续迁移引用 payables / finance_vouchers / fund_flows 报 "表不存在"。
--       本脚本使用 CREATE TABLE IF NOT EXISTS 幂等创建，兼容已有数据库。
-- 目标数据库：PostgreSQL 18 / H2 (MODE=PostgreSQL)
-- ============================================================

-- ===============================
-- 1. 应付账款表 (payables)
-- ===============================
CREATE TABLE IF NOT EXISTS payables (
    payable_id          BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    payable_no          VARCHAR(32) NOT NULL UNIQUE,
    supplier_id         BIGINT,
    supplier_name       VARCHAR(100) NOT NULL,
    purchase_order_id   BIGINT,
    stockin_id          BIGINT,
    stockin_no          VARCHAR(32),
    order_no            VARCHAR(100),
    original_amount     BIGINT NOT NULL,
    paid_amount         BIGINT DEFAULT 0,
    balance_amount      BIGINT NOT NULL,
    due_date            DATE,
    payment_term        INTEGER DEFAULT 30,
    status              INTEGER DEFAULT 1,
    related_invoice_id  BIGINT,
    invoice_type        VARCHAR(50),
    created_by          VARCHAR(50),
    updated_by          VARCHAR(50),
    version             INTEGER DEFAULT 0,
    create_time         TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time         TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted             INTEGER NOT NULL DEFAULT 0
);

CREATE INDEX IF NOT EXISTS idx_payables_supplier ON payables(supplier_id) WHERE deleted = 0;
CREATE INDEX IF NOT EXISTS idx_payables_status ON payables(status) WHERE deleted = 0;
CREATE INDEX IF NOT EXISTS idx_payables_due_date ON payables(due_date) WHERE deleted = 0;
CREATE INDEX IF NOT EXISTS idx_payables_stockin ON payables(stockin_id) WHERE deleted = 0;
CREATE INDEX IF NOT EXISTS idx_payables_order_no ON payables(order_no) WHERE deleted = 0;

COMMENT ON TABLE payables IS '应付账款表';
COMMENT ON COLUMN payables.payable_no IS '应付编号';
COMMENT ON COLUMN payables.purchase_order_id IS '关联采购订单ID';
COMMENT ON COLUMN payables.stockin_id IS '关联采购入库单ID';
COMMENT ON COLUMN payables.order_no IS '关联采购订单号';
COMMENT ON COLUMN payables.deleted IS '逻辑删除: 0-未删除, 1-已删除';

-- ===============================
-- 2. 记账凭证表 (finance_vouchers)
-- ===============================
CREATE TABLE IF NOT EXISTS finance_vouchers (
    voucher_id          BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    voucher_no          VARCHAR(32) NOT NULL UNIQUE,
    voucher_date        DATE NOT NULL,
    voucher_type        INTEGER DEFAULT 1,
    voucher_status      INTEGER DEFAULT 0,
    total_debit         BIGINT NOT NULL DEFAULT 0,
    total_credit        BIGINT NOT NULL DEFAULT 0,
    attachment_count    INTEGER DEFAULT 0,
    reference_no        VARCHAR(100),
    source_type         INTEGER,
    source_id           BIGINT,
    remark              TEXT,
    create_user_id      BIGINT,
    approve_user_id     BIGINT,
    approve_time        TIMESTAMP,
    created_by          VARCHAR(50),
    updated_by          VARCHAR(50),
    version             INTEGER DEFAULT 0,
    create_time         TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time         TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted             INTEGER NOT NULL DEFAULT 0
);

CREATE INDEX IF NOT EXISTS idx_finance_vouchers_date ON finance_vouchers(voucher_date) WHERE deleted = 0;
CREATE INDEX IF NOT EXISTS idx_finance_vouchers_status ON finance_vouchers(voucher_status) WHERE deleted = 0;
CREATE INDEX IF NOT EXISTS idx_finance_vouchers_type ON finance_vouchers(voucher_type) WHERE deleted = 0;
CREATE INDEX IF NOT EXISTS idx_finance_vouchers_no ON finance_vouchers(voucher_no) WHERE deleted = 0;

COMMENT ON TABLE finance_vouchers IS '记账凭证表';
COMMENT ON COLUMN finance_vouchers.voucher_no IS '凭证号';
COMMENT ON COLUMN finance_vouchers.deleted IS '逻辑删除: 0-未删除, 1-已删除';

-- ===============================
-- 3. 资金流水表 (fund_flows)
-- ===============================
CREATE TABLE IF NOT EXISTS fund_flows (
    flow_id             BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    flow_no             VARCHAR(32) NOT NULL UNIQUE,
    account_id          BIGINT NOT NULL,
    flow_direction      INTEGER NOT NULL,
    flow_category       INTEGER NOT NULL,
    amount              BIGINT NOT NULL,
    balance_after       BIGINT,
    counterparty_name   VARCHAR(100),
    counterparty_account VARCHAR(50),
    business_date       DATE NOT NULL,
    voucher_id          BIGINT,
    remark              TEXT,
    create_user_id      BIGINT,
    created_by          VARCHAR(50),
    updated_by          VARCHAR(50),
    version             INTEGER DEFAULT 0,
    create_time         TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time         TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted             INTEGER NOT NULL DEFAULT 0
);

CREATE INDEX IF NOT EXISTS idx_fund_flows_account ON fund_flows(account_id) WHERE deleted = 0;
CREATE INDEX IF NOT EXISTS idx_fund_flows_direction ON fund_flows(flow_direction) WHERE deleted = 0;
CREATE INDEX IF NOT EXISTS idx_fund_flows_date ON fund_flows(business_date) WHERE deleted = 0;
CREATE INDEX IF NOT EXISTS idx_fund_flows_category ON fund_flows(flow_category) WHERE deleted = 0;

COMMENT ON TABLE fund_flows IS '资金流水表';
COMMENT ON COLUMN fund_flows.flow_no IS '流水号';
COMMENT ON COLUMN fund_flows.deleted IS '逻辑删除: 0-未删除, 1-已删除';
