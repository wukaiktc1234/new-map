-- ============================================================
-- PD-014 裁决落地（OICBE-B1-001 / ETM-001）：代码引用优先建表
-- 架构裁决 2026-08-16：registration_code 独立建表（含 code_expiry_time 列）
--   + onboarding_records 补 code_expiry_time 列；不删代码，按「代码已引用即建」最小路径
-- 三向核对（2026-08-16 information_schema 只读实测）：
--   - 实体：entity/RegistrationCode.java @TableName("registration_code")
--     字段：id(String ASSIGN_UUID)/code/type/validity_start/validity_end/status/
--           created_by/onboarding_record_id/create_time/update_time/use_time
--           + code_expiry_time（PD-014 决策要求表内含此列）
--   - migration：全仓无 registration_code 建表（V20260625_004 仅作 onboarding_records 列）
--   - 真实表：registration_code 不存在；onboarding_records 28 列实测缺 code_expiry_time
-- 幂等：CREATE TABLE IF NOT EXISTS / ADD COLUMN IF NOT EXISTS / CREATE INDEX IF NOT EXISTS
-- 类型惯例对齐：code VARCHAR(50)（对齐 onboarding_records.registration_code VARCHAR(50)）、
--   onboarding_record_id VARCHAR(32)（对齐 onboarding_records.id VARCHAR(32)）、
--   created_by VARCHAR(36)（对齐 onboarding_records.created_by VARCHAR(36)）
-- ============================================================

CREATE TABLE IF NOT EXISTS registration_code (
    id                   VARCHAR(64)    NOT NULL,
    code                 VARCHAR(50)    NOT NULL,
    type                 VARCHAR(20),
    validity_start       TIMESTAMP,
    validity_end         TIMESTAMP,
    status               VARCHAR(20)    DEFAULT 'UNUSED',
    code_expiry_time     TIMESTAMP,
    created_by           VARCHAR(36),
    onboarding_record_id VARCHAR(32),
    create_time          TIMESTAMP      NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time          TIMESTAMP      NOT NULL DEFAULT CURRENT_TIMESTAMP,
    use_time             TIMESTAMP,
    CONSTRAINT pk_registration_code PRIMARY KEY (id)
);

CREATE INDEX IF NOT EXISTS idx_registration_code_code ON registration_code (code);
CREATE INDEX IF NOT EXISTS idx_registration_code_onboarding_record_id ON registration_code (onboarding_record_id);

COMMENT ON TABLE registration_code IS '注册码表（PD-014 裁决：代码引用优先建表，与 onboarding_records 列并存双写）';
COMMENT ON COLUMN registration_code.id IS '主键ID（ASSIGN_UUID）';
COMMENT ON COLUMN registration_code.code IS '注册码';
COMMENT ON COLUMN registration_code.type IS '注册码类型: INTERNAL-内部员工 EXTERNAL-外部合作';
COMMENT ON COLUMN registration_code.validity_start IS '有效期开始时间';
COMMENT ON COLUMN registration_code.validity_end IS '有效期结束时间';
COMMENT ON COLUMN registration_code.status IS '状态: UNUSED-未使用 USED-已使用 EXPIRED-已过期 INVALID-无效';
COMMENT ON COLUMN registration_code.code_expiry_time IS '注册码过期时间（PD-014 补列，与 onboarding_records.code_expiry_time 对齐）';
COMMENT ON COLUMN registration_code.created_by IS '创建人ID';
COMMENT ON COLUMN registration_code.onboarding_record_id IS '入职记录ID（外键→onboarding_records.id）';
COMMENT ON COLUMN registration_code.create_time IS '创建时间';
COMMENT ON COLUMN registration_code.update_time IS '更新时间';
COMMENT ON COLUMN registration_code.use_time IS '使用时间';

-- onboarding_records 补 code_expiry_time 列（实体 OnboardingRecord.codeExpiryTime 映射，
-- 三向核对实测缺失——该列缺失为注册码生成链路第一失败点（selectById 列漂移 500），先于 registration_code 表缺失）
ALTER TABLE onboarding_records ADD COLUMN IF NOT EXISTS code_expiry_time TIMESTAMP;
COMMENT ON COLUMN onboarding_records.code_expiry_time IS '注册码过期时间（PD-014 补列）';
