-- ============================================================
-- POS 历史订单表拆分
-- 说明：orders/order_items 已被新订单核心系统（OrderNew/OrderItemNew）使用，
--       旧 POS 模块仍依赖字段不同的 Order/OrderItem 实体。为避免数据模型冲突，
--       将旧实体迁移到独立的 legacy 表，保证两套系统互不影响。
-- ============================================================

-- 旧 POS 订单主表
CREATE TABLE IF NOT EXISTS orders_legacy (
    order_id                VARCHAR(50)  PRIMARY KEY,
    user_id                 VARCHAR(50),
    order_number            VARCHAR(50)  NOT NULL UNIQUE,
    order_type              SMALLINT     DEFAULT 0,
    order_status            SMALLINT     DEFAULT 0,
    order_amount            DECIMAL(12,2) DEFAULT 0,
    discount_amount         DECIMAL(12,2) DEFAULT 0,
    actual_amount           DECIMAL(12,2) DEFAULT 0,
    payment_method          SMALLINT     DEFAULT 0,
    transaction_id          VARCHAR(100),
    payment_time            TIMESTAMP,
    delivery_address        VARCHAR(500),
    contact_name            VARCHAR(100),
    contact_phone           VARCHAR(20),
    remarks                 VARCHAR(500),
    idempotency_key         VARCHAR(128),
    order_source            SMALLINT     DEFAULT 0,
    merchant_id             VARCHAR(50),
    deliveryman_id          VARCHAR(50),
    estimated_delivery_time TIMESTAMP,
    actual_delivery_time    TIMESTAMP,
    cancel_reason           VARCHAR(500),
    refund_reason           VARCHAR(500),
    refund_amount           DECIMAL(12,2) DEFAULT 0,
    refund_time             TIMESTAMP,
    create_time             TIMESTAMP    DEFAULT CURRENT_TIMESTAMP,
    update_time             TIMESTAMP    DEFAULT CURRENT_TIMESTAMP,
    create_by               VARCHAR(50),
    update_by               VARCHAR(50),
    deleted                 SMALLINT     DEFAULT 0
);

CREATE INDEX IF NOT EXISTS idx_orders_legacy_order_number ON orders_legacy(order_number);
CREATE INDEX IF NOT EXISTS idx_orders_legacy_user_id ON orders_legacy(user_id);
CREATE INDEX IF NOT EXISTS idx_orders_legacy_order_status ON orders_legacy(order_status);
CREATE INDEX IF NOT EXISTS idx_orders_legacy_create_time ON orders_legacy(create_time);
CREATE INDEX IF NOT EXISTS idx_orders_legacy_deleted ON orders_legacy(deleted);

-- 旧 POS 订单明细表
CREATE TABLE IF NOT EXISTS order_items_legacy (
    order_item_id           VARCHAR(50)  PRIMARY KEY,
    order_id                VARCHAR(50)  NOT NULL,
    food_id                 VARCHAR(50),
    food_name               VARCHAR(100),
    unit_price              DECIMAL(12,2) DEFAULT 0,
    quantity                INT          DEFAULT 0,
    subtotal_amount         DECIMAL(12,2) DEFAULT 0,
    food_image_url          VARCHAR(255),
    specification           VARCHAR(100),
    create_time             TIMESTAMP    DEFAULT CURRENT_TIMESTAMP,
    update_time             TIMESTAMP    DEFAULT CURRENT_TIMESTAMP,
    deleted                 SMALLINT     DEFAULT 0
);

CREATE INDEX IF NOT EXISTS idx_order_items_legacy_order_id ON order_items_legacy(order_id);
CREATE INDEX IF NOT EXISTS idx_order_items_legacy_food_id ON order_items_legacy(food_id);
CREATE INDEX IF NOT EXISTS idx_order_items_legacy_deleted ON order_items_legacy(deleted);
