-- =====================================================================
-- V20261028_006__batch3_structures.sql
-- 批 3 部署缺口补建（5 张，均有生产库结构可逐列对照）；只补结构不造数据。
-- =====================================================================

-- 1) app_device_registrations（APP 设备注册；生产用 serial 序列 + 两个唯一约束）
CREATE SEQUENCE IF NOT EXISTS app_device_registrations_registration_id_seq;
CREATE TABLE IF NOT EXISTS app_device_registrations (
    registration_id bigint NOT NULL DEFAULT nextval('app_device_registrations_registration_id_seq'),
    device_id       varchar(64),
    device_name     varchar(100),
    activation_code varchar(16)  NOT NULL,
    status          varchar(20)  NOT NULL DEFAULT 'PENDING',
    bound_username  varchar(64),
    code_expire_at  timestamp without time zone,
    activated_at    timestamp without time zone,
    last_seen_at    timestamp without time zone,
    create_time     timestamp without time zone DEFAULT now(),
    deleted         smallint DEFAULT 0,
    CONSTRAINT app_device_registrations_pkey PRIMARY KEY (registration_id),
    CONSTRAINT app_device_registrations_activation_code_key UNIQUE (activation_code),
    CONSTRAINT app_device_registrations_device_id_key UNIQUE (device_id)
);
ALTER SEQUENCE app_device_registrations_registration_id_seq OWNED BY app_device_registrations.registration_id;
CREATE INDEX IF NOT EXISTS idx_device_reg_status ON app_device_registrations (status);

-- 2) inventory_warning_rules（库存预警规则）
CREATE TABLE IF NOT EXISTS inventory_warning_rules (
    rule_id            bigint GENERATED ALWAYS AS IDENTITY,
    rule_name          varchar(100),
    warehouse_id       bigint,
    warning_type       integer,
    condition_field    varchar(100),
    condition_operator varchar(20),
    threshold_value    numeric(18,4),
    notify_method      integer,
    is_enabled         boolean,
    create_time        timestamp without time zone NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time        timestamp without time zone NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted            integer NOT NULL DEFAULT 0,
    CONSTRAINT inventory_warning_rules_pkey PRIMARY KEY (rule_id)
);
CREATE INDEX IF NOT EXISTS idx_inventory_warning_rules_enabled ON inventory_warning_rules (is_enabled);
CREATE INDEX IF NOT EXISTS idx_inventory_warning_rules_type ON inventory_warning_rules (warning_type);
CREATE INDEX IF NOT EXISTS idx_inventory_warning_rules_warehouse ON inventory_warning_rules (warehouse_id);

-- 3) material_template（物料模板）
CREATE TABLE IF NOT EXISTS material_template (
    id                 bigint GENERATED ALWAYS AS IDENTITY,
    template_code      varchar(50)  NOT NULL,
    material_name      varchar(200) NOT NULL,
    category           varchar(100),
    brand              varchar(100),
    specification      varchar(200),
    default_shelf_life integer,
    storage_condition  varchar(200),
    weight_unit        varchar(20),
    barcode            varchar(100),
    barcode_unique     integer DEFAULT 0,
    supplier_id        varchar(50),
    supplier_name      varchar(200),
    description        text,
    version            integer DEFAULT 1,
    parent_id          bigint,
    status             varchar(20) DEFAULT 'active',
    create_time        timestamp without time zone DEFAULT CURRENT_TIMESTAMP,
    update_time        timestamp without time zone DEFAULT CURRENT_TIMESTAMP,
    deleted            integer NOT NULL DEFAULT 0,
    CONSTRAINT material_template_pkey PRIMARY KEY (id),
    CONSTRAINT material_template_template_code_key UNIQUE (template_code)
);
CREATE INDEX IF NOT EXISTS idx_material_template_category ON material_template (category);
CREATE INDEX IF NOT EXISTS idx_material_template_code ON material_template (template_code);
CREATE INDEX IF NOT EXISTS idx_material_template_name ON material_template (material_name);
CREATE INDEX IF NOT EXISTS idx_material_template_parent ON material_template (parent_id);
CREATE INDEX IF NOT EXISTS idx_material_template_status ON material_template (status);

-- 4) transfer_templates（转账/调拨模板）
CREATE TABLE IF NOT EXISTS transfer_templates (
    template_id       bigint GENERATED ALWAYS AS IDENTITY,
    template_name     varchar(100) NOT NULL,
    template_type     integer      NOT NULL,
    source_subject_id bigint       NOT NULL,
    target_subject_id bigint       NOT NULL,
    amount_expression varchar(200),
    summary_template  varchar(200),
    is_enabled        boolean DEFAULT true,
    remark            text,
    create_user_id    bigint,
    create_time       timestamp without time zone NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time       timestamp without time zone NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted           integer NOT NULL DEFAULT 0,
    created_by        varchar(100),
    updated_by        varchar(100),
    version           integer DEFAULT 0,
    CONSTRAINT transfer_templates_pkey PRIMARY KEY (template_id)
);
CREATE INDEX IF NOT EXISTS idx_transfer_templates_source ON transfer_templates (source_subject_id) WHERE deleted = 0;
CREATE INDEX IF NOT EXISTS idx_transfer_templates_type ON transfer_templates (template_type) WHERE deleted = 0;

-- 5) invoice_reimbursement_approval_record（报销审批流水）
CREATE TABLE IF NOT EXISTS invoice_reimbursement_approval_record (
    record_id        bigint NOT NULL,
    tenant_id        bigint,
    reimbursement_id bigint NOT NULL,
    operator_id      bigint,
    operator_name    varchar(100),
    action           varchar(30),
    remark           varchar(500),
    operate_time     timestamp without time zone,
    created_by       bigint,
    create_time      timestamp without time zone DEFAULT CURRENT_TIMESTAMP,
    updated_by       bigint,
    update_time      timestamp without time zone DEFAULT CURRENT_TIMESTAMP,
    deleted          integer DEFAULT 0,
    CONSTRAINT invoice_reimbursement_approval_record_pkey PRIMARY KEY (record_id)
);
CREATE INDEX IF NOT EXISTS idx_inv_reimb_approve_action ON invoice_reimbursement_approval_record (action);
CREATE INDEX IF NOT EXISTS idx_inv_reimb_approve_operator ON invoice_reimbursement_approval_record (operator_id);
CREATE INDEX IF NOT EXISTS idx_inv_reimb_approve_reimb ON invoice_reimbursement_approval_record (reimbursement_id);

COMMENT ON TABLE app_device_registrations IS 'APP 设备注册；V20261028_006 补建的部署缺口表';
COMMENT ON TABLE inventory_warning_rules IS '库存预警规则；V20261028_006 补建的部署缺口表';
COMMENT ON TABLE material_template IS '物料模板；V20261028_006 补建的部署缺口表';
COMMENT ON TABLE transfer_templates IS '转账/调拨模板；V20261028_006 补建的部署缺口表';
COMMENT ON TABLE invoice_reimbursement_approval_record IS '报销审批流水；V20261028_006 补建的部署缺口表';
