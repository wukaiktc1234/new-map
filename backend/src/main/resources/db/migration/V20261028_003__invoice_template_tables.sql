-- =====================================================================
-- V20261028_003__invoice_template_tables.sql
-- 批 2 部署缺口补建（6 张，均有生产库结构可逐列对照）
-- 逐列对齐生产库 `\d <table>`；只补结构不造数据；IF NOT EXISTS 对已有环境无副作用。
-- =====================================================================

-- 1) invoice_reimbursement（报销主表；reimbursement_id 无 identity，与生产一致）
CREATE TABLE IF NOT EXISTS invoice_reimbursement (
    reimbursement_id   bigint      NOT NULL,
    tenant_id          bigint,
    reimbursement_no   varchar(50) NOT NULL,
    applicant_id       bigint,
    applicant_name     varchar(100),
    department_id      bigint,
    department_name    varchar(100),
    reimbursement_type varchar(50),
    total_amount       bigint,
    approved_amount    bigint,
    status             integer DEFAULT 0,
    apply_date         date,
    approve_date       date,
    approver_id        bigint,
    approver_name      varchar(100),
    reject_reason      varchar(500),
    remark             varchar(500),
    payment_status     integer DEFAULT 0,
    payment_date       date,
    payment_voucher_no varchar(100),
    created_by         bigint,
    create_time        timestamp without time zone DEFAULT CURRENT_TIMESTAMP,
    updated_by         bigint,
    update_time        timestamp without time zone DEFAULT CURRENT_TIMESTAMP,
    deleted            integer DEFAULT 0,
    CONSTRAINT invoice_reimbursement_pkey PRIMARY KEY (reimbursement_id),
    CONSTRAINT invoice_reimbursement_reimbursement_no_key UNIQUE (reimbursement_no)
);
CREATE INDEX IF NOT EXISTS idx_inv_reimb_applicant ON invoice_reimbursement (applicant_id);
CREATE INDEX IF NOT EXISTS idx_inv_reimb_apply_date ON invoice_reimbursement (apply_date);
CREATE INDEX IF NOT EXISTS idx_inv_reimb_no ON invoice_reimbursement (reimbursement_no);
CREATE INDEX IF NOT EXISTS idx_inv_reimb_status ON invoice_reimbursement (status);

-- 2) invoice_reimbursement_item（报销明细）
CREATE TABLE IF NOT EXISTS invoice_reimbursement_item (
    item_id             bigint NOT NULL,
    tenant_id           bigint,
    reimbursement_id    bigint NOT NULL,
    related_invoice_id  bigint,
    voucher_id          bigint,
    voucher_no          varchar(100),
    voucher_type        varchar(50),
    amount              bigint,
    approved_amount     bigint,
    expense_type        varchar(50),
    expense_description varchar(500),
    created_by          bigint,
    create_time         timestamp without time zone DEFAULT CURRENT_TIMESTAMP,
    updated_by          bigint,
    update_time         timestamp without time zone DEFAULT CURRENT_TIMESTAMP,
    deleted             integer DEFAULT 0,
    CONSTRAINT invoice_reimbursement_item_pkey PRIMARY KEY (item_id)
);
CREATE INDEX IF NOT EXISTS idx_inv_reimb_item_invoice ON invoice_reimbursement_item (related_invoice_id);
CREATE INDEX IF NOT EXISTS idx_inv_reimb_item_reimb ON invoice_reimbursement_item (reimbursement_id);
CREATE INDEX IF NOT EXISTS idx_inv_reimb_item_voucher ON invoice_reimbursement_item (voucher_id);

-- 3) receipt_print_log（小票打印日志；生产用 serial 序列，这里保持同形态）
CREATE SEQUENCE IF NOT EXISTS receipt_print_log_print_id_seq;
CREATE TABLE IF NOT EXISTS receipt_print_log (
    print_id         bigint NOT NULL DEFAULT nextval('receipt_print_log_print_id_seq'),
    confirmation_id  bigint NOT NULL,
    operator_user_id bigint,
    operator_name    varchar(64),
    print_time       timestamp without time zone NOT NULL DEFAULT now(),
    source_ip        varchar(45),
    CONSTRAINT receipt_print_log_pkey PRIMARY KEY (print_id)
);
ALTER SEQUENCE receipt_print_log_print_id_seq OWNED BY receipt_print_log.print_id;
CREATE INDEX IF NOT EXISTS idx_print_log_conf ON receipt_print_log (confirmation_id, print_time);

