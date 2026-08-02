-- ============================================================
-- 资产管理与门店运营系统 - 数据库表结构
-- 版本: V9.0.0
-- 日期: 2026-04-25
-- 说明: 创建资产管理(6张表) + 门店运营(5张表) 共11张表
-- ============================================================

-- ------------------------------------------------------------
-- A. 资产管理模块
-- ------------------------------------------------------------

-- 1. 资产分类表 (asset_categories)
CREATE TABLE IF NOT EXISTS asset_categories (
    category_id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    category_code VARCHAR(32) NOT NULL,           -- 分类编码
    category_name VARCHAR(100) NOT NULL,          -- 分类名称
    parent_id BIGINT DEFAULT 0,                   -- 父级ID（0表示顶级）
    depreciation_method INTEGER DEFAULT 1,       -- 折旧方法：1直线法 2年数总和法 3工作量法
    useful_life_years INTEGER DEFAULT 5,          -- 使用年限（年）
    residual_rate DECIMAL(5,4) DEFAULT 0.05,      -- 净残值率（默认5%）
    sort_order INTEGER DEFAULT 0,                 -- 排序号
    status INTEGER DEFAULT 1,                     -- 状态：1启用 0禁用
    remark TEXT,                                  -- 备注
    create_time TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted INTEGER NOT NULL DEFAULT 0            -- 逻辑删除
);

CREATE UNIQUE INDEX uk_asset_categories_code ON asset_categories(category_code) WHERE deleted = 0;
CREATE INDEX idx_asset_categories_parent ON asset_categories(parent_id);

-- 注释
COMMENT ON TABLE asset_categories IS '资产分类表';
COMMENT ON COLUMN asset_categories.category_id IS '分类ID';
COMMENT ON COLUMN asset_categories.category_code IS '分类编码';
COMMENT ON COLUMN asset_categories.category_name IS '分类名称';
COMMENT ON COLUMN asset_categories.parent_id IS '父级ID（0表示顶级）';
COMMENT ON COLUMN asset_categories.depreciation_method IS '折旧方法：1直线法 2年数总和法 3工作量法';
COMMENT ON COLUMN asset_categories.useful_life_years IS '使用年限（年）';
COMMENT ON COLUMN asset_categories.residual_rate IS '净残值率';
COMMENT ON COLUMN asset_categories.sort_order IS '排序号';


-- 2. 资产主数据表 (asset_masters) - 增强版
CREATE TABLE IF NOT EXISTS asset_masters_enhanced (
    asset_id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    asset_code VARCHAR(32) UNIQUE NOT NULL,        -- 资产编号（唯一）
    asset_name VARCHAR(100) NOT NULL,             -- 资产名称
    category_id BIGINT REFERENCES asset_categories(category_id),  -- 分类ID
    specification VARCHAR(200),                   -- 规格型号
    brand VARCHAR(100),                           -- 品牌
    purchase_date DATE,                           -- 购置日期
    original_cost BIGINT NOT NULL,                -- 原值（分）
    accumulated_depreciation BIGINT DEFAULT 0,    -- 累计折旧（分）
    net_book_value BIGINT,                        -- 净值 = 原值 - 累计折旧
    useful_life_months INTEGER,                   -- 使用月数
    remaining_months INTEGER,                     -- 剩余使用月数
    depreciation_method INTEGER DEFAULT 1,       -- 折旧方法：1直线法 2年数总和法 3工作量法
    status INTEGER DEFAULT 1,                     -- 状态：1在用 2闲置 3维修中 4已报废 5已处置
    location VARCHAR(200),                        -- 存放位置
    responsible_user_id BIGINT,                   -- 责任人ID
    store_id BIGINT,                              -- 所属门店ID
    supplier_info TEXT,                           -- 供应商信息
    warranty_expiry DATE,                         -- 保修到期日
    next_maintenance_date DATE,                   -- 下次保养日期
    qr_code VARCHAR(100),                         -- 二维码（用于盘点扫码）
    image_url VARCHAR(500),                       -- 资产照片URL
    remark TEXT,                                  -- 备注
    create_time TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted INTEGER NOT NULL DEFAULT 0            -- 逻辑删除
);

