-- 财务记录表添加审核相关字段
-- 执行时间: 2026-03-04
-- 作者: 系统审查修复

-- 添加审核状态字段
ALTER TABLE finance_records
ADD COLUMN IF NOT EXISTS audit_status VARCHAR(20) DEFAULT 'PENDING';

-- 添加审核人ID字段
ALTER TABLE finance_records
ADD COLUMN IF NOT EXISTS auditor_id BIGINT;

-- 添加审核人名称字段
ALTER TABLE finance_records
ADD COLUMN IF NOT EXISTS auditor_name VARCHAR(50);

-- 添加审核时间字段
ALTER TABLE finance_records
ADD COLUMN IF NOT EXISTS audit_time TIMESTAMP;

-- 添加审核备注字段
ALTER TABLE finance_records
ADD COLUMN IF NOT EXISTS audit_remark TEXT;

-- 添加创建人ID字段
ALTER TABLE finance_records
ADD COLUMN IF NOT EXISTS create_by_id BIGINT;

-- 添加更新人ID字段
ALTER TABLE finance_records
ADD COLUMN IF NOT EXISTS update_by_id BIGINT;

-- 添加创建时间字段（如果不存在）
ALTER TABLE finance_records
ADD COLUMN IF NOT EXISTS create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP;

-- 添加更新时间字段（如果不存在）
ALTER TABLE finance_records
ADD COLUMN IF NOT EXISTS update_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP;

-- 字段注释
COMMENT ON COLUMN finance_records.audit_status IS '审核状态：待审核(PENDING)、已通过(APPROVED)、已拒绝(REJECTED)';
COMMENT ON COLUMN finance_records.auditor_id IS '审核人ID';
COMMENT ON COLUMN finance_records.auditor_name IS '审核人名称';
COMMENT ON COLUMN finance_records.audit_time IS '审核时间';
COMMENT ON COLUMN finance_records.audit_remark IS '审核备注';
COMMENT ON COLUMN finance_records.create_by_id IS '创建人ID';
COMMENT ON COLUMN finance_records.update_by_id IS '更新人ID';
COMMENT ON COLUMN finance_records.create_time IS '创建时间';
COMMENT ON COLUMN finance_records.update_time IS '更新时间';

-- 添加审核状态索引
CREATE INDEX IF NOT EXISTS idx_finance_records_audit_status ON finance_records(audit_status);

-- 添加审核人索引
CREATE INDEX IF NOT EXISTS idx_finance_records_auditor_id ON finance_records(auditor_id);
