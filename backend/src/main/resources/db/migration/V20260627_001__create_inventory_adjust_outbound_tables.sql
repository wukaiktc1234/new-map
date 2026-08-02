-- ============================================================
-- 库存调整单与出库单表结构
-- 创建时间：2026-06-27
-- 说明：P1-5 Warehouse 模块后端补齐，对应前端 inventory-adjust.ts / inventory-outbound.ts
-- 兼容：H2 (MODE=PostgreSQL) + PostgreSQL 18
-- ============================================================

-- ------------------------------------------------------------
-- 1. 库存调整单主表
-- 用途：记录盘点差异、温度损耗、称重差异等调整操作
-- 状态流转：pending → approved → completed / pending → rejected
-- ------------------------------------------------------------
CREATE TABLE IF NOT EXISTS inventory_adjust (
    adjust_id              BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    adjust_code            VARCHAR(64)  NOT NULL,
    adjust_type            VARCHAR(32)  NOT NULL,
    warehouse_id           VARCHAR(64),
    warehouse_name         VARCHAR(128),
    reference_check_code   VARCHAR(64),
    adjust_reason          VARCHAR(256),
    reference_no           VARCHAR(64),
    total_adjust_quantity  DECIMAL(18, 4),
    total_adjust_amount    BIGINT,
    status                 VARCHAR(32)  NOT NULL DEFAULT 'pending',
    participating_depts    VARCHAR(512),
    apply_user_id          VARCHAR(64),
    apply_user_name        VARCHAR(64),
    apply_time             TIMESTAMP,
    approve_user_id        VARCHAR(64),
    approve_user_name      VARCHAR(64),
    approve_time           TIMESTAMP,
    complete_time          TIMESTAMP,
    remark                 VARCHAR(512),
    create_time            TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time            TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted                INTEGER      NOT NULL DEFAULT 0
);

-- 唯一索引：调整单号
CREATE UNIQUE INDEX IF NOT EXISTS uk_inventory_adjust_code ON inventory_adjust (adjust_code);
-- 普通索引：按状态/仓库/类型查询
CREATE INDEX IF NOT EXISTS idx_inventory_adjust_status ON inventory_adjust (status);
CREATE INDEX IF NOT EXISTS idx_inventory_adjust_warehouse ON inventory_adjust (warehouse_id);
CREATE INDEX IF NOT EXISTS idx_inventory_adjust_type ON inventory_adjust (adjust_type);

-- ------------------------------------------------------------
-- 2. 库存调整单明细表
-- 用途：记录每个物料的调整前/调整/调整后数量及金额
-- ------------------------------------------------------------
CREATE TABLE IF NOT EXISTS inventory_adjust_item (
    adjust_item_id    BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    adjust_id         BIGINT       NOT NULL,
    material_id       VARCHAR(64)  NOT NULL,
    material_name     VARCHAR(128),
    specification     VARCHAR(128),
    unit              VARCHAR(32),
    before_quantity   DECIMAL(18, 4),
    adjust_quantity   DECIMAL(18, 4),
    after_quantity    DECIMAL(18, 4),
    unit_cost         BIGINT,
    adjust_amount     BIGINT,
    batch_no          VARCHAR(64),
    reason            VARCHAR(256),
    create_time       TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time       TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- 索引：按调整单ID查询明细
CREATE INDEX IF NOT EXISTS idx_inventory_adjust_item_adjust_id ON inventory_adjust_item (adjust_id);
-- 索引：按物料ID查询
CREATE INDEX IF NOT EXISTS idx_inventory_adjust_item_material ON inventory_adjust_item (material_id);

-- ------------------------------------------------------------
-- 3. 库存出库单主表
-- 用途：记录领料出库、销售出库、退货出库等操作
-- 状态流转：pending → approved → completed / pending → rejected
-- ------------------------------------------------------------
CREATE TABLE IF NOT EXISTS inventory_outbound (
    outbound_id        BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    outbound_code      VARCHAR(64)  NOT NULL,
    outbound_type      VARCHAR(32)  NOT NULL,
    warehouse_id       VARCHAR(64),
    warehouse_name     VARCHAR(128),
    target_id          VARCHAR(64),
    target_name        VARCHAR(128),
    reference_no       VARCHAR(64),
    outbound_date      DATE,
    total_quantity     DECIMAL(18, 4),
    total_amount       BIGINT,
    status             VARCHAR(32)  NOT NULL DEFAULT 'pending',
    apply_user_id      VARCHAR(64),
    apply_user_name    VARCHAR(64),
    apply_time         TIMESTAMP,
    approve_user_id    VARCHAR(64),
    approve_user_name  VARCHAR(64),
    approve_time       TIMESTAMP,
    complete_time      TIMESTAMP,
    remark             VARCHAR(512),
    create_time        TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time        TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted            INTEGER      NOT NULL DEFAULT 0
);

-- 唯一索引：出库单号
CREATE UNIQUE INDEX IF NOT EXISTS uk_inventory_outbound_code ON inventory_outbound (outbound_code);
-- 普通索引：按状态/仓库/类型/日期查询
CREATE INDEX IF NOT EXISTS idx_inventory_outbound_status ON inventory_outbound (status);
CREATE INDEX IF NOT EXISTS idx_inventory_outbound_warehouse ON inventory_outbound (warehouse_id);
CREATE INDEX IF NOT EXISTS idx_inventory_outbound_type ON inventory_outbound (outbound_type);
CREATE INDEX IF NOT EXISTS idx_inventory_outbound_date ON inventory_outbound (outbound_date);

-- ------------------------------------------------------------
-- 4. 库存出库单明细表
-- 用途：记录每个物料的申请/实际出库数量、批次号、金额
-- ------------------------------------------------------------
CREATE TABLE IF NOT EXISTS inventory_outbound_item (
    outbound_item_id   BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    outbound_id        BIGINT       NOT NULL,
    material_id        VARCHAR(64)  NOT NULL,
    material_name      VARCHAR(128),
    specification      VARCHAR(128),
    unit               VARCHAR(32),
    request_quantity   DECIMAL(18, 4),
    actual_quantity    DECIMAL(18, 4),
    batch_no           VARCHAR(64),
    unit_cost          BIGINT,
    total_cost         BIGINT,
    remark             VARCHAR(256),
    create_time        TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time        TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- 索引：按出库单ID查询明细
CREATE INDEX IF NOT EXISTS idx_inventory_outbound_item_outbound_id ON inventory_outbound_item (outbound_id);
-- 索引：按物料ID查询
CREATE INDEX IF NOT EXISTS idx_inventory_outbound_item_material ON inventory_outbound_item (material_id);
