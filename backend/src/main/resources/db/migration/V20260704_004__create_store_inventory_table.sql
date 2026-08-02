-- ============================================================
-- 迁移脚本：创建门店库存表 store_inventory
-- 说明：实现独立的门店库存维度，不再复用 inventory 表
--       按门店+物料维度记录门店库存
-- ============================================================

CREATE TABLE IF NOT EXISTS store_inventory (
    id BIGSERIAL PRIMARY KEY,
    store_id VARCHAR(64) NOT NULL,
    material_id BIGINT NOT NULL,
    material_name VARCHAR(128),
    current_stock DECIMAL(15,3) DEFAULT 0,
    unit VARCHAR(32),
    safety_stock DECIMAL(15,3),
    max_stock DECIMAL(15,3),
    create_time TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted INTEGER NOT NULL DEFAULT 0
);

-- 索引：门店ID查询
CREATE INDEX IF NOT EXISTS idx_store_inventory_store_id ON store_inventory(store_id);
-- 索引：物料ID查询
CREATE INDEX IF NOT EXISTS idx_store_inventory_material_id ON store_inventory(material_id);
-- 唯一索引：门店+物料维度唯一
CREATE UNIQUE INDEX IF NOT EXISTS uk_store_inventory_store_material ON store_inventory(store_id, material_id);

COMMENT ON TABLE store_inventory IS '门店库存表（按门店+物料维度）';
COMMENT ON COLUMN store_inventory.id IS '主键ID';
COMMENT ON COLUMN store_inventory.store_id IS '门店ID，关联stores表';
COMMENT ON COLUMN store_inventory.material_id IS '物料ID';
COMMENT ON COLUMN store_inventory.material_name IS '物料名称（冗余字段便于展示）';
COMMENT ON COLUMN store_inventory.current_stock IS '当前库存数量';
COMMENT ON COLUMN store_inventory.unit IS '单位';
COMMENT ON COLUMN store_inventory.safety_stock IS '安全库存预警线';
COMMENT ON COLUMN store_inventory.max_stock IS '最大库存容量';
COMMENT ON COLUMN store_inventory.create_time IS '创建时间';
COMMENT ON COLUMN store_inventory.update_time IS '更新时间';
COMMENT ON COLUMN store_inventory.deleted IS '逻辑删除标记（0:未删除 1:已删除）';
