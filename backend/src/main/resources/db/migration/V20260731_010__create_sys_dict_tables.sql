-- ============================================================
-- 字典管理模块数据库表结构
-- 表名规范：小写+下划线
-- 主键：使用 SERIAL 自增
-- 逻辑删除：deleted 字段 (0-未删除, 1-已删除)
-- 时间字段：create_time, update_time
-- ============================================================

-- ----------------------------------------------------------
-- 1. 字典类型表 (sys_dict)
-- 用途：存储字典分类信息，如：用户状态、订单状态、商品类型等
-- ----------------------------------------------------------
CREATE TABLE IF NOT EXISTS sys_dict (
    -- 主键：使用SERIAL自增
    dict_id SERIAL PRIMARY KEY,

    -- 基本信息
    dict_name VARCHAR(100) NOT NULL DEFAULT '',
    dict_code VARCHAR(100) NOT NULL,
    dict_group VARCHAR(50) NOT NULL DEFAULT 'default',
    description VARCHAR(500) DEFAULT '',

    -- 状态控制
    status INTEGER NOT NULL DEFAULT 1,

    -- 系统字段
    is_system INTEGER NOT NULL DEFAULT 0,
    sort_order INTEGER NOT NULL DEFAULT 0,

    -- 审计字段
    create_user_id VARCHAR(64) DEFAULT '',
    create_username VARCHAR(100) DEFAULT '',
    update_user_id VARCHAR(64) DEFAULT '',
    update_username VARCHAR(100) DEFAULT '',

    -- 时间字段
    create_time TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,

    -- 逻辑删除标记（必须）
    deleted INTEGER NOT NULL DEFAULT 0,

    -- 乐观锁版本号
    version INTEGER NOT NULL DEFAULT 0
);

-- 创建索引
CREATE UNIQUE INDEX uk_sys_dict_code ON sys_dict(dict_code) WHERE deleted = 0;
CREATE INDEX idx_sys_dict_group ON sys_dict(dict_group) WHERE deleted = 0;
CREATE INDEX idx_sys_dict_status ON sys_dict(status) WHERE deleted = 0;

-- 添加表注释（PostgreSQL 风格）
COMMENT ON TABLE sys_dict IS '字典类型表';
COMMENT ON COLUMN sys_dict.dict_id IS '字典ID';
COMMENT ON COLUMN sys_dict.dict_name IS '字典名称（如：用户状态）';
COMMENT ON COLUMN sys_dict.dict_code IS '字典编码（唯一标识，如：user_status）';
COMMENT ON COLUMN sys_dict.dict_group IS '字典分组（如：system、business）';
COMMENT ON COLUMN sys_dict.description IS '字典描述';
COMMENT ON COLUMN sys_dict.status IS '状态：0-禁用，1-启用';
COMMENT ON COLUMN sys_dict.is_system IS '是否系统内置：0-否，1-是（系统内置不可删除）';
COMMENT ON COLUMN sys_dict.sort_order IS '排序序号（数值越小越靠前）';
COMMENT ON COLUMN sys_dict.create_user_id IS '创建人ID';
COMMENT ON COLUMN sys_dict.create_username IS '创建人姓名';
COMMENT ON COLUMN sys_dict.update_user_id IS '更新人ID';
COMMENT ON COLUMN sys_dict.update_username IS '更新人姓名';
COMMENT ON COLUMN sys_dict.create_time IS '创建时间';
COMMENT ON COLUMN sys_dict.update_time IS '更新时间';
COMMENT ON COLUMN sys_dict.deleted IS '逻辑删除：0-未删除，1-已删除';
COMMENT ON COLUMN sys_dict.version IS '乐观锁版本号';


-- ----------------------------------------------------------
-- 2. 字典项明细表 (sys_dict_item)
-- 用途：存储字典的具体选项值，如：启用/禁用、待处理/已完成等
-- ----------------------------------------------------------
CREATE TABLE IF NOT EXISTS sys_dict_item (
    -- 主键：使用SERIAL自增
    item_id SERIAL PRIMARY KEY,

    -- 关联字段
    dict_id INTEGER NOT NULL,

    -- 基本字段
    item_label VARCHAR(200) NOT NULL DEFAULT '',
    item_value VARCHAR(200) NOT NULL DEFAULT '',

    -- 排序与展示
    sort_order INTEGER NOT NULL DEFAULT 0,
    css_class VARCHAR(100) DEFAULT '',
    list_class VARCHAR(100) DEFAULT '',
    color_type VARCHAR(20) DEFAULT '',

    -- 默认值与状态
    is_default INTEGER NOT NULL DEFAULT 0,
    status INTEGER NOT NULL DEFAULT 1,

    -- 扩展字段
    remark VARCHAR(500) DEFAULT '',

    -- 审计字段
    create_user_id VARCHAR(64) DEFAULT '',
    create_username VARCHAR(100) DEFAULT '',
    update_user_id VARCHAR(64) DEFAULT '',
    update_username VARCHAR(100) DEFAULT '',

    -- 时间字段
    create_time TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,

    -- 逻辑删除标记（必须）
    deleted INTEGER NOT NULL DEFAULT 0,

    -- 乐观锁版本号
    version INTEGER NOT NULL DEFAULT 0,

    -- 外键约束
    CONSTRAINT fk_dict_item_dict FOREIGN KEY (dict_id) REFERENCES sys_dict(dict_id)
);

