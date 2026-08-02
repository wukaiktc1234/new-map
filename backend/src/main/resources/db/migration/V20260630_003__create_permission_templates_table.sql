-- ============================================================
-- 权限模板表 + 4 种模式初始数据
-- 创建时间: 2026-06-30
-- 说明: 权限中心 4 种模式（集中式单店/标准连锁/大型连锁/自定义）的数据源
-- ============================================================

-- 1. 创建 permission_templates 表
CREATE TABLE IF NOT EXISTS permission_templates (
    id              INTEGER GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    name            VARCHAR(100) NOT NULL,
    code            VARCHAR(50)  NOT NULL UNIQUE,
    description     VARCHAR(500),
    enterprise_type VARCHAR(50)  NOT NULL DEFAULT 'restaurant',
    scale_range     VARCHAR(50)  NOT NULL DEFAULT 'all',
    role_config     TEXT,
    is_system       BOOLEAN      NOT NULL DEFAULT TRUE,
    status          INTEGER      NOT NULL DEFAULT 1,
    created_at      TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at      TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by      VARCHAR(32),
    updated_by      VARCHAR(32),
    deleted         INTEGER      NOT NULL DEFAULT 0
);

-- 2. 索引
CREATE INDEX IF NOT EXISTS idx_permission_templates_code ON permission_templates(code);
CREATE INDEX IF NOT EXISTS idx_permission_templates_status ON permission_templates(status);

-- 3. 注释
COMMENT ON TABLE  permission_templates IS '权限模板表（4 种模式：集中式单店/标准连锁/大型连锁/自定义）';
COMMENT ON COLUMN permission_templates.name            IS '模板名称';
COMMENT ON COLUMN permission_templates.code            IS '模板编码（centralized-single/standard-chain/large-chain/custom）';
COMMENT ON COLUMN permission_templates.description     IS '模板描述';
COMMENT ON COLUMN permission_templates.enterprise_type IS '适用企业类型（restaurant-餐饮/retail-零售/service-服务/all-通用）';
COMMENT ON COLUMN permission_templates.scale_range     IS '适用规模范围（1-20/20-200/200+/all）';
COMMENT ON COLUMN permission_templates.role_config     IS '角色配置（JSON：{admin:[...], hr_director:[...], ...}）';
COMMENT ON COLUMN permission_templates.is_system       IS '是否系统模板';
COMMENT ON COLUMN permission_templates.status         IS '状态（1-启用/0-禁用）';

-- ============================================================
-- 4. 插入 4 种模式初始数据
-- role_config JSON 结构：{ "admin": ["域1","域2",...], "owner": [...], "hr_director": [...], ... }
-- 14 个业务域：workspace/store-ops/product/order/operations/purchase/warehouse/member/finance/hr/traceability/device/asset/system
-- ============================================================

-- 4.1 集中式单店模式（小型餐饮，单店，5-50人，年营收50万-500万）
-- 核心：工作台/门店运营/产品/订单/运营/会员/溯源/设备/系统
-- 隐藏：采购/仓储/财务/HR/资产（对单店非核心，可用简化版）
INSERT INTO permission_templates (name, code, description, enterprise_type, scale_range, role_config, is_system, status, created_by)
VALUES (
    '集中式单店模式',
    'centralized-single',
    '适用于单店小型餐饮企业（5-50人，年营收50万-500万），聚焦核心营业功能，简化后台管理',
    'restaurant',
    '1-20',
    '{"admin":["workspace","store-ops","product","order","operations","member","traceability","device","system"],"owner":["workspace","store-ops","product","order","operations","member","traceability","device","system"],"hr_director":["workspace","hr"],"finance_director":["workspace"],"ops_director":["workspace","store-ops","order","operations"],"employee":["workspace"]}',
    TRUE,
    1,
    'system-init'
) ON CONFLICT (code) DO NOTHING;

-- 4.2 标准连锁模式（2-5家门店，5-50人，年营收500万-2000万）
-- 全部 14 个业务域开放，但财务/HR 角色权限受限
INSERT INTO permission_templates (name, code, description, enterprise_type, scale_range, role_config, is_system, status, created_by)
VALUES (
    '标准连锁模式',
    'standard-chain',
    '适用于2-5家门店的小型连锁餐饮（5-50人，年营收500万-2000万），全业务域开放，支持完整运营管理',
    'restaurant',
    '20-200',
    '{"admin":["workspace","store-ops","product","order","operations","purchase","warehouse","member","finance","hr","traceability","device","asset","system"],"owner":["workspace","store-ops","product","order","operations","purchase","warehouse","member","finance","hr","traceability","device","asset","system"],"hr_director":["workspace","hr"],"finance_director":["workspace","finance"],"ops_director":["workspace","store-ops","order","operations","purchase","warehouse"],"employee":["workspace"]}',
    TRUE,
    1,
    'system-init'
) ON CONFLICT (code) DO NOTHING;

-- 4.3 大型连锁模式（多门店、复杂组织、年营收2000万+）
-- 全部 14 个业务域开放，所有角色权限完整
INSERT INTO permission_templates (name, code, description, enterprise_type, scale_range, role_config, is_system, status, created_by)
VALUES (
    '大型连锁模式',
    'large-chain',
    '适用于多门店大型连锁餐饮（200+人，年营收2000万+），全功能开放，支持复杂组织架构与精细化管理',
    'restaurant',
    '200+',
    '{"admin":["workspace","store-ops","product","order","operations","purchase","warehouse","member","finance","hr","traceability","device","asset","system"],"owner":["workspace","store-ops","product","order","operations","purchase","warehouse","member","finance","hr","traceability","device","asset","system"],"hr_director":["workspace","hr"],"finance_director":["workspace","finance","asset"],"ops_director":["workspace","store-ops","order","operations","purchase","warehouse","member","traceability"],"employee":["workspace"]}',
    TRUE,
    1,
    'system-init'
) ON CONFLICT (code) DO NOTHING;

-- 4.4 自定义模式（admin 全开，由用户自行调整）
INSERT INTO permission_templates (name, code, description, enterprise_type, scale_range, role_config, is_system, status, created_by)
VALUES (
    '自定义',
    'custom',
    '完全自定义权限配置，admin 默认可见所有模块，其他角色权限需用户在「域权限配置」中手动编辑',
    'all',
    'all',
    '{"admin":["workspace","store-ops","product","order","operations","purchase","warehouse","member","finance","hr","traceability","device","asset","system"],"owner":["workspace","store-ops","product","order","operations","purchase","warehouse","member","finance","hr","traceability","device","asset","system"],"hr_director":["workspace","hr"],"finance_director":["workspace","finance"],"ops_director":["workspace","store-ops","order","operations"],"employee":["workspace"]}',
    TRUE,
    1,
    'system-init'
) ON CONFLICT (code) DO NOTHING;
