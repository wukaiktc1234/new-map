-- ============================================================
-- 调拨表建表脚本
-- 对应实体: InventoryTransfer (inventory_transfers)
-- 对应初始化器: InventoryDatabaseInitializer.createInventoryTransfersTable()
--
-- 注意：H2 开发环境由 InventoryDatabaseInitializer.createInventoryTransfersTable 建表，
--       本脚本仅在生产环境（PostgreSQL）执行。
-- ============================================================

-- 调拨表
CREATE TABLE IF NOT EXISTS inventory_transfers (
    transfer_id           BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    transfer_code        VARCHAR(50)  NOT NULL,
    from_warehouse_id    BIGINT,
    to_warehouse_id      BIGINT,
    transfer_status      INTEGER      DEFAULT 0,
    transfer_date        DATE,
    receive_date         DATE,
    total_quantity       DECIMAL(12, 2),
    remark               VARCHAR(500),
    create_user_id      BIGINT,
    product_id          BIGINT,
    product_name        VARCHAR(200),
    from_warehouse_name VARCHAR(100),
    to_warehouse_name   VARCHAR(100),
    transfer_quantity   DECIMAL(12, 2),
    create_time         TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time         TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted             INTEGER      NOT NULL DEFAULT 0
);

COMMENT ON TABLE  inventory_transfers IS '库存调拨表';
COMMENT ON COLUMN inventory_transfers.transfer_id IS '调拨单ID（主键，自增）';
COMMENT ON COLUMN inventory_transfers.transfer_code IS '调拨单号（唯一）';
COMMENT ON COLUMN inventory_transfers.from_warehouse_id IS '调出仓库ID';
COMMENT ON COLUMN inventory_transfers.to_warehouse_id IS '调入仓库ID';
COMMENT ON COLUMN inventory_transfers.transfer_status IS '调拨状态：0待审批 1已审批 2已完成 3已拒绝';
COMMENT ON COLUMN inventory_transfers.transfer_date IS '调拨日期';
COMMENT ON COLUMN inventory_transfers.receive_date IS '接收日期';
COMMENT ON COLUMN inventory_transfers.total_quantity IS '总数量';
COMMENT ON COLUMN inventory_transfers.remark IS '备注';
COMMENT ON COLUMN inventory_transfers.create_user_id IS '创建人ID';
COMMENT ON COLUMN inventory_transfers.product_id IS '产品ID（兼容旧版单条调拨）';
COMMENT ON COLUMN inventory_transfers.product_name IS '产品名称';
COMMENT ON COLUMN inventory_transfers.from_warehouse_name IS '调出仓名（冗余）';
COMMENT ON COLUMN inventory_transfers.to_warehouse_name IS '调入仓名（冗余）';
COMMENT ON COLUMN inventory_transfers.transfer_quantity IS '调拨数量';
COMMENT ON COLUMN inventory_transfers.create_time IS '创建时间';
COMMENT ON COLUMN inventory_transfers.update_time IS '更新时间';
COMMENT ON COLUMN inventory_transfers.deleted IS '逻辑删除：0未删除 1已删除';

-- 唯一索引：调拨单号
CREATE UNIQUE INDEX IF NOT EXISTS uk_inventory_transfers_transfer_code ON inventory_transfers (transfer_code);
-- 普通索引：按调出/调入仓库/状态查询
CREATE INDEX IF NOT EXISTS idx_inventory_transfers_from_warehouse_id ON inventory_transfers (from_warehouse_id);
CREATE INDEX IF NOT EXISTS idx_inventory_transfers_to_warehouse_id   ON inventory_transfers (to_warehouse_id);
CREATE INDEX IF NOT EXISTS idx_inventory_transfers_transfer_status   ON inventory_transfers (transfer_status);
