-- ============================================================
-- scripts/db-fix/2026-10-24-audit-trace-confirm-gap.sql
-- DATA-007 只读盘点：追溯码确认入库 → inventory / inventory_movement / cost_records 的漏账缺口
--
-- 【只读承诺】整个事务 SET TRANSACTION READ ONLY，全部 SELECT；不建表、不写库。
--
-- 【⚠ 与任务书的前提不符的现实表名（2026-10-24 活库实测，先看这里再看数字）】
--   任务书写的            →  活库实际
--   purchase_stockin      →  purchase_stockins + purchase_stockin_items（stockin_type 1=普通入库）
--   store_inventory_log   →  store_inventory_log_legacy（列：id/inventory_id/product_id/store_id/type/
--                            change_quantity/unit_cost/total_cost_change/remark，type=1 入库、2 出库）
--   inventory.current_stock → inventory.quantity（numeric）
--   store→location 映射表  →  inventory_locations 存在但**0 行**；只能用 inventory 现有行的
--                            (material_id → location_id) 作为映射，属弱映射（见 A 口径）
--   这些差异会直接影响口径，A/B/C 都按活库真实结构写，并在每段注明替代口径。
--
-- 【三类断言（期望 0，但本轮是盘点、不是断言）】
--   A  已确认入库的追溯码（status='in_stock'）里，在 inventory 无对应数量增量的条数
--      口径：trace_code.material_id → inventory.location_id（取该 material 现有 inventory 行的最小 location）
--            按 (location_id, material_id) 汇总 SUM(quantity) 与 inventory.quantity 比对；
--            无任何 inventory 行的 material 视为「无法映射」，单独计数。
--   B  purchase_stockins.stockin_type=2（扫码确认）里，没有 inventory_movement、也没有 cost_records 的条数
--      口径：movement 侧 source_type='PURCHASE_STOCKIN' 且 source_ref LIKE '%'||stockin_code||'%'
--            cost 侧 cost_records.remark/cost_no 里含 stockin_code（活库实测该关联**完全不存在**）
--      附加：stockin_type=1 的同口径缺口（因为这个才是活库里真有的数据）
--   C  store_inventory_log_legacy 有记录但 inventory_movement 无对应行的条数（最直接的漏账证据）
--      口径 C1（幂等键口径）：movement 中不存在 source_type='BACKFILL_TRACE_CONFIRM'
--                            且 source_ref = 'SIL:<id>' 的行 → 这类行永远没被记账
--      口径 C2（业务等价口径）：再用 (material_id=product_id, change_quantity, 时间窗±1 天) 去「抵扣」
--                            已有的 IN movement，统计仍未被覆盖的行（更贴近真实缺口）
--
-- 用法：psql -h localhost -U postgres -d food_traceability -f scripts/db-fix/2026-10-24-audit-trace-confirm-gap.sql
-- ============================================================

\set ON_ERROR_STOP on
\pset pager off

BEGIN;
SET TRANSACTION READ ONLY;

-- ------------------------------------------------------------
-- A：追溯码确认入库 vs inventory
-- ------------------------------------------------------------
\echo ''
\echo '===== A 追溯码 status 分布（先确认目标集是否存在）====='
SELECT status, count(*) AS rows,
       COALESCE(SUM(quantity), 0) AS qty_sum,
       COALESCE(SUM(weight), 0)   AS weight_sum
  FROM material_trace_code
 WHERE deleted = 0
 GROUP BY status
 ORDER BY status;