CREATE INDEX idx_asset_masters_category ON asset_masters_enhanced(category_id);
CREATE INDEX idx_asset_masters_store ON asset_masters_enhanced(store_id);
CREATE INDEX idx_asset_masters_status ON asset_masters_enhanced(status);
CREATE INDEX idx_asset_masters_code ON asset_masters_enhanced(asset_code);

COMMENT ON TABLE asset_masters_enhanced IS '资产主数据表（增强版）';
COMMENT ON COLUMN asset_masters_enhanced.asset_id IS '资产ID';
COMMENT ON COLUMN asset_masters_enhanced.asset_code IS '资产编号';
COMMENT ON COLUMN asset_masters_enhanced.original_cost IS '原值（分）';
COMMENT ON COLUMN asset_masters_enhanced.accumulated_depreciation IS '累计折旧（分）';
COMMENT ON COLUMN asset_masters_enhanced.net_book_value IS '净值（分）';
COMMENT ON COLUMN asset_masters_enhanced.status IS '状态：1在用 2闲置 3维修中 4已报废 5已处置';


-- 3. 资产变动记录表 (asset_flow_records)
CREATE TABLE IF NOT EXISTS asset_flow_records (
    flow_id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    asset_id BIGINT NOT NULL,                     -- 资产ID
    flow_type INTEGER NOT NULL,                   -- 变动类型：1购置 2折旧 3调拨 4维修 5报废 6处置 7盘点盈余 8盘点亏损
    before_value BIGINT DEFAULT 0,                -- 变动前值（分）
    after_value BIGINT DEFAULT 0,                 -- 变动后值（分）
    change_amount BIGINT DEFAULT 0,               -- 变动金额（分）
    reference_no VARCHAR(64),                     -- 关联单据号
    flow_date DATE NOT NULL,                      -- 变动日期
    operator_id BIGINT,                           -- 操作人ID
    remark TEXT,                                  -- 备注
    create_time TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted INTEGER NOT NULL DEFAULT 0            -- 逻辑删除
);

CREATE INDEX idx_asset_flow_records_asset ON asset_flow_records(asset_id);
CREATE INDEX idx_asset_flow_records_type ON asset_flow_records(flow_type);
CREATE INDEX idx_asset_flow_records_date ON asset_flow_records(flow_date);

COMMENT ON TABLE asset_flow_records IS '资产变动记录表';
COMMENT ON COLUMN asset_flow_records.flow_type IS '变动类型：1购置 2折旧 3调拨 4维修 5报废 6处置 7盘点盈余 8盘点亏损';


-- 4. 折旧记录表 (asset_depreciation_records)
CREATE TABLE IF NOT EXISTS asset_depreciation_records (
    record_id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    asset_id BIGINT NOT NULL,                     -- 资产ID
    period VARCHAR(7) NOT NULL,                   -- 折旧期间（格式：YYYY-MM）
    original_cost BIGINT NOT NULL,                -- 原值（分）
    this_period_depreciation BIGINT NOT NULL,     -- 本期折旧额（分）
    accumulated_depreciation BIGINT NOT NULL,     -- 累计折旧（分）
    net_book_value_after BIGINT NOT NULL,         -- 折旧后净值（分）
    create_time TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted INTEGER NOT NULL DEFAULT 0            -- 逻辑删除
);

CREATE UNIQUE INDEX uk_asset_depreciation_record ON asset_depreciation_records(asset_id, period) WHERE deleted = 0;
CREATE INDEX idx_asset_depreciation_records_period ON asset_depreciation_records(period);

COMMENT ON TABLE asset_depreciation_records IS '资产折旧记录表';
COMMENT ON COLUMN asset_depreciation_records.period IS '折旧期间（YYYY-MM）';
COMMENT ON COLUMN asset_depreciation_records.this_period_depreciation IS '本期折旧额（分）';


