-- =============================================================================
-- 2026-10-23 清理「现结订单不应生成的应收」脏数据（可复放 / 幂等）
--
-- 背景：应收账款编号 "AR" + order_code 由 OrderCompletedEventListener 在「已完成但未付清」时生成。
--       真实链路「后厨先完成、收银后付款」时完成事件早于支付，payment_status 还不是 2，
--       于是现结单被误生成 ART… 应收，随后又被 POS 支付核销，污染应收列表。
--       代码侧已加防线（order_source=4 的 POS 现结单一律不生成应收），本脚本负责清历史脏号。
--
-- 处理口径（只标记，不物理删除）：
--   A 已结清：receivable_no = 'AR'+订单号 且 订单 payment_status=2 且已全额收款
--             → status=3(已核销)、balance_amount=0、deleted=1、remark 追加清理标记
--   B 孤儿：匹配不到订单（如探针/手工插入，order_id 为空）且余额已为 0
--             → status=3、deleted=1、remark 追加清理标记
--   C 其余（匹配到订单但确实未付清）→ 保留，仅打印待人工复核清单
--
-- 备份：receivables_backup_20261023（LIKE receivables，含全部列），先备份后更新。
-- 用法：psql -h localhost -U postgres -d food_traceability -v ON_ERROR_STOP=1 -f <本文件>
-- =============================================================================
\set ON_ERROR_STOP on

BEGIN;

-- ---------- 0) 备份表（幂等） ----------
CREATE TABLE IF NOT EXISTS receivables_backup_20261023 (LIKE receivables INCLUDING DEFAULTS);

INSERT INTO receivables_backup_20261023
SELECT r.*
FROM receivables r
WHERE r.receivable_no LIKE 'ART%'
  AND NOT EXISTS (
        SELECT 1 FROM receivables_backup_20261023 b WHERE b.receivable_id = r.receivable_id
      );

-- ---------- 1) 执行前统计 ----------
SELECT 'BEFORE total(all)'                AS metric, count(*)::text AS value FROM receivables
UNION ALL
SELECT 'BEFORE ART(all)',                        count(*)::text FROM receivables WHERE receivable_no LIKE 'ART%'
UNION ALL
SELECT 'BEFORE ART(active)',                     count(*)::text FROM receivables WHERE receivable_no LIKE 'ART%' AND deleted = 0
UNION ALL
SELECT 'BEFORE ART(订单已结清)',
       count(*)::text
FROM receivables r
JOIN orders o ON ('AR' || COALESCE(o.order_code, o.order_number)) = r.receivable_no
WHERE r.receivable_no LIKE 'ART%' AND r.deleted = 0 AND o.payment_status = 2
UNION ALL
SELECT 'BEFORE ART(孤儿:无匹配订单且余额0)',
       count(*)::text
FROM receivables r
WHERE r.receivable_no LIKE 'ART%' AND r.deleted = 0
  AND r.balance_amount = 0
  AND NOT EXISTS (
        SELECT 1 FROM orders o WHERE ('AR' || COALESCE(o.order_code, o.order_number)) = r.receivable_no
      );

-- ---------- 2) 清理 A：订单已结清的现结应收 ----------
WITH target AS (
    SELECT r.receivable_id
    FROM receivables r
    JOIN orders o ON ('AR' || COALESCE(o.order_code, o.order_number)) = r.receivable_no
    WHERE r.receivable_no LIKE 'ART%'
      AND r.deleted = 0
      AND o.payment_status = 2
      AND COALESCE(o.paid_amount, 0) >= COALESCE(o.final_amount, o.total_amount, 0)
)
UPDATE receivables r
SET status          = 3,
    balance_amount  = 0,
    received_amount = GREATEST(COALESCE(r.received_amount, 0), COALESCE(r.original_amount, 0)),
    deleted         = 1,
    remark          = COALESCE(r.remark, '') || ' [CLEANUP-20261023: 现结订单不应生成应收，已核销并逻辑删除]',
    update_time     = now()
WHERE r.receivable_id IN (SELECT receivable_id FROM target);

-- ---------- 3) 清理 B：孤儿 ART 且余额为 0（探针/手工数据） ----------
WITH orphan AS (
    SELECT r.receivable_id
    FROM receivables r
    WHERE r.receivable_no LIKE 'ART%'
      AND r.deleted = 0
      AND r.balance_amount = 0
      AND NOT EXISTS (
            SELECT 1 FROM orders o WHERE ('AR' || COALESCE(o.order_code, o.order_number)) = r.receivable_no
          )
)
UPDATE receivables r
SET status      = 3,
    deleted     = 1,
    remark      = COALESCE(r.remark, '') || ' [CLEANUP-20261023: 无对应订单的现结应收残留，已归档]',
    update_time = now()
WHERE r.receivable_id IN (SELECT receivable_id FROM orphan);

COMMIT;

-- ---------- 4) 执行后统计 ----------
SELECT 'AFTER total(all)'                 AS metric, count(*)::text AS value FROM receivables
UNION ALL
SELECT 'AFTER ART(all)',                         count(*)::text FROM receivables WHERE receivable_no LIKE 'ART%'
UNION ALL
SELECT 'AFTER ART(active)',                      count(*)::text FROM receivables WHERE receivable_no LIKE 'ART%' AND deleted = 0
UNION ALL
SELECT 'AFTER ART(active 且订单已结清)',
       count(*)::text
FROM receivables r
JOIN orders o ON ('AR' || COALESCE(o.order_code, o.order_number)) = r.receivable_no
WHERE r.receivable_no LIKE 'ART%' AND r.deleted = 0 AND o.payment_status = 2
UNION ALL
SELECT 'AFTER backup rows',                      count(*)::text FROM receivables_backup_20261023
UNION ALL
SELECT 'AFTER 待人工复核(ART active 且订单仍未付清)',
       count(*)::text
FROM receivables r
JOIN orders o ON ('AR' || COALESCE(o.order_code, o.order_number)) = r.receivable_no
WHERE r.receivable_no LIKE 'ART%' AND r.deleted = 0 AND COALESCE(o.payment_status, 0) <> 2;

-- 残留明细（用于人工确认，正常情况下为空）
SELECT r.receivable_id, r.receivable_no, r.status, r.deleted, r.balance_amount, r.remark
FROM receivables r
WHERE r.receivable_no LIKE 'ART%' AND r.deleted = 0
ORDER BY r.receivable_id;