\echo ''
\echo '===== A1 已确认入库(status=in_stock)但 inventory 无对应增量的追溯码 ====='
WITH loc AS (
    -- 弱映射：material → 现有 inventory 行的最小 location_id
    SELECT material_id, MIN(location_id) AS location_id
      FROM inventory WHERE deleted = 0 AND location_id IS NOT NULL
     GROUP BY material_id
), t AS (
    SELECT tc.id, tc.trace_code, tc.material_id::bigint AS material_id, tc.store_id,
           COALESCE(tc.quantity, 0) AS qty, loc.location_id
      FROM material_trace_code tc
      LEFT JOIN loc ON loc.material_id::text = tc.material_id
     WHERE tc.deleted = 0 AND tc.status = 'in_stock'
)
SELECT (SELECT count(*) FROM t)                                        AS in_stock_codes,
       (SELECT count(*) FROM t WHERE location_id IS NULL)              AS no_location_mapping,
       (SELECT COALESCE(SUM(qty), 0) FROM t)                           AS in_stock_qty_sum,
       (SELECT count(*) FROM t WHERE location_id IS NULL
           OR qty > COALESCE((SELECT i.quantity FROM inventory i
                               WHERE i.deleted = 0 AND i.location_id = t.location_id
                                 AND i.material_id = t.material_id
                               ORDER BY i.inventory_id LIMIT 1), 0))   AS missing_inventory_increment;

\echo ''
\echo '===== A2 按 (location, material) 汇总比对：追溯码入库数量 vs inventory.quantity ====='
WITH loc AS (
    SELECT material_id, MIN(location_id) AS location_id
      FROM inventory WHERE deleted = 0 AND location_id IS NOT NULL GROUP BY material_id
), tc AS (
    SELECT loc.location_id, tc.material_id::bigint AS material_id,
           SUM(COALESCE(tc.quantity, 0)) AS trace_qty, COUNT(*) AS codes
      FROM material_trace_code tc
      LEFT JOIN loc ON loc.material_id::text = tc.material_id
     WHERE tc.deleted = 0 AND tc.status = 'in_stock'
     GROUP BY loc.location_id, tc.material_id::bigint
), inv AS (
    SELECT location_id, material_id, SUM(quantity) AS inv_qty
      FROM inventory WHERE deleted = 0 GROUP BY location_id, material_id
)
SELECT COALESCE(tc.location_id, inv.location_id) AS location_id,
       COALESCE(tc.material_id, inv.material_id)  AS material_id,
       COALESCE(tc.codes, 0)      AS trace_codes,
       COALESCE(tc.trace_qty, 0)  AS trace_qty,
       COALESCE(inv.inv_qty, 0)   AS inventory_qty,
       COALESCE(tc.trace_qty, 0) - COALESCE(inv.inv_qty, 0) AS diff_qty
  FROM tc FULL OUTER JOIN inv
         ON inv.location_id = tc.location_id AND inv.material_id = tc.material_id
 ORDER BY abs(COALESCE(tc.trace_qty, 0) - COALESCE(inv.inv_qty, 0)) DESC
 LIMIT 20;

\echo ''
\echo '===== A3 追溯码明细（前 20，含映射结果）====='
SELECT tc.id, tc.trace_code, tc.material_id, tc.store_id, tc.status,
       COALESCE(tc.quantity, 0) AS qty, tc.scan_time,
       (SELECT MIN(i.location_id) FROM inventory i
         WHERE i.deleted = 0 AND i.material_id::text = tc.material_id) AS mapped_location
  FROM material_trace_code tc
 WHERE tc.deleted = 0
 ORDER BY tc.id
 LIMIT 20;

-- ------------------------------------------------------------
-- B：purchase_stockins 扫码确认 vs movement / cost_records
-- ------------------------------------------------------------
\echo ''
\echo '===== B 库存单类型分布（先确认 stockin_type=2 是否存在）====='
SELECT stockin_type, status, count(*) AS rows,
       COALESCE(SUM(total_quantity), 0) AS qty_sum,
       COALESCE(SUM(total_amount), 0)   AS amount_fen
  FROM purchase_stockins
 WHERE deleted = 0
 GROUP BY stockin_type, status
 ORDER BY stockin_type, status;