-- 5. 资产盘点表 (inventory_check_assets)
CREATE TABLE IF NOT EXISTS inventory_check_assets (
    check_id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    check_code VARCHAR(32) UNIQUE NOT NULL,        -- 盘点单号
    check_type INTEGER DEFAULT 1,                  -- 盘点类型：1全面 2抽样 3抽查
    check_date DATE NOT NULL,                      -- 盘点日期
    status INTEGER DEFAULT 0,                      -- 状态：0待盘 1盘中 2已审核 3已完成
    total_assets_count INTEGER DEFAULT 0,          -- 应盘资产总数
    actual_count INTEGER DEFAULT 0,                -- 实盘数量
    diff_count INTEGER DEFAULT 0,                  -- 差异数量
    create_user_id BIGINT,                         -- 创建人ID
    approve_user_id BIGINT,                        -- 审核人ID
    approve_time TIMESTAMP,                        -- 审核时间
    remark TEXT,                                   -- 备注
    create_time TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted INTEGER NOT NULL DEFAULT 0             -- 逻辑删除
);

CREATE INDEX idx_inventory_check_assets_status ON inventory_check_assets(status);
CREATE INDEX idx_inventory_check_assets_date ON inventory_check_assets(check_date);

COMMENT ON TABLE inventory_check_assets IS '资产盘点表';
COMMENT ON COLUMN inventory_check_assets.check_type IS '盘点类型：1全面 2抽样 3抽查';
COMMENT ON COLUMN inventory_check_assets.status IS '状态：0待盘 1盘中 2已审核 3已完成';


-- 6. 盘点明细表 (inventory_check_asset_items)
CREATE TABLE IF NOT EXISTS inventory_check_asset_items (
    item_id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    check_id BIGINT NOT NULL,                      -- 盘点单ID
    asset_id BIGINT NOT NULL,                      -- 资产ID
    expected_location VARCHAR(200),                -- 账面位置
    actual_location VARCHAR(200),                  -- 实际位置
    expected_status INTEGER,                       -- 账面状态
    actual_status INTEGER,                         -- 实际状态
    condition_rating INTEGER DEFAULT 3,            -- 成色评级：1优 2良 3中 4差 5报废
    remark TEXT,                                   -- 备注
    photo_evidence VARCHAR(500),                   -- 照片证据
    create_time TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted INTEGER NOT NULL DEFAULT 0             -- 逻辑删除
);

CREATE INDEX idx_inventory_check_items_check ON inventory_check_asset_items(check_id);
CREATE INDEX idx_inventory_check_items_asset ON inventory_check_asset_items(asset_id);

COMMENT ON TABLE inventory_check_asset_items IS '资产盘点明细表';
COMMENT ON COLUMN inventory_check_asset_items.condition_rating IS '成色评级：1优 2良 3中 4差 5报废';


-- ------------------------------------------------------------
-- B. 门店运营模块
-- ------------------------------------------------------------

-- 7. 门店表 (stores_new) - 增强版
CREATE TABLE IF NOT EXISTS stores_new (
    store_id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    store_code VARCHAR(32) UNIQUE NOT NULL,         -- 门店编码（唯一）
    store_name VARCHAR(100) NOT NULL,              -- 门店名称
    store_type INTEGER DEFAULT 1,                  -- 门店类型：1直营 2加盟 3合作
    address TEXT,                                   -- 地址
    phone VARCHAR(20),                              -- 电话
    business_hours_start TIME,                     -- 营业开始时间
    business_hours_end TIME,                       -- 营业结束时间
    area_size DECIMAL(10,2),                       -- 面积（平方米）
    manager_id BIGINT,                             -- 店长ID
    open_date DATE,                                -- 开业日期
    status INTEGER DEFAULT 1,                      -- 状态：1营业中 2装修中 3暂停营业 4已关闭
    license_no VARCHAR(100),                       -- 食品经营许可证号
    license_expiry DATE,                           -- 许可证到期日
    config_json JSONB,                             -- 门店配置（桌台数量/收银机等）
    remark TEXT,                                   -- 备注
    create_time TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted INTEGER NOT NULL DEFAULT 0             -- 逻辑删除
);

