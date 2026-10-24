-- ============================================================
-- scripts/db-fix/2026-10-24-backfill-cost-records.sql
-- DATA-007 v2.2：采购入库成本补账（cost_records.cost_type=7）
--
-- 状态：默认 dry-run（**先只试算**）；只有 -v apply=1 才写库。
--   psql ... -f 本文件               → 只打印试算数字（本轮交付就是这个）
--   psql ... -v apply=1 -f 本文件    → 真正插入成本行（等 lead 确认后再跑）
--
-- 【为什么要「防重复」到这么细】
--   活库 cost_records 里已经有 25 行 cost_type=7 的「采购入库成本」，但 remark 全是
--   「采购入库成本 - null」（旧代码拿不到单号），金额与 25 张入库单高度重合一
--   只是**没有单号关联**，不是没记账。所以要按两条规则去重，否则会把成本记两遍：
--     (a) 单号命中：cost_records.remark / cost_no 里含该 stockin_code（当前 0 条）
--     (b) 时间+金额命中：cost_type=7 的成本行与该入库单 create_time 相差 < 5 分钟，
--         且 amount = 该单 total_amount 或 amount = 0（0 是旧代码拿不到金额时的写法）
--
-- 【只处理有真实单价的 item】
--   purchase_stockin_items.unit_price > 0。unit_price = 0/NULL 的一律跳过并计数（不臆造单价）。
--   单位口径：items.unit_price / amount / cost_records.amount 全部是「分」。
--
-- 【目标】
--   cost_records 中没有任何一条能与该 (stockin, item) 对上（上面 (a)(b) 都不命中）。
--   幂等键：cost_no = 'D7STK<stockin_id>-<stockin_item_id>'（UNIQUE 约束 cost_records_cost_no_key）。
--
-- 回滚：见文件末尾。
-- ============================================================

\set ON_ERROR_STOP on
\pset pager off

-- ------------------------------------------------------------
-- 试算
-- ------------------------------------------------------------
\echo ''
\echo '===== DRY-RUN 1：成本补账试算 ====='
WITH items AS (
    SELECT s.stockin_id, s.stockin_code, s.stockin_date, s.total_amount, s.create_time AS stockin_create,
           it.stockin_item_id, it.material_id, it.material_name, it.actual_quantity, it.unit_price,
           COALESCE(it.amount, it.actual_quantity * it.unit_price) AS item_amount_fen
      FROM purchase_stockins s
      JOIN purchase_stockin_items it ON it.stockin_id = s.stockin_id AND it.deleted = 0
     WHERE s.deleted = 0 AND s.status = 1
), marked AS (
    SELECT items.*,
           EXISTS (SELECT 1 FROM cost_records c
                    WHERE c.deleted = 0
                      AND (c.remark LIKE '%' || items.stockin_code || '%'
                        OR c.cost_no LIKE '%' || items.stockin_code || '%'))          AS booked_by_code,
           EXISTS (SELECT 1 FROM cost_records c
                    WHERE c.deleted = 0 AND c.cost_type = 7
                      AND abs(EXTRACT(EPOCH FROM (c.create_time - items.stockin_create))) < 300
                      AND (c.amount = items.total_amount OR c.amount = 0))            AS booked_by_time_amount,
           EXISTS (SELECT 1 FROM cost_records c
                    WHERE c.deleted = 0 AND c.cost_no = 'D7STK' || items.stockin_id || '-' || items.stockin_item_id) AS already_backfilled
      FROM items
)
SELECT count(*)                                                                  AS items_total,
       count(*) FILTER (WHERE unit_price IS NULL OR unit_price <= 0)              AS skip_no_price,
       count(*) FILTER (WHERE unit_price > 0 AND (booked_by_code OR booked_by_time_amount)) AS skip_already_booked,
       count(*) FILTER (WHERE unit_price > 0 AND already_backfilled)              AS skip_already_backfilled,
       count(*) FILTER (WHERE unit_price > 0 AND NOT booked_by_code AND NOT booked_by_time_amount AND NOT already_backfilled) AS candidate_rows,
       COALESCE(SUM(item_amount_fen) FILTER (WHERE unit_price > 0 AND NOT booked_by_code AND NOT booked_by_time_amount AND NOT already_backfilled), 0) AS candidate_amount_fen,
       COALESCE(SUM(actual_quantity) FILTER (WHERE unit_price > 0 AND NOT booked_by_code AND NOT booked_by_time_amount AND NOT already_backfilled), 0) AS candidate_qty,
       COALESCE(SUM(item_amount_fen) FILTER (WHERE unit_price > 0 AND (booked_by_code OR booked_by_time_amount)), 0) AS already_booked_amount_fen
  FROM marked;