-- 创建索引
CREATE INDEX idx_sys_dict_item_dict_id ON sys_dict_item(dict_id) WHERE deleted = 0;
CREATE UNIQUE INDEX uk_sys_dict_item_value ON sys_dict_item(dict_id, item_value) WHERE deleted = 0;
CREATE INDEX idx_sys_dict_item_sort ON sys_dict_item(dict_id, sort_order) WHERE deleted = 0;
CREATE INDEX idx_sys_dict_item_status ON sys_dict_item(status) WHERE deleted = 0;

-- 添加表注释（PostgreSQL 风格）
COMMENT ON TABLE sys_dict_item IS '字典项明细表';
COMMENT ON COLUMN sys_dict_item.item_id IS '字典项ID';
COMMENT ON COLUMN sys_dict_item.dict_id IS '关联的字典类型ID';
COMMENT ON COLUMN sys_dict_item.item_label IS '字典项标签（显示文本，如：启用）';
COMMENT ON COLUMN sys_dict_item.item_value IS '字典项值（实际值，如：active/1）';
COMMENT ON COLUMN sys_dict_item.sort_order IS '排序序号（数值越小越靠前）';
COMMENT ON COLUMN sys_dict_item.css_class IS 'CSS样式类（如：el-tag--success）';
COMMENT ON COLUMN sys_dict_item.list_class IS '列表样式类（如：el-icon-check）';
COMMENT ON COLUMN sys_dict_item.color_type IS '颜色类型：primary/success/warning/danger/info';
COMMENT ON COLUMN sys_dict_item.is_default IS '是否默认项：0-否，1-是（同一字典只有一个默认项）';
COMMENT ON COLUMN sys_dict_item.status IS '状态：0-禁用，1-启用';
COMMENT ON COLUMN sys_dict_item.remark IS '备注说明';
COMMENT ON COLUMN sys_dict_item.create_user_id IS '创建人ID';
COMMENT ON COLUMN sys_dict_item.create_username IS '创建人姓名';
COMMENT ON COLUMN sys_dict_item.update_user_id IS '更新人ID';
COMMENT ON COLUMN sys_dict_item.update_username IS '更新人姓名';
COMMENT ON COLUMN sys_dict_item.create_time IS '创建时间';
COMMENT ON COLUMN sys_dict_item.update_time IS '更新时间';
COMMENT ON COLUMN sys_dict_item.deleted IS '逻辑删除：0-未删除，1-已删除';
COMMENT ON COLUMN sys_dict_item.version IS '乐观锁版本号';


-- ============================================================
-- 初始化示例数据（可选，用于测试）
-- ============================================================

-- 插入字典类型示例
INSERT INTO sys_dict (dict_name, dict_code, dict_group, description, status, is_system, sort_order) VALUES
('用户状态', 'user_status', 'system', '用户账号状态', 1, 1, 1),
('通用开关', 'common_status', 'system', '通用启用/禁用状态', 1, 1, 2),
('订单状态', 'order_status', 'business', '订单处理状态', 1, 0, 1),
('商品类型', 'product_type', 'business', '商品分类类型', 1, 0, 2);

-- 插入字典项示例 - 用户状态
INSERT INTO sys_dict_item (dict_id, item_label, item_value, sort_order, color_type, is_default, status) VALUES
(1, '正常', 'active', 1, 'success', 1, 1),
(1, '禁用', 'disabled', 2, 'danger', 0, 1),
(1, '锁定', 'locked', 3, 'warning', 0, 1);

-- 插入字典项示例 - 通用开关
INSERT INTO sys_dict_item (dict_id, item_label, item_value, sort_order, color_type, is_default, status) VALUES
(2, '启用', '1', 1, 'success', 1, 1),
(2, '禁用', '0', 2, 'info', 0, 1);

-- 插入字典项示例 - 订单状态
INSERT INTO sys_dict_item (dict_id, item_label, item_value, sort_order, color_type, is_default, status) VALUES
(3, '待支付', 'pending', 1, 'warning', 1, 1),
(3, '已支付', 'paid', 2, '', 0, 1),
(3, '已完成', 'completed', 3, 'success', 0, 1),
(3, '已取消', 'cancelled', 4, 'danger', 0, 1),
(3, '已退款', 'refunded', 5, 'info', 0, 1);
