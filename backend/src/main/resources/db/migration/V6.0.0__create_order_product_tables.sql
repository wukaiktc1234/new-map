-- =====================================================
-- 销售与订单子系统 - 数据库表结构
-- 版本: V6.0.0
-- 说明: 包含产品中心、订单管理、桌台排队、收银POS等核心表
-- =====================================================

-- -----------------------------------------------------
-- 模块A: 产品中心
-- -----------------------------------------------------

-- A1. 菜品/食品表 (foods)
CREATE TABLE IF NOT EXISTS foods (
    food_id              BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    food_code            VARCHAR(32) UNIQUE NOT NULL,
    food_name            VARCHAR(100) NOT NULL,
    category_id          BIGINT,                    -- 分类ID，关联food_categories
    specification        VARCHAR(200),              -- 规格（如"大份/中份/小份"）
    unit                 VARCHAR(20),               -- 单位（份/碗/杯/只）
    sale_price           BIGINT NOT NULL,           -- 售价（分）
    cost_price           BIGINT,                    -- 成本价（分），用于计算毛利
    stock                INTEGER DEFAULT 0,         -- 库存数量（可选，用于限量菜品）
    min_stock            INTEGER,                   -- 最低库存预警
    image_url            VARCHAR(500),               -- 菜品图片
    description          TEXT,                       -- 菜品描述
    cooking_time         INTEGER,                   -- 制作时间（分钟）
    status               INTEGER DEFAULT 1,         -- 1在售 2停售 3售罄
    is_recommend         BOOLEAN DEFAULT false,     -- 是否推荐
    is_spicy             BOOLEAN DEFAULT false,      -- 是否辣
    sort_order           INTEGER DEFAULT 0,         -- 排序权重
    create_time          TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    update_time          TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    deleted              INTEGER DEFAULT 0
);

CREATE INDEX IF NOT EXISTS idx_foods_category_id ON foods(category_id);
CREATE INDEX IF NOT EXISTS idx_foods_status ON foods(status);
CREATE UNIQUE INDEX IF NOT EXISTS uk_foods_food_code ON foods(food_code);

-- A2. 菜品分类表 (food_categories)
CREATE TABLE IF NOT EXISTS food_categories (
    category_id          BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    category_name        VARCHAR(100) NOT NULL,
    parent_id            BIGINT DEFAULT 0,           -- 父级分类ID，0表示顶级
    icon_url             VARCHAR(500),               -- 分类图标
    sort_order           INTEGER DEFAULT 0,         -- 排序权重
    status               INTEGER DEFAULT 1,         -- 1启用 2停用
    create_time          TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    update_time          TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    deleted              INTEGER DEFAULT 0
);

CREATE INDEX IF NOT EXISTS idx_food_categories_parent_id ON food_categories(parent_id);

-- A3. 套餐表 (dish_combos)
CREATE TABLE IF NOT EXISTS dish_combos (
    combo_id             BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    combo_code           VARCHAR(32) UNIQUE NOT NULL,
    combo_name           VARCHAR(100) NOT NULL,
    combo_price          BIGINT NOT NULL,            -- 套餐价格（分）
    original_price       BIGINT,                     -- 原价（各单品之和，用于显示优惠）
    discount_amount      BIGINT,                     -- 优惠金额（分）
    image_url            VARCHAR(500),
    description          TEXT,
    valid_start_date     DATE,                       -- 有效期开始
    valid_end_date       DATE,                       -- 有效期结束
    daily_limit          INTEGER,                    -- 每日限量（NULL不限）
    sold_today           INTEGER DEFAULT 0,          -- 今日已售
    status               INTEGER DEFAULT 1,          -- 1在售 2停售
    sort_order           INTEGER DEFAULT 0,
    create_time          TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    update_time          TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    deleted              INTEGER DEFAULT 0
);

CREATE UNIQUE INDEX IF NOT EXISTS uk_dish_combos_combo_code ON dish_combos(combo_code);
CREATE INDEX IF NOT EXISTS idx_dish_combos_status ON dish_combos(status);

-- A4. 套餐明细/配料表 (combo_ingredients)
CREATE TABLE IF NOT EXISTS combo_ingredients (
    ingredient_id        BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    combo_id             BIGINT NOT NULL,            -- 关联套餐
    food_id              BIGINT NOT NULL,            -- 关联的菜品
    quantity             INTEGER NOT NULL DEFAULT 1, -- 数量
    unit                 VARCHAR(20),                -- 单位
    is_required          BOOLEAN DEFAULT true,        -- 是否必选
    max_select           INTEGER,                    -- 最多可选几样
    sort_order           INTEGER DEFAULT 0,
    create_time          TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    update_time          TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    deleted              INTEGER DEFAULT 0
);

