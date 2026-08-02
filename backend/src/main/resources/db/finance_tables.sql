-- ============================================================
-- 财务中心模块 — 数据库表初始化脚本
-- 适用环境: H2(PostgreSQL兼容模式) / PostgreSQL
-- 使用 CREATE TABLE IF NOT EXISTS 确保幂等执行
-- ============================================================

-- ===============================
-- 1. 会计科目表 (accounting_subjects)
-- ===============================
CREATE TABLE IF NOT EXISTS accounting_subjects (
    subject_id          BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    subject_code        VARCHAR(20) NOT NULL UNIQUE,
    subject_name        VARCHAR(100) NOT NULL,
    parent_id           BIGINT,
    subject_type        INTEGER NOT NULL,
    direction           INTEGER NOT NULL,
    is_leaf             BOOLEAN DEFAULT TRUE,
    status              INTEGER DEFAULT 1,
    remark              TEXT,
    balance             BIGINT NOT NULL DEFAULT 0,
    create_time         TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time         TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted             INTEGER NOT NULL DEFAULT 0,
    version             INTEGER NOT NULL DEFAULT 0
);
CREATE INDEX IF NOT EXISTS idx_accounting_subjects_parent_id ON accounting_subjects(parent_id) WHERE deleted = 0;
CREATE INDEX IF NOT EXISTS idx_accounting_subjects_type ON accounting_subjects(subject_type) WHERE deleted = 0;
CREATE INDEX IF NOT EXISTS idx_accounting_subjects_code ON accounting_subjects(subject_code) WHERE deleted = 0;

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
    create_time         TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time         TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted             INTEGER NOT NULL DEFAULT 0
);
CREATE INDEX IF NOT EXISTS idx_finance_vouchers_date ON finance_vouchers(voucher_date) WHERE deleted = 0;
CREATE INDEX IF NOT EXISTS idx_finance_vouchers_status ON finance_vouchers(voucher_status) WHERE deleted = 0;
CREATE INDEX IF NOT EXISTS idx_finance_vouchers_type ON finance_vouchers(voucher_type) WHERE deleted = 0;
CREATE INDEX IF NOT EXISTS idx_finance_vouchers_no ON finance_vouchers(voucher_no) WHERE deleted = 0;

-- ===============================
-- 3. 凭证明细/分录表 (finance_voucher_details)
-- ===============================
CREATE TABLE IF NOT EXISTS finance_voucher_details (
    detail_id           BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    voucher_id          BIGINT NOT NULL,
    summary             VARCHAR(200),
    subject_id          BIGINT NOT NULL,
    debit_amount        BIGINT DEFAULT 0,
    credit_amount       BIGINT DEFAULT 0,
    auxiliary_item      TEXT,
    sort_order          INTEGER DEFAULT 0,
    create_time         TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time         TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted             INTEGER NOT NULL DEFAULT 0
);
CREATE INDEX IF NOT EXISTS idx_voucher_detail_voucher_id ON finance_voucher_details(voucher_id) WHERE deleted = 0;
CREATE INDEX IF NOT EXISTS idx_voucher_detail_subject_id ON finance_voucher_details(subject_id) WHERE deleted = 0;

-- ===============================
-- 4. 收支记录表 (finance_records)
-- ===============================
CREATE TABLE IF NOT EXISTS finance_records (
    record_id           BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    record_no           VARCHAR(32) NOT NULL UNIQUE,
    record_type         INTEGER NOT NULL,
    record_category     INTEGER NOT NULL,
    amount              BIGINT NOT NULL,
    payment_method      INTEGER,
    account_subject_id  BIGINT,
    counterparty_name   VARCHAR(100),
    counterparty_type   INTEGER,
    business_date       DATE,
    record_date         DATE NOT NULL,
    voucher_id          BIGINT,
    invoice_id          BIGINT,
    approval_status     INTEGER DEFAULT 0,
    approve_user_id     BIGINT,
    approve_time        TIMESTAMP,
    remark              TEXT,
    create_user_id      BIGINT,
    create_time         TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time         TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted             INTEGER NOT NULL DEFAULT 0
);
CREATE INDEX IF NOT EXISTS idx_finance_records_date ON finance_records(record_date) WHERE deleted = 0;
CREATE INDEX IF NOT EXISTS idx_finance_records_type ON finance_records(record_type) WHERE deleted = 0;
CREATE INDEX IF NOT EXISTS idx_finance_records_approval ON finance_records(approval_status) WHERE deleted = 0;

