-- ============================================================
-- scripts/db-fix/2026-10-24-backfill-stockin-inventory-v2.sql
-- DATA-007 v2.1：以 purchase_stockins + purchase_stockin_items 为权威补库存/流水
--
-- 状态：默认 dry-run（试算）；只有 -v apply=1 才写库。
--   psql ... -f 本文件                 → 只打印试算数字
--   psql ... -v apply=1 -f 本文件      → 备份 + 更新库存 + 补 movement
--
-- 【目标集】
--   purchase_stockins.status = 1（已入库）且 items.deleted = 0
--   且该 (stockin_id, item_id) 没有 BACKFILL_STOCKIN_ITEM 记录（幂等键）
--   且该 stockin 的**任何** movement.source_ref 里都没出现过 stockin_code（防重复入账，见下）
--
-- 【幂等键】
--   inventory_movement: source_type='BACKFILL_STOCKIN_ITEM' AND source_ref='PSI:<stockin_id>:<item_id>'
--
-- 【⚠ 库位解析（这是 v2 最关键的现实约束，Origin 任务书假设 items.location_id 有值，但活库 22 条 items 全是 NULL）】
--   回退链：items.location_id → warehouses.warehouse_code = locations.location_code → locations.location_id
--   因为 inventory.location_id 有外键 REFERENCES locations(location_id)，仓库号 ≠ 库位号，
--   无法映射到 locations 的行**一律跳过**（否则 INSERT 会 FK 失败，或造出悬空库位）。
--   实测：15 条待补 items 里只有 **2 条**能解析出库位（都来自 warehouse_id=1 WH_A → location 5），
--   另外 13 条挂在 E2E 测试仓（WH-E2E-*／WH_TEST_*）上，locations 里根本没有对应库位 → 不可落地。
--
-- 【金额/单价单位（已核对）】
--   purchase_stockin_items.unit_price 是**分**（例：item 3 → unit_price 300 × qty 100 = amount 30000 = 300.00 元）
--   inventory.unit_cost / inventory_movement.unit_cost / total_cost 同为「分」口径，
--   因此 movement.unit_cost = items.unit_price、total_cost = actual_quantity × unit_price，无需换单位。
--
-- 【动作为什么只改 inventory + movement】
--   不补 cost_records（成本试算见 2026-10-24-backfill-cost-records.sql，单独确认后再执行）。
--
-- 回滚：见文件末尾。
-- ============================================================

\set ON_ERROR_STOP on
\pset pager off

-- ------------------------------------------------------------
-- 试算（dry-run 与 apply 都打印）
-- ------------------------------------------------------------
\echo ''
\echo '===== DRY-RUN 1：目标集与落地可行性 ====='
WITH base AS (
    SELECT s.stockin_id, s.stockin_code, s.stockin_date, s.total_amount, s.warehouse_id,
           w.warehouse_code, it.stockin_item_id, it.material_id, it.material_name, it.unit,
           it.batch_no, it.production_date, it.expiry_date,
           it.actual_quantity, it.unit_price,
           COALESCE(it.amount, it.actual_quantity * it.unit_price) AS item_amount_fen,
           COALESCE(it.location_id, l.location_id) AS location_id,
           EXISTS (SELECT 1 FROM inventory_movement m
                    WHERE m.source_ref LIKE '%' || s.stockin_code || '%') AS stockin_has_movement,
           EXISTS (SELECT 1 FROM inventory_movement m
                    WHERE m.source_type = 'BACKFILL_STOCKIN_ITEM'
                      AND m.source_ref = 'PSI:' || s.stockin_id || ':' || it.stockin_item_id) AS already_done
      FROM purchase_stockins s
      JOIN purchase_stockin_items it ON it.stockin_id = s.stockin_id AND it.deleted = 0
      LEFT JOIN warehouses w ON w.warehouse_id = s.warehouse_id
      LEFT JOIN locations  l ON l.location_code = w.warehouse_code AND l.deleted = 0
     WHERE s.deleted = 0 AND s.status = 1
)
SELECT count(*)                                                                  AS items_total,
       count(*) FILTER (WHERE stockin_has_movement)                               AS skip_stockin_already_has_movement,
       count(*) FILTER (WHERE already_done)                                       AS skip_already_backfilled,
       count(*) FILTER (WHERE NOT stockin_has_movement AND NOT already_done)      AS pending,
       count(*) FILTER (WHERE NOT stockin_has_movement AND NOT already_done AND location_id IS NOT NULL) AS pending_locatable,
       count(*) FILTER (WHERE NOT stockin_has_movement AND NOT already_done AND location_id IS NULL)     AS pending_unlocatable,
       COALESCE(SUM(actual_quantity) FILTER (WHERE NOT stockin_has_movement AND NOT already_done AND location_id IS NOT NULL), 0) AS qty_apply,
       COALESCE(SUM(item_amount_fen) FILTER (WHERE NOT stockin_has_movement AND NOT already_done AND location_id IS NOT NULL), 0) AS amount_apply_fen,
       COALESCE(SUM(actual_quantity) FILTER (WHERE NOT stockin_has_movement AND NOT already_done AND location_id IS NULL), 0)     AS qty_unlocatable
  FROM base;