CREATE INDEX IF NOT EXISTS idx_combo_ingredients_combo_id ON combo_ingredients(combo_id);
CREATE INDEX IF NOT EXISTS idx_combo_ingredients_food_id ON combo_ingredients(food_id);

-- A5. 菜品配方/BOM表 (dish_recipes)
CREATE TABLE IF NOT EXISTS dish_recipes (
    recipe_id            BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    food_id              BIGINT NOT NULL,            -- 关联菜品
    material_id          BIGINT,                     -- 原料ID（关联仓储的物料）
    material_name        VARCHAR(100) NOT NULL,      -- 原料名称（冗余）
    specification        VARCHAR(200),               -- 规格
    required_quantity    DECIMAL(10,3) NOT NULL,     -- 标准用量
    unit                 VARCHAR(20) NOT NULL,       -- 单位
    loss_rate            DECIMAL(5,2) DEFAULT 0,     -- 损耗率(%)
    unit_cost            BIGINT,                     -- 原料单价（分）
    subtotal_cost        BIGINT,                     -- 该项成本（分）
    create_time          TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    update_time          TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    deleted              INTEGER DEFAULT 0
);

CREATE INDEX IF NOT EXISTS idx_dish_recipes_food_id ON dish_recipes(food_id);

-- -----------------------------------------------------
-- 模块B: 订单管理（核心核心！）
-- -----------------------------------------------------

-- B1. 订单主表 (orders)
CREATE TABLE IF NOT EXISTS orders (
    order_id             BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    order_code           VARCHAR(32) UNIQUE NOT NULL,   -- ORD20260425001
    order_type           INTEGER NOT NULL,               -- 1堂食 2外卖 3自提 4打包
    order_source         INTEGER DEFAULT 1,             -- 1收银台 2小程序 3第三方平台
    customer_id          BIGINT,                         -- 会员ID（NULL表示非会员）
    customer_name        VARCHAR(50),                    -- 顾客姓名（外卖必填）
    customer_phone       VARCHAR(20),                    -- 顾客电话
    table_id             BIGINT,                         -- 桌台ID（堂食必填）
    table_name           VARCHAR(50),                    -- 冗余字段方便查询
    dining_people_count  INTEGER,                        -- 用餐人数
    order_status         INTEGER DEFAULT 0,             -- 见下方状态机
    payment_status       INTEGER DEFAULT 0,             -- 0未支付 1部分支付 2已支付 3已退款
    total_amount         BIGINT NOT NULL,                -- 订单总金额（分）
    discount_amount      BIGINT DEFAULT 0,               -- 折扣优惠（分）
    coupon_amount        BIGINT DEFAULT 0,               -- 优惠券抵扣（分）
    points_amount        BIGINT DEFAULT 0,               -- 积分抵扣（分）
    delivery_fee         BIGINT DEFAULT 0,               -- 配送费（分）
    packaging_fee        BIGINT DEFAULT 0,               -- 打包费（分）
    final_amount         BIGINT NOT NULL,                -- 实付金额（分）
    paid_amount          BIGINT DEFAULT 0,               -- 已付金额（分）
    refund_amount        BIGINT DEFAULT 0,               -- 已退金额（分）
    points_earned        INTEGER DEFAULT 0,              -- 获得积分
    remark               TEXT,                            -- 备注
    cancel_reason        TEXT,                            -- 取消原因
    delivery_address     TEXT,                            -- 外卖配送地址
    expected_time        TIMESTAMP,                      -- 期望送达时间（外卖）
    actual_delivery_time TIMESTAMP,                      -- 实际送达时间
    cashier_user_id      BIGINT,                          -- 收银员ID
    create_user_id       BIGINT,
    create_time          TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    update_time          TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    deleted              INTEGER DEFAULT 0
);

-- 订单状态: 0待确认 -> 1已确认(制作中) -> 2已完成 -> 3已取消
--          特殊状态: 4部分退款 5全额退款 6待评价
CREATE INDEX IF NOT EXISTS idx_orders_order_code ON orders(order_code);
CREATE INDEX IF NOT EXISTS idx_orders_customer_id ON orders(customer_id);
CREATE INDEX IF NOT EXISTS idx_orders_table_id ON orders(table_id);
CREATE INDEX IF NOT EXISTS idx_orders_order_status ON orders(order_status);
CREATE INDEX IF NOT EXISTS idx_orders_payment_status ON orders(payment_status);
CREATE INDEX IF NOT EXISTS idx_orders_order_type ON orders(order_type);
CREATE INDEX IF NOT EXISTS idx_orders_create_time ON orders(create_time);
CREATE UNIQUE INDEX IF NOT EXISTS uk_orders_order_code ON orders(order_code);

