-- ============================================================================
-- V20261024_001__stockin_location_hardening.sql
--
-- 整改 3：采购入库「库位」强约束（禁止 NULL 库位 / 禁止悬空外键）
--
-- 目标表：
--   purchase_stockins.location_id        —— 新增列（可空，历史单据无法可靠回溯）
--   purchase_stockin_items.location_id   —— 由可空改为 NOT NULL + FK
--   purchase_stockin_items.material_id   —— 由可空改为 NOT NULL
--   locations(location_id) 为主键（库位权威表）
--
-- 前置条件（由 fund-500 负责）：
--   * purchase_stockin_items.location_id 的历史 NULL 已按 ID 映射链回填
--     （scripts/db-fix/2026-10-24-fix-stockin-location-id.sql：
--      purchase_orders.warehouse_id/store_id → location_id_map → locations；禁止字符串软匹配）；
--   * 无法归属到任何库位的行已被逻辑删除（deleted=1），或按业务裁决另行处理。
--   本迁移**不造数据**：数据未清洗时直接抛可读异常，让 Flyway fail-fast。
--
-- 幂等性：
--   列 / 索引用 IF NOT EXISTS，约束用 pg_constraint 判断，NOT NULL 用 information_schema 判断，
--   因此本文件在已执行过的库上重复手工执行不会报错（Flyway 本身只会跑一次）。
--
-- 手工验证（psql）：**请用单事务方式执行**，否则 psql 逐条自动提交，一旦中途失败会留下半成品结构：
--   psql -1 -v ON_ERROR_STOP=1 -h localhost -p 5432 -U postgres -d food_traceability -f 本文件
--   （Flyway 本身会把整个迁移放在一个事务里执行，失败自动整体回滚。）
--   select is_nullable from information_schema.columns
--    where table_name='purchase_stockin_items' and column_name='location_id';   -- 期望 NO
--   select conname from pg_constraint
--    where conname in ('fk_purchase_stockins_location','fk_purchase_stockin_items_location');
-- ============================================================================

-- ---------------------------------------------------------------------------
-- 0) 前置数据闸门：不满足条件时给出「可读的」失败原因（而不是晦涩的约束冲突）
-- ---------------------------------------------------------------------------
DO $$
DECLARE
    v_items_null      bigint;
    v_items_orphan    bigint;
    v_material_null   bigint;
BEGIN
    SELECT count(*) INTO v_items_null
      FROM purchase_stockin_items
     WHERE location_id IS NULL;

    IF v_items_null > 0 THEN
        RAISE EXCEPTION USING
            MESSAGE = format(
                'purchase_stockin_items.location_id 仍有 %s 行 NULL，无法 SET NOT NULL（整改3 前置数据清洗未完成）', v_items_null),
            DETAIL = format(
                '未删除行中 location_id 为空的明细数：%s', v_items_null),
            HINT = '请先执行 scripts/db-fix/2026-10-24-fix-stockin-location-id.sql（ID 映射链：purchase_orders.warehouse_id/store_id → location_id_map → locations），'
                || '无法归属的行按业务裁决逻辑删除后再启动应用/重跑本迁移。';
    END IF;

    SELECT count(*) INTO v_items_orphan
      FROM purchase_stockin_items it
     WHERE it.location_id IS NOT NULL
       AND NOT EXISTS (SELECT 1 FROM locations l WHERE l.location_id = it.location_id);

    IF v_items_orphan > 0 THEN
        RAISE EXCEPTION USING
            MESSAGE = format(
                'purchase_stockin_items.location_id 有 %s 行指向不存在的库位（悬空外键），无法建 fk_purchase_stockin_items_location', v_items_orphan),
            HINT = '请先把这些行回填为有效库位，或按业务裁决逻辑删除（deleted=1）。';
    END IF;

    SELECT count(*) INTO v_material_null
      FROM purchase_stockin_items
     WHERE material_id IS NULL;

    IF v_material_null > 0 THEN
        RAISE EXCEPTION USING
            MESSAGE = format(
                'purchase_stockin_items.material_id 仍有 %s 行 NULL，无法 SET NOT NULL', v_material_null),
            HINT = '请先补齐物料ID（或按业务裁决逻辑删除该行）后重跑本迁移；本迁移不擅自造数据。';
    END IF;

END
$$;

-- ---------------------------------------------------------------------------
-- 1) purchase_stockins.location_id：新增列 + 索引
-- ---------------------------------------------------------------------------
ALTER TABLE purchase_stockins
    ADD COLUMN IF NOT EXISTS location_id bigint;

CREATE INDEX IF NOT EXISTS idx_purchase_stockins_location
    ON purchase_stockins (location_id);

