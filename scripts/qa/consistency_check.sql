-- ============================================================
-- scripts/qa/consistency_check.sql
-- 阶段 2-B：SQL 一致性巡检（提交前检查用）
--
-- 断言清单（A1/A2/A3/A4/A5/A7/A9/A10/A11/A12/A13/A14/A15/A19 参与 PASS/FAIL 判定，**期望 rows=0**；
-- A6/A8/A16/A17/A18/A20/A21 为信息性输出，不影响 TOTAL）：
--
-- ⚠️ 编号对齐（P2-C，避免与既有编号冲突）：
--    设计稿 AC-38 → 本脚本 **A12**（recon_runs.run_key 唯一）
--    设计稿 AC-39 → 本脚本 **A13**（deposit_slips.shift_id 唯一）
--    设计稿 AC-40 → 本脚本 **A14**（存行流水成对）
--    设计稿 AC-41 → 本脚本 **A15**（现金户余额非负）
--    （A7 已被 V3 账户余额占用、A10/A11 已被 P1-D 占用，故不复用 A7–A10）
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
--   A8  （信息性，不参与 TOTAL）普通流水（非补账豁免行）balance_after=0 的**纯统计条数**（仅观测，不判定）
--   A9  （**参与判定，进入 TOTAL FAIL**）普通流水 balance_after=0 总数（**零容忍**）
--       → [PASS] rows=0 / [FAIL] rows=<n> —— 存在 <n> 条普通流水 balance_after=0，禁止接受
--       **A9 零容忍：>0 即红，无白名单**。原「已知历史 0 值项（精确 flow_no 白名单）」机制已整体删除
--       （测试数据已物理清空，白名单不再有意义）；A8 与 A9 此时数值相同是**有意设计**：
--       A8 = 观测口径（INFO，便于看趋势），A9 = 判定口径（FAIL，用于门禁），与 A19 从 INFO 升 FAIL 同风格。
--       口径：users.roles 里形如 '["1"]' / '[7]' / '["ROLE_ADMIN"]' 的 token 归一成 role_id
--             （数字直接取，编码走 roles.role_id_str 映射），与 user_roles(deleted=0) 的
--             role_id 集合作对比（数字或 ROLE_ 编码都做同样归一）。
--       为什么不参与判定：授权权威源是 user_roles（鉴权只读它），users.roles 只是冗余镜像；
--       是否影响鉴权在明细里标注（受影响 = users.roles 有角色但 user_roles 无行 → 登录后无权限）。
--   A10 （参与判定，P1-D）status='FINALIZED' 的 recon_runs 却没有任何 recon_items
--       = 跑批「声称完成却零结果」；期望 rows=0。
--       守卫：recon_runs / recon_items（及其判定所需列）不存在时输出
--             [INFO] A10 跳过（表未创建），**不计入 TOTAL**。
--   A11 （参与判定，P1-D）status='APPROVED' 的 reconciliation_statements 中
--       approved_by = submitted_by（同一人自审）**或** approved_by 属 ROLE_TELLER（出纳自批）；
--       期望 rows=0。角色判定：
--         user_roles ur JOIN roles r ON r.role_id_str = ur.role_id
--          WHERE ur.user_id::text = approved_by::text
--            AND r.role_id_str = 'ROLE_TELLER' AND COALESCE(ur.deleted,0)=0
--       守卫：reconciliation_statements（及其判定所需列）不存在时输出
--             [INFO] A11 跳过（表未创建），**不计入 TOTAL**。
--   A12 （参与判定，AC-38）recon_runs.run_key 必须唯一：count(*) − count(DISTINCT run_key) = 0
--       守卫：recon_runs（含 run_key 列）不存在时输出 [INFO] A12 跳过（表未创建），不计入 TOTAL
--   A13 （参与判定，AC-39）deposit_slips.shift_id 必须唯一（TD-33 一班次一单，deleted=0）：
--       count(*) − count(DISTINCT shift_id) = 0
--       守卫：deposit_slips（含 shift_id/deleted 列）不存在时输出 [INFO] A13 跳过（表未创建），不计入 TOTAL
--   A14 （参与判定，AC-40）存行流水成对：对每个 remark 形如 [depositSlip=<slip_no>] 的幂等键，
--       必须恰好 2 条（出 1 / 入 1）且 sum(case when flow_direction=2 then amount else -amount end) = 0；
--       不满足的 slip 数 = 0。无该注释键时（P2 未落盘）天然为 0 → PASS，**不是 N/A**
--   A15 （参与判定，AC-41）现金户余额非负：bank_accounts 中 account_type=2 且 balance<0 且 deleted=0 的行数 = 0
--   A16 （信息性，不参与 TOTAL）近 24 小时内 fund_flows 中 remark LIKE '%[cashBackfill]%' 且 deleted=0 的条数
--       = POS 入账失败后被「补记」的次数；>0 时同一行追加提示需排查 pos_fund_flow_recorded=failed。
--       AC 无对应编号，属运维看板项；与 P3 backlog『POS 入账异常被吞』配套；与 A8/A9 同级，纯 INFO。
--   A18 （信息性，不进 TOTAL）**假平衡残留**：is_balanced=true 但 adjusted_book_balance ≠ adjusted_bank_balance，
--       或该 statement 下仍残留 reason LIKE 'AMBIGUOUS:%' 的 item。
--       **A18 是 BL-6 假平衡门禁的持续性保障：即使生成逻辑被改回，A18 也能巡检抓出。**
--       守卫：reconciliation_statements / reconciliation_items（含所需列）不存在 → [INFO] 跳过。
--   A19 （**参与判定，进入 TOTAL FAIL**）**同额两侧并存 = 形态假平衡**：
--       同一 statement 内某金额 X 同时出现在 (A|B) 与 (C|D) 两侧 → 计数即该 statement 数（不是金额对数量）。
--       这是 BL-6 假平衡的**形态兜底**：A18 依赖 'AMBIGUOUS:' 前缀，重算把前缀冲掉就查不出来，
--       而 A19 只看数据形态，因此**它是「禁止 APPROVED」的硬依据**：>0 即 FAIL。
--       FAIL 文案固定为「存在 N 张调节表同额两侧并存（形态假平衡），禁止 APPROVED」。
--   A20 （信息性，不进 TOTAL）**历史负 balance_after**：status=2 且 deleted=0 的 fund_flows 中 balance_after<0 的行数与账户数。
--       A15 只看**当前** bank_accounts.balance；A20 看**历史每一笔** fund_flows.balance_after，
--       两者不是替代关系 —— 当前为正不代表历史上没出现过负中间态（BL-4 修复前即存在）。
--       守卫：fund_flows（含 balance_after/status/deleted/account_id 列）不存在 → [INFO] 跳过。
--   A21 （信息性，不进 TOTAL）**可疑补账**：created_by 是 backfill 署名且 remark 含补记账 token，
--       但 flow_no **不是 FFH 前缀** → 前缀与审计署名不一致（可能伪造 / 历史手工补账）。
--       **A21 与 A7 分工：A7 只看 created_by + remark token（不看前缀）；前缀异常由 A21 单独报，
--         不纳入 A7 豁免，也不会让 A7 报红。**
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
-- A10 / A11 前置守卫（P1-D）
--   P1 迁移未落盘时，两张表（recon_runs / recon_items / reconciliation_statements）
--   可能尚不存在。psql 会在**解析期**就报 relation 不存在，因此不能用 CASE 包住静态查询，
--   必须用 \if 条件分支：缺表 → 只输出 [INFO] 跳过，且变量置 0（不进 TOTAL FAIL）。
--   \gset 同时把「整行输出文本」与「计数」写进 psql 变量，供下方汇总 UNION 与 TOTAL 使用。
-- ------------------------------------------------------------
SELECT (to_regclass('public.recon_runs') IS NOT NULL
        AND to_regclass('public.recon_items') IS NOT NULL
        AND EXISTS (SELECT 1 FROM information_schema.columns
                     WHERE table_schema = 'public' AND table_name = 'recon_runs'
                       AND column_name IN ('run_id', 'status') GROUP BY table_name HAVING count(*) = 2)
        AND EXISTS (SELECT 1 FROM information_schema.columns
                     WHERE table_schema = 'public' AND table_name = 'recon_items'
                       AND column_name = 'run_id')) AS recon_ready