\echo ''
\echo '===== DRY-RUN 2：可落地明细（apply 会处理这些）====='
WITH base AS (
    SELECT s.stockin_id, s.stockin_code, s.warehouse_id, w.warehouse_code,
           it.stockin_item_id, it.material_id, it.material_name, it.actual_quantity, it.unit_price,
           COALESCE(it.amount, it.actual_quantity * it.unit_price) AS item_amount_fen,
           COALESCE(it.location_id, l.location_id) AS location_id,
           EXISTS (SELECT 1 FROM inventory_movement m WHERE m.source_ref LIKE '%' || s.stockin_code || '%') AS stockin_has_movement,
           EXISTS (SELECT 1 FROM inventory_movement m
                    WHERE m.source_type = 'BACKFILL_STOCKIN_ITEM'
                      AND m.source_ref = 'PSI:' || s.stockin_id || ':' || it.stockin_item_id) AS already_done
      FROM purchase_stockins s
      JOIN purchase_stockin_items it ON it.stockin_id = s.stockin_id AND it.deleted = 0
      LEFT JOIN warehouses w ON w.warehouse_id = s.warehouse_id
      LEFT JOIN locations  l ON l.location_code = w.warehouse_code AND l.deleted = 0
     WHERE s.deleted = 0 AND s.status = 1
)
SELECT stockin_id, stockin_code, stockin_item_id, material_id, material_name, location_id,
       actual_quantity, unit_price, item_amount_fen,
       COALESCE((SELECT quantity FROM inventory i
                  WHERE i.deleted = 0 AND i.location_id = b.location_id AND i.material_id = b.material_id
                  LIMIT 1), NULL) AS inv_qty_now,
       CASE WHEN EXISTS (SELECT 1 FROM inventory i
                          WHERE i.deleted = 0 AND i.location_id = b.location_id AND i.material_id = b.material_id)
            THEN 'UPDATE 现有库存行' ELSE 'INSERT 新库存行（FK 已校验）' END AS action
  FROM base b
 WHERE NOT stockin_has_movement AND NOT already_done AND location_id IS NOT NULL
 ORDER BY stockin_id, stockin_item_id;

\echo ''
\echo '===== DRY-RUN 3：不可落地明细（跳过，不臆造库位）====='
WITH base AS (
    SELECT s.stockin_id, s.stockin_code, s.warehouse_id, w.warehouse_code,
           it.stockin_item_id, it.material_id, it.material_name, it.actual_quantity, it.unit_price,
           COALESCE(it.location_id, l.location_id) AS location_id,
           EXISTS (SELECT 1 FROM inventory_movement m WHERE m.source_ref LIKE '%' || s.stockin_code || '%') AS stockin_has_movement
      FROM purchase_stockins s
      JOIN purchase_stockin_items it ON it.stockin_id = s.stockin_id AND it.deleted = 0
      LEFT JOIN warehouses w ON w.warehouse_id = s.warehouse_id
      LEFT JOIN locations  l ON l.location_code = w.warehouse_code AND l.deleted = 0
     WHERE s.deleted = 0 AND s.status = 1
)
SELECT stockin_id, stockin_code, warehouse_id, warehouse_code, stockin_item_id,
       material_id, material_name, actual_quantity, unit_price,
       '仓库无对应库位（locations 无此 location_code），inventory.location_id 有 FK，无法落库' AS reason
  FROM base
 WHERE NOT stockin_has_movement AND location_id IS NULL
 ORDER BY stockin_id, stockin_item_id;