-- 4) tax_rate_configs（税率配置）
CREATE TABLE IF NOT EXISTS tax_rate_configs (
    config_id      bigint GENERATED ALWAYS AS IDENTITY,
    tax_type       integer      NOT NULL,
    taxpayer_type  integer      NOT NULL,
    tax_rate       numeric(8,4) NOT NULL,
    policy_version varchar(50),
    effective_date date         NOT NULL,
    expiry_date    date,
    is_active      boolean DEFAULT true,
    remark         text,
    create_time    timestamp without time zone NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time    timestamp without time zone NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted        integer NOT NULL DEFAULT 0,
    created_by     varchar(100),
    updated_by     varchar(100),
    version        integer DEFAULT 0,
    CONSTRAINT tax_rate_configs_pkey PRIMARY KEY (config_id)
);
CREATE INDEX IF NOT EXISTS idx_tax_rate_configs_active ON tax_rate_configs (is_active) WHERE deleted = 0;
CREATE INDEX IF NOT EXISTS idx_tax_rate_configs_type ON tax_rate_configs (tax_type) WHERE deleted = 0;

-- 5) standard_cost_cards（标准成本卡）
CREATE TABLE IF NOT EXISTS standard_cost_cards (
    card_id          bigint GENERATED ALWAYS AS IDENTITY,
    dish_id          bigint       NOT NULL,
    dish_name        varchar(100) NOT NULL,
    standard_cost    bigint       NOT NULL,
    loss_coefficient numeric(5,2) DEFAULT 1.00,
    warning_status   integer DEFAULT 0,
    last_update_date date,
    remark           text,
    create_time      timestamp without time zone NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time      timestamp without time zone NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted          integer NOT NULL DEFAULT 0,
    created_by       varchar(100),
    updated_by       varchar(100),
    version          integer DEFAULT 0,
    CONSTRAINT standard_cost_cards_pkey PRIMARY KEY (card_id)
);
CREATE INDEX IF NOT EXISTS idx_standard_cost_cards_dish ON standard_cost_cards (dish_id) WHERE deleted = 0;
CREATE INDEX IF NOT EXISTS idx_standard_cost_cards_status ON standard_cost_cards (warning_status) WHERE deleted = 0;

-- 6) summary_templates（摘要模板）
CREATE TABLE IF NOT EXISTS summary_templates (
    template_id     bigint GENERATED ALWAYS AS IDENTITY,
    summary_content varchar(200) NOT NULL,
    category        varchar(50),
    usage_count     integer DEFAULT 0,
    status          integer DEFAULT 1,
    create_user_id  bigint,
    create_time     timestamp without time zone NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time     timestamp without time zone NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted         integer NOT NULL DEFAULT 0,
    created_by      varchar(100),
    updated_by      varchar(100),
    version         integer DEFAULT 0,
    CONSTRAINT summary_templates_pkey PRIMARY KEY (template_id)
);
CREATE INDEX IF NOT EXISTS idx_summary_templates_category ON summary_templates (category) WHERE deleted = 0;
CREATE INDEX IF NOT EXISTS idx_summary_templates_content ON summary_templates (summary_content) WHERE deleted = 0;

COMMENT ON TABLE invoice_reimbursement IS '报销主表；V20261028_003 补建的部署缺口表';
COMMENT ON TABLE invoice_reimbursement_item IS '报销明细；V20261028_003 补建的部署缺口表';
COMMENT ON TABLE receipt_print_log IS '小票打印日志；V20261028_003 补建的部署缺口表';
COMMENT ON TABLE tax_rate_configs IS '税率配置；V20261028_003 补建的部署缺口表';
COMMENT ON TABLE standard_cost_cards IS '标准成本卡；V20261028_003 补建的部署缺口表';
COMMENT ON TABLE summary_templates IS '摘要模板；V20261028_003 补建的部署缺口表';