CREATE INDEX idx_stores_new_status ON stores_new(status);
CREATE INDEX idx_stores_new_type ON stores_new(store_type);

COMMENT ON TABLE stores_new IS '门店表（增强版）';
COMMENT ON COLUMN stores_new.store_type IS '门店类型：1直营 2加盟 3合作';
COMMENT ON COLUMN stores_new.status IS '状态：1营业中 2装修中 3暂停营业 4已关闭';
COMMENT ON COLUMN stores_new.config_json IS '门店配置JSON（桌台数量/收银机等）';


-- 8. 交接班记录表 (shift_records)
CREATE TABLE IF NOT EXISTS shift_records (
    shift_id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    shift_code VARCHAR(32) UNIQUE NOT NULL,         -- 交接班单号
    store_id BIGINT NOT NULL,                       -- 门店ID
    shift_type INTEGER DEFAULT 1,                   -- 班次类型：1早班 2中班 3晚班 4通宵
    cashier_user_id BIGINT,                         -- 收银员ID
    start_time TIMESTAMP NOT NULL,                  -- 开班时间
    end_time TIMESTAMP,                             -- 交班时间
    opening_cash BIGINT DEFAULT 0,                  -- 开机金额（分）
    closing_cash BIGINT DEFAULT 0,                  -- 交班金额（分）
    sales_amount BIGINT DEFAULT 0,                  -- 销售额（分）
    order_count INTEGER DEFAULT 0,                  -- 订单数
    refund_amount BIGINT DEFAULT 0,                 -- 退款额（分）
    difference_amount BIGINT DEFAULT 0,             -- 差异金额（分）
    handover_to_user_id BIGINT,                     -- 交接给谁
    handover_remark TEXT,                           -- 交接备注
    status INTEGER DEFAULT 1,                       -- 状态：1交接中 2已完成 3异常
    create_time TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted INTEGER NOT NULL DEFAULT 0              -- 逻辑删除
);

CREATE INDEX idx_shift_records_store ON shift_records(store_id);
CREATE INDEX idx_shift_records_date ON shift_records(start_time);
CREATE INDEX idx_shift_records_status ON shift_records(status);

COMMENT ON TABLE shift_records IS '交接班记录表';
COMMENT ON COLUMN shift_records.shift_type IS '班次类型：1早班 2中班 3晚班 4通宵';
COMMENT ON COLUMN shift_records.opening_cash IS '开机金额（分）';
COMMENT ON COLUMN shift_records.sales_amount IS '销售额（分）';
COMMENT ON COLUMN shift_records.status IS '状态：1交接中 2已完成 3异常';


-- 9. 门店公告表 (store_announcements)
CREATE TABLE IF NOT EXISTS store_announcements (
    announcement_id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    store_id BIGINT NOT NULL,                       -- 门店ID
    title VARCHAR(200) NOT NULL,                    -- 公告标题
    content TEXT NOT NULL,                          -- 公告内容
    announcement_type INTEGER DEFAULT 1,            -- 公告类型：1通知 2活动 3警告 4其他
    publish_user_id BIGINT,                         -- 发布人ID
    publish_time TIMESTAMP,                         -- 发布时间
    priority INTEGER DEFAULT 2,                     -- 优先级：1低 2中 3高 4紧急
    is_top BOOLEAN DEFAULT false,                   -- 是否置顶
    target_roles JSONB,                             -- 目标角色JSON ["all","cashier","manager"]
    read_count INTEGER DEFAULT 0,                   -- 阅读次数
    expiry_time TIMESTAMP,                          -- 过期时间
    status INTEGER DEFAULT 1,                       -- 状态：1草稿 2已发布 3已撤回
    create_time TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted INTEGER NOT NULL DEFAULT 0              -- 逻辑删除
);

CREATE INDEX idx_announcements_store ON store_announcements(store_id);
CREATE INDEX idx_announcements_status ON store_announcements(status);
CREATE INDEX idx_announcements_top ON store_announcements(is_top);