\gset

\if :recon_ready
SELECT CASE WHEN count(*) = 0 THEN '[PASS] ' ELSE '[FAIL] ' END
       || 'A10 FINALIZED 的 recon_run 无 recon_items rows=' || count(*) AS a10_line,
       count(*) AS a10_n
  FROM recon_runs r
 WHERE r.status = 'FINALIZED'
   AND NOT EXISTS (SELECT 1 FROM recon_items i WHERE i.run_id = r.run_id)
\gset
\else
\set a10_line '[INFO] A10 跳过（表未创建）'
\set a10_n 0
\endif

SELECT (to_regclass('public.reconciliation_statements') IS NOT NULL
        AND EXISTS (SELECT 1 FROM information_schema.columns
                     WHERE table_schema = 'public' AND table_name = 'reconciliation_statements'
                       AND column_name IN ('status', 'submitted_by', 'approved_by')
                     GROUP BY table_name HAVING count(*) = 3)) AS stmt_ready
\gset

\if :stmt_ready
SELECT CASE WHEN count(*) = 0 THEN '[PASS] ' ELSE '[FAIL] ' END
       || 'A11 APPROVED 调节表自审/出纳自批 rows=' || count(*) AS a11_line,
       count(*) AS a11_n
  FROM reconciliation_statements s
 WHERE s.status = 'APPROVED'
   AND s.approved_by IS NOT NULL
   AND (
         (s.submitted_by IS NOT NULL AND s.approved_by::text = s.submitted_by::text)
         OR EXISTS (
              SELECT 1
                FROM user_roles ur
                JOIN roles r ON r.role_id_str = ur.role_id
               WHERE ur.user_id::text = s.approved_by::text
                 AND r.role_id_str = 'ROLE_TELLER'
                 AND COALESCE(ur.deleted, 0) = 0)
       )
\gset
\else
\set a11_line '[INFO] A11 跳过（表未创建）'
\set a11_n 0
\endif

