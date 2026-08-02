-- ============================================================
-- 迁移脚本：为仅有实体类无建表脚本的表补充 CREATE TABLE
-- 说明：基于实体类 @TableName 与 @TableField 注解生成
-- 对应验收清单风险项 10.40（13张表）及 Section 8 其他风险项
-- ============================================================

-- 1. user_permission_overrides（用户权限覆盖）
CREATE TABLE IF NOT EXISTS user_permission_overrides (
    id BIGSERIAL PRIMARY KEY,
    user_id VARCHAR(64) NOT NULL,
    user_name VARCHAR(64),
    permission_code VARCHAR(128) NOT NULL,
    permission_name VARCHAR(128),
    domain_code VARCHAR(64),
    override_type VARCHAR(32) NOT NULL,
    reason VARCHAR(512),
    expire_time TIMESTAMP,
    status VARCHAR(32) NOT NULL DEFAULT 'ACTIVE',
    approve_by VARCHAR(64),
    approve_name VARCHAR(64),
    approve_time TIMESTAMP,
    created_by VARCHAR(64),
    created_by_name VARCHAR(64),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted INTEGER NOT NULL DEFAULT 0
);
CREATE INDEX IF NOT EXISTS idx_user_permission_overrides_user_id ON user_permission_overrides(user_id);
COMMENT ON TABLE user_permission_overrides IS '用户权限覆盖表';

-- 2. position_role_mapping（岗位-角色映射）
CREATE TABLE IF NOT EXISTS position_role_mapping (
    id BIGSERIAL PRIMARY KEY,
    position_id BIGINT NOT NULL,
    role_id BIGINT NOT NULL,
    is_primary INTEGER DEFAULT 0,
    priority INTEGER DEFAULT 0,
    status INTEGER DEFAULT 1,
    description VARCHAR(256),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by VARCHAR(64),
    updated_by VARCHAR(64),
    deleted INTEGER NOT NULL DEFAULT 0
);
CREATE INDEX IF NOT EXISTS idx_position_role_mapping_position_id ON position_role_mapping(position_id);
CREATE INDEX IF NOT EXISTS idx_position_role_mapping_role_id ON position_role_mapping(role_id);
COMMENT ON TABLE position_role_mapping IS '岗位-角色映射表';

-- 3. employee_archive_detail（员工档案详情）
CREATE TABLE IF NOT EXISTS employee_archive_detail (
    id BIGSERIAL PRIMARY KEY,
    archive_id BIGINT,
    employee_id VARCHAR(64),
    real_name VARCHAR(64),
    gender VARCHAR(16),
    birthday DATE,
    nation VARCHAR(32),
    political_status VARCHAR(32),
    marital_status VARCHAR(32),
    native_place VARCHAR(128),
    residence_address VARCHAR(256),
    current_address VARCHAR(256),
    education_level VARCHAR(32),
    graduation_school VARCHAR(128),
    major VARCHAR(128),
    graduation_date DATE,
    degree VARCHAR(32),
    work_experience TEXT,
    family_members TEXT,
    emergency_contact VARCHAR(64),
    emergency_relationship VARCHAR(32),
    emergency_phone VARCHAR(32),
    emergency_address VARCHAR(256),
    bank_name VARCHAR(64),
    bank_branch VARCHAR(128),
    bank_card VARCHAR(64),
    social_security_no VARCHAR(64),
    housing_fund_no VARCHAR(64),
    id_card_front_url VARCHAR(512),
    id_card_back_url VARCHAR(512),
    diploma_url VARCHAR(512),
    degree_certificate_url VARCHAR(512),
    photo_url VARCHAR(512),
    other_attachments TEXT,
    privacy_agreement_signed INTEGER DEFAULT 0,
    privacy_agreement_time TIMESTAMP,
    data_accuracy_confirmed INTEGER DEFAULT 0,
    data_confirm_time TIMESTAMP,
    completeness_score INTEGER,
    missing_fields TEXT,
    review_status VARCHAR(32) DEFAULT 'pending',
    review_by BIGINT,
    review_time TIMESTAMP,
    review_comment VARCHAR(512),
    create_by BIGINT,
    create_time TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted INTEGER NOT NULL DEFAULT 0
);
CREATE INDEX IF NOT EXISTS idx_employee_archive_detail_employee_id ON employee_archive_detail(employee_id);
CREATE INDEX IF NOT EXISTS idx_employee_archive_detail_archive_id ON employee_archive_detail(archive_id);
COMMENT ON TABLE employee_archive_detail IS '员工档案详情表';

