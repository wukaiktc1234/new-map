-- ============================================================
-- 生产环境剩余 schema 修复脚本
-- 版本: V20260707_002
-- 说明: 补齐权限表、职位表、门店表、任务执行日志、审计日志归档等缺失字段/表
-- ============================================================

-- ------------------------------------------------------
-- 1. 创建门店表(stores)，与 Store 实体对应
-- ------------------------------------------------------
CREATE TABLE IF NOT EXISTS stores (
    store_id      VARCHAR(32)  PRIMARY KEY,
    store_name    VARCHAR(100) NOT NULL,
    store_code    VARCHAR(50)  UNIQUE,
    address       VARCHAR(500),
    phone         VARCHAR(50),
    manager_id    VARCHAR(32),
    manager_name  VARCHAR(50),
    status        VARCHAR(20)  DEFAULT 'active',
    company_id    VARCHAR(32),
    region        VARCHAR(100),
    store_type    VARCHAR(20)  DEFAULT 'single',
    created_at    TIMESTAMP    DEFAULT CURRENT_TIMESTAMP,
    updated_at    TIMESTAMP    DEFAULT CURRENT_TIMESTAMP,
    created_by    VARCHAR(50),
    updated_by    VARCHAR(50),
    deleted       BOOLEAN      DEFAULT FALSE,
    version       INTEGER      DEFAULT 0,
    ext_data      TEXT
);

CREATE INDEX IF NOT EXISTS idx_stores_status ON stores(status);
CREATE INDEX IF NOT EXISTS idx_stores_company_id ON stores(company_id);

-- ------------------------------------------------------
-- 2. 职位表(positions)补齐实体所需字段
-- ------------------------------------------------------
ALTER TABLE positions ADD COLUMN IF NOT EXISTS level_id    BIGINT;
ALTER TABLE positions ADD COLUMN IF NOT EXISTS level_type  VARCHAR(50);
ALTER TABLE positions ADD COLUMN IF NOT EXISTS created_by  VARCHAR(50);
ALTER TABLE positions ADD COLUMN IF NOT EXISTS updated_by  VARCHAR(50);

-- ------------------------------------------------------
-- 3. 权限表(permissions)补齐实体所需字段并把状态改为整型
-- ------------------------------------------------------
ALTER TABLE permissions ADD COLUMN IF NOT EXISTS permission_type INTEGER DEFAULT 1;
ALTER TABLE permissions ADD COLUMN IF NOT EXISTS module          VARCHAR(50);
ALTER TABLE permissions ADD COLUMN IF NOT EXISTS level           INTEGER DEFAULT 1;
ALTER TABLE permissions ADD COLUMN IF NOT EXISTS icon            VARCHAR(100);
ALTER TABLE permissions ADD COLUMN IF NOT EXISTS path            VARCHAR(200);
ALTER TABLE permissions ADD COLUMN IF NOT EXISTS component       VARCHAR(200);
ALTER TABLE permissions ADD COLUMN IF NOT EXISTS redirect        VARCHAR(200);
ALTER TABLE permissions ADD COLUMN IF NOT EXISTS is_hidden       INTEGER DEFAULT 0;
ALTER TABLE permissions ADD COLUMN IF NOT EXISTS is_cache        INTEGER DEFAULT 0;
ALTER TABLE permissions ADD COLUMN IF NOT EXISTS created_by      BIGINT;
ALTER TABLE permissions ADD COLUMN IF NOT EXISTS updated_by      BIGINT;

-- 将权限状态从字符串转换为整数：active->1，其他->0
-- 注意：必须先 DROP DEFAULT，否则旧默认值（如 'active'）无法隐式转换为 INTEGER
DO $$
BEGIN
    IF EXISTS (
        SELECT 1 FROM information_schema.columns
        WHERE table_name = 'permissions' AND column_name = 'status'
          AND data_type = 'character varying'
    ) THEN
        ALTER TABLE permissions ALTER COLUMN status DROP DEFAULT;
        ALTER TABLE permissions ALTER COLUMN status TYPE INTEGER
            USING CASE WHEN status = 'active' THEN 1 ELSE 0 END;
        ALTER TABLE permissions ALTER COLUMN status SET DEFAULT 1;
    END IF;
