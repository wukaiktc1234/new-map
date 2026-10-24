-- ============================================================
-- scripts/db-fix/2026-10-24-fix-stockin-location-id.sql
-- 整改 2：采购入库 历史 location_id 清洗（**ID 关联，禁止软匹配**）
--
-- 【硬约束】
--   * 唯一合法映射表：location_id_map(src_table, src_id, location_id)，
--     src_table ∈ {'warehouses','stores_new'}。
--   * **禁止** warehouse_code ↔ location_code 之类的字符串匹配（上一轮 v2.1 的回退链已从脚本里删除）。
--
-- 【items 回填优先序（全部 ID 关联）】
--   ① purchase_stockins.order_id → purchase_orders.warehouse_id → location_id_map(warehouses)
--   ①b purchase_stockins.warehouse_id → location_id_map(warehouses)
--      ↳ 补充路径：活库里 purchase_orders.warehouse_id 对 26 条明细只有 11 条有值，
--        而 stockins.warehouse_id 全有值；且本轮 v2.1 已按这条 ID 路径补过 2 条库存流水
--        （PSI:23:24 / PSI:24:25 → location 5），若不认这条，会把刚入账的明细误判成历史废弃。
--        它依然是 ID 关联（不是字符串匹配），故保留；若 Owner 只认 ①，把本段 ①b 删掉即可。
--   ② 同上 order_id → purchase_orders.store_id → location_id_map(stores_new)
--   ③ 仍为空 → 标记历史废弃：deleted=1 + remark 追加 ' [LEGACY-NO-LOCATION 20261024]'（逻辑删除，不物理删）
--
-- 【表头 purchase_stockins.location_id】
--   该列由 ts-purchase 的 Flyway 添加；本脚本开头 DO 块检测列是否存在，
--   不存在则跳过表头回填并提示「先跑迁移」，不会报错中断。
--
-- 【备份】只备份受影响行：purchase_stockin_items_backup_20261024_loc / purchase_stockins_backup_20261024_loc
--
-- 【活库现实（2026-10-24 13:0x 实测，影响实现）】
--   1) purchase_stockins **没有 remark 列**（只有 void_remark / quality_remark，语义不同，不借用）。
--      因此表头 ③ 的「deleted=1 + remark 标记」在表头侧无法完整执行：脚本运行时探测 remark 列，
--      有则追加标记，无则只做逻辑删除并打 NOTICE（不硬塞到 void_remark 里，避免污染作废语义）。
--   2) purchase_stockins.location_id 列的存在性是**动态的**（ts-purchase 的 Flyway 与并发迁移在改），
--      实测一次运行里有、几分钟后再查就没了。脚本每次运行都重新探测，不存在就跳过表头回填。
-- 【幂等】所有写入均带 location_id IS NULL AND deleted = 0（或 deleted=0 且未标记）守卫，重复执行 0 行。
-- 【回滚】见文件末尾。
--
-- 用法：psql ... -f 本文件              → dry-run（只打印统计，不写）
--       psql ... -v apply=1 -f 本文件   → 备份 + 回填 + 逻辑删除
-- ============================================================

\set ON_ERROR_STOP on
\pset pager off

-- ------------------------------------------------------------
-- 0) BEFORE 快照
-- ------------------------------------------------------------
\echo ''
\echo '===== BEFORE：items / stockins 现状 ====='
SELECT (SELECT count(*) FROM purchase_stockin_items)                                              AS items_total,
       (SELECT count(*) FROM purchase_stockin_items WHERE deleted = 0 AND location_id IS NULL)     AS items_null_loc,
       (SELECT count(*) FROM purchase_stockin_items WHERE deleted = 1)                            AS items_soft_deleted,
       (SELECT count(*) FROM information_schema.columns
         WHERE table_name = 'purchase_stockins' AND column_name = 'location_id')                   AS stockins_has_location_col;