-- 4. employee_labor_contract（劳动合同）
CREATE TABLE IF NOT EXISTS employee_labor_contract (
    id BIGSERIAL PRIMARY KEY,
    employee_id VARCHAR(64) NOT NULL,
    employee_code VARCHAR(64),
    employee_name VARCHAR(64),
    contract_no VARCHAR(64) NOT NULL,
    contract_type VARCHAR(32),
    start_date DATE,
    end_date DATE,
    probation_months INTEGER,
    probation_end_date DATE,
    salary DECIMAL(12,2),
    work_location VARCHAR(128),
    position VARCHAR(64),
    status VARCHAR(32) DEFAULT 'draft',
    sign_date DATE,
    sign_method VARCHAR(32),
    contract_file_url VARCHAR(512),
    remark VARCHAR(512),
    archive_id BIGINT,
    template_id BIGINT,
    generated_time TIMESTAMP,
    pdf_file_url VARCHAR(512),
    signed_pdf_url VARCHAR(512),
    company_sign_time TIMESTAMP,
    employee_sign_time TIMESTAMP,
    create_by BIGINT,
    create_time TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted INTEGER NOT NULL DEFAULT 0
);
CREATE INDEX IF NOT EXISTS idx_employee_labor_contract_employee_id ON employee_labor_contract(employee_id);
CREATE UNIQUE INDEX IF NOT EXISTS uk_employee_labor_contract_no ON employee_labor_contract(contract_no);
COMMENT ON TABLE employee_labor_contract IS '劳动合同表';

-- 5. employee_approvals（员工审批主表）
CREATE TABLE IF NOT EXISTS employee_approvals (
    approval_id VARCHAR(32) PRIMARY KEY,
    approval_no VARCHAR(64),
    type VARCHAR(32) NOT NULL,
    title VARCHAR(128),
    applicant_id VARCHAR(64) NOT NULL,
    status VARCHAR(32) DEFAULT 'pending',
    priority VARCHAR(16) DEFAULT 'normal',
    current_approver_id VARCHAR(64),
    form_summary TEXT,
    context_data_type VARCHAR(64),
    risk_level VARCHAR(32),
    remark VARCHAR(512),
    store_id VARCHAR(64),
    department_id VARCHAR(64),
    created_time TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_time TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted INTEGER NOT NULL DEFAULT 0,
    created_by VARCHAR(64),
    updated_by VARCHAR(64)
);
CREATE INDEX IF NOT EXISTS idx_employee_approvals_applicant_id ON employee_approvals(applicant_id);
CREATE INDEX IF NOT EXISTS idx_employee_approvals_status ON employee_approvals(status);
COMMENT ON TABLE employee_approvals IS '员工审批主表';

-- 6. leave_requests（请假申请）
CREATE TABLE IF NOT EXISTS leave_requests (
    request_id VARCHAR(32) PRIMARY KEY,
    approval_id VARCHAR(32),
    employee_id VARCHAR(64) NOT NULL,
    leave_type VARCHAR(32),
    start_date DATE,
    end_date DATE,
    days DECIMAL(5,1),
    reason VARCHAR(512),
    annual_total_snapshot INTEGER,
    annual_used_snapshot INTEGER,
    contact_phone VARCHAR(32),
    handover_to VARCHAR(64),
    handover_note VARCHAR(512),
    created_time TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_time TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted INTEGER NOT NULL DEFAULT 0,
    created_by VARCHAR(64),
    updated_by VARCHAR(64)
);
CREATE INDEX IF NOT EXISTS idx_leave_requests_employee_id ON leave_requests(employee_id);
COMMENT ON TABLE leave_requests IS '请假申请表';

-- 7. overtime_requests（加班申请）
CREATE TABLE IF NOT EXISTS overtime_requests (
    request_id VARCHAR(32) PRIMARY KEY,
    approval_id VARCHAR(32),
    employee_id VARCHAR(64) NOT NULL,
    overtime_date DATE,
    start_time TIME,
    end_time TIME,
    hours DECIMAL(5,2),
    reason VARCHAR(512),
    compensate_type VARCHAR(32),
    overtime_type VARCHAR(32),
    meal_allowance DECIMAL(10,2),
    transport_allowance DECIMAL(10,2),
    created_time TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_time TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted INTEGER NOT NULL DEFAULT 0,
    created_by VARCHAR(64),
    updated_by VARCHAR(64)
);
CREATE INDEX IF NOT EXISTS idx_overtime_requests_employee_id ON overtime_requests(employee_id);
COMMENT ON TABLE overtime_requests IS '加班申请表';

