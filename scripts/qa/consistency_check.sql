-- ============================================================
-- scripts/qa/consistency_check.sql
-- 阶段 2-B：SQL 一致性巡检（提交前检查用）
--
-- 断言清单（A1/A2/A3/A4/A5/A7/A9/A10/A11/A12/A13/A14/A15/A19/A25/A27/A28/A29/A30/A31 参与 PASS/FAIL 判定，**期望 rows=0**；
-- A6/A8/A16/A17/A18/A20/A21/A22/A23/A24/A26 为信息性输出，不影响 TOTAL）：
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
--       反例：合法 0 值流水（balance_after=0）**不豁免**；若它成为账户最新流水且与账户余额不等，A7 必须报红
--   A8  （信息性，不参与 TOTAL）普通流水（非补账豁免行）balance_after **为空（NULL）** 的纯统计条数（仅观测，不判定）
--   A9  （**参与判定，进入 TOTAL FAIL**）普通流水 balance_after **为空（NULL）** 总数（**零容忍**）
--       → [PASS] rows=0 / [FAIL] rows=<n> —— 存在 <n> 条普通流水 balance_after 为空（快照未维护），禁止接受
--       【判据 2026-10-25 Owner 拍板】`= 0` → `IS NULL`：**0 = 合法（账户可取空）**，**NULL = 未维护（脏值）**；
--       对拍证据：以「前一条同账户快照 + 本条 delta」复算，所有 0 值行均成立，故 0 不再视为脏值。
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
--       口径补充（P4-3，2026-10-25 二次修正）：**排除 `cleared_flag = true` 的 item**
--       —— cleared_flag=true 表示该条已被**人工处置**：RESOLVE（改归属，审计 RECON_AMBIGUITY_RESOLVED）
--       或 EXEMPT（判定非歧义，审计 RECON_AMBIGUITY_EXEMPTED）；后端对两者都置 flag
--       （category/amount 不动，规避 ck_recon_items_ref_by_category）。
--       **不是白名单、不是基线**，是「已人工处置」的事实标记；若只排除 'EXEMPTED(' 前缀，
--       RESOLVE 的「已处置(AMBIGUITY->…)」会被漏掉 → 处置完 113 条仍红。
--       输出行末尾另附「其中已人工消除(cleared)条目数 = N」（count cleared_flag=true），便于看是否滥用。
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
--   A22 （信息性，不进 TOTAL）**FFH 前缀但无补账证据**：flow_no 是 FFH 前缀，但缺 backfill 审计名
--       或补记账 token → 与 A21 **对称**（A21=有证据无前缀；A22=有前缀无证据）。
--       **为什么需要**：A7 只在 balance_after 与实际余额不一致时才报，伪造 FFH 前缀且余额恰好
--       对齐的行会**静默通过**；A22 补上这个形态盲区。A7 只看余额一致性，不替代形态观测。
--   A23 （信息性，不进 TOTAL）**未关闭调节表最长挂账天数**：status NOT IN ('APPROVED') 且 deleted=0，
--       以 COALESCE(submitted_at, create_time) 起算挂账天数取 MAX；阈值 STALE_DAYS=7（脚本内常量），
--       >7 时同一行追加提醒。守卫：reconciliation_statements（含 status/deleted/submitted_at/create_time）不存在 → [INFO] 跳过。
--   A24 （信息性，不进 TOTAL）当天 FLOW 号段「取值 − 已落库条数」差值；≠0 提示号段空洞。
--       实际列名为 order_sequence.sequence_key / sequence_value（**不是** seq_key/seq_value），
--       键格式 = 'FLOW' || yyyyMMdd（代码实证 FundFlowServiceImpl / DepositSlipServiceImpl / ReconCashSupport）。
--   A25 （**参与判定，进入 TOTAL FAIL**）烧号账 fund_flow_no_burn_ledger 中 archived_flag=false 的行数
--       = 已取号但未落库的流水号；期望 0，>0 即 FAIL。守卫：缺表 → [INFO] 跳过（不计入 TOTAL）。
--   A26 （信息性，不进 TOTAL）finance_audit_logs.operation_type='RECON_DISPOSITION_LOST' 的行数
--       = 重算把人工处置（RESOLVE/EXEMPT）吃掉，需人工复核。只展示不判定。
--   A27 （**参与判定，进入 TOTAL FAIL**）DRAFT 调节表的 uncleared_* / adjusted_* 必须等于
--       「按 cleared_flag=false 聚合的 items」；差 1 分即不一致（防绕过 service 直改表）。
--       只查 DRAFT（SUBMITTED/APPROVED 为历史快照，允许与当前 items 不同）。
--   A28 （**参与判定，进入 TOTAL FAIL**）结账后无写入：is_closed=true 的期间内，若存在
--       fund_flows 活行满足 business_date ∈ [start_date, end_date] 且 create_time > close_time
--       → 结账门禁失效。期望 0；>0 即 FAIL。
--   A29 （**参与判定，进入 TOTAL FAIL**）结账快照自洽：finance_period_account_balance 每个
--       (period_code, account_id) 必须满足 closing_fen = 期间内末笔 balance_after（无流水时跳过此项）
--       且 closing_fen = opening_fen + income_fen - expense_fen。期望 0；>0 即 FAIL。
--   A30 （**参与判定**）退款侧勾稽①：orders.refund_amount>0 的订单必须有一条退款流水
--       （flow_direction=2、status=2、flow_category ∈ REFUND_FLOW_CATEGORIES、同 order_id、
--        amount = refund_amount::bigint 分）；期望 0。
--   A31 （**参与判定**）退款侧勾稽②：按日 SUM(退款流水) == SUM(订单退款额×100)。
--       orders 无 account_id → 退化为「当日全库合计」口径。
--   ⚠️ TODO(P4-5)：REFUND_FLOW_CATEGORIES 常量等 fund-500 落盘后回填（当前为空 → A30/A31 跳过）。
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
                       AND column_name IN ('statement_id', 'category', 'amount', 'deleted', 'cleared_flag')
                     GROUP BY table_name HAVING count(*) = 5)) AS ambiguity_shape_ready
\gset

