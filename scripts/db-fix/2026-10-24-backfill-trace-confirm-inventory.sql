-- ============================================================
-- scripts/db-fix/2026-10-24-backfill-trace-confirm-inventory.sql
--
-- ★★★ 状态：草案（DRAFT）——本轮【不要执行 UPDATE】★★★
--   默认行为是「只读试算」：不带 -v apply=1 时，文件里所有写语句都被 psql 的 \if 跳过，
--   只输出将要影响的行数/数量/成本缺口清单，可以直接跑给 Owner 看。
--
--   执行方式：
--     1) 试算（默认，无写入）：psql ... -f 本文件
--     2) 真正补账（需 Owner 批准后）：
--        psql ... -v apply=1 -f 本文件
--     3) 连不可归属的行也补「仅审计痕迹」的 movement（默认关闭，会造成 inventory 与
--        movement 合计不一致，只在明确需要审计闭环时开）：
--        psql ... -v apply=1 -v apply_unlinkable_audit=1 -f 本文件
--
-- ============================================================
-- 【背景（call-number 正在修代码，这里是数据侧的历史欠账）】
--   MaterialTraceCodeServiceImpl.updateInventoryAfterConfirm 历史实现只写 legacy
--   store_inventory_log_legacy，从未落 inventory/inventory_movement；扫码确认路径也没发
--   PurchaseStockInEvent，所以 cost_records 也没有。
--
-- 【活库事实（2026-10-24 12:20 盘点，见 2026-10-24-audit-trace-confirm-gap.sql）】
--   1) material_trace_code：20 条全部 status='pending'，**in_stock 为 0 条**
--      → 「追溯码 status='in_stock'」这一路今天没有数据可交叉；等 call-number 修完后才可能非空。
--   2) store_inventory_log_legacy：49 条（type=1 入库 21 条 / 557 数量；type=2 出库 28 条）。
--      inventory_movement 里 source_type='BACKFILL_TRACE_CONFIRM' 的 0 条 → 21 条入库行全都没被认领。
--   3) ⚠ 最关键：SIL.inventory_id 指向的 inventory 行**大部分已不存在**（21 条里只有 4 条还能对上），
--      因为后续迁移重建过 inventory。对不上库存行的 17 条（447 数量）**无法安全归属**：
--      往哪个库存行加数量都是猜的，本脚本一律跳过并单独列出（「不臆造」原则）。
--   4) 21 条入库行 unit_cost 全为 0/NULL → 成本无真实单价，**本脚本不补 cost_records**（见末尾清单）。
--   5) 存活库存行里还有 1 条「物料对不上」：SIL id=9 的 product_id=2（菠菜），但它指向的
--      inventory_id=2 现在的 material_id=22（另一物料）——迁移重建把 id 复用了。
--      这种行**同样不能安全归属**（往别人的库存行上加数量 = 记错账），
--      所以归属条件除了「inventory 行存活」还要求 inventory.material_id = sil.product_id。
--
-- 【补账口径】
--   目标集合 = store_inventory_log_legacy 中 deleted=0 且 type=1（入库）且 change_quantity>0 的行
--              ∪ 追溯码 status='in_stock' 的行（当前为空）
--              再按 (material_id/ product_id, change_quantity, 时间 ±1 天) 交叉去重
--              —— 同一物料同一数量同一时间窗只记一次，避免「追溯码 + 库存流水」重复入账。
--
--   幂等键：inventory_movement 中已存在
--          (source_type='BACKFILL_TRACE_CONFIRM' AND source_ref='SIL:<store_inventory_log_legacy.id>')
--          就跳过该行。重复执行 = 0 行。
--
--   写什么：
--     * inventory.quantity += 该 SIL 行的 change_quantity（**仅限 inventory 行仍存活的行**）
--     * inventory_movement 插一行 movement_type='IN'、source_type='BACKFILL_TRACE_CONFIRM'、
--       source_ref='SIL:<id>'、unit_cost/total_cost 留 NULL（不臆造单价）、
--       balance_after 取 SIL.after_stock（legacy 当时算好的余额，不是我推算的）
--   不写什么：
--     * 不补 cost_records（无真实单价）
--     * 不动 account/凭证/订单
--     * 对不可归属的 17 行不改任何库存数量
--
-- 【回滚】
--   见文件末尾 ROLLBACK 段（备份表 + 按 source_type/source_ref 删除）。
-- ============================================================

\set ON_ERROR_STOP on
\pset pager off