-- 8. reimbursement_requests（报销申请）
CREATE TABLE IF NOT EXISTS reimbursement_requests (
    request_id VARCHAR(32) PRIMARY KEY,
    approval_id VARCHAR(32),
    employee_id VARCHAR(64) NOT NULL,
    total_amount DECIMAL(12,2),
    monthly_budget DECIMAL(12,2),
    year_budget DECIMAL(12,2),
    receipt_count INTEGER,
    related_travel_id VARCHAR(32),
    reimbursement_category VARCHAR(32),
    bank_account VARCHAR(64),
    payee_name VARCHAR(64),
    description VARCHAR(512),
    invoice_nos VARCHAR(512),
    policy_notes VARCHAR(512),
    created_time TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_time TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted INTEGER NOT NULL DEFAULT 0,
    created_by VARCHAR(64),
    updated_by VARCHAR(64)
);
CREATE INDEX IF NOT EXISTS idx_reimbursement_requests_employee_id ON reimbursement_requests(employee_id);
COMMENT ON TABLE reimbursement_requests IS '报销申请表';

-- 9. finance_voucher（财务凭证）
CREATE TABLE IF NOT EXISTS finance_voucher (
    id BIGSERIAL PRIMARY KEY,
    voucher_no VARCHAR(64) NOT NULL,
    voucher_type VARCHAR(32),
    business_type VARCHAR(64),
    business_id BIGINT,
    debit_amount DECIMAL(15,2),
    credit_amount DECIMAL(15,2),
    currency VARCHAR(8) DEFAULT 'CNY',
    voucher_date DATE,
    status VARCHAR(32) DEFAULT 'draft',
    remark VARCHAR(512),
    store_id BIGINT,
    created_by VARCHAR(64),
    create_time TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_by VARCHAR(64),
    deleted INTEGER NOT NULL DEFAULT 0
);
CREATE UNIQUE INDEX IF NOT EXISTS uk_finance_voucher_no ON finance_voucher(voucher_no);
CREATE INDEX IF NOT EXISTS idx_finance_voucher_store_id ON finance_voucher(store_id);
COMMENT ON TABLE finance_voucher IS '财务凭证表';

-- 10. finance_voucher_detail（凭证明细）
CREATE TABLE IF NOT EXISTS finance_voucher_detail (
    id BIGSERIAL PRIMARY KEY,
    voucher_id BIGINT NOT NULL,
    account_code VARCHAR(64),
    account_name VARCHAR(128),
    account_type VARCHAR(32),
    debit_amount DECIMAL(15,2),
    credit_amount DECIMAL(15,2),
    summary VARCHAR(256),
    line_no INTEGER,
    create_time TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted INTEGER NOT NULL DEFAULT 0
);
CREATE INDEX IF NOT EXISTS idx_finance_voucher_detail_voucher_id ON finance_voucher_detail(voucher_id);
COMMENT ON TABLE finance_voucher_detail IS '财务凭证明细表';

-- 11. accounting_periods（会计期间）
CREATE TABLE IF NOT EXISTS accounting_periods (
    period_id BIGSERIAL PRIMARY KEY,
    period_code VARCHAR(32) NOT NULL,
    period_name VARCHAR(64),
    period_type INTEGER,
    start_date DATE,
    end_date DATE,
    status INTEGER DEFAULT 0,
    is_closed BOOLEAN DEFAULT FALSE,
    close_time TIMESTAMP,
    close_user_id BIGINT,
    trial_balance_passed BOOLEAN,
    remark VARCHAR(512),
    create_time TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by VARCHAR(64),
    updated_by VARCHAR(64),
    deleted INTEGER NOT NULL DEFAULT 0,
    version INTEGER DEFAULT 0
);
CREATE UNIQUE INDEX IF NOT EXISTS uk_accounting_periods_code ON accounting_periods(period_code);
COMMENT ON TABLE accounting_periods IS '会计期间表';

