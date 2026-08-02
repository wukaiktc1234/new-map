-- ============================================================
-- 报损单表建表脚本
-- 对应实体: InventoryLoss (inventory_losses)、InventoryLossDetail (inventory_loss_detail)
-- 对应初始化器: InventoryDatabaseInitializer.createInventoryLossesTable() / createInventoryLossDetailTable()
--
-- 注意：H2 开发环境由 InventoryDatabaseInitializer 建表，
--       本脚本仅在生产环境（PostgreSQL）执行。
-- ============================================================

-- ------------------------------------------------------------
-- 1. 报损单主表
-- ------------------------------------------------------------
CREATE TABLE IF NOT EXISTS inventory_losses (
    loss_id            BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    loss_code         VARCHAR(50)  NOT NULL,
    warehouse_id      BIGINT,
    loss_type         INTEGER,
    loss_status       INTEGER      DEFAULT 0,
    total_quantity    DECIMAL(12, 2),
    total_amount      BIGINT,
    reason            VARCHAR(500),
    create_user_id    BIGINT,
    approve_user_id   BIGINT,
    approve_time      TIMESTAMP,
    create_time       TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time       TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted           INTEGER      NOT NULL DEFAULT 0
);

COMMENT ON TABLE  inventory_losses IS '报损单表';
COMMENT ON COLUMN inventory_losses.loss_id IS '报损单ID（主键，自增）';
COMMENT ON COLUMN inventory_losses.loss_code IS '报损单号（唯一）';
COMMENT ON COLUMN inventory_losses.warehouse_id IS '仓库ID';
COMMENT ON COLUMN inventory_losses.loss_type IS '报损类型：1过期 2损坏 3丢失 4其他';
COMMENT ON COLUMN inventory_losses.loss_status IS '报损状态：0待审批 1已审批 2已处理 3已拒绝';
COMMENT ON COLUMN inventory_losses.total_quantity IS '报损总数量';
COMMENT ON COLUMN inventory_losses.total_amount IS '报损总金额（分）';
COMMENT ON COLUMN inventory_losses.reason IS '报损原因';
COMMENT ON COLUMN inventory_losses.create_user_id IS '创建人ID';
COMMENT ON COLUMN inventory_losses.approve_user_id IS '审批人ID';
COMMENT ON COLUMN inventory_losses.approve_time IS '审批时间';
COMMENT ON COLUMN inventory_losses.create_time IS '创建时间';
COMMENT ON COLUMN inventory_losses.update_time IS '更新时间';
COMMENT ON COLUMN inventory_losses.deleted IS '逻辑删除：0未删除 1已删除';

-- 唯一索引：报损单号
CREATE UNIQUE INDEX IF NOT EXISTS uk_inventory_losses_loss_code ON inventory_losses (loss_code);
-- 普通索引：按仓库/状态/类型查询
CREATE INDEX IF NOT EXISTS idx_inventory_losses_warehouse_id ON inventory_losses (warehouse_id);
CREATE INDEX IF NOT EXISTS idx_inventory_losses_loss_status  ON inventory_losses (loss_status);
CREATE INDEX IF NOT EXISTS idx_inventory_losses_loss_type    ON inventory_losses (loss_type);

-- ------------------------------------------------------------
-- 2. 报损明细表
-- 用途：记录每个物料的报损数量、单位、原因
-- 对应实体: InventoryLossDetail (inventory_loss_detail)
-- ------------------------------------------------------------
CREATE TABLE IF NOT EXISTS inventory_loss_detail (
    id               BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    loss_id         BIGINT       NOT NULL,
    product_id      BIGINT,
    quantity        DECIMAL(12, 2),
    unit            VARCHAR(20),
    reason          VARCHAR(500),
    created_at      TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at      TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP
);

COMMENT ON TABLE  inventory_loss_detail IS '报损明细表';
COMMENT ON COLUMN inventory_loss_detail.id IS '明细ID（主键，自增）';
COMMENT ON COLUMN inventory_loss_detail.loss_id IS '所属报损单ID';
COMMENT ON COLUMN inventory_loss_detail.product_id IS '产品/物料ID（对应 inventory.material_id）';
COMMENT ON COLUMN inventory_loss_detail.quantity IS '报损数量';
COMMENT ON COLUMN inventory_loss_detail.unit IS '计量单位';
COMMENT ON COLUMN inventory_loss_detail.reason IS '报损原因';
COMMENT ON COLUMN inventory_loss_detail.created_at IS '创建时间';
COMMENT ON COLUMN inventory_loss_detail.updated_at IS '更新时间';

-- 普通索引：按报损单ID/产品ID查询
CREATE INDEX IF NOT EXISTS idx_inventory_loss_detail_loss_id    ON inventory_loss_detail (loss_id);
CREATE INDEX IF NOT EXISTS idx_inventory_loss_detail_product_id  ON inventory_loss_detail (product_id);