BEGIN;

-- ------------------------------------------------------------
-- 0) 备份（仅 apply 模式才建；dry-run 不建表）
-- ------------------------------------------------------------
\if :{?apply}
DO $$
DECLARE
    v_rows bigint;
BEGIN
    IF to_regclass('public.inventory_backup_20261024_trace') IS NULL THEN
        CREATE TABLE public.inventory_backup_20261024_trace AS
        SELECT i.*
          FROM public.inventory i
         WHERE i.inventory_id IN (
               SELECT s.inventory_id
                 FROM public.store_inventory_log_legacy s
                 JOIN public.inventory i2
                   ON i2.inventory_id = s.inventory_id AND i2.deleted = 0
                  AND i2.material_id = s.product_id
                WHERE s.deleted = 0 AND s.type = 1
                  AND NOT EXISTS (SELECT 1 FROM public.inventory_movement m
                                   WHERE m.source_type = 'BACKFILL_TRACE_CONFIRM'
                                     AND m.source_ref = 'SIL:' || s.id));
        SELECT COUNT(*) INTO v_rows FROM public.inventory_backup_20261024_trace;
        RAISE NOTICE '[ok] 已备份 % 行 → inventory_backup_20261024_trace', v_rows;
    ELSE
        SELECT COUNT(*) INTO v_rows FROM public.inventory_backup_20261024_trace;
        RAISE NOTICE '[skip] 备份表已存在（% 行），不覆盖', v_rows;
    END IF;
END $$;
\endif

-- ------------------------------------------------------------
-- 1) 试算：将要处理什么（dry-run 与 apply 都会打印）
-- ------------------------------------------------------------
\echo ''
\echo '===== DRY-RUN 1：目标集合规模 ====='
WITH sil AS (
    SELECT s.id, s.inventory_id, s.product_id, s.change_quantity, s.create_time, s.after_stock, s.unit_cost
      FROM store_inventory_log_legacy s
     WHERE s.deleted = 0 AND s.type = 1 AND COALESCE(s.change_quantity, 0) > 0
), trace_in AS (
    -- 追溯码确认入库（今天 0 条，修完代码后才会出现）
    SELECT tc.id AS trace_id, tc.material_id::bigint AS product_id,
           COALESCE(tc.quantity, 0) AS qty, tc.scan_time AS create_time
      FROM material_trace_code tc
     WHERE tc.deleted = 0 AND tc.status = 'in_stock' AND COALESCE(tc.quantity, 0) > 0
), sil_dedup AS (
    -- 与追溯码交叉去重：同物料同数量 ±1 天已有追溯码入库 → 视为同一笔，SIL 行不再重复补
    SELECT s.*
      FROM sil s
     WHERE NOT EXISTS (SELECT 1 FROM trace_in t
                        WHERE t.product_id = s.product_id
                          AND t.qty = s.change_quantity
                          AND t.create_time BETWEEN s.create_time - INTERVAL '1 day'
                                                AND s.create_time + INTERVAL '1 day')
), target AS (
    SELECT s.id AS sil_id, s.inventory_id, s.product_id, s.change_quantity, s.after_stock, s.unit_cost,
           (SELECT i.quantity FROM inventory i
             WHERE i.inventory_id = s.inventory_id AND i.deleted = 0) AS inv_qty_now,
           (SELECT i.location_id FROM inventory i
             WHERE i.inventory_id = s.inventory_id AND i.deleted = 0) AS location_id,
           EXISTS (SELECT 1 FROM inventory i
                    WHERE i.inventory_id = s.inventory_id AND i.deleted = 0
                      AND i.material_id = s.product_id) AS linkable,
           EXISTS (SELECT 1 FROM inventory_movement m
                    WHERE m.source_type = 'BACKFILL_TRACE_CONFIRM'
                      AND m.source_ref = 'SIL:' || s.id) AS already_done
      FROM sil_dedup s
)
SELECT (SELECT count(*) FROM trace_in)                              AS trace_in_stock_codes,
       (SELECT count(*) FROM sil)                                   AS sil_in_rows,
       (SELECT count(*) FROM sil_dedup)                             AS sil_rows_after_cross_dedup,
       (SELECT count(*) FROM target WHERE NOT already_done)          AS pending_rows,
       (SELECT count(*) FROM target WHERE NOT already_done AND linkable)     AS pending_linkable,
       (SELECT count(*) FROM target WHERE NOT already_done AND NOT linkable) AS pending_unlinkable,
       (SELECT COALESCE(SUM(change_quantity),0) FROM target WHERE NOT already_done AND linkable)     AS qty_linkable,
       (SELECT COALESCE(SUM(change_quantity),0) FROM target WHERE NOT already_done AND NOT linkable) AS qty_unlinkable;