\echo ''
\echo '===== DRY-RUN 2：候选明细（前 20）====='
WITH items AS (
    SELECT s.stockin_id, s.stockin_code, s.stockin_date, s.total_amount, s.create_time AS stockin_create,
           it.stockin_item_id, it.material_id, it.material_name, it.actual_quantity, it.unit_price,
           COALESCE(it.amount, it.actual_quantity * it.unit_price) AS item_amount_fen
      FROM purchase_stockins s
      JOIN purchase_stockin_items it ON it.stockin_id = s.stockin_id AND it.deleted = 0
     WHERE s.deleted = 0 AND s.status = 1
)
SELECT i.stockin_id, i.stockin_code, i.stockin_date, i.stockin_item_id, i.material_id,
       i.material_name, i.actual_quantity, i.unit_price, i.item_amount_fen,
       to_char(i.stockin_date, 'YYYY-MM') AS period
  FROM items i
 WHERE i.unit_price > 0
   AND NOT EXISTS (SELECT 1 FROM cost_records c
                    WHERE c.deleted = 0
                      AND (c.remark LIKE '%' || i.stockin_code || '%' OR c.cost_no LIKE '%' || i.stockin_code || '%'))
   AND NOT EXISTS (SELECT 1 FROM cost_records c
                    WHERE c.deleted = 0 AND c.cost_type = 7
                      AND abs(EXTRACT(EPOCH FROM (c.create_time - i.stockin_create))) < 300
                      AND (c.amount = i.total_amount OR c.amount = 0))
   AND NOT EXISTS (SELECT 1 FROM cost_records c
                    WHERE c.deleted = 0 AND c.cost_no = 'D7STK' || i.stockin_id || '-' || i.stockin_item_id)
 ORDER BY i.stockin_id, i.stockin_item_id
 LIMIT 20;

\echo ''
\echo '===== DRY-RUN 3：无单价跳过明细（前 20，不臆造）====='
SELECT s.stockin_code, it.stockin_item_id, it.material_id, it.material_name,
       it.actual_quantity, it.unit_price
  FROM purchase_stockins s
  JOIN purchase_stockin_items it ON it.stockin_id = s.stockin_id AND it.deleted = 0
 WHERE s.deleted = 0 AND s.status = 1 AND (it.unit_price IS NULL OR it.unit_price <= 0)
 ORDER BY s.stockin_id, it.stockin_item_id
 LIMIT 20;

-- ------------------------------------------------------------
-- 写库（仅 -v apply=1；本轮不执行）
-- ------------------------------------------------------------
\if :{?apply}
BEGIN;

INSERT INTO public.cost_records (
    cost_no, cost_type, period, cost_center_id, amount, quantity, unit_price,
    calculation_method, related_voucher_id, remark, created_by, updated_by,
    create_time, update_time, deleted, version
)
SELECT 'D7STK' || s.stockin_id || '-' || it.stockin_item_id,
       7,
       to_char(s.stockin_date, 'YYYY-MM'),
       NULL,
       COALESCE(it.amount, it.actual_quantity * it.unit_price),      -- 分
       it.actual_quantity,
       it.unit_price,                                                -- 分
       1,
       NULL,
       '采购入库成本 - ' || s.stockin_code || '（D7BACKFILL 20261024）',
       'system-backfill-20261024',
       'system-backfill-20261024',
       CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 0, 0
  FROM public.purchase_stockins s
  JOIN public.purchase_stockin_items it ON it.stockin_id = s.stockin_id AND it.deleted = 0
 WHERE s.deleted = 0 AND s.status = 1
   AND it.unit_price > 0
   AND NOT EXISTS (SELECT 1 FROM public.cost_records c
                    WHERE c.deleted = 0
                      AND (c.remark LIKE '%' || s.stockin_code || '%' OR c.cost_no LIKE '%' || s.stockin_code || '%'))
   AND NOT EXISTS (SELECT 1 FROM public.cost_records c
                    WHERE c.deleted = 0 AND c.cost_type = 7
                      AND abs(EXTRACT(EPOCH FROM (c.create_time - s.create_time))) < 300
                      AND (c.amount = s.total_amount OR c.amount = 0))
   AND NOT EXISTS (SELECT 1 FROM public.cost_records c
                    WHERE c.deleted = 0 AND c.cost_no = 'D7STK' || s.stockin_id || '-' || it.stockin_item_id);

\echo '===== APPLY 结果复核 ====='
SELECT count(*) AS written_rows, COALESCE(SUM(amount), 0) AS written_amount_fen
  FROM public.cost_records
 WHERE cost_no LIKE 'D7STK%';

COMMIT;
\endif

-- ============================================================
-- ROLLBACK
-- ============================================================
-- DELETE FROM cost_records WHERE cost_no LIKE 'D7STK%';
-- ============================================================
