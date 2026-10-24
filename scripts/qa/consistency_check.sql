-- ============================================================
-- scripts/qa/consistency_check.sql
-- 阶段 2-B：SQL 一致性巡检（提交前检查用）
--
-- 六类断言（A1~A5 参与 PASS/FAIL 判定，**期望 rows=0**；A6 为信息性输出，不影响 TOTAL）：
--   A1  已支付但资金流水缺失：orders.payment_status=2 的订单，没有对应的 SALE 资金流水
--       匹配口径：fund_flows.order_id = orders.order_id
--                 AND flow_direction=1 AND flow_category=1 AND deleted=0
--       注：2026-10-23 起 fund_flows 已有 order_id 列（varchar），不再依赖 remark 字符串匹配；
--           早期靠备注 token 兜底的写法仅用于 order_id 尚未回填的数据，见报告说明。
--   A2  主表金额 ≠ 支付记录总额：orders.paid_amount（分）
--       ≠ SUM(order_payment_records.payment_amount WHERE deleted=0)
--   A3  库存为负：inventory.quantity < 0，或 food.stock < 0，或 foods.stock < 0
--   A4  （附加）同一订单存在多条 SALE 流水 = 重复入账风险
--       口径：fund_flows(order_id 命中订单, 方向=1, 分类=1, deleted=0) 按订单分组 count>1
--   A5  0 元「订单收入」财务流水残留：finance_records
--       amount=0 AND deleted=0 AND record_type=1（收入）AND record_category=101（订单收入）
--       口径来源：DATA-006 清理（scripts/db-fix/2026-10-24-cleanup-zero-amount-finance-records.sql）
--   A6  （信息性，不参与 TOTAL）users.roles 与 user_roles 的角色集合不一致的用户数
--   A7  （参与判定，V3）bank_accounts.balance <> 最新一笔「非补账豁免」flow.balance_after 的账户数
--       豁免白名单（唯一口径）：flow_no LIKE 'FFH%' 或 created_by LIKE 'system/backfill-%'
--       反例：普通流水 balance_after=0 **不豁免**；若它成为账户最新流水，A7 必须报红
--   A8  （信息性，不参与 TOTAL）普通流水（非豁免行）balance_after=0 的条数
--       口径：users.roles 里形如 '["1"]' / '[7]' / '["ROLE_ADMIN"]' 的 token 归一成 role_id
--             （数字直接取，编码走 roles.role_id_str 映射），与 user_roles(deleted=0) 的
--             role_id 集合作对比（数字或 ROLE_ 编码都做同样归一）。
--       为什么不参与判定：授权权威源是 user_roles（鉴权只读它），users.roles 只是冗余镜像；
--       是否影响鉴权在明细里标注（受影响 = users.roles 有角色但 user_roles 无行 → 登录后无权限）。
--
-- 安全约定：
--   * 整个事务 SET TRANSACTION READ ONLY，全部是 SELECT，不写库（wrapper 另外用
--     PGOPTIONS=-c default_transaction_read_only=on 兜底）
--   * 输出：每类一行 '[PASS]/[FAIL] <断言名> rows=<n>'，最后 'TOTAL FAIL=<k>'
--   * 明细：每类最多 20 行，便于直接贴报告
--
-- 用法：
--   psql -h localhost -U postgres -d food_traceability -f scripts/qa/consistency_check.sql
--   或 python scripts/qa/consistency_check.py      （exit 0 全 PASS / 1 有 FAIL / 2 执行错误）
-- ============================================================

\set ON_ERROR_STOP on
\pset pager off

BEGIN;
SET TRANSACTION READ ONLY;

