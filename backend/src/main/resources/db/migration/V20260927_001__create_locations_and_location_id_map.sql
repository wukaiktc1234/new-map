-- ============================================================
-- P1-LOCATION-MODEL-001 M1+M2
-- locations 表（统一 stores_new + warehouses 的库存位置维度）
-- location_id_map 表（store_id/warehouse_id ↔ location_id 映射桥）
-- 数据迁移：stores_new 全量 → STORE 型；warehouses 仅真实仓 → CENTRAL/DEPOT
--   （Owner"全清"决策：8 个 E2E/TEST 仓库不迁移，留在 warehouses 表待 M7 退役）
-- 设计依据：docs/design/location-organization-separation-design-001.md §1.1/§3.1
--          + -002.md §7（ID 空间规则）/ §1.3（批次一期单行化）
-- 回滚：见 docs/architecture/03-review/p1-location-model-001-implementation-record-001.md §回滚点
-- ============================================================

-- M1: 建表
CREATE TABLE IF NOT EXISTS locations (
    location_id     BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    location_code   VARCHAR(50)  NOT NULL,
    location_name   VARCHAR(100) NOT NULL,
    location_type   VARCHAR(20)  NOT NULL,          -- STORE / CENTRAL / DEPOT / TRANSIT
    storage_type    VARCHAR(20),                    -- 物理属性（吸收 warehouse_type 的冷/冻/常温口径），纯属性不参与归属
    address         VARCHAR(500),
    capacity        NUMERIC(12,2),
    manager_user_id BIGINT,                         -- 吸收 warehouses.manager_id
    phone           VARCHAR(20),
    status          SMALLINT     NOT NULL DEFAULT 1, -- 1 启用 0 停用
    remark          VARCHAR(500),
    deleted         SMALLINT     NOT NULL DEFAULT 0,
    create_time     TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time     TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uk_locations_code UNIQUE (location_code),
    CONSTRAINT ck_locations_type CHECK (location_type IN ('STORE', 'CENTRAL', 'DEPOT', 'TRANSIT'))
);

COMMENT ON TABLE locations IS '库存位置表（门店与仓库统一：库存维度"货在哪"，与组织维度正交）';
COMMENT ON COLUMN locations.location_type IS 'STORE=门店(有库存的最小仓库); CENTRAL=中央仓(采购默认落点); DEPOT=普通仓储仓; TRANSIT=调拨在途虚拟位(一期未启用)';
COMMENT ON COLUMN locations.storage_type IS '物理存储属性: NORMAL=常温 COLD=冷藏 FROZEN=冷冻; NULL=不适用';

CREATE INDEX IF NOT EXISTS idx_locations_type_status ON locations(location_type, status);

CREATE TABLE IF NOT EXISTS location_id_map (
    id          BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    src_table   VARCHAR(32)  NOT NULL,               -- 'stores_new' / 'warehouses'
    src_id      BIGINT       NOT NULL,               -- 原主键值
    location_id BIGINT       NOT NULL REFERENCES locations(location_id),
    create_time TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uq_location_id_map UNIQUE (src_table, src_id)
);

COMMENT ON TABLE location_id_map IS '旧 ID → location_id 映射桥（规则 3/4：一切跨 ID 空间的换算必须经此表）';

-- M2: 数据迁移
-- 2a. stores_new 全量（deleted=0）→ STORE 型 location
-- 注意：活体 stores_new.manager_id 为 varchar（与 DDL 漂移），非纯数字值安全置 NULL
INSERT INTO locations (location_code, location_name, location_type, address, capacity, manager_user_id, phone, status)
SELECT store_code, store_name, 'STORE', address, area_size,
       CASE WHEN manager_id ~ '^[0-9]+$' THEN manager_id::bigint ELSE NULL END,
       phone,
       CASE WHEN status = 1 THEN 1 ELSE 0 END
FROM stores_new
WHERE deleted = 0;

INSERT INTO location_id_map (src_table, src_id, location_id)
SELECT 'stores_new', s.store_id, l.location_id
FROM stores_new s
JOIN locations l ON l.location_code = s.store_code AND l.location_type = 'STORE'
WHERE s.deleted = 0;

-- 2b. warehouses 仅真实仓（Owner"全清"：E2E/TEST 仓不迁移）→ CENTRAL/DEPOT
INSERT INTO locations (location_code, location_name, location_type, storage_type, address, capacity, manager_user_id, phone, status)
SELECT warehouse_code, warehouse_name,
       CASE WHEN warehouse_type = 1 THEN 'CENTRAL' ELSE 'DEPOT' END,
       CASE warehouse_type WHEN 2 THEN 'COLD' WHEN 3 THEN 'FROZEN' WHEN 4 THEN 'NORMAL' ELSE NULL END,
       address, capacity, manager_id, phone, status
FROM warehouses
WHERE deleted = 0 AND warehouse_code IN ('WH_A', 'WH_B');

INSERT INTO location_id_map (src_table, src_id, location_id)
SELECT 'warehouses', w.warehouse_id, l.location_id
FROM warehouses w
JOIN locations l ON l.location_code = w.warehouse_code AND l.location_type IN ('CENTRAL', 'DEPOT')
WHERE w.deleted = 0 AND w.warehouse_code IN ('WH_A', 'WH_B');