\echo ''
\echo '===== BEFORE：按路径分类（dry-run 先看能回填多少）====='
WITH base AS (
    SELECT it.stockin_item_id, it.stockin_id, s.order_id, s.warehouse_id AS stockin_wh,
           po.warehouse_id AS order_wh, po.store_id AS order_store,
           mw1.location_id AS loc1, mw2.location_id AS loc1b, ms.location_id AS loc2
      FROM purchase_stockin_items it
      LEFT JOIN purchase_stockins s ON s.stockin_id = it.stockin_id
      LEFT JOIN purchase_orders po ON po.order_id = s.order_id
      LEFT JOIN location_id_map mw1 ON mw1.src_table = 'warehouses' AND mw1.src_id = po.warehouse_id
      LEFT JOIN location_id_map mw2 ON mw2.src_table = 'warehouses' AND mw2.src_id = s.warehouse_id
      LEFT JOIN location_id_map ms  ON ms.src_table = 'stores_new'  AND ms.src_id = po.store_id
     WHERE it.deleted = 0 AND it.location_id IS NULL
)
SELECT count(*)                                                                 AS pending_items,
       count(*) FILTER (WHERE loc1 IS NOT NULL)                                  AS will_fill_path_1,
       count(*) FILTER (WHERE loc1 IS NULL AND loc1b IS NOT NULL)                AS will_fill_path_1b,
       count(*) FILTER (WHERE loc1 IS NULL AND loc1b IS NULL AND loc2 IS NOT NULL) AS will_fill_path_2,
       count(*) FILTER (WHERE COALESCE(loc1, loc1b, loc2) IS NULL)               AS will_mark_legacy
  FROM base;

-- ------------------------------------------------------------
-- 1) 备份 + 回填 + 逻辑删除（仅 -v apply=1）
-- ------------------------------------------------------------
\if :{?apply}
BEGIN;

-- 1.1 备份受影响行
DO $$
DECLARE v_items bigint; v_stockins bigint; v_has_col boolean;
BEGIN
    IF to_regclass('public.purchase_stockin_items_backup_20261024_loc') IS NULL THEN
        CREATE TABLE public.purchase_stockin_items_backup_20261024_loc AS
        SELECT * FROM public.purchase_stockin_items WHERE location_id IS NULL AND deleted = 0;
        SELECT COUNT(*) INTO v_items FROM public.purchase_stockin_items_backup_20261024_loc;
        RAISE NOTICE '[ok] 备份 purchase_stockin_items % 行', v_items;
    ELSE
        RAISE NOTICE '[skip] purchase_stockin_items 备份表已存在';
    END IF;

    SELECT EXISTS (SELECT 1 FROM information_schema.columns
                    WHERE table_name = 'purchase_stockins' AND column_name = 'location_id') INTO v_has_col;
    IF v_has_col AND to_regclass('public.purchase_stockins_backup_20261024_loc') IS NULL THEN
        CREATE TABLE public.purchase_stockins_backup_20261024_loc AS
        SELECT * FROM public.purchase_stockins WHERE location_id IS NULL AND deleted = 0;
        SELECT COUNT(*) INTO v_stockins FROM public.purchase_stockins_backup_20261024_loc;
        RAISE NOTICE '[ok] 备份 purchase_stockins % 行', v_stockins;
    ELSIF NOT v_has_col THEN
        RAISE NOTICE '[skip] purchase_stockins.location_id 列不存在（等 ts-purchase 迁移），本次跳过表头回填';
    ELSE
        RAISE NOTICE '[skip] purchase_stockins 备份表已存在';
    END IF;
END $$;

-- 1.2 items ①：order_id → purchase_orders.warehouse_id → map(warehouses)
WITH src AS (
    SELECT it.stockin_item_id, m.location_id
      FROM public.purchase_stockin_items it
      JOIN public.purchase_stockins s ON s.stockin_id = it.stockin_id
      JOIN public.purchase_orders  po ON po.order_id = s.order_id
      JOIN public.location_id_map  m  ON m.src_table = 'warehouses' AND m.src_id = po.warehouse_id
     WHERE it.deleted = 0 AND it.location_id IS NULL
)
UPDATE public.purchase_stockin_items it
   SET location_id = src.location_id,
       update_time = CURRENT_TIMESTAMP
  FROM src
 WHERE it.stockin_item_id = src.stockin_item_id
   AND it.location_id IS NULL;

-- 1.3 items ①b：stockins.warehouse_id → map(warehouses)（ID 关联补充，见文件头说明）
WITH src AS (
    SELECT it.stockin_item_id, m.location_id
      FROM public.purchase_stockin_items it
      JOIN public.purchase_stockins s ON s.stockin_id = it.stockin_id
      JOIN public.location_id_map  m  ON m.src_table = 'warehouses' AND m.src_id = s.warehouse_id
     WHERE it.deleted = 0 AND it.location_id IS NULL
)
UPDATE public.purchase_stockin_items it
   SET location_id = src.location_id,
       update_time = CURRENT_TIMESTAMP
  FROM src
 WHERE it.stockin_item_id = src.stockin_item_id
   AND it.location_id IS NULL;

