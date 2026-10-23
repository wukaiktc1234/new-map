-- ============================================================
-- 2026-10-23-backfill-historical-sale-flows.sql
-- 存量订单补账：把「已收到钱但没有 SALE 资金流水」的历史订单补齐，
-- 让 scripts/check_finance_invariants.py 的 [a] 项收敛。
--
-- 【为什么要补】
--   orders.payment_status=2（现结）的订单有 161 笔 / 782330 分，
--   但 fund_flows 里 SALE 收入（flow_direction=1 且 flow_category=1）只有 105 条 / 344560 分。
--   缺的这批订单确实收过钱（order_payment_records 有合计等于 paid_amount 的支付记录，
--   其中 4 笔是 2026-10-23 的 C5 回填，payment_method=0 未记录），
--   只是入账链路（F6 / PosSaleFinanceRecorder）20:44 上线之前的历史单没有落流水。
--
-- 【安全约定（务必读）】
--   1) 先备份：fund_flows → fund_flows_backup_20261023_hist（表已存在则跳过，可复放）。
--   2) balance_after 一律写 0：历史补账**不回写银行账户余额**，
--      因为当前余额是上线后真实业务滚出来的，补账往余额里再加一遍会篡改现状。
--      平衡关系靠 order_payment_records 与 orders.paid_amount 对账，不靠 balance_after。
--   3) 金额只认 order_payment_records 该单的合计（分），不猜、不用 final_amount 顶替；
--      某单没有任何支付记录 → 跳过并计数（不臆造金额）。
--   4) 账户按「门店 + 支付方式」匹配 seed 三件套（现金 type=2 / 微信 / 支付宝），
--      银行卡落本店或总部 type=1 银行账户；payment_method=0（未记录）无法判渠道，
--      落 type=1 银行账户并在 remark 里标注「渠道未记录」，绝不猜成某个渠道。
--   5) 幂等：目标订单用 NOT EXISTS 按 remark 里的 [orderId=<orderId>] token 判重，
--      重复执行插入 0 行。flow_no 用 'FFH' + 业务日期 + 序号，与线上 'FF...' 不冲突。
--   6) 多支付方式拆成多条流水（每条 = 一个 payment_method 的汇总），
--      并在 remark 追加 [method=<n>]，保证「拆解合计 == 该单实收总额」。
--
-- 用法：
--   psql -h localhost -U postgres -d food_traceability -f 本文件
--   可选清理（默认关闭；把 3 条无主 SALE 流水软删除，让 [a] 的条数/金额残差归零）：
--   psql ... -v cleanup_extra_flows=1 -f 本文件
-- ============================================================

\set ON_ERROR_STOP on

BEGIN;

-- ---------- 0) 备份 ----------
DO $$
DECLARE
    v_rows bigint;
BEGIN
    IF to_regclass('public.fund_flows_backup_20261023_hist') IS NULL THEN
        CREATE TABLE public.fund_flows_backup_20261023_hist AS SELECT * FROM public.fund_flows;
        SELECT COUNT(*) INTO v_rows FROM public.fund_flows_backup_20261023_hist;
        RAISE NOTICE '[ok] 已备份 fund_flows → fund_flows_backup_20261023_hist（% 行）', v_rows;
    ELSE
        SELECT COUNT(*) INTO v_rows FROM public.fund_flows_backup_20261023_hist;
        RAISE NOTICE '[skip] 备份表 fund_flows_backup_20261023_hist 已存在（% 行），不覆盖', v_rows;
    END IF;
END $$;

-- ---------- 1) BEFORE 快照 ----------
SELECT 'BEFORE' AS phase,
       (SELECT COUNT(*) FROM public.fund_flows)                                             AS fund_flows_rows,
       (SELECT COUNT(*) FROM public.fund_flows WHERE deleted=0 AND flow_direction=1 AND flow_category=1) AS sale_flows,
       (SELECT COALESCE(SUM(amount),0) FROM public.fund_flows WHERE deleted=0 AND flow_direction=1 AND flow_category=1) AS sale_amount_fen,
       (SELECT COUNT(*) FROM public.orders WHERE deleted=0 AND payment_status=2)             AS paid_orders,
       (SELECT COALESCE(SUM(paid_amount),0) FROM public.orders WHERE deleted=0 AND payment_status=2) AS paid_amount_fen;