-- ===============================
-- 5. 发票管理表 (finance_invoices)
-- ===============================
CREATE TABLE IF NOT EXISTS finance_invoices (
    invoice_id          BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    invoice_code        VARCHAR(20),
    invoice_no          VARCHAR(30) NOT NULL,
    invoice_type        INTEGER NOT NULL,
    invoice_category    INTEGER NOT NULL,
    buyer_name          VARCHAR(200),
    buyer_tax_no        VARCHAR(50),
    seller_name         VARCHAR(200),
    seller_tax_no       VARCHAR(50),
    total_amount        BIGINT DEFAULT 0,
    tax_amount          BIGINT DEFAULT 0,
    total_amount_with_tax BIGINT DEFAULT 0,
    invoice_date        DATE,
    receive_date        DATE,
    invoice_status      INTEGER DEFAULT 0,
    verification_result TEXT,
    image_url           VARCHAR(500),
    ocr_result          TEXT,
    related_record_id   BIGINT,
    create_time         TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time         TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted             INTEGER NOT NULL DEFAULT 0
);
CREATE INDEX IF NOT EXISTS idx_finance_invoices_no ON finance_invoices(invoice_no) WHERE deleted = 0;
CREATE INDEX IF NOT EXISTS idx_finance_invoices_status ON finance_invoices(invoice_status) WHERE deleted = 0;
CREATE INDEX IF NOT EXISTS idx_finance_invoices_category ON finance_invoices(invoice_category) WHERE deleted = 0;

-- ===============================
-- 6. 成本记录表 (cost_records)
-- ===============================
CREATE TABLE IF NOT EXISTS cost_records (
    cost_id             BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    cost_no             VARCHAR(32) NOT NULL UNIQUE,
    cost_type           INTEGER NOT NULL,
    period              VARCHAR(10) NOT NULL,
    cost_center_id      BIGINT,
    amount              BIGINT NOT NULL,
    quantity            DECIMAL(14,4),
    unit_price          DECIMAL(14,4),
    calculation_method  INTEGER DEFAULT 1,
    related_voucher_id  BIGINT,
    remark              TEXT,
    create_time         TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time         TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted             INTEGER NOT NULL DEFAULT 0
);
CREATE INDEX IF NOT EXISTS idx_cost_records_period ON cost_records(period) WHERE deleted = 0;
CREATE INDEX IF NOT EXISTS idx_cost_records_type ON cost_records(cost_type) WHERE deleted = 0;

-- ===============================
-- 7. 成本分摊规则表 (cost_allocation_rules)
-- ===============================
CREATE TABLE IF NOT EXISTS cost_allocation_rules (
    rule_id             BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    rule_name           VARCHAR(100) NOT NULL,
    cost_type           INTEGER NOT NULL,
    allocation_basis    INTEGER NOT NULL,
    allocation_formula  TEXT,
    is_enabled          BOOLEAN DEFAULT TRUE,
    remark              TEXT,
    create_time         TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time         TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted             INTEGER NOT NULL DEFAULT 0
);

-- ===============================
-- 8. 利润表数据 (profit_statements)
-- ===============================
CREATE TABLE IF NOT EXISTS profit_statements (
    statement_id        BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    statement_type      INTEGER NOT NULL,
    period              VARCHAR(10) NOT NULL,
    revenue_amount      BIGINT DEFAULT 0,
    cogs_amount         BIGINT DEFAULT 0,
    gross_profit        BIGINT DEFAULT 0,
    operating_expenses  BIGINT DEFAULT 0,
    net_profit          BIGINT DEFAULT 0,
    generate_time       TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    create_time         TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time         TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted             INTEGER NOT NULL DEFAULT 0
);
CREATE INDEX IF NOT EXISTS idx_profit_statements_period ON profit_statements(period) WHERE deleted = 0;

-- ===============================
-- 9. 应收账款表 (receivables)
-- ===============================
CREATE TABLE IF NOT EXISTS receivables (
    receivable_id       BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    receivable_no       VARCHAR(32) NOT NULL UNIQUE,
    customer_type       INTEGER NOT NULL,
    customer_id         BIGINT,
    customer_name       VARCHAR(100) NOT NULL,
    order_id            BIGINT,
    original_amount     BIGINT NOT NULL,
    received_amount     BIGINT DEFAULT 0,
    balance_amount      BIGINT NOT NULL,
    overdue_days        INTEGER DEFAULT 0,
    status              INTEGER DEFAULT 1,
    due_date            DATE,
    last_payment_date   DATE,
    create_time         TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time         TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted             INTEGER NOT NULL DEFAULT 0
);
CREATE INDEX IF NOT EXISTS idx_receivables_customer ON receivables(customer_id) WHERE deleted = 0;
CREATE INDEX IF NOT EXISTS idx_receivables_status ON receivables(status) WHERE deleted = 0;
CREATE INDEX IF NOT EXISTS idx_receivables_due_date ON receivables(due_date) WHERE deleted = 0;