-- ------------------------------------------------------------
-- 写库（仅 -v apply=1）
-- ------------------------------------------------------------
\if :{?apply}
BEGIN;

-- 0) 备份：只备份会被改动的库存行
DO $$
DECLARE v_rows bigint;
BEGIN
    IF to_regclass('public.inventory_backup_20261024_stockin_v2') IS NULL THEN
        CREATE TABLE public.inventory_backup_20261024_stockin_v2 AS
        SELECT i.*
          FROM public.inventory i
         WHERE (i.location_id, i.material_id) IN (
               SELECT COALESCE(it.location_id, l.location_id), it.material_id
                 FROM public.purchase_stockins s
                 JOIN public.purchase_stockin_items it ON it.stockin_id = s.stockin_id AND it.deleted = 0
                 LEFT JOIN public.warehouses w ON w.warehouse_id = s.warehouse_id
                 LEFT JOIN public.locations  l ON l.location_code = w.warehouse_code AND l.deleted = 0
                WHERE s.deleted = 0 AND s.status = 1
                  AND COALESCE(it.location_id, l.location_id) IS NOT NULL
                  AND NOT EXISTS (SELECT 1 FROM public.inventory_movement m
                                   WHERE m.source_ref LIKE '%' || s.stockin_code || '%')
                  AND NOT EXISTS (SELECT 1 FROM public.inventory_movement m
                                   WHERE m.source_type = 'BACKFILL_STOCKIN_ITEM'
                                     AND m.source_ref = 'PSI:' || s.stockin_id || ':' || it.stockin_item_id));
        SELECT COUNT(*) INTO v_rows FROM public.inventory_backup_20261024_stockin_v2;
        RAISE NOTICE '[ok] 已备份 % 行 → inventory_backup_20261024_stockin_v2', v_rows;
    ELSE
        RAISE NOTICE '[skip] 备份表 inventory_backup_20261024_stockin_v2 已存在，不覆盖';
    END IF;
END $$;

-- 1) 目标集落到临时表（幂等判定在这之后不再变化）
CREATE TEMP TABLE _v2_target ON COMMIT DROP AS
WITH base AS (
    SELECT s.stockin_id, s.stockin_code, s.stockin_date,
           it.stockin_item_id, it.material_id, it.material_name, it.unit, it.batch_no,
           it.production_date, it.expiry_date, it.actual_quantity, it.unit_price,
           COALESCE(it.amount, it.actual_quantity * it.unit_price) AS item_amount_fen,
           COALESCE(it.location_id, l.location_id) AS location_id,
           EXISTS (SELECT 1 FROM public.inventory_movement m
                    WHERE m.source_ref LIKE '%' || s.stockin_code || '%') AS stockin_has_movement,
           EXISTS (SELECT 1 FROM public.inventory_movement m
                    WHERE m.source_type = 'BACKFILL_STOCKIN_ITEM'
                      AND m.source_ref = 'PSI:' || s.stockin_id || ':' || it.stockin_item_id) AS already_done
      FROM public.purchase_stockins s
      JOIN public.purchase_stockin_items it ON it.stockin_id = s.stockin_id AND it.deleted = 0
      LEFT JOIN public.warehouses w ON w.warehouse_id = s.warehouse_id
      LEFT JOIN public.locations  l ON l.location_code = w.warehouse_code AND l.deleted = 0
     WHERE s.deleted = 0 AND s.status = 1
)
SELECT b.*,
       COALESCE((SELECT i.quantity FROM public.inventory i
                  WHERE i.deleted = 0 AND i.location_id = b.location_id AND i.material_id = b.material_id), 0) AS inv_qty_before
  FROM base b
 WHERE NOT b.stockin_has_movement AND NOT b.already_done AND b.location_id IS NOT NULL;

