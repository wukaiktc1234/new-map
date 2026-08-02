-- ============================================================
-- 电子签章模块：印章主表 + 印章使用记录表
-- 兼容 H2 (MODE=PostgreSQL) 与 PostgreSQL 18
--
-- 说明：
--   1. authorized_users / authorized_scenes 使用 TEXT 存储 JSON 数组字符串
--      （H2 不支持 JSONB，使用 TEXT 保证开发/生产环境一致；
--       Java 实体使用 String，Service 层负责 JSON 序列化/反序列化）
--   2. 枚举值与前端 types/seal.ts 保持一致：
--      - seal_type: official/finance/contract/legal/custom
--      - status: active/inactive/revoked
--      - business_type: hr_contract/purchase_contract/electronic_contract
-- ============================================================

-- Part 1: 印章主表（seals）
-- 存储印章基本信息、授权使用人和授权使用场景
CREATE TABLE IF NOT EXISTS seals (
    seal_id VARCHAR(32) PRIMARY KEY,
    seal_name VARCHAR(100) NOT NULL,
    seal_type VARCHAR(20) NOT NULL,
    seal_image TEXT,
    status VARCHAR(20) NOT NULL DEFAULT 'active',
    keeper VARCHAR(50),
    authorized_users TEXT,
    authorized_scenes TEXT,
    create_by VARCHAR(50),
    remark VARCHAR(500),
    create_time TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted INTEGER NOT NULL DEFAULT 0,
    CONSTRAINT chk_seals_status CHECK (status IN ('active', 'inactive', 'revoked')),
    CONSTRAINT chk_seals_type CHECK (seal_type IN ('official', 'finance', 'contract', 'legal', 'custom'))
);

CREATE INDEX IF NOT EXISTS idx_seals_type ON seals(seal_type);
CREATE INDEX IF NOT EXISTS idx_seals_status ON seals(status);
CREATE INDEX IF NOT EXISTS idx_seals_keeper ON seals(keeper);
CREATE INDEX IF NOT EXISTS idx_seals_deleted ON seals(deleted);

COMMENT ON TABLE seals IS '印章主表';
COMMENT ON COLUMN seals.seal_id IS '印章ID(雪花算法)';
COMMENT ON COLUMN seals.seal_name IS '印章名称';
COMMENT ON COLUMN seals.seal_type IS '印章类型: official-公章 finance-财务专用章 contract-合同专用章 legal-法人章 custom-自定义';
COMMENT ON COLUMN seals.seal_image IS '印章图片(Base64/data URI)';
COMMENT ON COLUMN seals.status IS '状态: active-启用 inactive-停用 revoked-作废';
COMMENT ON COLUMN seals.keeper IS '保管人';
COMMENT ON COLUMN seals.authorized_users IS '授权使用人ID列表(JSON数组字符串)';
COMMENT ON COLUMN seals.authorized_scenes IS '授权使用场景列表(JSON数组字符串)';
COMMENT ON COLUMN seals.create_by IS '创建人';
COMMENT ON COLUMN seals.remark IS '备注';

-- Part 2: 印章使用记录表（seal_usage_logs）
-- 记录每次印章使用的业务信息，用于审计追溯
CREATE TABLE IF NOT EXISTS seal_usage_logs (
    log_id VARCHAR(32) PRIMARY KEY,
    seal_id VARCHAR(32) NOT NULL,
    seal_name VARCHAR(100),
    business_type VARCHAR(30) NOT NULL,
    business_id VARCHAR(32),
    business_no VARCHAR(100),
    operator VARCHAR(50),
    operate_time TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    ip_address VARCHAR(50),
    remark VARCHAR(500),
    create_time TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted INTEGER NOT NULL DEFAULT 0,
    CONSTRAINT chk_seal_usage_logs_business_type
        CHECK (business_type IN ('hr_contract', 'purchase_contract', 'electronic_contract'))
);

CREATE INDEX IF NOT EXISTS idx_seal_usage_logs_seal ON seal_usage_logs(seal_id);
CREATE INDEX IF NOT EXISTS idx_seal_usage_logs_business_type ON seal_usage_logs(business_type);
CREATE INDEX IF NOT EXISTS idx_seal_usage_logs_operate_time ON seal_usage_logs(operate_time);
CREATE INDEX IF NOT EXISTS idx_seal_usage_logs_deleted ON seal_usage_logs(deleted);

COMMENT ON TABLE seal_usage_logs IS '印章使用记录表';
COMMENT ON COLUMN seal_usage_logs.log_id IS '记录ID(雪花算法)';
COMMENT ON COLUMN seal_usage_logs.seal_id IS '印章ID';
COMMENT ON COLUMN seal_usage_logs.seal_name IS '印章名称(冗余,便于展示)';
COMMENT ON COLUMN seal_usage_logs.business_type IS '业务类型: hr_contract-人事合同 purchase_contract-采购合同 electronic_contract-电子合同';
COMMENT ON COLUMN seal_usage_logs.business_id IS '业务ID(合同ID)';
COMMENT ON COLUMN seal_usage_logs.business_no IS '业务编号(合同编号,便于展示)';
COMMENT ON COLUMN seal_usage_logs.operator IS '操作人';
COMMENT ON COLUMN seal_usage_logs.operate_time IS '操作时间';
COMMENT ON COLUMN seal_usage_logs.ip_address IS 'IP地址';
COMMENT ON COLUMN seal_usage_logs.remark IS '备注';