-- 1.4 items ②：order_id → purchase_orders.store_id → map(stores_new)
WITH src AS (
    SELECT it.stockin_item_id, m.location_id
      FROM public.purchase_stockin_items it
      JOIN public.purchase_stockins s ON s.stockin_id = it.stockin_id
      JOIN public.purchase_orders  po ON po.order_id = s.order_id
      JOIN public.location_id_map  m  ON m.src_table = 'stores_new' AND m.src_id = po.store_id
     WHERE it.deleted = 0 AND it.location_id IS NULL
)
UPDATE public.purchase_stockin_items it
   SET location_id = src.location_id,
       update_time = CURRENT_TIMESTAMP
  FROM src
 WHERE it.stockin_item_id = src.stockin_item_id
   AND it.location_id IS NULL;

-- 1.5 items ③：仍为空 → 历史废弃（逻辑删除 + 标记）
UPDATE public.purchase_stockin_items
   SET deleted     = 1,
       remark      = COALESCE(remark, '') || ' [LEGACY-NO-LOCATION 20261024]',
       update_time = CURRENT_TIMESTAMP
 WHERE deleted = 0
   AND location_id IS NULL;

-- 1.6 stockins 表头（列存在才做）：同 ① ①b ② ③
DO $$
DECLARE v_has_col boolean; v_rows bigint;
BEGIN
    SELECT EXISTS (SELECT 1 FROM information_schema.columns
                    WHERE table_name = 'purchase_stockins' AND column_name = 'location_id') INTO v_has_col;
    IF NOT v_has_col THEN
        RAISE NOTICE '[skip] purchase_stockins.location_id 尚未创建（等 ts-purchase 迁移），表头回填跳过';
        RETURN;
    END IF;

    -- ① order -> warehouse
    EXECUTE 'WITH src AS (
                 SELECT s.stockin_id, m.location_id
                   FROM public.purchase_stockins s
                   JOIN public.purchase_orders po ON po.order_id = s.order_id
                   JOIN public.location_id_map m  ON m.src_table = ''warehouses'' AND m.src_id = po.warehouse_id
                  WHERE s.deleted = 0 AND s.location_id IS NULL)
             UPDATE public.purchase_stockins s SET location_id = src.location_id, update_time = CURRENT_TIMESTAMP
               FROM src WHERE s.stockin_id = src.stockin_id AND s.location_id IS NULL';
    GET DIAGNOSTICS v_rows = ROW_COUNT; RAISE NOTICE '[stockins①] 回填 % 行', v_rows;

    -- ①b stockins.warehouse_id
    EXECUTE 'WITH src AS (
                 SELECT s.stockin_id, m.location_id
                   FROM public.purchase_stockins s
                   JOIN public.location_id_map m ON m.src_table = ''warehouses'' AND m.src_id = s.warehouse_id
                  WHERE s.deleted = 0 AND s.location_id IS NULL)
             UPDATE public.purchase_stockins s SET location_id = src.location_id, update_time = CURRENT_TIMESTAMP
               FROM src WHERE s.stockin_id = src.stockin_id AND s.location_id IS NULL';
    GET DIAGNOSTICS v_rows = ROW_COUNT; RAISE NOTICE '[stockins①b] 回填 % 行', v_rows;

    -- ② order -> store
    EXECUTE 'WITH src AS (
                 SELECT s.stockin_id, m.location_id
                   FROM public.purchase_stockins s
                   JOIN public.purchase_orders po ON po.order_id = s.order_id
                   JOIN public.location_id_map m  ON m.src_table = ''stores_new'' AND m.src_id = po.store_id
                  WHERE s.deleted = 0 AND s.location_id IS NULL)
             UPDATE public.purchase_stockins s SET location_id = src.location_id, update_time = CURRENT_TIMESTAMP
               FROM src WHERE s.stockin_id = src.stockin_id AND s.location_id IS NULL';
    GET DIAGNOSTICS v_rows = ROW_COUNT; RAISE NOTICE '[stockins②] 回填 % 行', v_rows;

    -- ③ 表头**不做逻辑删除**（lead 2026-10-24 裁决：入库单本身还有库存/成本意义，不能因为库位不可考就删单据）
    --    回填不到的走 1.7 统一指向 LEGACY-VOID，仍然满足 NOT NULL + FK，业务上标记为「库位不可考」。
    SELECT COUNT(*) INTO v_rows FROM public.purchase_stockins WHERE deleted = 0 AND location_id IS NULL;
    RAISE NOTICE '[stockins③] 仍不可解析 % 行 → 交给 1.7 指向 LEGACY-VOID（不逻辑删除单据）', v_rows;