-- ------------------------------------------------------------
-- 汇总：三类 COUNT + TOTAL FAIL（tuples_only + unaligned，输出便于 grep/CI 判定）
-- ------------------------------------------------------------
\pset format unaligned
\pset tuples_only on
WITH a1 AS (
    SELECT count(*) AS n
      FROM orders o
     WHERE o.deleted = 0
       AND o.payment_status = 2
       AND NOT EXISTS (
             SELECT 1 FROM fund_flows f
              WHERE f.deleted = 0
                AND f.flow_direction = 1
                AND f.flow_category = 1
                AND f.order_id = o.order_id)
), a2 AS (
    SELECT count(*) AS n
      FROM (
          SELECT o.order_id
            FROM orders o
            LEFT JOIN (
                SELECT r.order_id, COALESCE(SUM(r.payment_amount), 0) AS sum_records
                  FROM order_payment_records r
                 WHERE r.deleted = 0
                 GROUP BY r.order_id
            ) s ON s.order_id = o.order_id
           WHERE o.deleted = 0
             AND COALESCE(o.paid_amount, 0) <> COALESCE(s.sum_records, 0)
      ) t
), a3_raw AS (
    SELECT 'inventory:quantity' AS source, inventory_id::text AS pk
      FROM inventory WHERE deleted = 0 AND quantity < 0
    UNION ALL
    SELECT 'food:stock', food_code FROM food WHERE deleted = 0 AND stock < 0
    UNION ALL
    SELECT 'foods:stock', food_id::text FROM foods WHERE deleted = 0 AND stock < 0
), a3 AS (
    SELECT count(*) AS n FROM a3_raw
), a5 AS (
    SELECT count(*) AS n
      FROM finance_records
     WHERE amount = 0 AND deleted = 0 AND record_type = 1 AND record_category = 101
), a6 AS (
    -- 信息性：角色集合不一致的用户数（不计入 TOTAL FAIL）
    SELECT count(*) AS n
      FROM (
          WITH ur AS (
              SELECT ur.user_id::bigint AS uid,
                     CASE WHEN ur.role_id ~ '^[0-9]+$' THEN ur.role_id::bigint ELSE r.role_id END AS rid
                FROM user_roles ur
                LEFT JOIN roles r ON r.role_id_str = ur.role_id
               WHERE COALESCE(ur.deleted, 0) = 0
          ), ur_sets AS (
              SELECT uid, COALESCE(array_agg(DISTINCT rid) FILTER (WHERE rid IS NOT NULL), ARRAY[]::bigint[]) AS rids
                FROM ur GROUP BY uid
          ), u_tok AS (
              SELECT u.user_id AS uid, u.username, u.roles,
                     NULLIF(regexp_replace(btrim(t), '^[^0-9A-Za-z_]+|[^0-9A-Za-z_]+$', '', 'g'), '') AS tok
                FROM users u, LATERAL regexp_split_to_table(COALESCE(u.roles, ''), ',') t
               WHERE COALESCE(u.deleted, 0) = 0
          ), u_res AS (
              SELECT ut.uid, ut.username, ut.roles,
                     CASE WHEN ut.tok ~ '^[0-9]+$' THEN ut.tok::bigint ELSE r2.role_id END AS rid
                FROM u_tok ut
                LEFT JOIN roles r2 ON r2.role_id_str = ut.tok
          ), u_sets AS (
              SELECT uid, username, roles,
                     COALESCE(array_agg(DISTINCT rid) FILTER (WHERE rid IS NOT NULL), ARRAY[]::bigint[]) AS rids
                FROM u_res GROUP BY uid, username, roles
          )
          SELECT us.uid
            FROM u_sets us
            LEFT JOIN ur_sets ON ur_sets.uid = us.uid
           WHERE us.rids IS DISTINCT FROM COALESCE(ur_sets.rids, ARRAY[]::bigint[])
      ) t
), a4 AS (
    -- 附加断言：同一订单出现多条 SALE 流水 = 重复入账风险
    SELECT count(*) AS n
      FROM (
          SELECT f.order_id
            FROM fund_flows f
            JOIN orders o ON o.order_id = f.order_id AND o.deleted = 0
           WHERE f.deleted = 0 AND f.flow_direction = 1 AND f.flow_category = 1
           GROUP BY f.order_id
          HAVING count(*) > 1
      ) t
), a7 AS (
    -- V3（§4.2）：账户余额（缓存）必须等于「最新一笔**非补账豁免**流水」的 balance_after。
    -- 豁免白名单（唯一口径）：flow_no LIKE 'FFH%'（历史补账） 或 created_by LIKE 'system/backfill-%'。
    -- 反例（不得豁免）：普通流水 balance_after=0 仍参与比对 —— 若它是某账户最新一笔，就会在这里报红。
    SELECT count(*) AS n
      FROM bank_accounts b
      LEFT JOIN (
          SELECT account_id, balance_after,
                 ROW_NUMBER() OVER (PARTITION BY account_id ORDER BY flow_id DESC) rn
            FROM fund_flows
           WHERE deleted = 0
             AND NOT (flow_no LIKE 'FFH%' OR COALESCE(created_by, '') LIKE 'system/backfill-%')
      ) lf ON lf.account_id = b.account_id AND lf.rn = 1
     WHERE b.deleted = 0
       AND lf.balance_after IS NOT NULL
       AND lf.balance_after <> b.balance
), a8 AS (
    -- 信息性：普通流水（非补账豁免行）balance_after = 0 的条数。
    -- 这类行若成为账户最新流水会直接把 A7 打红；单独列出便于定位是历史遗留还是写入缺陷。
    SELECT count(*) AS n
      FROM fund_flows
     WHERE deleted = 0
       AND balance_after = 0
       AND NOT (flow_no LIKE 'FFH%' OR COALESCE(created_by, '') LIKE 'system/backfill-%')
)
SELECT line
  FROM (
    SELECT 1 AS ord,
           CASE WHEN n = 0 THEN '[PASS] ' ELSE '[FAIL] ' END
           || 'A1 已支付订单缺 SALE 资金流水 rows=' || n AS line
      FROM a1
    UNION ALL
    SELECT 2,
           CASE WHEN n = 0 THEN '[PASS] ' ELSE '[FAIL] ' END
           || 'A2 orders.paid_amount <> SUM(order_payment_records) rows=' || n
      FROM a2
    UNION ALL
    SELECT 3,
           CASE WHEN n = 0 THEN '[PASS] ' ELSE '[FAIL] ' END
           || 'A3 负库存(inventory.quantity / food.stock / foods.stock) rows=' || n
      FROM a3
    UNION ALL
    SELECT 4,
           CASE WHEN n = 0 THEN '[PASS] ' ELSE '[FAIL] ' END
           || 'A4 同一订单存在多条 SALE 流水(重复入账) rows=' || n
      FROM a4
    UNION ALL
    SELECT 5,
           CASE WHEN n = 0 THEN '[PASS] ' ELSE '[FAIL] ' END
           || 'A5 0 元订单收入财务流水残留 rows=' || n
      FROM a5
    UNION ALL
    -- A6 信息性：前缀固定 [INFO]，不进入 TOTAL FAIL（授权权威源是 user_roles）
    SELECT 6,
           '[INFO] A6 users.roles 与 user_roles 角色集合不一致 users=' || n
      FROM a6
    UNION ALL
    -- A7 参与判定：V3 账户余额 vs 最新非补账流水快照（豁免仅 FFH% 与 system/backfill-%）
    SELECT 7,
           CASE WHEN n = 0 THEN '[PASS] ' ELSE '[FAIL] ' END
           || 'A7 V3 账户余额<>最新非补账流水快照 accounts=' || n
      FROM a7
    UNION ALL
    -- A8 信息性：普通流水 balance_after=0 条数（不豁免、但不直接判红；若是最新流水会先让 A7 红）
    SELECT 8,
           '[INFO] A8 普通流水 balance_after=0 rows=' || n
      FROM a8
    UNION ALL
    SELECT 9,
           'TOTAL FAIL=' || (
               CASE WHEN (SELECT n FROM a1) > 0 THEN 1 ELSE 0 END
             + CASE WHEN (SELECT n FROM a2) > 0 THEN 1 ELSE 0 END
             + CASE WHEN (SELECT n FROM a3) > 0 THEN 1 ELSE 0 END
             + CASE WHEN (SELECT n FROM a4) > 0 THEN 1 ELSE 0 END
             + CASE WHEN (SELECT n FROM a5) > 0 THEN 1 ELSE 0 END
             + CASE WHEN (SELECT n FROM a7) > 0 THEN 1 ELSE 0 END)
  ) s
 ORDER BY ord;