-- ===============================
-- 10. 应付账款表 (payables)
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

-- ===============================
-- 11. 预算表 (budgets)
-- ===============================
CREATE TABLE IF NOT EXISTS budgets (
    budget_id           BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    budget_year         INTEGER NOT NULL,
    budget_month        INTEGER,
    budget_type         INTEGER NOT NULL,
    category_id         INTEGER,
    budget_amount       BIGINT NOT NULL,
    actual_amount       BIGINT DEFAULT 0,
    variance            BIGINT DEFAULT 0,
    variance_rate       DECIMAL(8,4),
    responsible_dept_id BIGINT,
    remark              TEXT,
    create_time         TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time         TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted             INTEGER NOT NULL DEFAULT 0
);
CREATE INDEX IF NOT EXISTS idx_budgets_year ON budgets(budget_year) WHERE deleted = 0;
CREATE INDEX IF NOT EXISTS idx_budgets_type ON budgets(budget_type) WHERE deleted = 0;

-- ============================================================
-- 以下为新增表（plan.md要求，原系统不存在）
-- ============================================================

-- ===============================
-- 12. 会计期间表 (accounting_periods)
-- ===============================
CREATE TABLE IF NOT EXISTS accounting_periods (
    period_id           BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    period_code         VARCHAR(20) NOT NULL UNIQUE,
    period_name         VARCHAR(50) NOT NULL,
    period_type         INTEGER NOT NULL,
    start_date          DATE NOT NULL,
    end_date            DATE NOT NULL,
    status              INTEGER DEFAULT 0,
    is_closed           BOOLEAN DEFAULT FALSE,
    close_time          TIMESTAMP,
    close_user_id       BIGINT,
    trial_balance_passed BOOLEAN DEFAULT FALSE,
    remark              TEXT,
    create_time         TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time         TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted             INTEGER NOT NULL DEFAULT 0
);
CREATE INDEX IF NOT EXISTS idx_accounting_periods_code ON accounting_periods(period_code) WHERE deleted = 0;
CREATE INDEX IF NOT EXISTS idx_accounting_periods_status ON accounting_periods(status) WHERE deleted = 0;
CREATE INDEX IF NOT EXISTS idx_accounting_periods_dates ON accounting_periods(start_date, end_date) WHERE deleted = 0;

-- ===============================
-- 13. 摘要模板表 (summary_templates)
-- ===============================
CREATE TABLE IF NOT EXISTS summary_templates (
    template_id         BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    summary_content     VARCHAR(200) NOT NULL,
    category            VARCHAR(50),
    usage_count         INTEGER DEFAULT 0,
    status              INTEGER DEFAULT 1,
    create_user_id      BIGINT,
    create_time         TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time         TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted             INTEGER NOT NULL DEFAULT 0
);
CREATE INDEX IF NOT EXISTS idx_summary_templates_category ON summary_templates(category) WHERE deleted = 0;
CREATE INDEX IF NOT EXISTS idx_summary_templates_content ON summary_templates(summary_content) WHERE deleted = 0;

-- ===============================
-- 14. 转账模板表 (transfer_templates)
-- ===============================
CREATE TABLE IF NOT EXISTS transfer_templates (
    template_id         BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    template_name       VARCHAR(100) NOT NULL,
    template_type       INTEGER NOT NULL,
    source_subject_id   BIGINT NOT NULL,
    target_subject_id   BIGINT NOT NULL,
    amount_expression   VARCHAR(200),
    summary_template    VARCHAR(200),
    is_enabled          BOOLEAN DEFAULT TRUE,
    remark              TEXT,
    create_user_id      BIGINT,
    create_time         TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time         TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted             INTEGER NOT NULL DEFAULT 0
);
CREATE INDEX IF NOT EXISTS idx_transfer_templates_type ON transfer_templates(template_type) WHERE deleted = 0;
CREATE INDEX IF NOT EXISTS idx_transfer_templates_source ON transfer_templates(source_subject_id) WHERE deleted = 0;