-- ------------------------------------------------------------
-- A12 – A15 前置守卫与计数（P2-C / AC-38 ~ AC-41）
--   同 A10/A11：表或列不存在时不能用 CASE 包住静态查询（解析期即报错），
--   必须用 \if 分支；缺表分支输出 [INFO] 跳过并把计数置 0（不进 TOTAL）。
--   A14 / A15 只依赖 fund_flows / bank_accounts（A1/A7 已在用的核心表），无守卫。
-- ------------------------------------------------------------
SELECT (to_regclass('public.recon_runs') IS NOT NULL
        AND EXISTS (SELECT 1 FROM information_schema.columns
                     WHERE table_schema = 'public' AND table_name = 'recon_runs'
                       AND column_name = 'run_key')) AS runs_key_ready
\gset

\if :runs_key_ready
SELECT CASE WHEN count(*) - count(DISTINCT run_key) = 0 THEN '[PASS] ' ELSE '[FAIL] ' END
       || 'A12 recon_runs.run_key 唯一 rows=' || (count(*) - count(DISTINCT run_key)) AS a12_line,
       count(*) - count(DISTINCT run_key) AS a12_n
  FROM recon_runs
\gset
\else
\set a12_line '[INFO] A12 跳过（表未创建）'
\set a12_n 0
\endif

SELECT (to_regclass('public.deposit_slips') IS NOT NULL
        AND EXISTS (SELECT 1 FROM information_schema.columns
                     WHERE table_schema = 'public' AND table_name = 'deposit_slips'
                       AND column_name IN ('shift_id', 'deleted')
                     GROUP BY table_name HAVING count(*) = 2)) AS slips_ready
\gset

\if :slips_ready
SELECT CASE WHEN count(*) - count(DISTINCT shift_id) = 0 THEN '[PASS] ' ELSE '[FAIL] ' END
       || 'A13 deposit_slips.shift_id 唯一(一班次一单) rows=' || (count(*) - count(DISTINCT shift_id)) AS a13_line,
       count(*) - count(DISTINCT shift_id) AS a13_n
  FROM deposit_slips
 WHERE deleted = 0
\gset
\else
\set a13_line '[INFO] A13 跳过（表未创建）'
\set a13_n 0
\endif