\pset format aligned
\pset tuples_only off

-- ------------------------------------------------------------
-- A1 明细（前 20 行，含历史/新增分类）
-- 基线 2026-10-23 20:44 = 订单支付落资金流水链路（F6/PosSaleFinanceRecorder）上线时刻
-- ------------------------------------------------------------
\echo ''
\echo '-- A1 明细（前 20）--'
SELECT o.order_id,
       o.order_number,
       COALESCE(o.paid_amount, 0) AS paid_amount_fen,
       o.payment_time,
       o.create_time,
       CASE WHEN o.create_time < TIMESTAMP '2026-10-23 20:44:00'
            THEN '历史(链路前)'
            ELSE '新增(链路后)' END AS bucket
  FROM orders o
 WHERE o.deleted = 0
   AND o.payment_status = 2
   AND NOT EXISTS (
         SELECT 1 FROM fund_flows f
          WHERE f.deleted = 0
            AND f.flow_direction = 1
            AND f.flow_category = 1
            AND f.order_id = o.order_id)
 ORDER BY o.create_time
 LIMIT 20;

-- ------------------------------------------------------------
-- A2 明细（前 20 行）
-- ------------------------------------------------------------
\echo ''
\echo '-- A2 明细（前 20）--'
SELECT o.order_number,
       o.order_id,
       COALESCE(o.paid_amount, 0)                    AS paid_amount_fen,
       COALESCE(s.sum_records, 0)                    AS sum_records_fen,
       COALESCE(o.paid_amount, 0) - COALESCE(s.sum_records, 0) AS diff_fen,
       COALESCE(s.cnt_records, 0)                    AS record_rows
  FROM orders o
  LEFT JOIN (
      SELECT r.order_id,
             COALESCE(SUM(r.payment_amount), 0) AS sum_records,
             COUNT(*)                           AS cnt_records
        FROM order_payment_records r
       WHERE r.deleted = 0
       GROUP BY r.order_id
  ) s ON s.order_id = o.order_id
 WHERE o.deleted = 0
   AND COALESCE(o.paid_amount, 0) <> COALESCE(s.sum_records, 0)
 ORDER BY abs(COALESCE(o.paid_amount, 0) - COALESCE(s.sum_records, 0)) DESC
 LIMIT 20;