-- ---------------------------------------------------------------------------
-- 2) purchase_stockins.location_id → locations(location_id)，ON DELETE RESTRICT
--    （可空：历史入库单无法可靠回溯库位；有值时必须是有效库位）
-- ---------------------------------------------------------------------------
DO $$
DECLARE
    v_stockin_orphan bigint;
BEGIN
    -- 列刚建出来（历史值为 NULL），这里只防「已被人工回填成悬空值」的情况
    SELECT count(*) INTO v_stockin_orphan
      FROM purchase_stockins s
     WHERE s.location_id IS NOT NULL
       AND NOT EXISTS (SELECT 1 FROM locations l WHERE l.location_id = s.location_id);

    IF v_stockin_orphan > 0 THEN
        RAISE EXCEPTION USING
            MESSAGE = format(
                'purchase_stockins.location_id 有 %s 行指向不存在的库位（悬空外键），无法建 fk_purchase_stockins_location', v_stockin_orphan),
            HINT = '请先修正这些入库单的库位引用后重跑本迁移。';
    END IF;

    IF NOT EXISTS (
        SELECT 1 FROM pg_constraint
         WHERE conname = 'fk_purchase_stockins_location'
           AND conrelid = 'purchase_stockins'::regclass
    ) THEN
        ALTER TABLE purchase_stockins
            ADD CONSTRAINT fk_purchase_stockins_location
            FOREIGN KEY (location_id) REFERENCES locations (location_id)
            ON DELETE RESTRICT;
    END IF;
END
$$;

-- ---------------------------------------------------------------------------
-- 3) purchase_stockin_items.location_id：先 NOT NULL，再建 FK（RESTRICT）
-- ---------------------------------------------------------------------------
DO $$
BEGIN
    IF EXISTS (
        SELECT 1 FROM information_schema.columns
         WHERE table_schema = current_schema()
           AND table_name = 'purchase_stockin_items'
           AND column_name = 'location_id'
           AND is_nullable = 'YES'
    ) THEN
        ALTER TABLE purchase_stockin_items
            ALTER COLUMN location_id SET NOT NULL;
    END IF;
END
$$;

DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM pg_constraint
         WHERE conname = 'fk_purchase_stockin_items_location'
           AND conrelid = 'purchase_stockin_items'::regclass
    ) THEN
        ALTER TABLE purchase_stockin_items
            ADD CONSTRAINT fk_purchase_stockin_items_location
            FOREIGN KEY (location_id) REFERENCES locations (location_id)
            ON DELETE RESTRICT;
    END IF;
END
$$;

-- ---------------------------------------------------------------------------
-- 4) purchase_stockin_items.material_id：NOT NULL（数据未清洗时在第 0 步已 fail）
-- ---------------------------------------------------------------------------
DO $$
BEGIN
    IF EXISTS (
        SELECT 1 FROM information_schema.columns
         WHERE table_schema = current_schema()
           AND table_name = 'purchase_stockin_items'
           AND column_name = 'material_id'
           AND is_nullable = 'YES'
    ) THEN
        ALTER TABLE purchase_stockin_items
            ALTER COLUMN material_id SET NOT NULL;
    END IF;
END
$$;

-- ---------------------------------------------------------------------------
-- 5) 迁移自检：结构不符合预期时直接失败，避免「静默半成品」
-- ---------------------------------------------------------------------------
DO $$
DECLARE
    v_nullable text;
BEGIN
    SELECT is_nullable INTO v_nullable
      FROM information_schema.columns
     WHERE table_schema = current_schema()
       AND table_name = 'purchase_stockin_items'
       AND column_name = 'location_id';

    IF v_nullable IS DISTINCT FROM 'NO' THEN
        RAISE EXCEPTION '自检失败：purchase_stockin_items.location_id 仍可空（is_nullable=%）', v_nullable;
    END IF;

    IF NOT EXISTS (
        SELECT 1 FROM pg_constraint
         WHERE conname = 'fk_purchase_stockin_items_location'
           AND conrelid = 'purchase_stockin_items'::regclass
    ) THEN
        RAISE EXCEPTION '自检失败：缺少外键 fk_purchase_stockin_items_location';
    END IF;

    IF NOT EXISTS (
        SELECT 1 FROM pg_constraint
         WHERE conname = 'fk_purchase_stockins_location'
           AND conrelid = 'purchase_stockins'::regclass
    ) THEN
        RAISE EXCEPTION '自检失败：缺少外键 fk_purchase_stockins_location';
    END IF;

    RAISE NOTICE '整改3 完成：purchase_stockins.location_id 已建列+索引+FK；purchase_stockin_items.location_id/material_id 已 NOT NULL，库位 FK 已就位。';
END
$$;