END $$;

-- 1.7 历史废弃库位兜底（满足 location_id NOT NULL + FK）
--   为什么必须做：SET NOT NULL 对**所有行**生效（含 deleted=1），逻辑删除并不能让 NULL 变成合法值；
--   Flyway 在 purchase_stockin_items.location_id 上加 NOT NULL 时会被这 13 行拦住。
--   做法：建一个「中枢虚拟库位」LEGACY-VOID-20261024（location_type=CENTRAL，按 location_code 幂等判重），
--   把所有 location_id IS NULL 的行（**不带 deleted 过滤**）指过去。业务语义不变：这些行仍是 deleted=1 的历史废弃。
INSERT INTO public.locations (location_code, location_name, location_type, status, deleted, remark, create_time, update_time)
VALUES ('LEGACY-VOID-20261024', '历史废弃入库（库位不可考，仅满足 NOT NULL/FK）', 'CENTRAL', 1, 0,
        'DATA-007 整改2：历史 location 不可解析的入库明细统一指向本虚拟库位', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
ON CONFLICT (location_code) DO NOTHING;

UPDATE public.purchase_stockin_items
   SET location_id = (SELECT location_id FROM public.locations WHERE location_code = 'LEGACY-VOID-20261024'),
       update_time = CURRENT_TIMESTAMP
 WHERE location_id IS NULL;

DO $$
DECLARE v_rows bigint;
BEGIN
    GET DIAGNOSTICS v_rows = ROW_COUNT;
    RAISE NOTICE '[items④] 指向 LEGACY-VOID 的行数 = %', v_rows;
END $$;

-- 表头：列存在时同样兜底（不做就会在 purchase_stockins.location_id 上加 NOT NULL 时被拦）
DO $$
DECLARE v_has_col boolean; v_rows bigint;
BEGIN
    SELECT EXISTS (SELECT 1 FROM information_schema.columns
                    WHERE table_name = 'purchase_stockins' AND column_name = 'location_id') INTO v_has_col;
    IF NOT v_has_col THEN
        RAISE NOTICE '[stockins④] 列不存在，跳过（迁移落库后再跑本脚本即可）';
        RETURN;
    END IF;

    EXECUTE 'UPDATE public.purchase_stockins
                SET location_id = (SELECT location_id FROM public.locations WHERE location_code = ''LEGACY-VOID-20261024''),
                    update_time = CURRENT_TIMESTAMP
              WHERE location_id IS NULL';
    GET DIAGNOSTICS v_rows = ROW_COUNT;
    RAISE NOTICE '[stockins④] 指向 LEGACY-VOID 的行数 = %', v_rows;
END $$;

COMMIT;
\endif

-- ------------------------------------------------------------
-- 2) AFTER：统计与终态断言（dry-run 也会打印）
-- ------------------------------------------------------------
\echo ''
\echo '===== AFTER：items 分类统计 ====='
WITH base AS (
    SELECT it.stockin_item_id, it.stockin_id, it.location_id, it.deleted, it.remark,
           mw1.location_id AS loc1, mw2.location_id AS loc1b, ms.location_id AS loc2
      FROM purchase_stockin_items it
      LEFT JOIN purchase_stockins s ON s.stockin_id = it.stockin_id
      LEFT JOIN purchase_orders po ON po.order_id = s.order_id
      LEFT JOIN location_id_map mw1 ON mw1.src_table = 'warehouses' AND mw1.src_id = po.warehouse_id
      LEFT JOIN location_id_map mw2 ON mw2.src_table = 'warehouses' AND mw2.src_id = s.warehouse_id
      LEFT JOIN location_id_map ms  ON ms.src_table = 'stores_new'  AND ms.src_id = po.store_id
)
-- 分类口径：按「该行按 ID 链能解析到的路径」归类，并核对其 location_id 是否等于该路径的映射值
SELECT count(*)                                                                        AS items_total,
       count(*) FILTER (WHERE deleted = 0)                                             AS live_items,
       count(*) FILTER (WHERE deleted = 0 AND location_id IS NOT NULL)                 AS filled_items,
       count(*) FILTER (WHERE deleted = 0 AND location_id IS NOT NULL AND loc1 = location_id)                    AS filled_by_path_1,
       count(*) FILTER (WHERE deleted = 0 AND location_id IS NOT NULL AND loc1 IS NULL AND loc1b = location_id)  AS filled_by_path_1b,
       count(*) FILTER (WHERE deleted = 0 AND location_id IS NOT NULL AND loc1 IS NULL AND loc1b IS NULL AND loc2 = location_id) AS filled_by_path_2,
       count(*) FILTER (WHERE deleted = 1 AND remark LIKE '%LEGACY-NO-LOCATION 20261024%')                       AS legacy_soft_deleted,
       count(*) FILTER (WHERE deleted = 0 AND location_id IS NULL)                     AS remaining_null_live
  FROM base;