-- 12. finance_invoice（财务发票）
CREATE TABLE IF NOT EXISTS finance_invoice (
    id BIGSERIAL PRIMARY KEY,
    invoice_no VARCHAR(64) NOT NULL,
    invoice_type VARCHAR(32),
    business_id BIGINT,
    business_type VARCHAR(64),
    amount DECIMAL(15,2),
    tax_amount DECIMAL(15,2),
    total_amount DECIMAL(15,2),
    currency VARCHAR(8) DEFAULT 'CNY',
    invoice_date DATE,
    status VARCHAR(32) DEFAULT 'draft',
    remark VARCHAR(512),
    store_id BIGINT,
    created_by VARCHAR(64),
    create_time TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted INTEGER NOT NULL DEFAULT 0
);
CREATE UNIQUE INDEX IF NOT EXISTS uk_finance_invoice_no ON finance_invoice(invoice_no);
COMMENT ON TABLE finance_invoice IS '财务发票表';

-- 13. approval_flow_configs（审批流配置）
CREATE TABLE IF NOT EXISTS approval_flow_configs (
    config_id BIGSERIAL PRIMARY KEY,
    config_name VARCHAR(128) NOT NULL,
    document_type VARCHAR(64) NOT NULL,
    approval_nodes TEXT,
    is_enabled BOOLEAN DEFAULT TRUE,
    remark VARCHAR(512),
    create_time TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by VARCHAR(64),
    updated_by VARCHAR(64),
    deleted INTEGER NOT NULL DEFAULT 0,
    version INTEGER DEFAULT 0
);
CREATE INDEX IF NOT EXISTS idx_approval_flow_configs_document_type ON approval_flow_configs(document_type);
COMMENT ON TABLE approval_flow_configs IS '审批流配置表';

-- 14. budgets（预算）
CREATE TABLE IF NOT EXISTS budgets (
    budget_id VARCHAR(32) PRIMARY KEY,
    budget_name VARCHAR(128) NOT NULL,
    budget_amount DECIMAL(15,2),
    used_amount DECIMAL(15,2) DEFAULT 0,
    remaining_amount DECIMAL(15,2),
    budget_period VARCHAR(32),
    status VARCHAR(32) DEFAULT 'active',
    create_time TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    create_by VARCHAR(64),
    update_by VARCHAR(64),
    deleted INTEGER NOT NULL DEFAULT 0
);
DO $$
BEGIN
    IF EXISTS (
        SELECT 1 FROM information_schema.columns
        WHERE table_name = 'budgets' AND column_name = 'budget_period'
    ) THEN
        CREATE INDEX IF NOT EXISTS idx_budgets_period ON budgets(budget_period);
    END IF;
END $$;
COMMENT ON TABLE budgets IS '预算表';

-- 15. account_balance（账户余额）
CREATE TABLE IF NOT EXISTS account_balance (
    id BIGSERIAL PRIMARY KEY,
    subject_id BIGINT,
    subject_code VARCHAR(64),
    subject_name VARCHAR(128),
    category VARCHAR(32),
    period VARCHAR(16),
    begin_debit DECIMAL(15,2),
    begin_credit DECIMAL(15,2),
    current_debit DECIMAL(15,2),
    current_credit DECIMAL(15,2),
    end_debit DECIMAL(15,2),
    end_credit DECIMAL(15,2),
    create_time TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted INTEGER NOT NULL DEFAULT 0
);
CREATE INDEX IF NOT EXISTS idx_account_balance_period ON account_balance(period);
CREATE INDEX IF NOT EXISTS idx_account_balance_subject_code ON account_balance(subject_code);
COMMENT ON TABLE account_balance IS '账户余额表';

-- 16. ai_model_configs（AI 模型配置）
CREATE TABLE IF NOT EXISTS ai_model_configs (
    id SERIAL PRIMARY KEY,
    model_code VARCHAR(64) NOT NULL,
    model_name VARCHAR(128) NOT NULL,
    model_type VARCHAR(32),
    endpoint VARCHAR(512),
    api_key VARCHAR(512),
    status VARCHAR(16) DEFAULT 'inactive',
    confidence VARCHAR(16),
    description VARCHAR(512),
    last_sync_time TIMESTAMP,
    model_path VARCHAR(512),
    model_params TEXT,
    timeout INTEGER DEFAULT 30,
    created_by VARCHAR(64),
    updated_by VARCHAR(64),
    deleted INTEGER NOT NULL DEFAULT 0,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);
CREATE UNIQUE INDEX IF NOT EXISTS uk_ai_model_configs_code ON ai_model_configs(model_code);
COMMENT ON TABLE ai_model_configs IS 'AI 模型配置表';

