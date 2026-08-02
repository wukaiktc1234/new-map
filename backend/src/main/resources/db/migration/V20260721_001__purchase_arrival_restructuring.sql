-- ============================================================
-- V20260721_001: 采购收货职责拆分与多门店到货
--
-- 变更内容：
--   1. 员工档案增加工作归属字段
--   2. 采购申请/订单明细增加收货地点字段
--   3. 新建采购到货单、收货确认单相关表
--   4. 新建系统配置表并初始化到货自动关闭天数
--
-- 注意：本次改造不保留历史测试数据，可直接修改表结构。
-- ============================================================

-- ============================================================
-- 1. 员工档案扩展
-- ============================================================
ALTER TABLE employees ADD COLUMN IF NOT EXISTS work_location_type VARCHAR(20) DEFAULT 'HEADQUARTERS'
    CHECK (work_location_type IN ('STORE', 'WAREHOUSE', 'HEADQUARTERS'));
ALTER TABLE employees ADD COLUMN IF NOT EXISTS warehouse_id BIGINT;

COMMENT ON COLUMN employees.work_location_type IS '工作归属类型：STORE 门店 / WAREHOUSE 仓库 / HEADQUARTERS 总部';
COMMENT ON COLUMN employees.warehouse_id IS '归属仓库ID';

CREATE INDEX IF NOT EXISTS idx_employees_work_location_type ON employees (work_location_type);
CREATE INDEX IF NOT EXISTS idx_employees_warehouse_id ON employees (warehouse_id);

-- ============================================================
-- 2. 采购申请扩展
-- ============================================================
ALTER TABLE purchase_request ADD COLUMN IF NOT EXISTS receiver_type VARCHAR(20);
ALTER TABLE purchase_request ADD COLUMN IF NOT EXISTS receiver_store_id VARCHAR(50);
ALTER TABLE purchase_request ADD COLUMN IF NOT EXISTS receiver_warehouse_id BIGINT;

COMMENT ON COLUMN purchase_request.receiver_type IS '申请级收货方类型：STORE / WAREHOUSE';
COMMENT ON COLUMN purchase_request.receiver_store_id IS '申请级收货门店ID';
COMMENT ON COLUMN purchase_request.receiver_warehouse_id IS '申请级收货仓库ID';

ALTER TABLE purchase_request_item ADD COLUMN IF NOT EXISTS planned_receiver_type VARCHAR(20);
ALTER TABLE purchase_request_item ADD COLUMN IF NOT EXISTS planned_store_id VARCHAR(50);
ALTER TABLE purchase_request_item ADD COLUMN IF NOT EXISTS planned_warehouse_id BIGINT;

COMMENT ON COLUMN purchase_request_item.planned_receiver_type IS '计划收货方类型：STORE / WAREHOUSE';
COMMENT ON COLUMN purchase_request_item.planned_store_id IS '计划收货门店ID';
COMMENT ON COLUMN purchase_request_item.planned_warehouse_id IS '计划收货仓库ID';

-- ============================================================
-- 3. 采购订单明细扩展
-- ============================================================
ALTER TABLE purchase_order_items ADD COLUMN IF NOT EXISTS planned_receiver_type VARCHAR(20);
ALTER TABLE purchase_order_items ADD COLUMN IF NOT EXISTS planned_store_id VARCHAR(50);
ALTER TABLE purchase_order_items ADD COLUMN IF NOT EXISTS planned_warehouse_id BIGINT;

COMMENT ON COLUMN purchase_order_items.planned_receiver_type IS '计划收货方类型：STORE / WAREHOUSE';
COMMENT ON COLUMN purchase_order_items.planned_store_id IS '计划收货门店ID';
COMMENT ON COLUMN purchase_order_items.planned_warehouse_id IS '计划收货仓库ID';

-- ============================================================
-- 4. 系统配置表
-- ============================================================
CREATE TABLE IF NOT EXISTS system_config (
    config_id     BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    config_key    VARCHAR(100) NOT NULL UNIQUE,
    config_value  VARCHAR(500) NOT NULL,
    description   VARCHAR(500),
    create_time   TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time   TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted       INTEGER NOT NULL DEFAULT 0
);

COMMENT ON TABLE system_config IS '系统配置表';

INSERT INTO system_config (config_key, config_value, description, config_type, tenant_id, encrypted, deleted)
VALUES ('arrival.auto_close.days', '7', '到货单超期自动关闭天数', 'general', 0, 0, 0)
ON CONFLICT (tenant_id, config_key) DO NOTHING;