-- 待补订单里「没有任何支付记录」的数量（这些会被跳过，不臆造金额）
SELECT 'SKIP(no payment records)' AS phase, COUNT(*) AS skip_orders
  FROM public.orders o
 WHERE o.deleted = 0 AND o.payment_status = 2
   AND NOT EXISTS (SELECT 1 FROM public.order_payment_records r WHERE r.order_id = o.order_id AND r.deleted = 0)
   AND NOT EXISTS (SELECT 1 FROM public.fund_flows f
                    WHERE f.deleted = 0 AND f.flow_direction = 1 AND f.flow_category = 1
                      AND f.remark LIKE '%[orderId=' || o.order_id || ']%');

-- ---------- 2) 补账 ----------
WITH paid AS (
    -- 目标订单：现结、未删除、且没有任何带 [orderId=xxx] token 的 SALE 流水
    SELECT o.order_id,
           o.order_number,
           COALESCE(NULLIF(o.order_code, ''), o.order_id) AS order_code,
           o.store_id,
           o.customer_name,
           COALESCE(o.create_time::date, CURRENT_DATE)    AS biz_date
      FROM public.orders o
     WHERE o.deleted = 0
       AND o.payment_status = 2
       AND NOT EXISTS (
             SELECT 1 FROM public.fund_flows f
              WHERE f.deleted = 0 AND f.flow_direction = 1 AND f.flow_category = 1
                AND f.remark LIKE '%[orderId=' || o.order_id || ']%')
), agg AS (
    -- 金额唯一来源：order_payment_records 按支付方式汇总（分）
    SELECT o.order_id,
           r.payment_method,
           SUM(r.payment_amount) AS amount_fen,
           COUNT(*)              AS record_cnt
      FROM public.orders o
      JOIN public.order_payment_records r
        ON r.order_id = o.order_id AND r.deleted = 0
     WHERE o.deleted = 0 AND o.payment_status = 2
       AND EXISTS (SELECT 1 FROM paid p WHERE p.order_id = o.order_id)
     GROUP BY o.order_id, r.payment_method
    HAVING SUM(r.payment_amount) > 0
), multi AS (
    -- 该单是否有多支付方式（决定 remark 是否追加 [method=n]）
    SELECT order_id, COUNT(*) AS method_cnt FROM agg GROUP BY order_id
), picked AS (
    SELECT p.order_id, p.order_number, p.order_code, p.store_id, p.biz_date, p.customer_name,
           a.payment_method, a.amount_fen, m.method_cnt,
           acct.account_id
      FROM paid p
      JOIN agg a   ON a.order_id = p.order_id
      JOIN multi m ON m.order_id = p.order_id
      LEFT JOIN LATERAL (
          -- 账户匹配：门店优先（本店 → 总部），再按支付方式匹配 seed 账户
          SELECT c.account_id
            FROM (
                SELECT ba.account_id,
                       CASE WHEN p.store_id IS NOT NULL AND ba.store_id = p.store_id THEN 2
                            WHEN ba.store_id IS NULL THEN 1
                            ELSE 0 END AS store_fit,
                       CASE
                           WHEN a.payment_method = 1 THEN CASE WHEN ba.account_type = 2 THEN 2
                                                               WHEN ba.account_name LIKE '%现金%' THEN 1
                                                               ELSE 0 END
                           WHEN a.payment_method = 2 THEN CASE WHEN ba.account_name LIKE '%微信%' THEN 2 ELSE 0 END
                           WHEN a.payment_method = 3 THEN CASE WHEN ba.account_name LIKE '%支付宝%' THEN 2 ELSE 0 END
                           WHEN a.payment_method = 4 THEN CASE WHEN ba.account_type = 1
                                                                AND ba.account_name NOT LIKE '%微信%'
                                                                AND ba.account_name NOT LIKE '%支付宝%' THEN 2 ELSE 0 END
                           -- 0/未知渠道：只认「中性银行账户」，排除微信/支付宝专户，避免把未记录的钱记到某个渠道户
                           ELSE CASE WHEN ba.account_type = 1
                                      AND ba.account_name NOT LIKE '%微信%'
                                      AND ba.account_name NOT LIKE '%支付宝%' THEN 1 ELSE 0 END
                       END AS method_fit
                  FROM public.bank_accounts ba
                 WHERE ba.deleted = 0 AND COALESCE(ba.status, 1) = 1
            ) c
           WHERE c.method_fit > 0
           ORDER BY c.store_fit DESC, c.method_fit DESC, c.account_id
           LIMIT 1
      ) acct ON TRUE
), numbered AS (
    SELECT k.*,
           COALESCE((
               SELECT MAX(NULLIF(regexp_replace(f.flow_no, '^FFH[0-9]{8}', ''), '')::int)
                 FROM public.fund_flows f
                WHERE f.flow_no LIKE 'FFH' || to_char(k.biz_date, 'YYYYMMDD') || '%'
           ), 0)
           + ROW_NUMBER() OVER (PARTITION BY k.biz_date ORDER BY k.order_id, k.payment_method) AS seq
      FROM picked k
)
INSERT INTO public.fund_flows (
    flow_no, account_id, flow_direction, flow_category, amount, balance_after,
    counterparty_name, business_date, remark,
    create_user_id, created_by, status
)
SELECT
    'FFH' || to_char(n.biz_date, 'YYYYMMDD') || lpad(n.seq::text, 4, '0')      AS flow_no,
    n.account_id,
    1,                                                                          -- flow_direction：收入
    1,                                                                          -- flow_category：销售收款(SALE)
    n.amount_fen,
    0,                                                                          -- balance_after：历史补账不回写余额（见头部安全约定 2）
    COALESCE(NULLIF(n.customer_name, ''), '散客'),
    n.biz_date,
    -- remark：线上 POS 入账同款幂等键 [orderId=xxx] + 补账标记；多渠道单追加 [method=n]
    'POS销售入账：' || n.order_number || '[orderId=' || n.order_id || '][历史补账20261023]'
        || CASE WHEN n.method_cnt > 1 THEN '[method=' || n.payment_method || ']' ELSE '' END,
    0,                                                                          -- create_user_id：0=系统补账
    'system-backfill-20261023',
    2                                                                           -- status：已完成
  FROM numbered n
 WHERE n.account_id IS NOT NULL;