-- 2) 补 movement：balance_after = 补账前余额 + 同库位同物料按 item 顺序的累计增量
INSERT INTO public.inventory_movement (
    location_id, material_id, change_qty, balance_after, movement_type,
    source_type, source_ref, operator_id, unit_cost, total_cost, remark, create_time
)
SELECT t.location_id,
       t.material_id,
       t.actual_quantity,
       t.inv_qty_before + SUM(t.actual_quantity) OVER (
             PARTITION BY t.location_id, t.material_id
             ORDER BY t.stockin_id, t.stockin_item_id ROWS BETWEEN UNBOUNDED PRECEDING AND CURRENT ROW),
       'IN',
       'BACKFILL_STOCKIN_ITEM',
       'PSI:' || t.stockin_id || ':' || t.stockin_item_id,
       NULL,
       t.unit_price,                                   -- 分（与 items.unit_price 同口径）
       t.item_amount_fen,                              -- 分
       '历史补账 20261024：采购入库单未落流水（' || t.stockin_code || '）',
       CURRENT_TIMESTAMP
  FROM _v2_target t;

-- 3) 更新已存在的库存行
UPDATE public.inventory i
   SET quantity    = i.quantity + agg.qty,
       unit_cost   = COALESCE(NULLIF(agg.unit_price, 0), i.unit_cost),
       update_time = CURRENT_TIMESTAMP,
       version     = COALESCE(i.version, 0) + 1
  FROM (SELECT location_id, material_id, SUM(actual_quantity) AS qty, MAX(unit_price) AS unit_price
          FROM _v2_target GROUP BY location_id, material_id) agg
 WHERE i.deleted = 0
   AND i.location_id = agg.location_id
   AND i.material_id = agg.material_id;

-- 4) 不存在的 (location, material) 库存行 → 新建（location 已校验存在于 locations）
INSERT INTO public.inventory (
    location_id, material_id, material_name, quantity, unit, safety_stock, locked_quantity,
    status, batch_no, production_date, expiry_date, unit_cost, total_cost,
    create_time, update_time, version, deleted
)
SELECT agg.location_id, agg.material_id, agg.material_name, agg.qty, agg.unit,
       0, 0, 1, agg.batch_no, agg.production_date, agg.expiry_date,
       agg.unit_price, agg.qty * agg.unit_price,
       CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 0, 0
  FROM (SELECT DISTINCT ON (location_id, material_id)
               location_id, material_id, material_name, unit, batch_no,
               production_date, expiry_date, unit_price,
               SUM(actual_quantity) OVER (PARTITION BY location_id, material_id) AS qty
          FROM _v2_target
         ORDER BY location_id, material_id, stockin_id, stockin_item_id) agg
 WHERE NOT EXISTS (SELECT 1 FROM public.inventory i
                    WHERE i.deleted = 0 AND i.location_id = agg.location_id AND i.material_id = agg.material_id);

\echo ''
\echo '===== APPLY 结果复核 ====='
SELECT (SELECT count(*) FROM public.inventory_movement WHERE source_type = 'BACKFILL_STOCKIN_ITEM') AS movements_written,
       (SELECT COALESCE(SUM(change_qty), 0) FROM public.inventory_movement WHERE source_type = 'BACKFILL_STOCKIN_ITEM') AS qty_written,
       (SELECT count(*) FROM public.inventory_movement m
         WHERE m.source_type = 'BACKFILL_STOCKIN_ITEM'
           AND m.balance_after <> (SELECT i.quantity FROM public.inventory i
                                    WHERE i.location_id = m.location_id AND i.material_id = m.material_id AND i.deleted = 0)) AS balance_mismatch_rows;

COMMIT;
\endif

-- ============================================================
-- ROLLBACK
-- ============================================================
-- 1) 删本次 movement
--    DELETE FROM inventory_movement WHERE source_type = 'BACKFILL_STOCKIN_ITEM';
-- 2) 库存行还原（已存在行）
--    UPDATE inventory i SET quantity = b.quantity, unit_cost = b.unit_cost,
--           version = b.version, update_time = CURRENT_TIMESTAMP
--      FROM inventory_backup_20261024_stockin_v2 b
--     WHERE i.inventory_id = b.inventory_id;
-- 3) 本次新建的库存行（备份表里没有的那些）需要人工核对后删除：
--    SELECT i.* FROM inventory i
--     WHERE (i.location_id, i.material_id) IN (SELECT location_id, material_id FROM _v2_target)
--       AND NOT EXISTS (SELECT 1 FROM inventory_backup_20261024_stockin_v2 b WHERE b.inventory_id = i.inventory_id);
-- ============================================================
