-- ============================================================
-- 库位表建表脚本
-- 对应实体: InventoryLocation (inventory_locations)
-- 对应初始化器: InventoryDatabaseInitializer.createInventoryLocationsTable()
--
-- 注意：H2 开发环境由 InventoryDatabaseInitializer.createInventoryLocationsTable 建表，
--       本脚本仅在生产环境（PostgreSQL）执行。
-- ============================================================

-- 库位表
CREATE TABLE IF NOT EXISTS inventory_locations (
    location_id       BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    warehouse_id     BIGINT       NOT NULL,
    location_code    VARCHAR(50)  NOT NULL,
    location_name   VARCHAR(100) NOT NULL,
    location_type   INTEGER,
    max_capacity    DECIMAL(12, 2),
    current_quantity DECIMAL(12, 2),
    status          INTEGER      DEFAULT 1,
    remark          VARCHAR(500),
    create_time     TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time     TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted         INTEGER      NOT NULL DEFAULT 0
);

COMMENT ON TABLE  inventory_locations IS '库位表';
COMMENT ON COLUMN inventory_locations.location_id IS '库位ID（主键，自增）';
COMMENT ON COLUMN inventory_locations.warehouse_id IS '所属仓库ID';
COMMENT ON COLUMN inventory_locations.location_code IS '库位编码（唯一）';
COMMENT ON COLUMN inventory_locations.location_name IS '库位名称';
COMMENT ON COLUMN inventory_locations.location_type IS '库位类型';
COMMENT ON COLUMN inventory_locations.max_capacity IS '最大容量';
COMMENT ON COLUMN inventory_locations.current_quantity IS '当前数量';
COMMENT ON COLUMN inventory_locations.status IS '状态：1启用 0停用';
COMMENT ON COLUMN inventory_locations.remark IS '备注';
COMMENT ON COLUMN inventory_locations.create_time IS '创建时间';
COMMENT ON COLUMN inventory_locations.update_time IS '更新时间';
COMMENT ON COLUMN inventory_locations.deleted IS '逻辑删除：0未删除 1已删除';

-- 唯一索引：库位编码
CREATE UNIQUE INDEX IF NOT EXISTS uk_inventory_locations_location_code ON inventory_locations (location_code);
-- 普通索引：按仓库ID/状态查询
CREATE INDEX IF NOT EXISTS idx_inventory_locations_warehouse_id ON inventory_locations (warehouse_id);
CREATE INDEX IF NOT EXISTS idx_inventory_locations_status        ON inventory_locations (status);