-- ---------- 3) AFTER 快照 ----------
SELECT 'AFTER' AS phase,
       (SELECT COUNT(*) FROM public.fund_flows)                                             AS fund_flows_rows,
       (SELECT COUNT(*) FROM public.fund_flows WHERE deleted=0 AND flow_direction=1 AND flow_category=1) AS sale_flows,
       (SELECT COALESCE(SUM(amount),0) FROM public.fund_flows WHERE deleted=0 AND flow_direction=1 AND flow_category=1) AS sale_amount_fen,
       (SELECT COUNT(*) FROM public.orders WHERE deleted=0 AND payment_status=2)             AS paid_orders,
       (SELECT COALESCE(SUM(paid_amount),0) FROM public.orders WHERE deleted=0 AND payment_status=2) AS paid_amount_fen;

-- 仍未覆盖的现结订单（应为「无支付记录」的那些，脚本按约定跳过）
SELECT 'REMAINING' AS phase, COUNT(*) AS still_uncovered
  FROM public.orders o
 WHERE o.deleted = 0 AND o.payment_status = 2
   AND NOT EXISTS (SELECT 1 FROM public.fund_flows f
                    WHERE f.deleted = 0 AND f.flow_direction = 1 AND f.flow_category = 1
                      AND f.remark LIKE '%[orderId=' || o.order_id || ']%');

COMMIT;

-- ---------- 4) 可选：清理无主 SALE 流水（默认关闭） ----------
-- 3 条 2026-09-08 的 QA 占位流水（remark 为 'QA????' 等，金额 100000/50600/500 分）
-- 没有任何已支付订单与之对应，会让 [a] 的条数/金额残差恒为 -3 / -151100。
-- 需要打绿时用 -v cleanup_extra_flows=1 显式开启；本质是软删除（deleted=1），可回滚。
\if :{?cleanup_extra_flows}
BEGIN;
\echo '== CLEANUP: 软删除无主 SALE 流水 =='
SELECT f.flow_id, f.flow_no, f.amount, f.remark
  FROM public.fund_flows f
 WHERE f.deleted = 0 AND f.flow_direction = 1 AND f.flow_category = 1
   AND NOT EXISTS (SELECT 1 FROM public.orders o
                    WHERE o.deleted = 0 AND o.payment_status = 2
                      AND f.remark LIKE '%[orderId=' || o.order_id || ']%');

UPDATE public.fund_flows f
   SET deleted = 1,
       update_time = CURRENT_TIMESTAMP
 WHERE f.deleted = 0 AND f.flow_direction = 1 AND f.flow_category = 1
   AND NOT EXISTS (SELECT 1 FROM public.orders o
                    WHERE o.deleted = 0 AND o.payment_status = 2
                      AND f.remark LIKE '%[orderId=' || o.order_id || ']%');

SELECT 'CLEANUP' AS phase,
       (SELECT COUNT(*) FROM public.fund_flows WHERE deleted=0 AND flow_direction=1 AND flow_category=1) AS sale_flows,
       (SELECT COALESCE(SUM(amount),0) FROM public.fund_flows WHERE deleted=0 AND flow_direction=1 AND flow_category=1) AS sale_amount_fen;
COMMIT;
\endif