COMMENT ON TABLE store_announcements IS '门店公告表';
COMMENT ON COLUMN store_announcements.announcement_type IS '公告类型：1通知 2活动 3警告 4其他';
COMMENT ON COLUMN store_announcements.priority IS '优先级：1低 2中 3高 4紧急';
COMMENT ON COLUMN store_announcements.target_roles IS '目标角色JSON';


-- 10. 开闭店记录表 (open_close_store_records)
CREATE TABLE IF NOT EXISTS open_close_store_records (
    record_id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    store_id BIGINT NOT NULL,                       -- 门店ID
    record_type INTEGER NOT NULL,                   -- 记录类型：1开店 2闭店
    operator_id BIGINT,                             -- 操作人ID
    operate_time TIMESTAMP NOT NULL,                -- 操作时间
    opening_cash_count BIGINT,                      -- 开机现金（分）
    inventory_check_result TEXT,                    -- 库存检查结果
    equipment_check_result TEXT,                    -- 设备检查结果
    safety_check_result TEXT,                       -- 安全检查结果
    abnormal_items JSONB,                           -- 异常项JSON
    remark TEXT,                                    -- 备注
    create_time TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted INTEGER NOT NULL DEFAULT 0              -- 逻辑删除
);

CREATE INDEX idx_open_close_store ON open_close_store_records(store_id);
CREATE INDEX idx_open_close_type ON open_close_store_records(record_type);
CREATE INDEX idx_open_close_time ON open_close_store_records(operate_time);

COMMENT ON TABLE open_close_store_records IS '开闭店记录表';
COMMENT ON COLUMN open_close_store_records.record_type IS '记录类型：1开店 2闭店';
COMMENT ON COLUMN open_close_store_records.abnormal_items IS '异常项JSON';


-- 11. 叫号记录表 (call_number_records)
CREATE TABLE IF NOT EXISTS call_number_records (
    call_record_id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    store_id BIGINT NOT NULL,                       -- 门店ID
    ticket_number VARCHAR(20) NOT NULL,             -- 取号号码
    queue_type INTEGER DEFAULT 1,                   -- 排队类型：1堂食 2外卖 3自提
    customer_count INTEGER DEFAULT 1,               -- 用餐人数
    table_number VARCHAR(20),                       -- 桌号
    call_time TIMESTAMP,                            -- 叫号时间
    served_time TIMESTAMP,                          -- 用餐时间
    cancel_time TIMESTAMP,                          -- 取消时间
    status INTEGER DEFAULT 1,                       -- 状态：1等待 2已叫号 3已用餐 4已取消
    create_time TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted INTEGER NOT NULL DEFAULT 0              -- 逻辑删除
);

CREATE INDEX idx_call_number_store ON call_number_records(store_id);
CREATE INDEX idx_call_number_status ON call_number_records(status);
CREATE INDEX idx_call_number_time ON call_number_records(call_time);

COMMENT ON TABLE call_number_records IS '叫号记录表';
COMMENT ON COLUMN call_number_records.queue_type IS '排队类型：1堂食 2外卖 3自提';
COMMENT ON COLUMN call_number_records.status IS '状态：1等待 2已叫号 3已用餐 4已取消';


-- ============================================================
-- 初始化数据
-- ============================================================

-- 插入默认资产分类数据
INSERT INTO asset_categories (category_code, category_name, parent_id, depreciation_method, useful_life_years, residual_rate, sort_order, status) VALUES
('ASSET-001', '电子设备', 0, 1, 5, 0.05, 1, 1),
('ASSET-002', '厨房设备', 0, 1, 10, 0.05, 2, 1),
('ASSET-003', '家具类', 0, 1, 5, 0.05, 3, 1),
('ASSET-004', '运输工具', 0, 2, 8, 0.05, 4, 1),
('ASSET-005', '电脑及外设', 1, 1, 3, 0.05, 1, 1),
('ASSET-006', '冷藏设备', 2, 1, 8, 0.05, 1, 1),
('ASSET-007', '烹饪设备', 2, 1, 10, 0.05, 2, 1);


-- 输出完成信息
SELECT 'V9.0.0__create_asset_store_tables.sql 执行成功！' AS message,
       '创建11张表：资产6张 + 门店运营5张' AS details;
