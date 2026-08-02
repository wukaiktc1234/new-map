-- 创建系统配置表
-- 用于存储税务平台API配置等系统级配置

CREATE TABLE IF NOT EXISTS system_config (
    id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    tenant_id BIGINT NOT NULL DEFAULT 0,
    config_key VARCHAR(100) NOT NULL,
    config_value TEXT,
    config_type VARCHAR(50) DEFAULT 'general',
    description VARCHAR(500),
    encrypted SMALLINT DEFAULT 0,
    created_by BIGINT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_by BIGINT,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    deleted SMALLINT DEFAULT 0,

    CONSTRAINT uk_tenant_key UNIQUE (tenant_id, config_key)
);

CREATE INDEX IF NOT EXISTS idx_system_config_tenant_type ON system_config(tenant_id, config_type);
CREATE INDEX IF NOT EXISTS idx_system_config_tenant_deleted ON system_config(tenant_id, deleted);

COMMENT ON TABLE system_config IS '系统配置表';
COMMENT ON COLUMN system_config.id IS '主键ID';
COMMENT ON COLUMN system_config.tenant_id IS '租户ID';
COMMENT ON COLUMN system_config.config_key IS '配置键';
COMMENT ON COLUMN system_config.config_value IS '配置值';
COMMENT ON COLUMN system_config.config_type IS '配置类型';
COMMENT ON COLUMN system_config.description IS '配置描述';
COMMENT ON COLUMN system_config.encrypted IS '是否加密：0-否，1-是';
COMMENT ON COLUMN system_config.created_by IS '创建人ID';
COMMENT ON COLUMN system_config.created_at IS '创建时间';
COMMENT ON COLUMN system_config.updated_by IS '更新人ID';
COMMENT ON COLUMN system_config.updated_at IS '更新时间';
COMMENT ON COLUMN system_config.deleted IS '逻辑删除：0-未删除，1-已删除';

-- 添加缺失的唯一索引（electronic_voucher 表）
CREATE UNIQUE INDEX IF NOT EXISTS uk_voucher_no_tenant ON electronic_voucher(tenant_id, voucher_no);
CREATE UNIQUE INDEX IF NOT EXISTS uk_file_hash_tenant ON electronic_voucher(tenant_id, source_file_hash);

-- 添加性能优化索引
CREATE INDEX IF NOT EXISTS idx_ev_signature_status ON electronic_voucher(signature_status);
CREATE INDEX IF NOT EXISTS idx_ev_verify_status ON electronic_voucher(verify_status);
CREATE INDEX IF NOT EXISTS idx_ev_issue_date ON electronic_voucher(issue_date);