\echo ''
\echo '===== DRY-RUN 2：可安全归属的行（apply 时会被 UPDATE inventory + INSERT movement）====='
WITH target AS (
    SELECT s.id AS sil_id, s.inventory_id, s.product_id, s.change_quantity, s.after_stock,
           i.quantity AS inv_qty_now, i.location_id, i.quantity + s.change_quantity AS inv_qty_after
      FROM store_inventory_log_legacy s
      JOIN inventory i ON i.inventory_id = s.inventory_id AND i.deleted = 0
                      AND i.material_id = s.product_id
     WHERE s.deleted = 0 AND s.type = 1 AND COALESCE(s.change_quantity, 0) > 0
       AND NOT EXISTS (SELECT 1 FROM inventory_movement m
                        WHERE m.source_type = 'BACKFILL_TRACE_CONFIRM'
                          AND m.source_ref = 'SIL:' || s.id)
)
SELECT sil_id, inventory_id, product_id, location_id, inv_qty_now,
       change_quantity, inv_qty_after, after_stock,
       CASE WHEN after_stock IS NOT NULL AND after_stock <> inv_qty_after
            THEN '注意：legacy after_stock 与「当前库存+增量」不一致，movement.balance_after 取 legacy 值'
            ELSE 'OK' END AS note
  FROM target
 ORDER BY sil_id;

\echo ''
\echo '===== DRY-RUN 3：不可归属的行（apply 时默认跳过；带 -v apply_unlinkable_audit=1 才写审计痕迹）====='
SELECT s.id AS sil_id, s.inventory_id, s.product_id, s.product_name, s.store_id,
       s.change_quantity, s.unit_cost, s.create_time, left(COALESCE(s.remark,''), 40) AS remark
  FROM store_inventory_log_legacy s
 WHERE s.deleted = 0 AND s.type = 1 AND COALESCE(s.change_quantity, 0) > 0
   AND NOT EXISTS (SELECT 1 FROM inventory i
                    WHERE i.inventory_id = s.inventory_id AND i.deleted = 0
                      AND i.material_id = s.product_id)
   AND NOT EXISTS (SELECT 1 FROM inventory_movement m
                    WHERE m.source_type = 'BACKFILL_TRACE_CONFIRM'
                      AND m.source_ref = 'SIL:' || s.id)
 ORDER BY s.create_time
 LIMIT 20;

\echo ''
\echo '===== DRY-RUN 4：成本缺口（本脚本不补，交 Owner 决策）====='
SELECT count(*) AS cost_missing_rows,
       COALESCE(SUM(change_quantity), 0) AS cost_missing_qty,
       count(*) FILTER (WHERE unit_cost IS NULL)        AS unit_cost_null,
       count(*) FILTER (WHERE COALESCE(unit_cost,0)=0)  AS unit_cost_zero
  FROM store_inventory_log_legacy
 WHERE deleted = 0 AND type = 1 AND COALESCE(unit_cost, 0) <= 0;

-- ------------------------------------------------------------
-- 2) 写库（仅 apply 模式）：只处理「可安全归属」的行
-- ------------------------------------------------------------
\if :{?apply}
\echo ''
\echo '===== APPLY：inventory.quantity += 增量（仅存活库存行）====='
WITH target AS (
    SELECT s.id AS sil_id, s.inventory_id, s.change_quantity
      FROM store_inventory_log_legacy s
      JOIN inventory i ON i.inventory_id = s.inventory_id AND i.deleted = 0
                      AND i.material_id = s.product_id
     WHERE s.deleted = 0 AND s.type = 1 AND COALESCE(s.change_quantity, 0) > 0
       AND NOT EXISTS (SELECT 1 FROM inventory_movement m
                        WHERE m.source_type = 'BACKFILL_TRACE_CONFIRM'
                          AND m.source_ref = 'SIL:' || s.id)
), agg AS (
    SELECT inventory_id, SUM(change_quantity) AS qty, COUNT(*) AS rows
      FROM target GROUP BY inventory_id
)
UPDATE inventory i
   SET quantity    = i.quantity + agg.qty,
       update_time = CURRENT_TIMESTAMP
  FROM agg
 WHERE i.inventory_id = agg.inventory_id
   AND i.deleted = 0;