END $$;

CREATE INDEX IF NOT EXISTS idx_permissions_permission_type ON permissions(permission_type);
CREATE INDEX IF NOT EXISTS idx_permissions_module ON permissions(module);

-- ------------------------------------------------------
-- 4. 任务执行日志结果字段由 JSONB 改为 TEXT，避免 MyBatis-Plus 更新类型不匹配
-- ------------------------------------------------------
ALTER TABLE task_execution_log ALTER COLUMN result_data TYPE TEXT;

-- ------------------------------------------------------
-- 5. 审计日志表补齐实体/归档所需的字段
-- ------------------------------------------------------
ALTER TABLE audit_log ADD COLUMN IF NOT EXISTS tenant_id       VARCHAR(50);
ALTER TABLE audit_log ADD COLUMN IF NOT EXISTS request_id      VARCHAR(100);
ALTER TABLE audit_log ADD COLUMN IF NOT EXISTS operation_type  VARCHAR(50);
ALTER TABLE audit_log ADD COLUMN IF NOT EXISTS user_agent      TEXT;
ALTER TABLE audit_log ADD COLUMN IF NOT EXISTS response_body   TEXT;
ALTER TABLE audit_log ADD COLUMN IF NOT EXISTS business_key    VARCHAR(100);
ALTER TABLE audit_log ADD COLUMN IF NOT EXISTS risk_level      VARCHAR(20);
ALTER TABLE audit_log ADD COLUMN IF NOT EXISTS sensitive_flag  SMALLINT DEFAULT 0;
ALTER TABLE audit_log ADD COLUMN IF NOT EXISTS session_id      VARCHAR(100);

-- ------------------------------------------------------
-- 6. 创建审计日志归档表
-- ------------------------------------------------------
CREATE TABLE IF NOT EXISTS audit_log_archive (
    id               BIGINT       PRIMARY KEY,
    tenant_id        VARCHAR(50),
    request_id       VARCHAR(100),
    user_id          VARCHAR(50),
    username         VARCHAR(100),
    operation        VARCHAR(100),
    module           VARCHAR(100),
    operation_type   VARCHAR(50),
    ip               VARCHAR(50),
    user_agent       TEXT,
    request_url      VARCHAR(500),
    request_method   VARCHAR(10),
    request_params   TEXT,
    response_status  VARCHAR(20),
    response_body    TEXT,
    error_message    TEXT,
    execution_time   BIGINT,
    business_key     VARCHAR(100),
    risk_level       VARCHAR(20),
    sensitive_flag   SMALLINT DEFAULT 0,
    created_at       TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_audit_log_archive_user_id ON audit_log_archive(user_id);
CREATE INDEX IF NOT EXISTS idx_audit_log_archive_created_at ON audit_log_archive(created_at);

-- ------------------------------------------------------
-- 7. 入职档案表(onboarding_archive)补齐实体所需字段
-- ------------------------------------------------------
ALTER TABLE onboarding_archive ADD COLUMN IF NOT EXISTS create_time   TIMESTAMP DEFAULT CURRENT_TIMESTAMP;
ALTER TABLE onboarding_archive ADD COLUMN IF NOT EXISTS update_time   TIMESTAMP DEFAULT CURRENT_TIMESTAMP;
ALTER TABLE onboarding_archive ADD COLUMN IF NOT EXISTS deleted       INTEGER   DEFAULT 0;
ALTER TABLE onboarding_archive ADD COLUMN IF NOT EXISTS create_by     BIGINT;
ALTER TABLE onboarding_archive ADD COLUMN IF NOT EXISTS update_by     BIGINT;

CREATE INDEX IF NOT EXISTS idx_onboarding_archive_status      ON onboarding_archive(status);
CREATE INDEX IF NOT EXISTS idx_onboarding_archive_create_time ON onboarding_archive(create_time);
CREATE INDEX IF NOT EXISTS idx_onboarding_archive_deleted     ON onboarding_archive(deleted);