\echo ''
\echo '===== B1 stockin_type=2（扫码确认）：无 movement 且无 cost_records 的条数 ====='
WITH s AS (
    SELECT stockin_id, stockin_code, total_quantity, total_amount, create_time
      FROM purchase_stockins
     WHERE deleted = 0 AND stockin_type = 2
), miss AS (
    SELECT s.*,
           EXISTS (SELECT 1 FROM inventory_movement m
                    WHERE m.source_type = 'PURCHASE_STOCKIN'
                      AND m.source_ref LIKE '%' || s.stockin_code || '%')          AS has_movement,
           EXISTS (SELECT 1 FROM cost_records c
                    WHERE c.remark LIKE '%' || s.stockin_code || '%'
                       OR c.cost_no LIKE '%' || s.stockin_code || '%')            AS has_cost
      FROM s
)
SELECT count(*)                                                   AS stockin_type2_rows,
       count(*) FILTER (WHERE NOT has_movement)                    AS no_movement,
       count(*) FILTER (WHERE NOT has_cost)                        AS no_cost_record,
       count(*) FILTER (WHERE NOT has_movement AND NOT has_cost)   AS both_missing,
       COALESCE(SUM(total_quantity) FILTER (WHERE NOT has_movement), 0) AS qty_missing,
       COALESCE(SUM(total_amount)   FILTER (WHERE NOT has_movement), 0) AS amount_missing_fen
  FROM miss;

\echo ''
\echo '===== B2【附加·活库真实缺口】stockin_type=1：无 movement / 无 cost 的条数 ====='
WITH s AS (
    SELECT stockin_id, stockin_code, total_quantity, total_amount, create_time
      FROM purchase_stockins
     WHERE deleted = 0 AND stockin_type = 1 AND status = 1
), miss AS (
    SELECT s.*,
           EXISTS (SELECT 1 FROM inventory_movement m
                    WHERE m.source_type = 'PURCHASE_STOCKIN'
                      AND m.source_ref LIKE '%' || s.stockin_code || '%') AS has_movement,
           EXISTS (SELECT 1 FROM cost_records c
                    WHERE c.remark LIKE '%' || s.stockin_code || '%'
                       OR c.cost_no LIKE '%' || s.stockin_code || '%')   AS has_cost
      FROM s
)
SELECT count(*)                                                 AS stockin_type1_done_rows,
       count(*) FILTER (WHERE NOT has_movement)                  AS no_movement,
       count(*) FILTER (WHERE NOT has_cost)                      AS no_cost_record,
       COALESCE(SUM(total_quantity) FILTER (WHERE NOT has_movement), 0) AS qty_missing,
       COALESCE(SUM(total_amount)   FILTER (WHERE NOT has_movement), 0) AS amount_missing_fen
  FROM miss;

\echo ''
\echo '===== B3 明细（前 20，type=1 里既无 movement 也无 cost 的）====='
SELECT s.stockin_id, s.stockin_code, s.stockin_type, s.status, s.total_quantity, s.total_amount, s.create_time
  FROM purchase_stockins s
 WHERE s.deleted = 0 AND s.status = 1
   AND NOT EXISTS (SELECT 1 FROM inventory_movement m
                    WHERE m.source_type = 'PURCHASE_STOCKIN'
                      AND m.source_ref LIKE '%' || s.stockin_code || '%')
   AND NOT EXISTS (SELECT 1 FROM cost_records c
                    WHERE c.remark LIKE '%' || s.stockin_code || '%'
                       OR c.cost_no  LIKE '%' || s.stockin_code || '%')
 ORDER BY s.create_time
 LIMIT 20;

-- ------------------------------------------------------------
-- C：store_inventory_log_legacy 有记录但 inventory_movement 无对应行
-- ------------------------------------------------------------
\echo ''
\echo '===== C 库存流水表 vs movement（先看总量）====='
SELECT (SELECT count(*) FROM store_inventory_log_legacy)                          AS sil_rows,
       (SELECT count(*) FROM store_inventory_log_legacy WHERE type = 1)           AS sil_in_rows,
       (SELECT COALESCE(SUM(change_quantity),0) FROM store_inventory_log_legacy WHERE type = 1) AS sil_in_qty,
       (SELECT count(*) FROM inventory_movement WHERE source_type = 'BACKFILL_TRACE_CONFIRM') AS backfill_movements,
       (SELECT count(*) FROM inventory_movement
         WHERE COALESCE(source_ref,'') LIKE '%SIL%'
            OR COALESCE(remark,'')    LIKE '%SIL%')                               AS movements_mentioning_sil;