-- ===============================
-- 15. 税率配置表 (tax_rate_configs)
-- ===============================
CREATE TABLE IF NOT EXISTS tax_rate_configs (
    config_id           BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    tax_type            INTEGER NOT NULL,
    taxpayer_type       INTEGER NOT NULL,
    tax_rate            DECIMAL(8,4) NOT NULL,
    policy_version      VARCHAR(50),
    effective_date      DATE NOT NULL,
    expiry_date         DATE,
    is_active           BOOLEAN DEFAULT TRUE,
    remark              TEXT,
    create_time         TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time         TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted             INTEGER NOT NULL DEFAULT 0
);
CREATE INDEX IF NOT EXISTS idx_tax_rate_configs_type ON tax_rate_configs(tax_type) WHERE deleted = 0;
CREATE INDEX IF NOT EXISTS idx_tax_rate_configs_active ON tax_rate_configs(is_active) WHERE deleted = 0;

-- ===============================
-- 16. 标准成本卡表 (standard_cost_cards)
-- ===============================
CREATE TABLE IF NOT EXISTS standard_cost_cards (
    card_id             BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    dish_id             BIGINT NOT NULL,
    dish_name           VARCHAR(100) NOT NULL,
    standard_cost       BIGINT NOT NULL,
    loss_coefficient    DECIMAL(5,2) DEFAULT 1.00,
    warning_status      INTEGER DEFAULT 0,
    last_update_date    DATE,
    remark              TEXT,
    create_time         TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time         TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted             INTEGER NOT NULL DEFAULT 0
);
CREATE INDEX IF NOT EXISTS idx_standard_cost_cards_dish ON standard_cost_cards(dish_id) WHERE deleted = 0;
CREATE INDEX IF NOT EXISTS idx_standard_cost_cards_status ON standard_cost_cards(warning_status) WHERE deleted = 0;

-- ===============================
-- 17. 银行账户表 (bank_accounts)
-- ===============================
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
    create_time         TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time         TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted             INTEGER NOT NULL DEFAULT 0
);
CREATE INDEX IF NOT EXISTS idx_bank_accounts_type ON bank_accounts(account_type) WHERE deleted = 0;
CREATE INDEX IF NOT EXISTS idx_bank_accounts_status ON bank_accounts(status) WHERE deleted = 0;

-- ===============================
-- 18. 资金流水表 (fund_flows)
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
    create_time         TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time         TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted             INTEGER NOT NULL DEFAULT 0
);
CREATE INDEX IF NOT EXISTS idx_fund_flows_account ON fund_flows(account_id) WHERE deleted = 0;
CREATE INDEX IF NOT EXISTS idx_fund_flows_direction ON fund_flows(flow_direction) WHERE deleted = 0;
CREATE INDEX IF NOT EXISTS idx_fund_flows_date ON fund_flows(business_date) WHERE deleted = 0;
CREATE INDEX IF NOT EXISTS idx_fund_flows_category ON fund_flows(flow_category) WHERE deleted = 0;

-- ===============================
-- 19. 审批流配置表 (approval_flow_configs)
-- ===============================
CREATE TABLE IF NOT EXISTS approval_flow_configs (
    config_id           BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    config_name         VARCHAR(100) NOT NULL,
    document_type       VARCHAR(50) NOT NULL,
    approval_nodes      TEXT,
    is_enabled          BOOLEAN DEFAULT TRUE,
    remark              TEXT,
    create_time         TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time         TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted             INTEGER NOT NULL DEFAULT 0
);
CREATE INDEX IF NOT EXISTS idx_approval_flow_configs_type ON approval_flow_configs(document_type) WHERE deleted = 0;

-- ===============================
-- 20. 财务审计日志表 (finance_audit_logs)
-- 仅允许INSERT，不逻辑删除
-- ===============================
CREATE TABLE IF NOT EXISTS finance_audit_logs (
    log_id              BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    operator_id         BIGINT,
    operator_name       VARCHAR(50),
    operation_type      VARCHAR(50) NOT NULL,
    module              VARCHAR(50),
    target_id           BIGINT,
    target_type         VARCHAR(50),
    operation_desc      VARCHAR(500),
    operation_data      TEXT,
    ip_address          VARCHAR(50),
    create_time         TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);
CREATE INDEX IF NOT EXISTS idx_finance_audit_logs_operator ON finance_audit_logs(operator_id);
CREATE INDEX IF NOT EXISTS idx_finance_audit_logs_type ON finance_audit_logs(operation_type);
CREATE INDEX IF NOT EXISTS idx_finance_audit_logs_time ON finance_audit_logs(create_time);
CREATE INDEX IF NOT EXISTS idx_finance_audit_logs_module ON finance_audit_logs(module);