-- ============================================================
-- 5. 采购到货单主表
-- ============================================================
CREATE TABLE IF NOT EXISTS purchase_arrivals (
    arrival_id            BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    arrival_code          VARCHAR(50)   NOT NULL,
    order_id              BIGINT        NOT NULL,
    supplier_id           BIGINT,
    receiver_type         VARCHAR(20)   NOT NULL CHECK (receiver_type IN ('STORE', 'WAREHOUSE')),
    store_id              VARCHAR(50),
    warehouse_id          BIGINT,
    shipment_status       VARCHAR(20),
    logistics_no          VARCHAR(100),
    estimated_arrival_date DATE,
    actual_arrival_date   DATE,
    total_quantity        DECIMAL(12,3) DEFAULT 0,
    total_amount          BIGINT        DEFAULT 0,
    received_quantity     DECIMAL(12,3) DEFAULT 0,
    status                INTEGER       NOT NULL DEFAULT 0,
    close_reason          VARCHAR(50),
    remark                VARCHAR(500),
    create_user_id        BIGINT,
    create_time           TIMESTAMP     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time           TIMESTAMP     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted               INTEGER       NOT NULL DEFAULT 0
);

COMMENT ON TABLE  purchase_arrivals IS '采购到货单主表';
COMMENT ON COLUMN purchase_arrivals.arrival_id IS '到货单主键ID';
COMMENT ON COLUMN purchase_arrivals.arrival_code IS '到货单编号';
COMMENT ON COLUMN purchase_arrivals.order_id IS '关联采购订单ID';
COMMENT ON COLUMN purchase_arrivals.supplier_id IS '供应商ID';
COMMENT ON COLUMN purchase_arrivals.receiver_type IS '收货方类型：STORE 门店 / WAREHOUSE 仓库';
COMMENT ON COLUMN purchase_arrivals.store_id IS '收货门店ID';
COMMENT ON COLUMN purchase_arrivals.warehouse_id IS '收货仓库ID';
COMMENT ON COLUMN purchase_arrivals.shipment_status IS '发货状态';
COMMENT ON COLUMN purchase_arrivals.logistics_no IS '物流单号';
COMMENT ON COLUMN purchase_arrivals.estimated_arrival_date IS '预计到货日期';
COMMENT ON COLUMN purchase_arrivals.actual_arrival_date IS '实际到货日期';
COMMENT ON COLUMN purchase_arrivals.total_quantity IS '到货总数量';
COMMENT ON COLUMN purchase_arrivals.total_amount IS '到货总金额（分）';
COMMENT ON COLUMN purchase_arrivals.received_quantity IS '已确认收货数量';
COMMENT ON COLUMN purchase_arrivals.status IS '到货单状态：0待收货 1收货中 2部分收货 3已收货 4已关闭';
COMMENT ON COLUMN purchase_arrivals.close_reason IS '关闭原因：manual_closed / auto_closed_overdue';

CREATE INDEX IF NOT EXISTS idx_purchase_arrivals_code             ON purchase_arrivals (arrival_code);
CREATE INDEX IF NOT EXISTS idx_purchase_arrivals_order_id         ON purchase_arrivals (order_id);
CREATE INDEX IF NOT EXISTS idx_purchase_arrivals_status           ON purchase_arrivals (status);
CREATE INDEX IF NOT EXISTS idx_purchase_arrivals_store_id         ON purchase_arrivals (store_id);
CREATE INDEX IF NOT EXISTS idx_purchase_arrivals_warehouse_id     ON purchase_arrivals (warehouse_id);
CREATE INDEX IF NOT EXISTS idx_purchase_arrivals_est_date         ON purchase_arrivals (estimated_arrival_date);

-- 补齐采购到货单实体扩展字段（兼容已执行过本迁移的环境）
ALTER TABLE purchase_arrivals ADD COLUMN IF NOT EXISTS logistics_company VARCHAR(100);
ALTER TABLE purchase_arrivals ADD COLUMN IF NOT EXISTS transport_mode    VARCHAR(20);
ALTER TABLE purchase_arrivals ADD COLUMN IF NOT EXISTS vehicle_plate_no  VARCHAR(50);
ALTER TABLE purchase_arrivals ADD COLUMN IF NOT EXISTS vehicle_type      VARCHAR(20);
ALTER TABLE purchase_arrivals ADD COLUMN IF NOT EXISTS driver_name       VARCHAR(50);
ALTER TABLE purchase_arrivals ADD COLUMN IF NOT EXISTS driver_phone      VARCHAR(20);
ALTER TABLE purchase_arrivals ADD COLUMN IF NOT EXISTS freight_amount    BIGINT DEFAULT 0;

-- ============================================================
-- 6. 采购到货单明细表
-- ============================================================
CREATE TABLE IF NOT EXISTS purchase_arrival_items (
    arrival_item_id  BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    arrival_id       BIGINT        NOT NULL,
    order_item_id    BIGINT        NOT NULL,
    material_id      BIGINT        NOT NULL,
    material_name    VARCHAR(200)  NOT NULL,
    specification    VARCHAR(200),
    unit             VARCHAR(20)   NOT NULL,
    expected_quantity DECIMAL(12,3) NOT NULL DEFAULT 0,
    received_quantity DECIMAL(12,3) NOT NULL DEFAULT 0,
    unit_price       BIGINT        NOT NULL DEFAULT 0,
    amount           BIGINT        NOT NULL DEFAULT 0,
    remark           VARCHAR(500),
    create_time      TIMESTAMP     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time      TIMESTAMP     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted          INTEGER       NOT NULL DEFAULT 0
);