-- ------------------------------------------------------------
-- A3 明细（前 20 行）
-- ------------------------------------------------------------
\echo ''
\echo '-- A3 明细（前 20）--'
WITH a3_raw AS (
    SELECT 'inventory:quantity' AS source, inventory_id::text AS pk,
           quantity AS qty, material_name AS name
      FROM inventory WHERE deleted = 0 AND quantity < 0
    UNION ALL
    SELECT 'food:stock', food_code, stock::numeric, food_name FROM food WHERE deleted = 0 AND stock < 0
    UNION ALL
    SELECT 'foods:stock', food_id::text, stock::numeric, food_name FROM foods WHERE deleted = 0 AND stock < 0
)
SELECT source, pk, name, qty FROM a3_raw ORDER BY source, pk LIMIT 20;

-- ------------------------------------------------------------
-- A4 明细（前 20 行）：同一订单多条 SALE 流水 = 重复入账
-- ------------------------------------------------------------
\echo ''
\echo '-- A4 明细（前 20）--'
SELECT f.order_id,
       o.order_number,
       count(*)                      AS sale_flow_cnt,
       COALESCE(SUM(f.amount), 0)    AS sale_amount_fen,
       COALESCE(o.paid_amount, 0)    AS paid_amount_fen,
       string_agg(f.flow_no, ',' ORDER BY f.flow_id) AS flow_nos
  FROM fund_flows f
  JOIN orders o ON o.order_id = f.order_id AND o.deleted = 0
 WHERE f.deleted = 0 AND f.flow_direction = 1 AND f.flow_category = 1
 GROUP BY f.order_id, o.order_number, o.paid_amount