\echo ''
\echo '===== AFTER：终态断言 1（NOT NULL 约束口径：不带 deleted 过滤，必须为 0）====='
SELECT count(*) AS items_null_all,
       CASE WHEN count(*) = 0 THEN 'PASS: purchase_stockin_items.location_id 全表无 NULL，可加 NOT NULL'
            ELSE 'FAIL: 仍有 ' || count(*) || ' 行 NULL，SET NOT NULL 会被拦' END AS assertion
  FROM purchase_stockin_items
 WHERE location_id IS NULL;

\echo ''
\echo '===== AFTER：终态断言 2（live 行口径，必须为 0）====='
SELECT CASE WHEN count(*) = 0 THEN 'PASS: 无 location_id IS NULL 且 deleted=0 的明细'
            ELSE 'FAIL: 仍有 ' || count(*) || ' 行 location_id 为空' END AS assertion
  FROM purchase_stockin_items
 WHERE location_id IS NULL AND deleted = 0;

\echo ''
\echo '===== AFTER：历史废弃库位与兜底分布 ====='
SELECT (SELECT location_id FROM locations WHERE location_code = 'LEGACY-VOID-20261024') AS legacy_void_location_id,
       (SELECT count(*) FROM purchase_stockin_items
         WHERE location_id = (SELECT location_id FROM locations WHERE location_code = 'LEGACY-VOID-20261024')) AS items_on_legacy_void,
       (SELECT count(*) FROM purchase_stockin_items WHERE location_id IS NULL) AS items_null_total;

\echo ''
\echo '===== AFTER：已补账明细与 v2.1 movement 的一致性（应 0 冲突）====='
SELECT count(*) AS conflict_rows
  FROM inventory_movement m
  JOIN purchase_stockin_items it
    ON m.source_ref = 'PSI:' || it.stockin_id || ':' || it.stockin_item_id
 WHERE m.source_type = 'BACKFILL_STOCKIN_ITEM'
   AND it.deleted = 0
   AND it.location_id IS DISTINCT FROM m.location_id;

\echo ''
\echo '===== AFTER：明细（前 30）====='
SELECT it.stockin_item_id, it.stockin_id, it.location_id, it.deleted, left(COALESCE(it.remark,''), 45) AS remark
  FROM purchase_stockin_items it
 ORDER BY it.stockin_item_id
 LIMIT 30;

-- ============================================================
-- ROLLBACK
-- ============================================================
-- 1) 明细：还原 location_id / deleted / remark
--    UPDATE purchase_stockin_items it
--       SET location_id = b.location_id, deleted = b.deleted, remark = b.remark, update_time = CURRENT_TIMESTAMP
--      FROM purchase_stockin_items_backup_20261024_loc b
--     WHERE it.stockin_item_id = b.stockin_item_id;
-- 2) 表头（若已执行）：
--    UPDATE purchase_stockins s
--       SET location_id = b.location_id, deleted = b.deleted, remark = b.remark, update_time = CURRENT_TIMESTAMP
--      FROM purchase_stockins_backup_20261024_loc b
--     WHERE s.stockin_id = b.stockin_id;
-- ============================================================
