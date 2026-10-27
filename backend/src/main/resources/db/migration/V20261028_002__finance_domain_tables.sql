-- =====================================================================
-- V20261028_002__finance_domain_tables.sql
-- 批 1（财务域）部署缺口补建：从零建的库缺这 6 张表，功能上线即炸。
-- 每张表逐列对齐生产库 `\d <table>`（列数/类型/默认值/NOT NULL/索引/CHECK 全对齐）；
-- 只补结构不造数据。IF NOT EXISTS 对已有该表的环境（生产/开发）无副作用。
-- =====================================================================

-- 1) finance_audit_logs（财务审计日志）
CREATE TABLE IF NOT EXISTS finance_audit_logs (
    log_id         bigint       GENERATED ALWAYS AS IDENTITY,
    operator_id    bigint,
    operator_name  varchar(50),
    operation_type varchar(50)  NOT NULL,
    module         varchar(50),
    target_id      bigint,
    target_type    varchar(50),
    operation_desc varchar(500),
    operation_data text,
    ip_address     varchar(50),
    create_time    timestamp without time zone NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by     varchar(100),
    updated_by     varchar(100),
    version        integer DEFAULT 0,
    CONSTRAINT finance_audit_logs_pkey PRIMARY KEY (log_id)
);
CREATE INDEX IF NOT EXISTS idx_finance_audit_logs_module ON finance_audit_logs (module);
CREATE INDEX IF NOT EXISTS idx_finance_audit_logs_operator ON finance_audit_logs (operator_id);
CREATE INDEX IF NOT EXISTS idx_finance_audit_logs_time ON finance_audit_logs (create_time);
CREATE INDEX IF NOT EXISTS idx_finance_audit_logs_type ON finance_audit_logs (operation_type);

-- 2) finance_invoices（发票主表；部分索引带 WHERE deleted = 0）
CREATE TABLE IF NOT EXISTS finance_invoices (
    invoice_id            bigint GENERATED ALWAYS AS IDENTITY,
    invoice_code          varchar(20),
    invoice_no            varchar(30)  NOT NULL,
    invoice_type          integer      NOT NULL,
    invoice_category      integer      NOT NULL,
    buyer_name            varchar(200),
    buyer_tax_no          varchar(50),
    seller_name           varchar(200),
    seller_tax_no         varchar(50),
    total_amount          bigint DEFAULT 0,
    tax_amount            bigint DEFAULT 0,
    total_amount_with_tax bigint DEFAULT 0,
    invoice_date          date,
    receive_date          date,
    invoice_status        integer DEFAULT 0,
    verification_result   text,
    image_url             varchar(500),
    ocr_result            text,
    related_record_id     bigint,
    create_time           timestamp without time zone NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time           timestamp without time zone NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted               integer      NOT NULL DEFAULT 0,
    created_by            varchar(100),
    updated_by            varchar(100),
    version               integer DEFAULT 0,
    CONSTRAINT finance_invoices_pkey PRIMARY KEY (invoice_id)
);
CREATE INDEX IF NOT EXISTS idx_finance_invoices_category ON finance_invoices (invoice_category) WHERE deleted = 0;
CREATE INDEX IF NOT EXISTS idx_finance_invoices_no ON finance_invoices (invoice_no) WHERE deleted = 0;
CREATE INDEX IF NOT EXISTS idx_finance_invoices_status ON finance_invoices (invoice_status) WHERE deleted = 0;

-- 3) finance_voucher_details（凭证明细）
CREATE TABLE IF NOT EXISTS finance_voucher_details (
    detail_id      bigint GENERATED ALWAYS AS IDENTITY,
    voucher_id     bigint       NOT NULL,
    summary        varchar(200),
    subject_id     bigint       NOT NULL,
    debit_amount   bigint DEFAULT 0,
    credit_amount  bigint DEFAULT 0,
    auxiliary_item text,
    sort_order     integer DEFAULT 0,
    create_time    timestamp without time zone NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time    timestamp without time zone NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted        integer      NOT NULL DEFAULT 0,
    created_by     varchar(100),
    updated_by     varchar(100),
    version        integer DEFAULT 0,
    CONSTRAINT finance_voucher_details_pkey PRIMARY KEY (detail_id)
);
CREATE INDEX IF NOT EXISTS idx_voucher_detail_subject_id ON finance_voucher_details (subject_id) WHERE deleted = 0;
CREATE INDEX IF NOT EXISTS idx_voucher_detail_voucher_id ON finance_voucher_details (voucher_id) WHERE deleted = 0;

