-- 创建密码策略表
CREATE TABLE IF NOT EXISTS password_policies (
    policy_id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    policy_name VARCHAR(100) NOT NULL,
    min_length INTEGER NOT NULL DEFAULT 8,
    max_length INTEGER NOT NULL DEFAULT 32,
    require_uppercase BOOLEAN NOT NULL DEFAULT TRUE,
    require_lowercase BOOLEAN NOT NULL DEFAULT TRUE,
    require_digit BOOLEAN NOT NULL DEFAULT TRUE,
    require_special BOOLEAN NOT NULL DEFAULT TRUE,
    password_expiry_days INTEGER NOT NULL DEFAULT 90,
    history_check_count INTEGER NOT NULL DEFAULT 5,
    max_login_failures INTEGER NOT NULL DEFAULT 5,
    lock_duration_minutes INTEGER NOT NULL DEFAULT 30,
    status INTEGER NOT NULL DEFAULT 1,
    is_default BOOLEAN NOT NULL DEFAULT FALSE,
    create_time TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    version INTEGER NOT NULL DEFAULT 0,
    deleted INTEGER NOT NULL DEFAULT 0
);

-- 创建唯一索引，确保只有一个默认策略
CREATE UNIQUE INDEX IF NOT EXISTS uk_password_policies_is_default ON password_policies(is_default) WHERE is_default = TRUE AND deleted = 0;

-- 创建策略名称查询索引
CREATE INDEX IF NOT EXISTS idx_password_policies_policy_name ON password_policies(policy_name);

-- 插入默认密码策略（幂等：仅在表为空时插入）
INSERT INTO password_policies (
    policy_name, min_length, max_length, require_uppercase, require_lowercase,
    require_digit, require_special, password_expiry_days, history_check_count,
    max_login_failures, lock_duration_minutes, status, is_default
) SELECT
    '默认密码策略', 8, 32, TRUE, TRUE, TRUE, TRUE, 90, 5, 5, 30, 1, TRUE
WHERE NOT EXISTS (SELECT 1 FROM password_policies WHERE policy_name = '默认密码策略');