\echo '===== APPLY：插入 inventory_movement（幂等：source_ref=SIL:<id>）====='
WITH target AS (
    SELECT s.id AS sil_id, s.inventory_id, s.product_id, s.change_quantity, s.after_stock,
           i.location_id
      FROM store_inventory_log_legacy s
      JOIN inventory i ON i.inventory_id = s.inventory_id AND i.deleted = 0
                      AND i.material_id = s.product_id
     WHERE s.deleted = 0 AND s.type = 1 AND COALESCE(s.change_quantity, 0) > 0
       AND NOT EXISTS (SELECT 1 FROM inventory_movement m
                        WHERE m.source_type = 'BACKFILL_TRACE_CONFIRM'
                          AND m.source_ref = 'SIL:' || s.id)
)
INSERT INTO inventory_movement (
    location_id, material_id, change_qty, balance_after, movement_type,
    source_type, source_ref, operator_id, unit_cost, total_cost, remark, create_time
)
SELECT t.location_id,
       t.product_id,
       t.change_quantity,
       t.after_stock,                                   -- legacy 当时算好的余额，不推算
       'IN',
       'BACKFILL_TRACE_CONFIRM',
       'SIL:' || t.sil_id,
       NULL,
       NULL,                                            -- 单价未知：不臆造
       NULL,                                            -- 金额未知：不臆造
       '历史补账 20261024：追溯码确认入库未落 movement（成本缺失，未补 cost_records）',
       CURRENT_TIMESTAMP
  FROM target t;

\echo ''
\echo '===== APPLY：剩余未认领（应只剩「不可归属」的行）====='
SELECT count(*) AS still_unclaimed
  FROM store_inventory_log_legacy s
 WHERE s.deleted = 0 AND s.type = 1
   AND NOT EXISTS (SELECT 1 FROM inventory_movement m
                    WHERE m.source_type = 'BACKFILL_TRACE_CONFIRM'
                      AND m.source_ref = 'SIL:' || s.id);

-- 可选：为不可归属行写「仅审计痕迹」的 movement（balance_after=NULL，不改库存）
\if :{?apply_unlinkable_audit}
\echo '===== APPLY(可选)：不可归属行只补审计痕迹 ====='
INSERT INTO inventory_movement (
    location_id, material_id, change_qty, balance_after, movement_type,
    source_type, source_ref, operator_id, unit_cost, total_cost, remark, create_time
)
SELECT (SELECT MIN(i.location_id) FROM inventory i
         WHERE i.deleted = 0 AND i.material_id = s.product_id),   -- 弱映射，可能为 NULL
       s.product_id,
       s.change_quantity,
       NULL,                                                   -- 无归属库存行，余额不可考
       'IN',
       'BACKFILL_TRACE_CONFIRM',
       'SIL:' || s.id,
       NULL, NULL, NULL,
       '历史补账 20261024（仅审计痕迹）：SIL 指向的 inventory 行已不存在，未改库存数量',
       CURRENT_TIMESTAMP
  FROM store_inventory_log_legacy s
 WHERE s.deleted = 0 AND s.type = 1 AND COALESCE(s.change_quantity, 0) > 0
   AND NOT EXISTS (SELECT 1 FROM inventory i
                    WHERE i.inventory_id = s.inventory_id AND i.deleted = 0
                      AND i.material_id = s.product_id)
   AND NOT EXISTS (SELECT 1 FROM inventory_movement m
                    WHERE m.source_type = 'BACKFILL_TRACE_CONFIRM'
                      AND m.source_ref = 'SIL:' || s.id);
\endif
\endif

COMMIT;

-- ============================================================
-- ROLLBACK（如需撤销本次补账）
-- ============================================================
-- 1) 删掉本次写入的 movement
--    DELETE FROM inventory_movement
--     WHERE source_type = 'BACKFILL_TRACE_CONFIRM';
-- 2) 库存数量回滚到备份快照
--    UPDATE inventory i
--       SET quantity = b.quantity, update_time = CURRENT_TIMESTAMP
--      FROM inventory_backup_20261024_trace b
--     WHERE i.inventory_id = b.inventory_id;
-- 3) 复核
--    SELECT count(*) FROM inventory_movement WHERE source_type = 'BACKFILL_TRACE_CONFIRM';  -- 应为 0
-- ============================================================