\echo ''
\echo '===== C1 幂等键口径：没有任何 movement 认领的 SIL 行 ====='
SELECT type,
       count(*)                                        AS sil_rows,
       count(*) FILTER (WHERE NOT EXISTS (
           SELECT 1 FROM inventory_movement m
            WHERE m.source_type = 'BACKFILL_TRACE_CONFIRM'
              AND m.source_ref = 'SIL:' || sil.id))    AS unclaimed_rows,
       COALESCE(SUM(change_quantity), 0)               AS qty_sum
  FROM store_inventory_log_legacy sil
 WHERE deleted = 0
 GROUP BY type
 ORDER BY type;

\echo ''
\echo '===== C2 业务等价口径：用 (material, qty, ±1天) 抵扣后仍未被覆盖的入库行 ====='
WITH sil AS (
    SELECT sil.id, sil.product_id, sil.store_id, sil.change_quantity, sil.create_time,
           sil.unit_cost, sil.total_cost_change
      FROM store_inventory_log_legacy sil
     WHERE sil.deleted = 0 AND sil.type = 1
), covered AS (
    SELECT sil.id,
           EXISTS (SELECT 1 FROM inventory_movement m
                    WHERE m.movement_type = 'IN'
                      AND m.material_id = sil.product_id
                      AND m.change_qty = sil.change_quantity
                      AND m.create_time BETWEEN sil.create_time - INTERVAL '1 day'
                                            AND sil.create_time + INTERVAL '1 day') AS is_covered
      FROM sil
)
SELECT count(*)                                          AS sil_in_rows,
       count(*) FILTER (WHERE NOT is_covered)             AS uncovered_rows,
       COALESCE(SUM(sil.change_quantity) FILTER (WHERE NOT covered.is_covered), 0) AS uncovered_qty
  FROM covered
  JOIN sil ON sil.id = covered.id;

\echo ''
\echo '===== C3 明细（前 20：C2 未覆盖的入库行）====='
WITH sil AS (
    SELECT sil.id, sil.inventory_id, sil.product_id, sil.product_name, sil.store_id,
           sil.change_quantity, sil.unit_cost, sil.total_cost_change, sil.create_time, sil.remark
      FROM store_inventory_log_legacy sil
     WHERE sil.deleted = 0 AND sil.type = 1
)
SELECT sil.*
  FROM sil
 WHERE NOT EXISTS (SELECT 1 FROM inventory_movement m
                    WHERE m.movement_type = 'IN'
                      AND m.material_id = sil.product_id
                      AND m.change_qty = sil.change_quantity
                      AND m.create_time BETWEEN sil.create_time - INTERVAL '1 day'
                                            AND sil.create_time + INTERVAL '1 day')
 ORDER BY sil.create_time
 LIMIT 20;

-- ------------------------------------------------------------
-- D：成本缺口（为补账报告服务：只补库存/movement，不补成本）
-- ------------------------------------------------------------
\echo ''
\echo '===== D 成本缺口清单（前 20）：有入库流水但拿不到真实单价 ====='
SELECT sil.id, sil.product_id, sil.product_name, sil.store_id, sil.change_quantity,
       sil.unit_cost, sil.total_cost_change, sil.create_time, left(COALESCE(sil.remark,''),40) AS remark,
       CASE WHEN COALESCE(sil.unit_cost, 0) <= 0 THEN '单价缺失(0/NULL)' ELSE '有单价' END AS cost_flag
  FROM store_inventory_log_legacy sil
 WHERE sil.deleted = 0 AND sil.type = 1
   AND COALESCE(sil.unit_cost, 0) <= 0
 ORDER BY sil.create_time
 LIMIT 20;

SELECT count(*) AS cost_missing_rows,
       COALESCE(SUM(change_quantity), 0) AS cost_missing_qty
  FROM store_inventory_log_legacy
 WHERE deleted = 0 AND type = 1 AND COALESCE(unit_cost, 0) <= 0;

COMMIT;