HAVING count(*) > 1
 ORDER BY count(*) DESC, f.order_id
 LIMIT 20;

-- ------------------------------------------------------------
-- A5 明细（前 20 行）：0 元「订单收入」财务流水残留
-- 期望为空（DATA-006 清理后必须为 0）；若再现，说明订单收入落账链路又写入了 0 元
-- ------------------------------------------------------------
\echo ''
\echo '-- A5 明细（前 20）--'
SELECT record_id, record_no, amount, record_type, record_category,
       approval_status, voucher_id, order_code, deleted, remark, create_time
  FROM finance_records
 WHERE amount = 0 AND deleted = 0 AND record_type = 1 AND record_category = 101
 ORDER BY create_time
 LIMIT 20;

-- ------------------------------------------------------------
-- A6 明细（前 20 行）：users.roles 与 user_roles 角色集合不一致（信息性）
-- 鉴权影响判定：
--   user_roles 无行 + users.roles 有角色 → '受影响：实际无授权（鉴权只读 user_roles）'
--   两边都有但集合不同            → '仅表示法差异/镜像过期，不影响鉴权'
-- ------------------------------------------------------------
\echo ''
\echo '-- A6 明细（前 20）--'
WITH ur AS (
    SELECT ur.user_id::bigint AS uid,
           CASE WHEN ur.role_id ~ '^[0-9]+$' THEN ur.role_id::bigint ELSE r.role_id END AS rid
      FROM user_roles ur
      LEFT JOIN roles r ON r.role_id_str = ur.role_id
     WHERE COALESCE(ur.deleted, 0) = 0
), ur_sets AS (
    SELECT uid, COALESCE(array_agg(DISTINCT rid) FILTER (WHERE rid IS NOT NULL), ARRAY[]::bigint[]) AS rids
      FROM ur GROUP BY uid
), u_tok AS (
    SELECT u.user_id AS uid, u.username, u.roles,
           NULLIF(regexp_replace(btrim(t), '^[^0-9A-Za-z_]+|[^0-9A-Za-z_]+$', '', 'g'), '') AS tok
      FROM users u, LATERAL regexp_split_to_table(COALESCE(u.roles, ''), ',') t
     WHERE COALESCE(u.deleted, 0) = 0
), u_res AS (
    SELECT ut.uid, ut.username, ut.roles,
           CASE WHEN ut.tok ~ '^[0-9]+$' THEN ut.tok::bigint ELSE r2.role_id END AS rid
      FROM u_tok ut
      LEFT JOIN roles r2 ON r2.role_id_str = ut.tok
), u_sets AS (
    SELECT uid, username, roles,
           COALESCE(array_agg(DISTINCT rid) FILTER (WHERE rid IS NOT NULL), ARRAY[]::bigint[]) AS rids
      FROM u_res GROUP BY uid, username, roles
)
SELECT us.uid,
       us.username,
       us.roles                       AS users_roles_raw,
       us.rids                        AS users_roles_ids,
       COALESCE(urs.rids, ARRAY[]::bigint[]) AS user_roles_ids,
       CASE WHEN COALESCE(urs.rids, ARRAY[]::bigint[]) = ARRAY[]::bigint[]
             AND us.rids <> ARRAY[]::bigint[]
            THEN '受影响：实际无授权（鉴权只读 user_roles）'
            ELSE '仅表示法差异/镜像过期，不影响鉴权' END AS auth_impact
  FROM u_sets us
  LEFT JOIN ur_sets urs ON urs.uid = us.uid
 WHERE us.rids IS DISTINCT FROM COALESCE(urs.rids, ARRAY[]::bigint[])
 ORDER BY us.uid
 LIMIT 20;

COMMIT;