\if :ambiguity_shape_ready
SELECT CASE WHEN count(DISTINCT t.statement_id) = 0
            THEN '[PASS] A19 同额两侧并存(形态假平衡) statements=0'
            ELSE '[FAIL] A19 同额两侧并存(形态假平衡) statements=' || count(DISTINCT t.statement_id)
                 || ' —— 存在 ' || count(DISTINCT t.statement_id)
                 || ' 张调节表同额两侧并存（形态假平衡），禁止 APPROVED'
       END
       || '（其中已人工消除(cleared)条目数 = ' ||
          (SELECT count(*) FROM reconciliation_items i2
            WHERE i2.deleted = 0 AND i2.cleared_flag = true) || '）' AS a19_line,
       count(DISTINCT t.statement_id) AS a19_n
  FROM (
      SELECT i.statement_id, i.amount
        FROM reconciliation_items i
       WHERE i.deleted = 0
         -- 人工处置后该条不再参与形态判据（2026-10-25 P4-3 第二处口径修正）：
         --   cleared_flag = true ← RESOLVE（改归属，审计 RECON_AMBIGUITY_RESOLVED）
         --                        或 EXEMPT（判定非歧义，审计 RECON_AMBIGUITY_EXEMPTED）。
         --   后端对两者都置 cleared_flag=true（category/amount 不动，规避 ck_recon_items_ref_by_category）。
         --   若只排除 'EXEMPTED(' 前缀，RESOLVE 的「已处置(AMBIGUITY->…)」会被漏掉 → 处置完仍红。
         --   **不是白名单、不是基线**，是「已人工处置」的事实标记。
         AND i.cleared_flag = false
         -- 兼容保留：历史数据/旧接口可能只写前缀未置 flag
         AND COALESCE(i.reason, '') NOT LIKE 'EXEMPTED(%'
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
-- A22（信息性，不参与 TOTAL）：FFH 前缀但无补账证据
--   与 A21 对称：A21 = 有证据无前缀；A22 = 有前缀无证据。
--   A7 只看余额一致性，不替代形态观测（伪造前缀且余额恰好对齐时会静默通过）。
-- ------------------------------------------------------------
\if :backfill_shape_ready
SELECT CASE WHEN count(*) = 0
            THEN '[INFO] A22 FFH 前缀但无补账证据 rows=0'
            ELSE '[INFO] A22 FFH 前缀但无补账证据 rows=' || count(*)
                 || '（存在 FFH 前缀却无 backfill 审计名/补记账 token 的流水，可能为伪造前缀，请人工核对）'
       END AS a22_line,
       count(*) AS a22_n
  FROM fund_flows
 WHERE deleted = 0
   AND flow_no LIKE 'FFH%'
   AND NOT (COALESCE(created_by, '') LIKE 'system/backfill-%'
            AND (COALESCE(remark, '') LIKE '%[cashBackfill]%' OR COALESCE(remark, '') LIKE '%[posShift=%'))
\gset
\else
\set a22_line '[INFO] A22 跳过（表未创建）'
\set a22_n 0
\endif

-- ------------------------------------------------------------
-- A23（信息性，不参与 TOTAL）：未关闭调节表最长挂账天数
--   口径：status NOT IN ('APPROVED') 且 deleted=0 的调节表，
--         以 COALESCE(submitted_at, create_time) 为起点算「已挂账天数」并取最大值。
--   阈值：**STALE_DAYS = 7**（脚本内常量；如需可配再改成 app 配置）。
--   注：天数用 epoch/86400 取整，而非 EXTRACT(DAY ...)——后者只取 interval 的「日」分量，
--       跨月（≥1 个月）会截断失真；两者在 <1 个月时结果一致。
--   守卫：reconciliation_statements（含 status/deleted/submitted_at/create_time 列）不存在 → [INFO] 跳过。
-- ------------------------------------------------------------
SELECT (to_regclass('public.reconciliation_statements') IS NOT NULL
        AND EXISTS (SELECT 1 FROM information_schema.columns
                     WHERE table_schema = 'public' AND table_name = 'reconciliation_statements'
                       AND column_name IN ('status', 'deleted', 'submitted_at', 'create_time')
                     GROUP BY table_name HAVING count(*) = 4)) AS stmt_stale_ready
\gset

\if :stmt_stale_ready
SELECT CASE WHEN COALESCE(MAX(t.days), 0) > 7
            THEN '[INFO] A23 未关闭调节表最长挂账天数 days=' || COALESCE(MAX(t.days), 0)
                 || '（存在超过 7 天未关闭的调节表，请关注）'
            ELSE '[INFO] A23 未关闭调节表最长挂账天数 days=' || COALESCE(MAX(t.days), 0)
       END AS a23_line,
       COALESCE(MAX(t.days), 0) AS a23_n,
       (COALESCE(MAX(t.days), 0) > 7) AS a23_stale
  FROM (
      SELECT floor(EXTRACT(EPOCH FROM (now() - COALESCE(s.submitted_at, s.create_time))) / 86400)::int AS days
        FROM reconciliation_statements s
       WHERE s.status NOT IN ('APPROVED')
         AND s.deleted = 0
  ) t
\gset
\else
\set a23_line '[INFO] A23 跳过（表未创建）'
\set a23_n 0
\set a23_stale false
\endif

-- ------------------------------------------------------------
-- A24（信息性，不参与 TOTAL）：当天 FLOW 号段「已取值 − 已落库条数」差值
--   ⚠️ 实际列名与键格式（2026-10-25 information_schema 实测）：
--      order_sequence 的列是 **sequence_key / sequence_value**（不是 seq_key / seq_value）；
--      号段键格式为 **'FLOW' || yyyyMMdd**（代码实证：FundFlowServiceImpl:347、
--      DepositSlipServiceImpl:853、ReconCashSupport:76 均为 "FLOW" + datePart）。
--   差值 ≠ 0 说明号段取过号但 fund_flows 没有对应行（号段空洞），详见 A25 烧号账。
--   ⚠️ 已知基线：**清库后需同步重置 order_sequence**（见 purge 脚本）；
--      数据清空但号段保留会产生一个恒定的历史差额（当前为 456），属已知残差、不是新烧号。
--   守卫：order_sequence 或 fund_flows 不存在 → [INFO] 跳过。
--   注：号段行不存在时按 0 计（先 COALESCE 再相减），避免 NULL 把整式吞成 0 而掩盖负差。
-- ------------------------------------------------------------
SELECT (to_regclass('public.order_sequence') IS NOT NULL
        AND to_regclass('public.fund_flows') IS NOT NULL) AS flow_seq_ready
\gset

\if :flow_seq_ready
SELECT CASE WHEN COALESCE((SELECT sequence_value FROM order_sequence
                            WHERE sequence_key = 'FLOW' || to_char(current_date, 'YYYYMMDD')), 0)
                 - (SELECT count(*) FROM fund_flows
                     WHERE business_date = current_date AND deleted = 0) = 0
            THEN '[INFO] A24 当天号段取值-已落库条数 diff=0'
            ELSE '[INFO] A24 当天号段取值-已落库条数 diff=' ||
                 (COALESCE((SELECT sequence_value FROM order_sequence
                             WHERE sequence_key = 'FLOW' || to_char(current_date, 'YYYYMMDD')), 0)
                  - (SELECT count(*) FROM fund_flows
                      WHERE business_date = current_date AND deleted = 0))
                 || '（存在号段空洞，见 A25 烧号账）'
       END AS a24_line,
       (COALESCE((SELECT sequence_value FROM order_sequence
                   WHERE sequence_key = 'FLOW' || to_char(current_date, 'YYYYMMDD')), 0)
        - (SELECT count(*) FROM fund_flows
            WHERE business_date = current_date AND deleted = 0))::bigint AS a24_n
\gset
\else
\set a24_line '[INFO] A24 跳过（表未创建）'
\set a24_n 0
\endif

-- ------------------------------------------------------------
-- A25（**参与判定，进入 TOTAL FAIL**）：烧号账「已取号但迄今仍未落库」行数
--   口径（2026-10-25 修正）：archived_flag = false **AND NOT EXISTS(fund_flows 同 flow_no)**。
--   **为何必须 NOT EXISTS**：取号与 insert 在同一事务时，失败回滚会把**号段本身也回滚**，
--   该号会被后续请求重新取到并成功落库；若只数 archived_flag=false，这一行会永久留在账上，
--   A25 就会**永久误报**（实测：FF202610250474 已被后续请求重用并存在于 fund_flows）。
--   → 语义 = 「曾经交出、迄今仍未落库」= 真正的号洞；已被重用的号不算。
--   **号段语义（Owner 拍板 2026-10-25，不再混淆）**：本系统号段 = **可重用**——取号与 insert 同事务，
--   失败回滚会连号段自增一起回滚，不产生永久空洞；烧号账是「曾经交出过」的审计痕迹，不是号洞账本。
--   因此 A25 的健康值是 0（当前即 0），且**不应**因为一次失败的业务请求而变红。
--   期望 0；>0 即 FAIL（文案：存在 N 个已取号但仍未落库的流水号，禁止接受）。
--   守卫：表或 archived_flag 列不存在 → [INFO] A25 跳过（表未创建），不计入 TOTAL。
-- ------------------------------------------------------------
SELECT (to_regclass('public.fund_flow_no_burn_ledger') IS NOT NULL
        AND EXISTS (SELECT 1 FROM information_schema.columns
                     WHERE table_schema = 'public' AND table_name = 'fund_flow_no_burn_ledger'
                       AND column_name = 'archived_flag')) AS burn_ledger_ready
\gset

\if :burn_ledger_ready
SELECT CASE WHEN count(*) = 0
            THEN '[PASS] A25 烧号账未落库行数 rows=0'
            ELSE '[FAIL] A25 烧号账未落库行数 rows=' || count(*)
                 || ' —— 存在 ' || count(*) || ' 个已取号但仍未落库的流水号，禁止接受'
       END AS a25_line,
       count(*) AS a25_n
  FROM fund_flow_no_burn_ledger b
 WHERE b.archived_flag = false
   AND NOT EXISTS (SELECT 1 FROM fund_flows f WHERE f.flow_no = b.flow_no)
\gset
\else
\set a25_line '[INFO] A25 跳过（表未创建）'
\set a25_n 0
\endif

-- ------------------------------------------------------------
-- A26（信息性，不参与 TOTAL）：重算吃掉人工处置的次数
--   口径：finance_audit_logs.operation_type = 'RECON_DISPOSITION_LOST'
--   语义：重算把此前的人工处置（RESOLVE/EXEMPT）覆盖掉了，需要人工复核。只展示、不判定。
-- ------------------------------------------------------------
SELECT (to_regclass('public.finance_audit_logs') IS NOT NULL
        AND EXISTS (SELECT 1 FROM information_schema.columns
                     WHERE table_schema = 'public' AND table_name = 'finance_audit_logs'
                       AND column_name = 'operation_type'
                     GROUP BY table_name HAVING count(*) = 1)) AS audit_log_ready
\gset

\if :audit_log_ready
SELECT '[INFO] A26 重算吃掉人工处置 RECON_DISPOSITION_LOST rows=' || count(*) AS a26_line,
       count(*) AS a26_n
  FROM finance_audit_logs l
 WHERE l.operation_type = 'RECON_DISPOSITION_LOST'
\gset
\else
\set a26_line '[INFO] A26 跳过（表未创建）'
\set a26_n 0
\endif

-- ------------------------------------------------------------
-- A27（**参与判定，零容忍**）：DRAFT 调节表汇总字段必须等于「按 cleared_flag=false 聚合的 items」
--   目的：防有人绕过 service 直接改表（汇总与明细脱钩）。
--   口径：**只查 DRAFT**；SUBMITTED/APPROVED 是历史快照，允许与当前 items 不同。
--   差 1 分都算不一致；期望 0，>0 即 FAIL。
--   守卫：两表及所需列不存在 → [INFO] 跳过，不计入 TOTAL。
-- ------------------------------------------------------------
SELECT (to_regclass('public.reconciliation_statements') IS NOT NULL
        AND to_regclass('public.reconciliation_items') IS NOT NULL
        AND EXISTS (SELECT 1 FROM information_schema.columns
                     WHERE table_schema = 'public' AND table_name = 'reconciliation_statements'
                       AND column_name IN ('book_balance', 'bank_balance', 'uncleared_in_credit', 'uncleared_in_debit',
                                           'uncleared_out_credit', 'uncleared_out_debit',
                                           'adjusted_book_balance', 'adjusted_bank_balance', 'status', 'deleted')
                     GROUP BY table_name HAVING count(*) = 10)
        AND EXISTS (SELECT 1 FROM information_schema.columns
                     WHERE table_schema = 'public' AND table_name = 'reconciliation_items'
                       AND column_name IN ('statement_id', 'category', 'amount', 'deleted', 'cleared_flag')
                     GROUP BY table_name HAVING count(*) = 5)) AS stmt_sum_ready
\gset

\if :stmt_sum_ready
SELECT CASE WHEN count(*) = 0
            THEN '[PASS] A27 DRAFT 汇总与未清 items 聚合一致 rows=0'
            ELSE '[FAIL] A27 DRAFT 汇总与未清 items 聚合一致 rows=' || count(*)
                 || ' —— 存在 ' || count(*) || ' 张 DRAFT 调节表汇总与明细不一致（疑绕过 service 直改表），禁止接受'
       END AS a27_line,
       count(*) AS a27_n
  FROM reconciliation_statements s
 WHERE s.deleted = 0
   AND s.status = 'DRAFT'
   AND ( s.uncleared_in_credit  <> COALESCE((SELECT SUM(i.amount) FILTER (WHERE i.category = 'A') FROM reconciliation_items i WHERE i.statement_id = s.statement_id AND i.deleted = 0 AND i.cleared_flag = false), 0)
      OR s.uncleared_in_debit   <> COALESCE((SELECT SUM(i.amount) FILTER (WHERE i.category = 'B') FROM reconciliation_items i WHERE i.statement_id = s.statement_id AND i.deleted = 0 AND i.cleared_flag = false), 0)
      OR s.uncleared_out_credit <> COALESCE((SELECT SUM(i.amount) FILTER (WHERE i.category = 'C') FROM reconciliation_items i WHERE i.statement_id = s.statement_id AND i.deleted = 0 AND i.cleared_flag = false), 0)
      OR s.uncleared_out_debit  <> COALESCE((SELECT SUM(i.amount) FILTER (WHERE i.category = 'D') FROM reconciliation_items i WHERE i.statement_id = s.statement_id AND i.deleted = 0 AND i.cleared_flag = false), 0)
      OR s.adjusted_book_balance <> s.book_balance
           + COALESCE((SELECT SUM(i.amount) FILTER (WHERE i.category = 'C') FROM reconciliation_items i WHERE i.statement_id = s.statement_id AND i.deleted = 0 AND i.cleared_flag = false), 0)
           - COALESCE((SELECT SUM(i.amount) FILTER (WHERE i.category = 'D') FROM reconciliation_items i WHERE i.statement_id = s.statement_id AND i.deleted = 0 AND i.cleared_flag = false), 0)
      OR s.adjusted_bank_balance <> s.bank_balance
           + COALESCE((SELECT SUM(i.amount) FILTER (WHERE i.category = 'A') FROM reconciliation_items i WHERE i.statement_id = s.statement_id AND i.deleted = 0 AND i.cleared_flag = false), 0)
           - COALESCE((SELECT SUM(i.amount) FILTER (WHERE i.category = 'B') FROM reconciliation_items i WHERE i.statement_id = s.statement_id AND i.deleted = 0 AND i.cleared_flag = false), 0)
      )
\gset
\else
\set a27_line '[INFO] A27 跳过（表未创建）'
\set a27_n 0
\endif

-- ------------------------------------------------------------
-- A28（**参与判定**）：结账后无写入 —— 已结账期间内不得再有活流水
--   口径：accounting_periods.is_closed = true 的期间，若有 fund_flows 活行满足
--         business_date 落在 [start_date, end_date] 且 create_time > close_time
--         → 结账门禁失效（结账后又写了账）。期望 0，>0 即 FAIL。
--   守卫：任一表/列缺失 → [INFO] A28 跳过（不计入 TOTAL）。
-- ------------------------------------------------------------
SELECT (to_regclass('public.accounting_periods') IS NOT NULL
        AND to_regclass('public.fund_flows') IS NOT NULL
        AND EXISTS (SELECT 1 FROM information_schema.columns
                     WHERE table_schema = 'public' AND table_name = 'accounting_periods'
                       AND column_name IN ('period_code', 'is_closed', 'close_time', 'start_date', 'end_date', 'deleted')
                     GROUP BY table_name HAVING count(*) = 6)
        AND EXISTS (SELECT 1 FROM information_schema.columns
                     WHERE table_schema = 'public' AND table_name = 'fund_flows'
                       AND column_name IN ('account_id', 'business_date', 'create_time', 'deleted')
                     GROUP BY table_name HAVING count(*) = 4)) AS closed_write_ready
\gset

\if :closed_write_ready
SELECT CASE WHEN count(*) = 0
            THEN '[PASS] A28 结账后仍写入流水 rows=0'
            ELSE '[FAIL] A28 结账后仍写入流水 rows=' || count(*)
                 || ' —— 存在 ' || count(*) || ' 条结账后写入的活流水（结账门禁失效），禁止接受'
       END AS a28_line,
       count(*) AS a28_n
  FROM accounting_periods p
  JOIN fund_flows f
    ON f.deleted = 0
   AND f.business_date BETWEEN p.start_date AND p.end_date
   AND f.create_time > p.close_time
 WHERE p.deleted = 0
   AND p.is_closed = true
   AND p.close_time IS NOT NULL
   AND p.start_date IS NOT NULL
   AND p.end_date IS NOT NULL
\gset
\else
\set a28_line '[INFO] A28 跳过（表未创建）'
\set a28_n 0
\endif

-- ------------------------------------------------------------
-- A29（**参与判定**）：结账快照自洽
--   对每个已结账期间 + 账户，finance_period_account_balance.closing_fen 必须：
--     ① = 该账户期间内「最后一笔」balance_after（business_date DESC, flow_id DESC 取末笔）；
--        期间内无流水时①跳过（无事实可比，避免误红）；
--     ② = opening_fen + income_fen - expense_fen。
--   任一不成立即 1 行不一致；期望 0，>0 即 FAIL。
--   守卫：表/列缺失 → [INFO] A29 跳过（不计入 TOTAL）。
-- ------------------------------------------------------------
SELECT (to_regclass('public.finance_period_account_balance') IS NOT NULL
        AND to_regclass('public.accounting_periods') IS NOT NULL
        AND to_regclass('public.fund_flows') IS NOT NULL
        AND EXISTS (SELECT 1 FROM information_schema.columns
                     WHERE table_schema = 'public' AND table_name = 'finance_period_account_balance'
                       AND column_name IN ('period_code', 'account_id', 'opening_fen', 'income_fen', 'expense_fen', 'closing_fen')
                     GROUP BY table_name HAVING count(*) = 6)
        AND EXISTS (SELECT 1 FROM information_schema.columns
                     WHERE table_schema = 'public' AND table_name = 'fund_flows'
                       AND column_name IN ('account_id', 'balance_after', 'business_date', 'flow_id', 'deleted')
                     GROUP BY table_name HAVING count(*) = 5)) AS period_balance_ready
\gset

\if :period_balance_ready
SELECT CASE WHEN count(*) = 0
            THEN '[PASS] A29 结账快照自洽(closing=末笔余额且=期初+收-支) rows=0'
            ELSE '[FAIL] A29 结账快照自洽(closing=末笔余额且=期初+收-支) rows=' || count(*)
                 || ' —— 存在 ' || count(*) || ' 个期间账户快照不自洽，禁止接受'
       END AS a29_line,
       count(*) AS a29_n
  FROM finance_period_account_balance b
  JOIN accounting_periods p
    ON p.period_code = b.period_code
   AND p.deleted = 0
   AND p.is_closed = true
  LEFT JOIN LATERAL (
      SELECT f.balance_after
        FROM fund_flows f
       WHERE f.deleted = 0
         AND f.account_id = b.account_id
         AND f.balance_after IS NOT NULL
         AND f.business_date BETWEEN p.start_date AND p.end_date
       ORDER BY f.business_date DESC, f.flow_id DESC
       LIMIT 1
  ) last ON true
 WHERE ( last.balance_after IS NOT NULL AND b.closing_fen <> last.balance_after )
    OR b.closing_fen <> b.opening_fen + b.income_fen - b.expense_fen
\gset
\else
\set a29_line '[INFO] A29 跳过（表未创建）'
\set a29_n 0
\endif

-- ------------------------------------------------------------
-- A30 / A31（**参与判定**，退款侧勾稽）
--   【P4-5 已定案】FLOW_CATEGORY_REFUND = **2**（fund-500 已落盘：1=SALE、2=REFUND、
--      5=存款/内部转账）。**5 是存款内部转账，勿与退款混用**（现有 flow_direction=2 且
--      category=5 的行是存行出账，不是退款）。常量清空则 A30/A31 一律 [INFO] 跳过（不误红）。
--   单位口径（2026-10-26 修正）：orders.refund_amount 虽是 numeric(12,2)，但**按「分」存**
--            （与 paid_amount / total_amount 同尺度）；fund_flows.amount 也是分 → **直接比较，禁止 ×100**。
--            证据（fund-500 实测）：某单 paid_amount=1800(分)、total_amount=1800(分)、
--            refund_amount=1800.00，对应退款流水 amount=1800(分)。若当成「元」，18 元订单会退 1800 元，不合理。
-- ------------------------------------------------------------
\set REFUND_FLOW_CATEGORIES '2'

SELECT (to_regclass('public.orders') IS NOT NULL
        AND to_regclass('public.fund_flows') IS NOT NULL
        AND :'REFUND_FLOW_CATEGORIES' <> ''
        AND EXISTS (SELECT 1 FROM information_schema.columns
                     WHERE table_schema = 'public' AND table_name = 'orders'
                       AND column_name IN ('order_id', 'refund_amount', 'deleted', 'refund_time', 'create_time')
                     GROUP BY table_name HAVING count(*) = 5)
        AND EXISTS (SELECT 1 FROM information_schema.columns
                     WHERE table_schema = 'public' AND table_name = 'fund_flows'
                       AND column_name IN ('order_id', 'amount', 'flow_direction', 'flow_category', 'status', 'deleted', 'business_date')
                     GROUP BY table_name HAVING count(*) = 7)) AS refund_ready
\gset

\if :refund_ready
-- A30：有退款额但缺对应退款流水（零容忍）
SELECT CASE WHEN count(*) = 0
            THEN '[PASS] A30 有退款额但无退款流水 orders=0'
            ELSE '[FAIL] A30 有退款额但无退款流水 orders=' || count(*)
                 || ' —— 存在 ' || count(*) || ' 笔订单有退款额但缺对应退款流水，禁止接受'
       END AS a30_line,
       count(*) AS a30_n
  FROM orders o
 WHERE o.deleted = 0
   AND o.refund_amount > 0
   AND NOT EXISTS (
         SELECT 1 FROM fund_flows f
          WHERE f.deleted = 0
            AND f.status = 2
            AND f.flow_direction = 2
            AND f.order_id = o.order_id
            AND f.flow_category = ANY (string_to_array(:'REFUND_FLOW_CATEGORIES', ',')::int[])
            AND f.amount = o.refund_amount::bigint)
\gset

-- A31：按日退款流水合计 vs 订单退款合计（orders 无 account_id → 退化为「当日全库合计」口径）
SELECT CASE WHEN count(*) = 0
            THEN '[PASS] A31 当日退款流水合计=订单退款合计 days=0'
            ELSE '[FAIL] A31 当日退款流水合计=订单退款合计 days=' || count(*)
                 || ' —— 存在 ' || count(*) || ' 个自然日的退款流水合计与订单退款合计不符，禁止接受'
       END AS a31_line,
       count(*) AS a31_n
  FROM (
      SELECT COALESCE(fl.d, od.d) AS d,
             COALESCE(fl.amt, 0) AS flow_amt,
             COALESCE(od.amt, 0) AS order_amt
        FROM (SELECT f.business_date AS d, SUM(f.amount) AS amt
                FROM fund_flows f
               WHERE f.deleted = 0 AND f.status = 2 AND f.flow_direction = 2
                 AND f.flow_category = ANY (string_to_array(:'REFUND_FLOW_CATEGORIES', ',')::int[])
               GROUP BY 1) fl
        FULL JOIN (SELECT COALESCE(o.refund_time, o.create_time)::date AS d,
                          SUM(o.refund_amount)::bigint AS amt
                     FROM orders o
                    WHERE o.deleted = 0 AND o.refund_amount > 0
                    GROUP BY 1) od ON od.d = fl.d
  ) t
 WHERE t.flow_amt IS DISTINCT FROM t.order_amt
\gset
\else
\set a30_line '[INFO] A30 跳过（退款 flow_category 常量未配置 / 表未创建）'
\set a30_n 0
\set a31_line '[INFO] A31 跳过（退款 flow_category 常量未配置 / 表未创建）'
\set a31_n 0
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
    -- A8（信息性，仅观测）：普通流水（非补账豁免行）**balance_after IS NULL** 的纯统计条数。
    -- 【2026-10-25 修正】原判据 `balance_after = 0` 无法区分「从未维护（脏值）」与「账户被取空（合法 0）」，
    --   实测会误报（存行把余额恰好扣到 0 时产生合法 0 值行）；改为 `IS NULL` = 未维护，0 视为合法值。
    -- 2026-10-25 起**白名单机制已整体删除**；豁免口径与 A7 **完全统一**（三条件 AND）：
    --   created_by LIKE 'system/backfill-%' AND (remark 含 [cashBackfill] 或 [posShift=)
    --   —— flow_no LIKE 'FFH%' **不再单独构成豁免**（前缀可被伪造），防止靠前缀绕过零容忍。
    -- 保留 INFO 的理由：与 A9 同源同值，一个看趋势、一个做门禁（A9 零容忍）。
    SELECT count(*) AS n
      FROM fund_flows
     WHERE deleted = 0
       AND balance_after IS NULL
       AND NOT (COALESCE(created_by, '') LIKE 'system/backfill-%'
                AND (COALESCE(remark, '') LIKE '%[cashBackfill]%'
                     OR COALESCE(remark, '') LIKE '%[posShift=%'))
), a9 AS (
    -- A9（**参与判定，零容忍**）：普通流水 balance_after **为空（NULL）** 的总数。**无白名单、无已知历史例外**。
    -- 【2026-10-25 Owner 拍板】判据由 `balance_after = 0` 改为 `IS NULL`：
    --   0 = **合法值**（账户被取空，如存行把现金户恰好扣到 0；用「前一条同账户快照 + 本条 delta」复算成立）；
    --   NULL = **未维护（脏值）**，这才是要拦的。原判据会把合法 0 误判为脏值（A9 曾因此直接打红）。
    -- >0 即 FAIL（文案：存在 N 条普通流水 balance_after 为空（快照未维护），禁止接受）。
    -- 豁免口径与 A7/A8 统一（三条件 AND）：backfill 审计名 + 补记账备注 token；
    --   flow_no 前缀不作为豁免依据（前缀异常由 A21 单独监控）。
    SELECT count(*) AS n
      FROM fund_flows
     WHERE deleted = 0
       AND balance_after IS NULL
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
    -- A8 信息性：普通流水 balance_after 为空（NULL）纯统计（不判定，便于看趋势）
    SELECT 8,
           '[INFO] A8 普通流水 balance_after 为空 rows=' || n
      FROM a8
    UNION ALL
    -- A9 参与判定（零容忍）：balance_after IS NULL → >0 即 FAIL，无白名单（0 值属合法）
    SELECT 9,
           CASE WHEN n = 0
                THEN '[PASS] A9 普通流水 balance_after 为空(零容忍) rows=0'
                ELSE '[FAIL] A9 普通流水 balance_after 为空(零容忍) rows=' || n
                     || ' —— 存在 ' || n || ' 条普通流水 balance_after 为空（快照未维护），禁止接受'
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
    -- A22 信息性：FFH 前缀但无补账证据（与 A21 对称；不进 TOTAL）
    SELECT 22,
           :'a22_line'
    UNION ALL
    -- A23 信息性：未关闭调节表最长挂账天数（不进 TOTAL）
    SELECT 23,
           :'a23_line'
    UNION ALL
    -- A24 信息性：当天号段取值-已落库条数（不进 TOTAL）
    SELECT 24,
           :'a24_line'
    UNION ALL
    -- A25 参与判定：烧号账未归档行数（缺表时 [INFO] 跳过；>0 进 TOTAL）
    SELECT 25,
           :'a25_line'
    UNION ALL
    -- A26 信息性：重算吃掉人工处置（不进 TOTAL）
    SELECT 26,
           :'a26_line'
    UNION ALL
    -- A27 参与判定：DRAFT 汇总 vs 未清 items 聚合（>0 进 TOTAL）
    SELECT 27,
           :'a27_line'
    UNION ALL
    -- A28 参与判定：结账后仍写入流水（>0 进 TOTAL）
    SELECT 28,
           :'a28_line'
    UNION ALL
    -- A29 参与判定：结账快照自洽（>0 进 TOTAL）
    SELECT 29,
           :'a29_line'
    UNION ALL
    -- A30 参与判定：有退款额但无退款流水（>0 进 TOTAL）
    SELECT 30,
           :'a30_line'
    UNION ALL
    -- A31 参与判定：当日退款流水合计 vs 订单退款合计（>0 进 TOTAL）
    SELECT 31,
           :'a31_line'
    UNION ALL
    SELECT 32,
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
             + CASE WHEN :a19_n > 0 THEN 1 ELSE 0 END
             + CASE WHEN :a25_n > 0 THEN 1 ELSE 0 END
             + CASE WHEN :a27_n > 0 THEN 1 ELSE 0 END
             + CASE WHEN :a28_n > 0 THEN 1 ELSE 0 END
             + CASE WHEN :a29_n > 0 THEN 1 ELSE 0 END
             + CASE WHEN :a30_n > 0 THEN 1 ELSE 0 END
             + CASE WHEN :a31_n > 0 THEN 1 ELSE 0 END)
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
-- 与判定同口径：**排除 cleared_flag=true 的 item**（人工处置：RESOLVE / EXEMPT，审计留痕
-- RECON_AMBIGUITY_RESOLVED / RECON_AMBIGUITY_EXEMPTED）；另兼容排除 'EXEMPTED(' 前缀（旧数据）
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
   AND i.cleared_flag = false
   AND COALESCE(i.reason, '') NOT LIKE 'EXEMPTED(%'
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
-- A9 明细（前 20）：普通流水 balance_after 为空（NULL，零容忍判定项）
-- 期望为空；出现即为「流水链路未维护余额快照」（0 值属合法：账户可取空）
-- ------------------------------------------------------------
\echo ''
\echo '-- A9 明细（前 20）--'
SELECT f.flow_id, f.flow_no, f.account_id, f.flow_direction, f.flow_category,
       f.amount, f.balance_after, f.business_date, f.create_time,
       left(COALESCE(f.remark, ''), 60) AS remark_snippet
  FROM fund_flows f
 WHERE f.deleted = 0
   AND f.balance_after IS NULL
   AND NOT (COALESCE(f.created_by, '') LIKE 'system/backfill-%'
            AND (COALESCE(f.remark, '') LIKE '%[cashBackfill]%'
                 OR COALESCE(f.remark, '') LIKE '%[posShift=%'))
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

-- ------------------------------------------------------------
-- A22 明细（前 20）：FFH 前缀但无补账证据（与 A21 对称）
-- 期望为空；出现需人工核对是否伪造前缀
-- ------------------------------------------------------------
\if :backfill_shape_ready
\echo ''
\echo '-- A22 明细（前 20）--'
SELECT f.flow_no, f.account_id, f.amount, f.balance_after, f.created_by,
       left(COALESCE(f.remark, ''), 60) AS remark_snippet,
       f.create_time
  FROM fund_flows f
 WHERE f.deleted = 0
   AND f.flow_no LIKE 'FFH%'
   AND NOT (COALESCE(f.created_by, '') LIKE 'system/backfill-%'
            AND (COALESCE(f.remark, '') LIKE '%[cashBackfill]%' OR COALESCE(f.remark, '') LIKE '%[posShift=%'))
 ORDER BY f.create_time DESC
 LIMIT 20;
\else
\echo ''
\echo '-- A22 明细跳过（表未创建）--'
\endif

-- ------------------------------------------------------------
-- A23 明细（前 20 行）：超过阈值的未关闭调节表（挂账天数倒序）
-- 阈值同 A23 判定：STALE_DAYS = 7；未超阈值时不输出明细（只提示跳过）
-- ------------------------------------------------------------
\if :a23_stale
\echo ''
\echo '-- A23 明细（前 20）--'
SELECT s.statement_id,
       s.status,
       s.submitted_by,
       COALESCE(s.submitted_at, s.create_time) AS submitted_at,
       floor(EXTRACT(EPOCH FROM (now() - COALESCE(s.submitted_at, s.create_time))) / 86400)::int AS stale_days
  FROM reconciliation_statements s
 WHERE s.status NOT IN ('APPROVED')
   AND s.deleted = 0
 ORDER BY stale_days DESC
 LIMIT 20;
\else
\echo ''
\echo '-- A23 明细跳过（无超过阈值的未关闭调节表）--'
\endif

-- ------------------------------------------------------------
-- A25 明细（前 20）：烧号账中「已取号且仍未落库」的行（archived_flag=false 且 fund_flows 无同号）
-- in_flows 列 = 该 flow_no 当前在 fund_flows 的条数（0=真空洞；>0 说明号已被重用，正常不在此列表）
-- 期望为空；出现即需人工核对（重复键 / 乐观锁耗尽 / 业务校验失败）
-- ------------------------------------------------------------
\if :burn_ledger_ready
\echo ''
\echo '-- A25 明细（前 20）--'
SELECT b.ledger_id, b.flow_no, b.seq_key, b.seq_value, b.reason, b.created_by, b.create_time, b.archived_flag,
       (SELECT count(*) FROM fund_flows f WHERE f.flow_no = b.flow_no) AS in_flows
  FROM fund_flow_no_burn_ledger b
 WHERE b.archived_flag = false
   AND NOT EXISTS (SELECT 1 FROM fund_flows f WHERE f.flow_no = b.flow_no)
 ORDER BY b.create_time DESC
 LIMIT 20;
\else
\echo ''
\echo '-- A25 明细跳过（表未创建）--'
\endif

-- ------------------------------------------------------------
-- A26 明细（前 20）：重算吃掉人工处置（需人工复核）
-- ------------------------------------------------------------
\if :audit_log_ready
\echo ''
\echo '-- A26 明细（前 20）--'
SELECT l.log_id, l.operation_type, l.module, l.target_type, l.target_id,
       l.operator_name, l.create_time,
       left(COALESCE(l.operation_desc, ''), 80) AS desc_snippet
  FROM finance_audit_logs l
 WHERE l.operation_type = 'RECON_DISPOSITION_LOST'
 ORDER BY l.create_time DESC
 LIMIT 20;
\else
\echo ''
\echo '-- A26 明细跳过（表未创建）--'
\endif

-- ------------------------------------------------------------
-- A27 明细（前 20）：DRAFT 调节表的汇总字段与「按 cleared_flag=false 聚合」的差值
-- 期望为空；出现即存在绕过 service 直改表（或重算后未回写汇总）
-- ------------------------------------------------------------
\if :stmt_sum_ready
\echo ''
\echo '-- A27 明细（前 20）--'
SELECT s.statement_id, s.status,
       s.uncleared_in_credit  - COALESCE((SELECT SUM(i.amount) FILTER (WHERE i.category='A') FROM reconciliation_items i WHERE i.statement_id=s.statement_id AND i.deleted=0 AND i.cleared_flag=false),0) AS diff_in_credit,
       s.uncleared_in_debit   - COALESCE((SELECT SUM(i.amount) FILTER (WHERE i.category='B') FROM reconciliation_items i WHERE i.statement_id=s.statement_id AND i.deleted=0 AND i.cleared_flag=false),0) AS diff_in_debit,
       s.uncleared_out_credit - COALESCE((SELECT SUM(i.amount) FILTER (WHERE i.category='C') FROM reconciliation_items i WHERE i.statement_id=s.statement_id AND i.deleted=0 AND i.cleared_flag=false),0) AS diff_out_credit,
       s.uncleared_out_debit  - COALESCE((SELECT SUM(i.amount) FILTER (WHERE i.category='D') FROM reconciliation_items i WHERE i.statement_id=s.statement_id AND i.deleted=0 AND i.cleared_flag=false),0) AS diff_out_debit,
       s.adjusted_book_balance - (s.book_balance
           + COALESCE((SELECT SUM(i.amount) FILTER (WHERE i.category='C') FROM reconciliation_items i WHERE i.statement_id=s.statement_id AND i.deleted=0 AND i.cleared_flag=false),0)
           - COALESCE((SELECT SUM(i.amount) FILTER (WHERE i.category='D') FROM reconciliation_items i WHERE i.statement_id=s.statement_id AND i.deleted=0 AND i.cleared_flag=false),0)) AS diff_adj_book,
       s.adjusted_bank_balance - (s.bank_balance
           + COALESCE((SELECT SUM(i.amount) FILTER (WHERE i.category='A') FROM reconciliation_items i WHERE i.statement_id=s.statement_id AND i.deleted=0 AND i.cleared_flag=false),0)
           - COALESCE((SELECT SUM(i.amount) FILTER (WHERE i.category='B') FROM reconciliation_items i WHERE i.statement_id=s.statement_id AND i.deleted=0 AND i.cleared_flag=false),0)) AS diff_adj_bank
  FROM reconciliation_statements s
 WHERE s.deleted = 0
   AND s.status = 'DRAFT'
   AND ( s.uncleared_in_credit  <> COALESCE((SELECT SUM(i.amount) FILTER (WHERE i.category='A') FROM reconciliation_items i WHERE i.statement_id=s.statement_id AND i.deleted=0 AND i.cleared_flag=false),0)
      OR s.uncleared_in_debit   <> COALESCE((SELECT SUM(i.amount) FILTER (WHERE i.category='B') FROM reconciliation_items i WHERE i.statement_id=s.statement_id AND i.deleted=0 AND i.cleared_flag=false),0)
      OR s.uncleared_out_credit <> COALESCE((SELECT SUM(i.amount) FILTER (WHERE i.category='C') FROM reconciliation_items i WHERE i.statement_id=s.statement_id AND i.deleted=0 AND i.cleared_flag=false),0)
      OR s.uncleared_out_debit  <> COALESCE((SELECT SUM(i.amount) FILTER (WHERE i.category='D') FROM reconciliation_items i WHERE i.statement_id=s.statement_id AND i.deleted=0 AND i.cleared_flag=false),0)
      OR s.adjusted_book_balance <> s.book_balance
           + COALESCE((SELECT SUM(i.amount) FILTER (WHERE i.category='C') FROM reconciliation_items i WHERE i.statement_id=s.statement_id AND i.deleted=0 AND i.cleared_flag=false),0)
           - COALESCE((SELECT SUM(i.amount) FILTER (WHERE i.category='D') FROM reconciliation_items i WHERE i.statement_id=s.statement_id AND i.deleted=0 AND i.cleared_flag=false),0)
      OR s.adjusted_bank_balance <> s.bank_balance
           + COALESCE((SELECT SUM(i.amount) FILTER (WHERE i.category='A') FROM reconciliation_items i WHERE i.statement_id=s.statement_id AND i.deleted=0 AND i.cleared_flag=false),0)
           - COALESCE((SELECT SUM(i.amount) FILTER (WHERE i.category='B') FROM reconciliation_items i WHERE i.statement_id=s.statement_id AND i.deleted=0 AND i.cleared_flag=false),0)
      )
 ORDER BY s.statement_id
 LIMIT 20;
\else
\echo ''
\echo '-- A27 明细跳过（表未创建）--'
\endif

-- ------------------------------------------------------------
-- A28 明细（前 20）：已结账期间内、create_time 晚于 close_time 的活流水
-- 期望为空；出现即结账门禁失效（或反结账流程漏写）
-- ------------------------------------------------------------
\if :closed_write_ready
\echo ''
\echo '-- A28 明细（前 20）--'
SELECT p.period_code, p.close_time,
       f.flow_id, f.flow_no, f.account_id, f.amount, f.business_date, f.create_time,
       f.create_time - p.close_time AS lag_after_close
  FROM accounting_periods p
  JOIN fund_flows f
    ON f.deleted = 0
   AND f.business_date BETWEEN p.start_date AND p.end_date
   AND f.create_time > p.close_time
 WHERE p.deleted = 0
   AND p.is_closed = true
   AND p.close_time IS NOT NULL
   AND p.start_date IS NOT NULL
   AND p.end_date IS NOT NULL
 ORDER BY f.create_time DESC
 LIMIT 20;
\else
\echo ''
\echo '-- A28 明细跳过（表未创建）--'
\endif

-- ------------------------------------------------------------
-- A29 明细（前 20）：结账快照不自洽（closing_fen 与末笔余额或 期初+收-支 不符）
-- 期望为空；出现即结账快照写错（或结账后流水被改动）
-- ------------------------------------------------------------
\if :period_balance_ready
\echo ''
\echo '-- A29 明细（前 20）--'
SELECT b.period_code, b.account_id,
       b.opening_fen, b.income_fen, b.expense_fen, b.closing_fen,
       last.balance_after AS last_balance_after,
       b.closing_fen - COALESCE(last.balance_after, b.closing_fen) AS diff_vs_last,
       b.closing_fen - (b.opening_fen + b.income_fen - b.expense_fen) AS diff_rollforward
  FROM finance_period_account_balance b
  JOIN accounting_periods p
    ON p.period_code = b.period_code
   AND p.deleted = 0
   AND p.is_closed = true
  LEFT JOIN LATERAL (
      SELECT f.balance_after
        FROM fund_flows f
       WHERE f.deleted = 0
         AND f.account_id = b.account_id
         AND f.balance_after IS NOT NULL
         AND f.business_date BETWEEN p.start_date AND p.end_date
       ORDER BY f.business_date DESC, f.flow_id DESC
       LIMIT 1
  ) last ON true
 WHERE ( last.balance_after IS NOT NULL AND b.closing_fen <> last.balance_after )
    OR b.closing_fen <> b.opening_fen + b.income_fen - b.expense_fen
 ORDER BY b.period_code, b.account_id
 LIMIT 20;
\else
\echo ''
\echo '-- A29 明细跳过（表未创建）--'
\endif

-- ------------------------------------------------------------
-- A30 明细（前 20）：有退款额但缺对应退款流水的订单
-- 期望为空；出现即退款未入账（或退款流水 category/金额口径不符）
-- ------------------------------------------------------------
\if :refund_ready
\echo ''
\echo '-- A30 明细（前 20）--'
SELECT o.order_id, o.order_status, o.payment_status,
       o.refund_amount, o.refund_amount::bigint AS refund_fen,
       o.refund_time, o.create_time
  FROM orders o
 WHERE o.deleted = 0
   AND o.refund_amount > 0
   AND NOT EXISTS (
         SELECT 1 FROM fund_flows f
          WHERE f.deleted = 0 AND f.status = 2 AND f.flow_direction = 2
            AND f.order_id = o.order_id
            AND f.flow_category = ANY (string_to_array(:'REFUND_FLOW_CATEGORIES', ',')::int[])
            AND f.amount = o.refund_amount::bigint)
 ORDER BY o.refund_time DESC NULLS LAST, o.order_id
 LIMIT 20;
\else
\echo ''
\echo '-- A30 明细跳过（退款 flow_category 常量未配置 / 表未创建）--'
\endif

-- ------------------------------------------------------------
-- A31 明细（前 20）：当日退款流水合计 ≠ 订单退款合计（差值为 flow-orders）
-- ------------------------------------------------------------
\if :refund_ready
\echo ''
\echo '-- A31 明细（前 20）--'
SELECT t.d,
       t.flow_amt  AS flow_refund_fen,
       t.order_amt AS order_refund_fen,
       t.flow_amt - t.order_amt AS diff_fen
  FROM (
      SELECT COALESCE(fl.d, od.d) AS d,
             COALESCE(fl.amt, 0) AS flow_amt,
             COALESCE(od.amt, 0) AS order_amt
        FROM (SELECT f.business_date AS d, SUM(f.amount) AS amt
                FROM fund_flows f
               WHERE f.deleted = 0 AND f.status = 2 AND f.flow_direction = 2
                 AND f.flow_category = ANY (string_to_array(:'REFUND_FLOW_CATEGORIES', ',')::int[])
               GROUP BY 1) fl
        FULL JOIN (SELECT COALESCE(o.refund_time, o.create_time)::date AS d,
                          SUM(o.refund_amount)::bigint AS amt
                     FROM orders o WHERE o.deleted = 0 AND o.refund_amount > 0 GROUP BY 1) od ON od.d = fl.d
  ) t
 WHERE t.flow_amt IS DISTINCT FROM t.order_amt
 ORDER BY t.d DESC
 LIMIT 20;
\else
\echo ''
\echo '-- A31 明细跳过（退款 flow_category 常量未配置 / 表未创建）--'
\endif

COMMIT;
