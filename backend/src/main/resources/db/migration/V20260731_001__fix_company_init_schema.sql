-- ============================================================
-- V20260731_001: 修复公司初始化相关表结构
-- 背景：
--   公司初始化向导（CompanyInit）依赖 system_init_status 与
--   enterprise_config 两张表，但既有 schema 中字段与实体不兼容，
--   导致 /v1/system/init/status 等接口 500。
-- 操作：
--   1. 重建 system_init_status（匹配 SystemInitStatus 实体）
--   2. 重建 enterprise_config（匹配 EnterpriseConfig 实体）
-- 兼容性：
--   本脚本使用 H2 / PostgreSQL 兼容语法，Flyway 仅执行一次。
-- ============================================================

-- 1. 系统初始化状态表
DROP TABLE IF EXISTS system_init_status CASCADE;

CREATE TABLE system_init_status (
    id            BIGSERIAL PRIMARY KEY,
    step          VARCHAR(50)  NOT NULL DEFAULT 'welcome',
    is_completed  BOOLEAN      NOT NULL DEFAULT FALSE,
    completed_at  TIMESTAMP    DEFAULT NULL,
    created_at    TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at    TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP
);

COMMENT ON TABLE system_init_status IS '系统初始化状态表';
COMMENT ON COLUMN system_init_status.step IS '当前步骤：welcome/enterprise/organization/roles/completed';
COMMENT ON COLUMN system_init_status.is_completed IS '是否完成初始化';

-- 2. 企业配置表
DROP TABLE IF EXISTS enterprise_config CASCADE;

CREATE TABLE enterprise_config (
    id                BIGSERIAL PRIMARY KEY,
    enterprise_name   VARCHAR(200) DEFAULT NULL,
    enterprise_type   VARCHAR(50)  DEFAULT NULL,
    scale             VARCHAR(50)  DEFAULT NULL,
    init_template_id  INTEGER      DEFAULT NULL,
    init_completed    BOOLEAN      NOT NULL DEFAULT FALSE,
    init_completed_at TIMESTAMP    DEFAULT NULL,
    custom_config     TEXT         DEFAULT NULL,
    created_at        TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at        TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by        VARCHAR(64)  DEFAULT NULL,
    updated_by        VARCHAR(64)  DEFAULT NULL,
    deleted           INTEGER      NOT NULL DEFAULT 0
);

COMMENT ON TABLE enterprise_config IS '企业配置表';
COMMENT ON COLUMN enterprise_config.enterprise_name IS '企业名称';
COMMENT ON COLUMN enterprise_config.enterprise_type IS '企业类型：restaurant/retail/service';
COMMENT ON COLUMN enterprise_config.scale IS '企业规模';
COMMENT ON COLUMN enterprise_config.init_completed IS '初始化是否完成';
COMMENT ON COLUMN enterprise_config.custom_config IS '自定义配置（JSON）';
