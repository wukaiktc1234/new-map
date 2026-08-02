-- ============================================================
-- 仓库主表建表脚本
-- 对应实体: Warehouse (warehouses)
-- 对应初始化器: InventoryDatabaseInitializer.createWarehousesTable()
--
-- 注意：H2 开发环境由 InventoryDatabaseInitializer.createWarehousesTable 建表，
--       本脚本仅在生产环境（PostgreSQL）执行。
-- ============================================================

-- 仓库主表
CREATE TABLE IF NOT EXISTS warehouses (
    warehouse_id     BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    warehouse_code   VARCHAR(50)  NOT NULL,
    warehouse_name  VARCHAR(100) NOT NULL,
    warehouse_type  INTEGER,
    address         VARCHAR(500),
    manager_id      BIGINT,
    phone           VARCHAR(20),
    capacity        DECIMAL(12, 2),
    status          INTEGER      DEFAULT 1,
    remark          VARCHAR(500),
    create_time     TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time     TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted         INTEGER      NOT NULL DEFAULT 0
);

COMMENT ON TABLE  warehouses IS '仓库主表';
COMMENT ON COLUMN warehouses.warehouse_id IS '仓库ID（主键，自增）';
COMMENT ON COLUMN warehouses.warehouse_code IS '仓库编码（唯一）';
COMMENT ON COLUMN warehouses.warehouse_name IS '仓库名称';
COMMENT ON COLUMN warehouses.warehouse_type IS '仓库类型：1主仓库 2备用仓库 3临时仓库';
COMMENT ON COLUMN warehouses.address IS '仓库地址';
COMMENT ON COLUMN warehouses.manager_id IS '仓库管理员ID';
COMMENT ON COLUMN warehouses.phone IS '联系电话';
COMMENT ON COLUMN warehouses.capacity IS '仓库容量';
COMMENT ON COLUMN warehouses.status IS '状态：1启用 0停用';
COMMENT ON COLUMN warehouses.remark IS '备注';
COMMENT ON COLUMN warehouses.create_time IS '创建时间';
COMMENT ON COLUMN warehouses.update_time IS '更新时间';
COMMENT ON COLUMN warehouses.deleted IS '逻辑删除：0未删除 1已删除';

-- 唯一索引：仓库编码
CREATE UNIQUE INDEX IF NOT EXISTS uk_warehouses_warehouse_code ON warehouses (warehouse_code);
-- 普通索引：按类型/状态/管理员查询
CREATE INDEX IF NOT EXISTS idx_warehouses_warehouse_type ON warehouses (warehouse_type);
CREATE INDEX IF NOT EXISTS idx_warehouses_status          ON warehouses (status);
CREATE INDEX IF NOT EXISTS idx_warehouses_manager_id      ON warehouses (manager_id);