COMMENT ON TABLE  purchase_arrival_items IS '采购到货单明细表';
COMMENT ON COLUMN purchase_arrival_items.arrival_id IS '关联到货单ID';
COMMENT ON COLUMN purchase_arrival_items.order_item_id IS '关联采购订单明细ID';
COMMENT ON COLUMN purchase_arrival_items.expected_quantity IS '计划到货数量';
COMMENT ON COLUMN purchase_arrival_items.received_quantity IS '已确认收货数量';

CREATE INDEX IF NOT EXISTS idx_purchase_arrival_items_arrival_id    ON purchase_arrival_items (arrival_id);
CREATE INDEX IF NOT EXISTS idx_purchase_arrival_items_order_item_id ON purchase_arrival_items (order_item_id);
CREATE INDEX IF NOT EXISTS idx_purchase_arrival_items_material_id   ON purchase_arrival_items (material_id);

-- ============================================================
-- 7. 收货确认单主表
-- ============================================================
CREATE TABLE IF NOT EXISTS receipt_confirmations (
    confirmation_id   BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    confirmation_code VARCHAR(50)   NOT NULL,
    arrival_id        BIGINT        NOT NULL,
    order_id          BIGINT        NOT NULL,
    receiver_type     VARCHAR(20)   NOT NULL CHECK (receiver_type IN ('STORE', 'WAREHOUSE')),
    store_id          VARCHAR(50),
    warehouse_id      BIGINT,
    confirm_user_id   BIGINT,
    confirm_time      TIMESTAMP     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    total_quantity    DECIMAL(12,3) DEFAULT 0,
    total_amount      BIGINT        DEFAULT 0,
    quality_check_result INTEGER,
    quality_remark    VARCHAR(500),
    status            INTEGER       NOT NULL DEFAULT 1,
    remark            VARCHAR(500),
    create_time       TIMESTAMP     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time       TIMESTAMP     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted           INTEGER       NOT NULL DEFAULT 0
);

COMMENT ON TABLE  receipt_confirmations IS '收货确认单主表';
COMMENT ON COLUMN receipt_confirmations.arrival_id IS '关联到货单ID';
COMMENT ON COLUMN receipt_confirmations.confirm_user_id IS '确认人ID';
COMMENT ON COLUMN receipt_confirmations.confirm_time IS '确认时间';
COMMENT ON COLUMN receipt_confirmations.quality_check_result IS '质检结果：1合格 2不合格';
COMMENT ON COLUMN receipt_confirmations.status IS '确认单状态：1已确认 2已拒收';

CREATE INDEX IF NOT EXISTS idx_receipt_confirmations_code             ON receipt_confirmations (confirmation_code);
CREATE INDEX IF NOT EXISTS idx_receipt_confirmations_arrival_id       ON receipt_confirmations (arrival_id);
CREATE INDEX IF NOT EXISTS idx_receipt_confirmations_order_id         ON receipt_confirmations (order_id);
CREATE INDEX IF NOT EXISTS idx_receipt_confirmations_store_id         ON receipt_confirmations (store_id);
CREATE INDEX IF NOT EXISTS idx_receipt_confirmations_warehouse_id     ON receipt_confirmations (warehouse_id);

-- ============================================================
-- 8. 收货确认单明细表
-- ============================================================
CREATE TABLE IF NOT EXISTS receipt_confirmation_items (
    confirmation_item_id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    confirmation_id      BIGINT        NOT NULL,
    arrival_item_id      BIGINT        NOT NULL,
    order_item_id        BIGINT        NOT NULL,
    material_id          BIGINT        NOT NULL,
    material_name        VARCHAR(200)  NOT NULL,
    specification        VARCHAR(200),
    unit                 VARCHAR(20)   NOT NULL,
    confirmed_quantity   DECIMAL(12,3) NOT NULL DEFAULT 0,
    rejected_quantity    DECIMAL(12,3) NOT NULL DEFAULT 0,
    unit_price           BIGINT        NOT NULL DEFAULT 0,
    amount               BIGINT        NOT NULL DEFAULT 0,
    batch_no             VARCHAR(100),
    production_date      DATE,
    expiry_date          DATE,
    remark               VARCHAR(500),
    create_time          TIMESTAMP     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time          TIMESTAMP     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted              INTEGER       NOT NULL DEFAULT 0
);

CREATE INDEX IF NOT EXISTS idx_receipt_confirmation_items_confirmation_id ON receipt_confirmation_items (confirmation_id);
CREATE INDEX IF NOT EXISTS idx_receipt_confirmation_items_arrival_item_id ON receipt_confirmation_items (arrival_item_id);
CREATE INDEX IF NOT EXISTS idx_receipt_confirmation_items_material_id     ON receipt_confirmation_items (material_id);
