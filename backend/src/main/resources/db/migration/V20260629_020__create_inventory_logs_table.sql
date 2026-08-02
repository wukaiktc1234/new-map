-- ============================================================
-- 库存日志表建表脚本
-- 对应实体: InventoryLog (inventory_log)
-- 对应初始化器: InventoryDatabaseInitializer.createInventoryLogsTable()
--
-- 注意：H2 开发环境由 InventoryDatabaseInitializer.createInventoryLogsTable 建表，
--       本脚本仅在生产环境（PostgreSQL）执行。
-- ============================================================

-- 库存日志表
CREATE TABLE IF NOT EXISTS inventory_log (
    id               BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    product_id      BIGINT,
    warehouse_id    BIGINT,
    operation_type  VARCHAR(50),
    before_stock    DECIMAL(12, 2),
    after_stock     DECIMAL(12, 2),
    change_amount   DECIMAL(12, 2),
    operator_id     BIGINT,
    remark           VARCHAR(500),
    create_time      TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time      TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted          INTEGER      NOT NULL DEFAULT 0
);

COMMENT ON TABLE  inventory_log IS '库存操作日志表';
COMMENT ON COLUMN inventory_log.id IS '日志ID（主键，自增）';
COMMENT ON COLUMN inventory_log.product_id IS '产品/物料ID';
COMMENT ON COLUMN inventory_log.warehouse_id IS '仓库ID';
COMMENT ON COLUMN inventory_log.operation_type IS '操作类型：入库/出库/调拨/盘点等';
COMMENT ON COLUMN inventory_log.before_stock IS '操作前库存';
COMMENT ON COLUMN inventory_log.after_stock IS '操作后库存';
COMMENT ON COLUMN inventory_log.change_amount IS '变动量';
COMMENT ON COLUMN inventory_log.operator_id IS '操作人ID';
COMMENT ON COLUMN inventory_log.remark IS '备注';
COMMENT ON COLUMN inventory_log.create_time IS '创建时间';
COMMENT ON COLUMN inventory_log.update_time IS '更新时间';
COMMENT ON COLUMN inventory_log.deleted IS '逻辑删除：0未删除 1已删除';

-- 普通索引：按产品/仓库/操作类型/创建时间查询
CREATE INDEX IF NOT EXISTS idx_inventory_log_product_id     ON inventory_log (product_id);
CREATE INDEX IF NOT EXISTS idx_inventory_log_warehouse_id    ON inventory_log (warehouse_id);
CREATE INDEX IF NOT EXISTS idx_inventory_log_operation_type  ON inventory_log (operation_type);
CREATE INDEX IF NOT EXISTS idx_inventory_log_create_time     ON inventory_log (create_time);