-- 4) profit_statements（利润表）
CREATE TABLE IF NOT EXISTS profit_statements (
    statement_id       bigint GENERATED ALWAYS AS IDENTITY,
    statement_type     integer      NOT NULL,
    period             varchar(10)  NOT NULL,
    revenue_amount     bigint DEFAULT 0,
    cogs_amount        bigint DEFAULT 0,
    gross_profit       bigint DEFAULT 0,
    operating_expenses bigint DEFAULT 0,
    net_profit         bigint DEFAULT 0,
    generate_time      timestamp without time zone NOT NULL DEFAULT CURRENT_TIMESTAMP,
    create_time        timestamp without time zone NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time        timestamp without time zone NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted            integer      NOT NULL DEFAULT 0,
    created_by         varchar(100),
    updated_by         varchar(100),
    version            integer DEFAULT 0,
    CONSTRAINT profit_statements_pkey PRIMARY KEY (statement_id)
);
CREATE INDEX IF NOT EXISTS idx_profit_statements_period ON profit_statements (period) WHERE deleted = 0;

-- 5) receivables（应收账款）
CREATE TABLE IF NOT EXISTS receivables (
    receivable_id     bigint GENERATED ALWAYS AS IDENTITY,
    receivable_no     varchar(32)  NOT NULL,
    customer_type     integer      NOT NULL,
    customer_id       bigint,
    customer_name     varchar(100) NOT NULL,
    order_id          bigint,
    original_amount   bigint       NOT NULL,
    received_amount   bigint DEFAULT 0,
    balance_amount    bigint       NOT NULL,
    overdue_days      integer DEFAULT 0,
    status            integer DEFAULT 1,
    due_date          date,
    last_payment_date date,
    create_time       timestamp without time zone NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time       timestamp without time zone NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted           integer      NOT NULL DEFAULT 0,
    created_by        varchar(100),
    updated_by        varchar(100),
    version           integer DEFAULT 0,
    remark            varchar(500),
    CONSTRAINT receivables_pkey PRIMARY KEY (receivable_id),
    CONSTRAINT receivables_receivable_no_key UNIQUE (receivable_no)
);
CREATE INDEX IF NOT EXISTS idx_receivables_customer ON receivables (customer_id) WHERE deleted = 0;
CREATE INDEX IF NOT EXISTS idx_receivables_due_date ON receivables (due_date) WHERE deleted = 0;
CREATE INDEX IF NOT EXISTS idx_receivables_status ON receivables (status) WHERE deleted = 0;

-- 6) inventory_movement（库存流水；含 CHECK 约束）
CREATE TABLE IF NOT EXISTS inventory_movement (
    movement_id   bigint GENERATED ALWAYS AS IDENTITY,
    location_id   bigint        NOT NULL,
    material_id   bigint        NOT NULL,
    change_qty    numeric(14,4) NOT NULL,
    balance_after numeric(14,4),
    movement_type varchar(30)   NOT NULL,
    source_type   varchar(40)   NOT NULL,
    source_ref    varchar(64)   NOT NULL,
    operator_id   bigint,
    unit_cost     numeric(14,4),
    total_cost    numeric(16,4),
    remark        varchar(500),
    create_time   timestamp without time zone NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT inventory_movement_pkey PRIMARY KEY (movement_id),
    CONSTRAINT ck_movement_source CHECK (length(source_type) > 0 AND length(source_ref) > 0)
);
CREATE INDEX IF NOT EXISTS idx_mv_location_time ON inventory_movement (location_id, create_time);
CREATE INDEX IF NOT EXISTS idx_mv_source ON inventory_movement (source_type, source_ref);

COMMENT ON TABLE finance_audit_logs IS '财务审计日志；V20261028_002 补建的部署缺口表';
COMMENT ON TABLE finance_invoices IS '财务发票主表；V20261028_002 补建的部署缺口表';
COMMENT ON TABLE finance_voucher_details IS '财务凭证明细；V20261028_002 补建的部署缺口表';
COMMENT ON TABLE profit_statements IS '利润表；V20261028_002 补建的部署缺口表';
COMMENT ON TABLE receivables IS '应收账款；V20261028_002 补建的部署缺口表';
COMMENT ON TABLE inventory_movement IS '库存流水；V20261028_002 补建的部署缺口表';