-- B2. 订单明细表 (order_items)
CREATE TABLE IF NOT EXISTS order_items (
    item_id              BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    order_id             BIGINT NOT NULL,                -- 关联订单
    product_type         INTEGER NOT NULL,               -- 1单品 2套餐
    food_id              BIGINT,                         -- 单品ID（product_type=1时使用）
    combo_id             BIGINT,                         -- 套餐ID（product_type=2时使用）
    product_name         VARCHAR(100) NOT NULL,          -- 商品名称（冗余方便查询）
    specification        VARCHAR(200),                   -- 规格（如：大/中/小）
    unit_price           BIGINT NOT NULL,                -- 单价（分）
    quantity             INTEGER NOT NULL,                -- 数量
    amount               BIGINT NOT NULL,                -- 小计金额（分）
    discount_amount      BIGINT DEFAULT 0,               -- 单项优惠（分）
    remark               TEXT,                            -- 备注（如"不要辣"）
    kitchen_status       INTEGER DEFAULT 0,             -- 0待制作 1制作中 2已完成 3已上菜 4已退款
    create_time          TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    update_time          TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    deleted              INTEGER DEFAULT 0
);

CREATE INDEX IF NOT EXISTS idx_order_items_order_id ON order_items(order_id);
CREATE INDEX IF NOT EXISTS idx_order_items_food_id ON order_items(food_id);
CREATE INDEX IF NOT EXISTS idx_order_items_combo_id ON order_items(combo_id);
CREATE INDEX IF NOT EXISTS idx_order_items_kitchen_status ON order_items(kitchen_status);

-- B3. 支付记录表 (order_payment_records)
CREATE TABLE IF NOT EXISTS order_payment_records (
    payment_id           BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    order_id             BIGINT NOT NULL,                -- 关联订单
    payment_method       INTEGER NOT NULL,               -- 1现金 2微信 3支付宝 4银行卡 5积分 6混合支付
    payment_amount       BIGINT NOT NULL,                -- 支付金额（分）
    transaction_no       VARCHAR(100),                   -- 交易流水号
    payment_time         TIMESTAMP NOT NULL,             -- 支付时间
    operator_id          BIGINT,                          -- 操作人ID
    remark               TEXT,
    create_time          TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    update_time          TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    deleted              INTEGER DEFAULT 0
);

CREATE INDEX IF NOT EXISTS idx_order_payment_records_order_id ON order_payment_records(order_id);
CREATE INDEX IF NOT EXISTS idx_order_payment_records_payment_time ON order_payment_records(payment_time);

-- B4. 退款记录表 (order_refund_records)
CREATE TABLE IF NOT EXISTS order_refund_records (
    refund_id            BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    order_id             BIGINT NOT NULL,                -- 关联订单
    refund_type          INTEGER NOT NULL,               -- 1全额退款 2部分退款
    refund_amount        BIGINT NOT NULL,                -- 退款金额（分）
    refund_reason        VARCHAR(500),                   -- 退款原因
    refund_method        INTEGER DEFAULT 1,             -- 1原路返回 2现金 3其他
    approve_user_id      BIGINT,                          -- 审批人ID
    refund_status        INTEGER DEFAULT 0,             -- 0待审核 1已同意 2已拒绝 3已退款
    create_time          TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    complete_time        TIMESTAMP,                      -- 完成时间
    update_time          TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    deleted              INTEGER DEFAULT 0
);

CREATE INDEX IF NOT EXISTS idx_order_refund_records_order_id ON order_refund_records(order_id);
CREATE INDEX IF NOT EXISTS idx_order_refund_records_refund_status ON order_refund_records(refund_status);

-- -----------------------------------------------------
-- 模块C: 桌台与排队
-- -----------------------------------------------------

-- C1. 桌台表 (dining_tables)
CREATE TABLE IF NOT EXISTS dining_tables (
    table_id             BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    table_code           VARCHAR(32) NOT NULL,
    table_name           VARCHAR(50) NOT NULL,
    area_id              BIGINT,                          -- 区域ID
    table_type           INTEGER DEFAULT 1,             -- 1大厅 2包厢 3吧台 4户外
    seats_count          INTEGER DEFAULT 4,              -- 座位数
    min_people           INTEGER DEFAULT 1,              -- 最少建议人数
    max_people           INTEGER DEFAULT 10,             -- 最多建议人数
    status               INTEGER DEFAULT 1,             -- 1空闲 2用餐中 3预订 4维护中 5停用
    current_order_id     BIGINT,                          -- 当前关联的订单ID
    qr_code              VARCHAR(500),                   -- 桌台二维码（扫码点餐）
    sort_order           INTEGER DEFAULT 0,
    create_time          TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    update_time          TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    deleted              INTEGER DEFAULT 0
);

CREATE UNIQUE INDEX IF NOT EXISTS uk_dining_tables_table_code ON dining_tables(table_code);
CREATE INDEX IF NOT EXISTS idx_dining_tables_area_id ON dining_tables(area_id);
CREATE INDEX IF NOT EXISTS idx_dining_tables_status ON dining_tables(status);