-- 17. electronic_signature（电子签名）
CREATE TABLE IF NOT EXISTS electronic_signature (
    id BIGSERIAL PRIMARY KEY,
    contract_id BIGINT,
    signer_type VARCHAR(32),
    signer_id VARCHAR(64),
    signer_name VARCHAR(64),
    signature_data TEXT,
    signature_image_url VARCHAR(512),
    sign_time TIMESTAMP,
    sign_ip VARCHAR(64),
    sign_device VARCHAR(128),
    verify_code VARCHAR(64),
    verify_time TIMESTAMP,
    status VARCHAR(32) DEFAULT 'pending',
    create_time TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted INTEGER NOT NULL DEFAULT 0
);
CREATE INDEX IF NOT EXISTS idx_electronic_signature_contract_id ON electronic_signature(contract_id);
COMMENT ON TABLE electronic_signature IS '电子签名表';

-- 18. store_inventory_log（门店库存日志，单数表名）
-- 注意：实体类 @TableName 为单数 store_inventory_log
CREATE TABLE IF NOT EXISTS store_inventory_log (
    id BIGSERIAL PRIMARY KEY,
    inventory_id BIGINT,
    product_id BIGINT,
    product_name VARCHAR(128),
    store_id VARCHAR(64),
    store_name VARCHAR(128),
    type INTEGER,
    before_stock INTEGER,
    after_stock INTEGER,
    change_quantity INTEGER,
    operator_id BIGINT,
    operator_name VARCHAR(64),
    remark VARCHAR(512),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted INTEGER DEFAULT 0
);
CREATE INDEX IF NOT EXISTS idx_store_inventory_log_store_id ON store_inventory_log(store_id);
CREATE INDEX IF NOT EXISTS idx_store_inventory_log_inventory_id ON store_inventory_log(inventory_id);
COMMENT ON TABLE store_inventory_log IS '门店库存日志表';

-- ============================================================
-- 修复单复数命名不一致问题：为单数表创建复数视图（或别名）
-- 实体类使用复数 @TableName，迁移脚本仅有单数 CREATE TABLE
-- 通过创建 VIEW 解决（避免数据迁移风险）
-- ============================================================

-- accounting_subject（迁移单数）→ accounting_subjects（实体复数）
-- 幂等保护：目标若已为 BASE TABLE 则跳过（实体表优先于视图别名）
DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM information_schema.tables
        WHERE table_schema = 'public' AND table_name = 'accounting_subjects' AND table_type = 'BASE TABLE'
    ) THEN
        CREATE OR REPLACE VIEW accounting_subjects AS SELECT * FROM accounting_subject;
        COMMENT ON VIEW accounting_subjects IS '会计科目视图（兼容复数命名的实体类）';
    END IF;
END $$;

-- cost_record（迁移单数）→ cost_records（实体复数）
DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM information_schema.tables
        WHERE table_schema = 'public' AND table_name = 'cost_records' AND table_type = 'BASE TABLE'
    ) THEN
        CREATE OR REPLACE VIEW cost_records AS SELECT * FROM cost_record;
        COMMENT ON VIEW cost_records IS '成本记录视图（兼容复数命名的实体类）';
    END IF;
END $$;

-- dining_tables（迁移复数）→ dining_table（旧版实体单数）
DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM information_schema.tables
        WHERE table_schema = 'public' AND table_name = 'dining_table' AND table_type = 'BASE TABLE'
    ) THEN
        CREATE OR REPLACE VIEW dining_table AS SELECT * FROM dining_tables;
        COMMENT ON VIEW dining_table IS '桌台视图（兼容单数命名的旧版实体类）';
    END IF;
END $$;

-- asset_master（旧版实体单数）→ asset_masters_enhanced（迁移新版）
DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM information_schema.tables
        WHERE table_schema = 'public' AND table_name = 'asset_master' AND table_type = 'BASE TABLE'
    ) THEN
        CREATE OR REPLACE VIEW asset_master AS SELECT * FROM asset_masters_enhanced;
        COMMENT ON VIEW asset_master IS '资产主表视图（兼容旧版单数实体类）';
    END IF;
END $$;

-- ============================================================
-- 说明：以下9张表在 10.40 中列为"无迁移脚本"，但实体类也不存在
-- 这些表是规划中的表，无需创建迁移脚本：
-- EmployeePositionHistory / StoreFoodStatus / MemberConsumptionRecord
-- / DeviceMaintenanceRecord / AssetInventoryDetail / AssetScrapRecord
-- / TraceNodeWarning / FinanceBankAccountLog / RecruitmentInterviewEvaluation
-- ============================================================