-- A14：存行流水成对（无 [depositSlip= 键时为 0 → PASS，不是 N/A）
SELECT CASE WHEN count(*) = 0 THEN '[PASS] ' ELSE '[FAIL] ' END
       || 'A14 存行流水未成对(出/入各 1 条且金额相等) slips=' || count(*) AS a14_line,
       count(*) AS a14_n
  FROM (
      SELECT split_part(split_part(f.remark, '[depositSlip=', 2), ']', 1) AS slip_no,
             count(*)                                                       AS cnt,
             sum(CASE WHEN f.flow_direction = 2 THEN f.amount ELSE -f.amount END) AS net
        FROM fund_flows f
       WHERE f.deleted = 0
         AND f.remark LIKE '%[depositSlip=%'
       GROUP BY 1
  ) k
 WHERE k.cnt <> 2 OR k.net <> 0
\gset

-- A15：现金户（account_type=2）余额非负
SELECT CASE WHEN count(*) = 0 THEN '[PASS] ' ELSE '[FAIL] ' END
       || 'A15 现金户余额为负(account_type=2) accounts=' || count(*) AS a15_line,
       count(*) AS a15_n
  FROM bank_accounts
 WHERE deleted = 0
   AND account_type = 2
   AND balance < 0
\gset

-- A16：近 24h POS 入账失败后补记条数（纯 INFO，不进 TOTAL；n>0 时同一行追加排查提示）
SELECT CASE WHEN count(*) = 0
            THEN '[INFO] A16 近24h POS 入账失败后补记 rows=0'
            ELSE '[INFO] A16 近24h POS 入账失败后补记 rows=' || count(*)
                 || '（存在 POS 入账失败后补记，需排查 pos_fund_flow_recorded=failed）'
       END AS a16_line,
       count(*) AS a16_n
  FROM fund_flows f
 WHERE f.deleted = 0
   AND f.remark LIKE '%[cashBackfill]%'
   AND f.create_time >= now() - interval '24 hours'
\gset

-- A17：近 24h 夹具余额抹平动作（finance_audit_logs.operation_type='FIXTURE_FLOW_REVERSED'）
--      纯 INFO，不进 TOTAL；与 A8/A9/A16 同级。
--      意义：该动作只应出现在测试/迁移期（生产 profile 不加载 FinanceOpsController），
--      生产巡检看到 >0 就是「生产上启用了 ops profile」的强信号，需要排查。
--      守卫修复（2026-10-25）：原写法在 CASE 里判 to_regclass，但 FROM finance_audit_logs
--      在**解析期**就会被解析 → 表不存在时直接报错，CASE 根本不会执行（守卫无效）。
--      现改为与 A10–A13/A18–A20 同款：先 \gset 探测表/列，再 \if 分支（缺表时该语句不发送）。
SELECT (to_regclass('public.finance_audit_logs') IS NOT NULL
        AND EXISTS (SELECT 1 FROM information_schema.columns
                     WHERE table_schema = 'public' AND table_name = 'finance_audit_logs'
                       AND column_name IN ('operation_type', 'create_time')
                     GROUP BY table_name HAVING count(*) = 2)) AS fixture_audit_ready
\gset

\if :fixture_audit_ready
SELECT CASE WHEN count(*) = 0
            THEN '[INFO] A17 近24h 夹具余额抹平 FIXTURE_FLOW_REVERSED rows=0'
            ELSE '[INFO] A17 近24h 夹具余额抹平 FIXTURE_FLOW_REVERSED rows=' || count(*)
                 || '（生产 profile 下出现即为异常：ops 端点不该启用）'
       END AS a17_line,
       count(*) AS a17_n
  FROM finance_audit_logs l
 WHERE l.operation_type = 'FIXTURE_FLOW_REVERSED'
   AND l.create_time >= now() - interval '24 hours'
\gset
\else
\set a17_line '[INFO] A17 跳过（表未创建）'
\set a17_n 0
\endif

-- ------------------------------------------------------------
-- A18 / A19 前置守卫（P3 收口）
--   A18 = BL-6「假平衡」门禁的**持续性保障**：即使生成逻辑被改回，
--         A18 也能把 is_balanced=true 但 adjusted_book ≠ adjusted_bank、或残留
--         reason LIKE 'AMBIGUOUS:%' 的调节表巡检抓出。
--   A19 = A18 的数据形态兜底：A18 依赖「AMBIGUOUS:」前缀，若重算把前缀冲掉就查不到；
--         A19 不看前缀，直接找「同一 statement 内同一金额同时出现在 (A|B) 与 (C|D) 两侧」。
--   两表（含所需列）不存在时均输出 [INFO] 跳过，计数置 0（不进 TOTAL）。
-- ------------------------------------------------------------
SELECT (to_regclass('public.reconciliation_statements') IS NOT NULL
        AND to_regclass('public.reconciliation_items') IS NOT NULL
        AND EXISTS (SELECT 1 FROM information_schema.columns
                     WHERE table_schema = 'public' AND table_name = 'reconciliation_statements'
                       AND column_name IN ('is_balanced', 'adjusted_book_balance', 'adjusted_bank_balance')
                     GROUP BY table_name HAVING count(*) = 3)
        AND EXISTS (SELECT 1 FROM information_schema.columns
                     WHERE table_schema = 'public' AND table_name = 'reconciliation_items'
                       AND column_name IN ('statement_id', 'reason')
                     GROUP BY table_name HAVING count(*) = 2)) AS fake_balance_ready
\gset

\if :fake_balance_ready
SELECT CASE WHEN count(*) = 0 THEN '[INFO] ' ELSE '[INFO] ' END
       || 'A18 假平衡残留(adj 不等或含 AMBIGUOUS) statements=' || count(*) AS a18_line,
       count(*) AS a18_n
  FROM reconciliation_statements s
 WHERE s.is_balanced = true
   AND (s.adjusted_book_balance <> s.adjusted_bank_balance
        OR EXISTS (SELECT 1 FROM reconciliation_items i
                    WHERE i.statement_id = s.statement_id
                      AND i.reason LIKE 'AMBIGUOUS:%'))
\gset
\else
\set a18_line '[INFO] A18 跳过（表未创建）'
\set a18_n 0
\endif

SELECT (to_regclass('public.reconciliation_items') IS NOT NULL
        AND EXISTS (SELECT 1 FROM information_schema.columns
                     WHERE table_schema = 'public' AND table_name = 'reconciliation_items'
                       AND column_name IN ('statement_id', 'category', 'amount', 'deleted')
                     GROUP BY table_name HAVING count(*) = 4)) AS ambiguity_shape_ready
\gset

\if :ambiguity_shape_ready
SELECT CASE WHEN count(DISTINCT t.statement_id) = 0
            THEN '[PASS] A19 同额两侧并存(形态假平衡) statements=0'
            ELSE '[FAIL] A19 同额两侧并存(形态假平衡) statements=' || count(DISTINCT t.statement_id)
                 || ' —— 存在 ' || count(DISTINCT t.statement_id)
                 || ' 张调节表同额两侧并存（形态假平衡），禁止 APPROVED'
       END AS a19_line,
       count(DISTINCT t.statement_id) AS a19_n
  FROM (
      SELECT i.statement_id, i.amount
        FROM reconciliation_items i
       WHERE i.deleted = 0
       GROUP BY i.statement_id, i.amount
      HAVING count(*) FILTER (WHERE i.category IN ('A', 'B')) > 0
         AND count(*) FILTER (WHERE i.category IN ('C', 'D')) > 0
  ) t
\gset
\else
\set a19_line '[INFO] A19 跳过（表未创建）'
\set a19_n 0
\endif

SELECT (to_regclass('public.fund_flows') IS NOT NULL
        AND EXISTS (SELECT 1 FROM information_schema.columns
                     WHERE table_schema = 'public' AND table_name = 'fund_flows'
                       AND column_name IN ('balance_after', 'status', 'deleted', 'account_id')
                     GROUP BY table_name HAVING count(*) = 4)) AS negative_history_ready
\gset

\if :negative_history_ready
-- A20：历史负 balance_after 扫描（纯 INFO，不进 TOTAL）
--   A15 只看**当前** bank_accounts.balance；A20 看**历史每一笔** fund_flows.balance_after，
--   两者不是替代关系 —— 当前为正不代表历史上没出现过负中间态（BL-4 修复前即存在）。
SELECT '[INFO] A20 历史负 balance_after rows=' || COALESCE(sum(cnt), 0)
       || ' accounts=' || count(*) AS a20_line,
       COALESCE(sum(cnt), 0) AS a20_n,
       count(*) AS a20_accounts
  FROM (SELECT f.account_id, count(*) AS cnt
          FROM fund_flows f
         WHERE f.balance_after < 0
           AND f.status = 2
           AND f.deleted = 0
         GROUP BY f.account_id) t
\gset
\else
\set a20_line '[INFO] A20 跳过（表未创建）'
\set a20_n 0
\set a20_accounts 0
\endif

-- ------------------------------------------------------------
-- A21 可疑补账（纯 INFO，不进 TOTAL）
--   口径：created_by LIKE 'system/backfill-%' AND remark 含 [cashBackfill] / [posShift=)
--         **但 flow_no 不是 FFH 前缀** → 前缀与审计署名不一致的可疑补账。
--   与 A7 分工：A7 的豁免只看 created_by + remark token（不看 flow_no 前缀）；
--              前缀异常由 A21 单独报出，既不纳入 A7 豁免、也不会让 A7 报红。
-- ------------------------------------------------------------
SELECT (to_regclass('public.fund_flows') IS NOT NULL
        AND EXISTS (SELECT 1 FROM information_schema.columns
                     WHERE table_schema = 'public' AND table_name = 'fund_flows'
                       AND column_name IN ('created_by', 'remark', 'flow_no', 'deleted')
                     GROUP BY table_name HAVING count(*) = 4)) AS backfill_shape_ready
\gset

\if :backfill_shape_ready
SELECT CASE WHEN count(*) = 0
            THEN '[INFO] A21 可疑补账 flow_no 非 FFH 前缀 rows=0'
            ELSE '[INFO] A21 可疑补账 flow_no 非 FFH 前缀 rows=' || count(*)
                 || '（存在非 FFH 前缀的补账流水，可能为伪造或历史手工补账，请人工核对）'
       END AS a21_line,
       count(*) AS a21_n
  FROM fund_flows
 WHERE deleted = 0
   AND COALESCE(created_by, '') LIKE 'system/backfill-%'
   AND (COALESCE(remark, '') LIKE '%[cashBackfill]%' OR COALESCE(remark, '') LIKE '%[posShift=%')
   AND flow_no NOT LIKE 'FFH%'
\gset
\else
\set a21_line '[INFO] A21 跳过（表未创建）'
\set a21_n 0
\endif

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
    --
    -- 豁免口径（2026-10-25 收口，三条**同时**满足才算豁免）：
    --   ① created_by LIKE 'system/backfill-%'            —— backfill 审计署名
    --   ② AND (remark LIKE '%[cashBackfill]%' OR remark LIKE '%[posShift=%')  —— 补记账备注 token
    --   ③ flow_no LIKE 'FFH%' **单独不再构成豁免依据**（前缀可被伪造/复用，防止绕过 A7）
    -- 原因：补账行不维护 balance_after → 若把它们当作账户最新流水会误报；
    --       但豁免不能只看 flow_no 前缀，必须同时有「backfill 审计名 + 补记账备注 token」，二者缺一不免。
    -- 反例（不得豁免）：普通流水 balance_after=0 仍参与比对 —— 若它是某账户最新一笔，就会在这里报红。
    -- 与 A21 分工：A7 豁免**只看 created_by + remark token，不看 flow_no 前缀**；前缀不一致的可疑补账
    --   由 A21 单独报出，既不被 A7 豁免、也不会让 A7 报红。
    SELECT count(*) AS n
      FROM bank_accounts b
      LEFT JOIN (
          SELECT account_id, balance_after,
                 ROW_NUMBER() OVER (PARTITION BY account_id ORDER BY flow_id DESC) rn
            FROM fund_flows
           WHERE deleted = 0
             AND NOT (COALESCE(created_by, '') LIKE 'system/backfill-%'
                      AND (COALESCE(remark, '') LIKE '%[cashBackfill]%'
                           OR COALESCE(remark, '') LIKE '%[posShift=%'))
      ) lf ON lf.account_id = b.account_id AND lf.rn = 1
     WHERE b.deleted = 0
       AND lf.balance_after IS NOT NULL
       AND lf.balance_after <> b.balance
), a8 AS (
    -- A8（信息性，仅观测）：普通流水（非补账豁免行）balance_after = 0 的纯统计条数。
    -- 2026-10-25 起**白名单机制已整体删除**；豁免口径与 A7 **完全统一**（三条件 AND）：
    --   created_by LIKE 'system/backfill-%' AND (remark 含 [cashBackfill] 或 [posShift=)
    --   —— flow_no LIKE 'FFH%' **不再单独构成豁免**（前缀可被伪造），防止靠前缀绕过零容忍。
    -- 保留 INFO 的理由：与 A9 同源同值，一个看趋势、一个做门禁（A9 零容忍）。
    SELECT count(*) AS n
      FROM fund_flows
     WHERE deleted = 0
       AND balance_after = 0
       AND NOT (COALESCE(created_by, '') LIKE 'system/backfill-%'
                AND (COALESCE(remark, '') LIKE '%[cashBackfill]%'
                     OR COALESCE(remark, '') LIKE '%[posShift=%'))
), a9 AS (
    -- A9（**参与判定，零容忍**）：普通流水 balance_after = 0 总数。**无白名单、无已知历史例外**，
    -- >0 即 FAIL（文案：存在 N 条普通流水 balance_after=0，禁止接受）。
    -- 豁免口径与 A7/A8 统一（三条件 AND）：backfill 审计名 + 补记账备注 token；
    --   flow_no 前缀不作为豁免依据（前缀异常由 A21 单独监控）。
    SELECT count(*) AS n
      FROM fund_flows
     WHERE deleted = 0
       AND balance_after = 0
       AND NOT (COALESCE(created_by, '') LIKE 'system/backfill-%'
                AND (COALESCE(remark, '') LIKE '%[cashBackfill]%'
                     OR COALESCE(remark, '') LIKE '%[posShift=%'))
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
    -- A8 信息性：普通流水 balance_after=0 纯统计（不判定，便于看趋势）
    SELECT 8,
           '[INFO] A8 普通流水 balance_after=0 rows=' || n
      FROM a8
    UNION ALL
    -- A9 参与判定（零容忍）：普通流水 balance_after=0 → >0 即 FAIL，无白名单
    SELECT 9,
           CASE WHEN n = 0
                THEN '[PASS] A9 普通流水 balance_after=0(零容忍) rows=0'
                ELSE '[FAIL] A9 普通流水 balance_after=0(零容忍) rows=' || n
                     || ' —— 存在 ' || n || ' 条普通流水 balance_after=0，禁止接受'
           END
      FROM a9
    UNION ALL
    -- A10 参与判定：FINALIZED 的 run 必须有 items（缺表时输出 [INFO] 跳过，不进 TOTAL）
    SELECT 10,
           :'a10_line'
    UNION ALL
    -- A11 参与判定：APPROVED 调节表不得自审/出纳自批（缺表时输出 [INFO] 跳过，不进 TOTAL）
    SELECT 11,
           :'a11_line'
    UNION ALL
    -- A12 参与判定（AC-38）：recon_runs.run_key 唯一
    SELECT 12,
           :'a12_line'
    UNION ALL
    -- A13 参与判定（AC-39）：deposit_slips.shift_id 唯一（一班次一单）
    SELECT 13,
           :'a13_line'
    UNION ALL
    -- A14 参与判定（AC-40）：存行流水成对（出 1 / 入 1，金额相等）
    SELECT 14,
           :'a14_line'
    UNION ALL
    -- A15 参与判定（AC-41）：现金户余额非负
    SELECT 15,
           :'a15_line'
    UNION ALL
    -- A16 信息性：近 24h POS 入账失败后补记（不进 TOTAL；与 A8/A9 同级）
    SELECT 16,
           :'a16_line'
    UNION ALL
    -- A17 信息性：近 24h 夹具余额抹平（不进 TOTAL；与 A8/A9/A16 同级）
    SELECT 17,
           :'a17_line'
    UNION ALL
    -- A18 信息性：假平衡残留（BL-6 门禁的持续性保障；不进 TOTAL）
    SELECT 18,
           :'a18_line'
    UNION ALL
    -- A19 参与判定：同额两侧并存（BL-6 形态兜底；进入 TOTAL FAIL）
    SELECT 19,
           :'a19_line'
    UNION ALL
    -- A20 信息性：历史负 balance_after（BL-4 遗留；不进 TOTAL）
    SELECT 20,
           :'a20_line'
    UNION ALL
    -- A21 信息性：可疑补账（flow_no 非 FFH 前缀；不进 TOTAL）
    SELECT 21,
           :'a21_line'
    UNION ALL
    SELECT 22,
           'TOTAL FAIL=' || (
               CASE WHEN (SELECT n FROM a1) > 0 THEN 1 ELSE 0 END
             + CASE WHEN (SELECT n FROM a2) > 0 THEN 1 ELSE 0 END
             + CASE WHEN (SELECT n FROM a3) > 0 THEN 1 ELSE 0 END
             + CASE WHEN (SELECT n FROM a4) > 0 THEN 1 ELSE 0 END
             + CASE WHEN (SELECT n FROM a5) > 0 THEN 1 ELSE 0 END
             + CASE WHEN (SELECT n FROM a9) > 0 THEN 1 ELSE 0 END
             + CASE WHEN (SELECT n FROM a7) > 0 THEN 1 ELSE 0 END
             + CASE WHEN :a10_n > 0 THEN 1 ELSE 0 END
             + CASE WHEN :a11_n > 0 THEN 1 ELSE 0 END
             + CASE WHEN :a12_n > 0 THEN 1 ELSE 0 END
             + CASE WHEN :a13_n > 0 THEN 1 ELSE 0 END
             + CASE WHEN :a14_n > 0 THEN 1 ELSE 0 END
             + CASE WHEN :a15_n > 0 THEN 1 ELSE 0 END
             + CASE WHEN :a19_n > 0 THEN 1 ELSE 0 END)
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

-- ------------------------------------------------------------
-- A10 明细（前 20 行）：status='FINALIZED' 但没有任何 recon_items 的跑批
-- 期望为空；出现即为「跑批假装完成」——报表会显示已对账但零结果
-- ------------------------------------------------------------
\if :recon_ready
\echo ''
\echo '-- A10 明细（前 20）--'
SELECT r.run_id, r.run_key, r.account_id, r.status, r.started_at, r.finished_at
  FROM recon_runs r
 WHERE r.status = 'FINALIZED'
   AND NOT EXISTS (SELECT 1 FROM recon_items i WHERE i.run_id = r.run_id)
 ORDER BY r.run_id DESC
 LIMIT 20;
\else
\echo ''
\echo '-- A10 明细跳过（表未创建）--'
\endif

-- ------------------------------------------------------------
-- A11 明细（前 20 行）：APPROVED 调节表中的「自审」与「出纳自批」
-- 期望为空；四眼原则要求提交人与审核人不同，且审核人不得是出纳（ROLE_TELLER）
-- ------------------------------------------------------------
\if :stmt_ready
\echo ''
\echo '-- A11 明细（前 20）--'
SELECT s.status,
       s.submitted_by,
       s.approved_by,
       CASE WHEN s.submitted_by IS NOT NULL AND s.approved_by::text = s.submitted_by::text
            THEN 'approved_by = submitted_by（同一人自审）'
            ELSE 'approved_by 属 ROLE_TELLER（出纳自批）' END AS reason
  FROM reconciliation_statements s
 WHERE s.status = 'APPROVED'
   AND s.approved_by IS NOT NULL
   AND (
         (s.submitted_by IS NOT NULL AND s.approved_by::text = s.submitted_by::text)
         OR EXISTS (
              SELECT 1
                FROM user_roles ur
                JOIN roles r ON r.role_id_str = ur.role_id
               WHERE ur.user_id::text = s.approved_by::text
                 AND r.role_id_str = 'ROLE_TELLER'
                 AND COALESCE(ur.deleted, 0) = 0)
       )
 ORDER BY s.approved_by
 LIMIT 20;
\else
\echo ''
\echo '-- A11 明细跳过（表未创建）--'
\endif

-- ------------------------------------------------------------
-- A12 明细（前 20）：run_key 重复（同一账户同一业务日被建了多个 run）
-- 期望为空；重复会导致「重跑」语义失效、勾稽率被重复计入
-- ------------------------------------------------------------
\if :runs_key_ready
\echo ''
\echo '-- A12 明细（前 20）--'
SELECT r.run_key, count(*) AS run_cnt, min(r.run_id) AS first_run_id, max(r.run_id) AS last_run_id,
       string_agg(r.status, ',' ORDER BY r.run_id) AS statuses
  FROM recon_runs r
 GROUP BY r.run_key
HAVING count(*) > 1
 ORDER BY count(*) DESC, r.run_key
 LIMIT 20;
\else
\echo ''
\echo '-- A12 明细跳过（表未创建）--'
\endif

-- ------------------------------------------------------------
-- A13 明细（前 20）：一个班次挂多张缴款单（违反 TD-33 一班次一单）
-- 期望为空；出现即意味着现金被重复交款/重复入账
-- ------------------------------------------------------------
\if :slips_ready
\echo ''
\echo '-- A13 明细（前 20）--'
SELECT s.shift_id, count(*) AS slip_cnt,
       string_agg(s.slip_no, ',' ORDER BY s.slip_id) AS slip_nos,
       string_agg(s.status, ',' ORDER BY s.slip_id) AS statuses
  FROM deposit_slips s
 WHERE s.deleted = 0
 GROUP BY s.shift_id
HAVING count(*) > 1
 ORDER BY count(*) DESC, s.shift_id
 LIMIT 20;
\else
\echo ''
\echo '-- A13 明细跳过（表未创建）--'
\endif

-- ------------------------------------------------------------
-- A14 明细（前 20）：存行流水未成对（[depositSlip=...] 键对应的出/入条数或净额不合格）
-- 期望为空；P2 缴款单未落盘时本段天然 0 行
-- ------------------------------------------------------------
\echo ''
\echo '-- A14 明细（前 20）--'
SELECT split_part(split_part(f.remark, '[depositSlip=', 2), ']', 1) AS slip_no,
       count(*)                                                        AS flow_cnt,
       sum(CASE WHEN f.flow_direction = 1 THEN 1 ELSE 0 END)           AS in_cnt,
       sum(CASE WHEN f.flow_direction = 2 THEN 1 ELSE 0 END)           AS out_cnt,
       sum(CASE WHEN f.flow_direction = 2 THEN f.amount ELSE -f.amount END) AS net_fen,
       string_agg(f.flow_no, ',' ORDER BY f.flow_id)                   AS flow_nos
  FROM fund_flows f
 WHERE f.deleted = 0
   AND f.remark LIKE '%[depositSlip=%'
 GROUP BY 1
HAVING count(*) <> 2
    OR sum(CASE WHEN f.flow_direction = 2 THEN f.amount ELSE -f.amount END) <> 0
 ORDER BY 1
 LIMIT 20;

-- ------------------------------------------------------------
-- A15 明细（前 20）：现金户余额为负
-- 期望为空；负余额说明存行流水多记、或手工改过余额（见 TD-08 禁止改历史）
-- ------------------------------------------------------------
\echo ''
\echo '-- A15 明细（前 20）--'
SELECT b.account_id, b.account_name, b.account_type, b.store_id, b.balance, b.version, b.update_time
  FROM bank_accounts b
 WHERE b.deleted = 0
   AND b.account_type = 2
   AND b.balance < 0
 ORDER BY b.balance
 LIMIT 20;

-- ------------------------------------------------------------
-- A16 明细（近 24h 前 20 行）：POS 入账失败后补记
-- 出现即说明支付链路曾有「已收款但资金流水未写成」，由补记兜底；需排查上游失败原因
-- ------------------------------------------------------------
\echo ''
\echo '-- A16 明细（近 24h 前 20）--'
SELECT f.flow_no,
       f.order_id,
       f.amount,
       left(f.remark, 80) AS remark_snippet,
       f.create_time
  FROM fund_flows f
 WHERE f.deleted = 0
   AND f.remark LIKE '%[cashBackfill]%'
   AND f.create_time >= now() - interval '24 hours'
 ORDER BY f.create_time DESC
 LIMIT 20;

-- ------------------------------------------------------------
-- A18 明细（前 20）：is_balanced=true 但 (adj 不相等 或 含 AMBIGUOUS 前缀 item)
-- 期望为空；>0 说明「平衡」是数值凑出来的，实际存在未处置歧义
-- ------------------------------------------------------------
\if :fake_balance_ready
\echo ''
\echo '-- A18 明细（前 20）--'
SELECT s.statement_id,
       s.is_balanced,
       s.adjusted_book_balance,
       s.adjusted_bank_balance,
       (SELECT count(*) FROM reconciliation_items i
         WHERE i.statement_id = s.statement_id AND i.reason LIKE 'AMBIGUOUS:%') AS ambiguous_items
  FROM reconciliation_statements s
 WHERE s.is_balanced = true
   AND (s.adjusted_book_balance <> s.adjusted_bank_balance
        OR EXISTS (SELECT 1 FROM reconciliation_items i
                    WHERE i.statement_id = s.statement_id
                      AND i.reason LIKE 'AMBIGUOUS:%'))
 ORDER BY s.statement_id
 LIMIT 20;
\else
\echo ''
\echo '-- A18 明细跳过（表未创建）--'
\endif

-- ------------------------------------------------------------
-- A19 明细（前 20）：同一 statement 内同一金额在 (A|B) 与 (C|D) 两侧同时存在
-- 期望为空；出现即「同一笔钱被记成两边未达」，属未处置歧义（不依赖 AMBIGUOUS 前缀）
-- ------------------------------------------------------------
\if :ambiguity_shape_ready
\echo ''
\echo '-- A19 明细（前 20）--'
SELECT i.statement_id,
       i.amount,
       count(*) FILTER (WHERE i.category IN ('A', 'B')) AS ab_cnt,
       count(*) FILTER (WHERE i.category IN ('C', 'D')) AS cd_cnt
  FROM reconciliation_items i
 WHERE i.deleted = 0
 GROUP BY i.statement_id, i.amount
HAVING count(*) FILTER (WHERE i.category IN ('A', 'B')) > 0
   AND count(*) FILTER (WHERE i.category IN ('C', 'D')) > 0
 ORDER BY i.statement_id, i.amount
 LIMIT 20;
\else
\echo ''
\echo '-- A19 明细跳过（表未创建）--'
\endif

-- ------------------------------------------------------------
-- A20 明细（前 20 个账户）：历史负 balance_after 分布
-- 只统计 status=2（已完成）且 deleted=0；status=3（已取消）的负值不计入
-- ------------------------------------------------------------
\if :negative_history_ready
\echo ''
\echo '-- A20 明细（前 20 账户）--'
SELECT f.account_id,
       count(*)                    AS rows,
       min(f.balance_after)        AS min_ba,
       max(f.balance_after)        AS max_ba,
       min(f.create_time)          AS first_seen,
       max(f.create_time)          AS last_seen
  FROM fund_flows f
 WHERE f.balance_after < 0
   AND f.status = 2
   AND f.deleted = 0
 GROUP BY f.account_id
 ORDER BY f.account_id
 LIMIT 20;
\else
\echo ''
\echo '-- A20 明细跳过（表未创建）--'
\endif


-- ------------------------------------------------------------
-- A9 明细（前 20）：普通流水 balance_after = 0（零容忍判定项）
-- 期望为空；出现即为「流水链路未维护余额快照」，且无任何白名单豁免
-- ------------------------------------------------------------
\echo ''
\echo '-- A9 明细（前 20）--'
SELECT f.flow_id, f.flow_no, f.account_id, f.flow_direction, f.flow_category,
       f.amount, f.balance_after, f.business_date, f.create_time,
       left(COALESCE(f.remark, ''), 60) AS remark_snippet
  FROM fund_flows f
 WHERE f.deleted = 0
   AND f.balance_after = 0
   AND NOT (f.flow_no LIKE 'FFH%' OR COALESCE(f.created_by, '') LIKE 'system/backfill-%')
 ORDER BY f.flow_id
 LIMIT 20;

-- ------------------------------------------------------------
-- A21 明细（前 20）：可疑补账（审计署名/备注 token 齐全但 flow_no 非 FFH 前缀）
-- 期望为空；出现需人工核对来源（伪造 / 历史手工补账）
-- ------------------------------------------------------------
\if :backfill_shape_ready
\echo ''
\echo '-- A21 明细（前 20）--'
SELECT f.flow_no, f.account_id, f.amount, f.created_by,
       left(COALESCE(f.remark, ''), 60) AS remark_snippet,
       f.create_time
  FROM fund_flows f
 WHERE f.deleted = 0
   AND COALESCE(f.created_by, '') LIKE 'system/backfill-%'
   AND (COALESCE(f.remark, '') LIKE '%[cashBackfill]%' OR COALESCE(f.remark, '') LIKE '%[posShift=%')
   AND f.flow_no NOT LIKE 'FFH%'
 ORDER BY f.create_time DESC
 LIMIT 20;
\else
\echo ''
\echo '-- A21 明细跳过（表未创建）--'
\endif

COMMIT;