-- C2. 预约记录表 (table_reservations)
CREATE TABLE IF NOT EXISTS table_reservations (
    reservation_id       BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    reservation_code     VARCHAR(32) UNIQUE NOT NULL,
    customer_name        VARCHAR(50) NOT NULL,
    customer_phone       VARCHAR(20) NOT NULL,
    table_id             BIGINT,                          -- 预约桌台ID
    reservation_date     DATE NOT NULL,
    reservation_time     TIME NOT NULL,
    people_count         INTEGER NOT NULL,
    deposit_amount       BIGINT DEFAULT 0,               -- 定金（分）
    status               INTEGER DEFAULT 1,             -- 1待确认 2已确认 3已到店 4已取消 5未到
    create_time          TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    confirm_time         TIMESTAMP,                      -- 确认时间
    arrive_time          TIMESTAMP,                      -- 到店时间
    cancel_time          TIMESTAMP,                      -- 取消时间
    update_time          TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    deleted              INTEGER DEFAULT 0
);

CREATE UNIQUE INDEX IF NOT EXISTS uk_table_reservations_reservation_code ON table_reservations(reservation_code);
CREATE INDEX IF NOT EXISTS idx_table_reservations_table_id ON table_reservations(table_id);
CREATE INDEX IF NOT EXISTS idx_table_reservations_reservation_date ON table_reservations(reservation_date);
CREATE INDEX IF NOT EXISTS idx_table_reservations_status ON table_reservations(status);

-- C3. 叫号队列表 (call_number_queues)
CREATE TABLE IF NOT EXISTS call_number_queues (
    queue_id             BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    store_id             BIGINT DEFAULT 1,
    ticket_number        VARCHAR(20) NOT NULL,          -- 取号号码
    queue_type           INTEGER DEFAULT 1,             -- 1堂食 2外卖 3自提
    people_count         INTEGER DEFAULT 1,
    table_preference    VARCHAR(50),                    -- 桌型偏好（大桌/小桌/包厢）
    status               INTEGER DEFAULT 1,             -- 1等待 2已叫号 3已过号 4已用餐 5已取消
    call_time            TIMESTAMP,                      -- 叫号时间
    called_count         INTEGER DEFAULT 0,             -- 叫号次数
    create_time          TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    update_time          TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    deleted              INTEGER DEFAULT 0
);

CREATE INDEX IF NOT EXISTS idx_call_number_queues_queue_type ON call_number_queues(queue_type);
CREATE INDEX IF NOT EXISTS idx_call_number_queues_status ON call_number_queues(status);
CREATE INDEX IF NOT EXISTS idx_call_number_queues_create_time ON call_number_queues(create_time);

-- -----------------------------------------------------
-- 初始化数据
-- -----------------------------------------------------

-- 为菜品分类创建部分唯一索引（本迁移先执行，不能依赖后续迁移 V20260710_001）
CREATE UNIQUE INDEX IF NOT EXISTS uk_food_categories_name_parent
    ON food_categories (category_name, parent_id)
    WHERE deleted = 0;

-- 插入默认菜品分类
-- 注意: ON CONFLICT 必须指定冲突列，否则 category_id 自增主键永远不冲突，
--       导致每次启动都插入重复记录
INSERT INTO food_categories (category_name, parent_id, sort_order, status) VALUES
('热菜', 0, 1, 1),
('凉菜', 0, 2, 1),
('主食', 0, 3, 1),
('汤类', 0, 4, 1),
('饮品', 0, 5, 1),
('甜品', 0, 6, 1)
ON CONFLICT (category_name, parent_id) WHERE deleted = 0 DO NOTHING;

-- 插入默认区域（桌台区域）
-- 注意：此表可能需要与现有系统集成

-- 添加外键约束（可选，根据实际需求决定是否启用）
-- ALTER TABLE foods ADD CONSTRAINT fk_foods_category FOREIGN KEY (category_id) REFERENCES food_categories(category_id);
-- ALTER TABLE combo_ingredients ADD CONSTRAINT fk_combo_ingredient_combo FOREIGN KEY (combo_id) REFERENCES dish_combos(combo_id);
-- ALTER TABLE combo_ingredients ADD CONSTRAINT fk_combo_ingredient_food FOREIGN KEY (food_id) REFERENCES foods(food_id);
-- ALTER TABLE order_items ADD CONSTRAINT fk_order_item_order FOREIGN KEY (order_id) REFERENCES orders(order_id);
-- ALTER TABLE order_payment_records ADD CONSTRAINT fk_payment_order FOREIGN KEY (order_id) REFERENCES orders(order_id);
-- ALTER TABLE order_refund_records ADD CONSTRAINT fk_refund_order FOREIGN KEY (order_id) REFERENCES orders(order_id);
