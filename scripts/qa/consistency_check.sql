-- ============================================================
-- scripts/qa/consistency_check.sql
-- 阶段 2-B：SQL 一致性巡检（提交前检查用）
--
-- 断言清单（A1/A2/A3/A4/A5/A7/A9/A10/A11/A12/A13/A14/A15/A19/A25/A27/A28/A29/A30/A31/A32/A33/A36/A37/A39 参与 PASS/FAIL 判定，**期望 rows=0**；
-- A6/A8/A16/A17/A18/A20/A21/A22/A23/A24/A26/A32-DIST/A35/A38/A41 为信息性输出，不影响 TOTAL）：
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
--   A32 （**参与判定**）孤儿退款流水：business_date < CURRENT_DATE-1 且「既无可配银行退款
--       （bank_transactions is_refund=true & direction=2 & 同额同日）」又「不在未清 A/B/C/D 明细中」
--       —— 「待匹配」是合法状态，故用时间窗兜。期望 0。
--       A32-DIST（信息性）退款匹配状态分布 matched / pending-B / pending-D / orphan。
--   A33 （**参与判定**）逐单对拍：orders.refund_amount::bigint vs SUM(退款流水 amount)，均按分直比。期望 0。
--   A34 （**预留，不执行**）NEW-23 储值充值双向断言：A34-入 = recharge_records 已支付但无
--       flow_category=3 等额入账流水；A34-退 = recharge_records.refund_amount>0 但无
--       flow_category=4 等额退款流水。阻塞：充值功能未启用、3/4 类别与字段待 NEW-23 落盘定义，
--       常量未定前不猜值、不写空断言；落地后替换占位注释并计入判定（22 → 24）。
--   A35 （信息性，不进 TOTAL）采购付款流水计数（flow_category=6，NEW-21 迁移进度）；
--       当前预期 0，采购付款首次落库后 >0 属正常，故只观测不判定。
--   A41 （信息性，不进 TOTAL）近 24h「对账跳过现金户」审计条数（operation_type='RECON_SKIP_CASH'）：
--       现金户入口直接返回、不建 statement，每次跳过留痕；跳过是**正常设计行为**（N19-1/N19-2），
--       故只观测不判定（>0 健康、=0 说明近 24h 无现金户对账请求）。
--   —— N19-2 四项（2026-10-26 落盘）——
--   A36 （**参与判定**）现金户退款流水未被任何缴款单闭环：account_type=2 的退款流水，
--       business_date < CURRENT_DATE-1，且无 [slipPair] token、无 refund_shift_attribution 记录、
--       不在未清 A/B/C/D 明细中。期望 0。
--   A37 （**参与判定**）退款流水 refund_time 落不进任何班次窗口（pos_shifts start/end 缺失或不覆盖）。期望 0。
--       窗口语义：班次窗口 = [start_time, COALESCE(end_time, (NOW() AT TIME ZONE 'Asia/Shanghai'))] —— 进行中班次（end_time 为 NULL）也算覆盖到当前时刻；
--       **时区口径**：所有时间比较统一用 Asia/Shanghai 墙钟（refund_time 先 AT TIME ZONE 再比较；NOW() 也显式转墙钟），
--       不依赖数据库会话时区 —— 旧版隐式依赖 TimeZone=Asia/Shanghai，客户端把 session TZ 设成 UTC 会整体偏 8 小时。
--       旧版要求 end_time IS NOT NULL，会把「进行中班次里的退款」误判成落不进窗口（实测 refund 125/shift 375 即此例）。
--   A38 （信息性）现金退款闭环率 + 近 24h RECON_SHIFT_BOUNDARY_PENDING 待人工确认笔数。只观测。
--   A39 （**参与判定**）缴款单恒等式：difference_net_fen = (actual_amount − reserve_fen) − (expected_amount − refund_component_fen)
--       且 refund_component_fen = 该 slip 已归属退款流水合计。期望 0。
--   ⚠️ 四条**全部带分母**：分母 = 0 时输出 `[INFO] Axx skipped_no_data 分母=0`（不输出 PASS）。
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
SELECT CASE WHEN (SELECT count(*) FROM recon_runs WHERE status = 'FINALIZED') = 0
            THEN '[INFO] A10 skipped_no_data 分母=0（无 FINALIZED 的 recon_run）'
            WHEN count(*) = 0 THEN '[PASS] A10 FINALIZED 的 recon_run 无 recon_items rows=0 分母=' || (SELECT count(*) FROM recon_runs WHERE status = 'FINALIZED')
            ELSE '[FAIL] A10 FINALIZED 的 recon_run 无 recon_items rows=' || count(*) || ' 分母=' || (SELECT count(*) FROM recon_runs WHERE status = 'FINALIZED')
       END AS a10_line,
       CASE WHEN (SELECT count(*) FROM recon_runs WHERE status = 'FINALIZED') = 0 THEN 0 ELSE count(*) END AS a10_n,
       (SELECT count(*) FROM recon_runs WHERE status = 'FINALIZED') AS a10_den
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
        AND to_regclass('public.fund_flows') IS NOT NULL
        AND to_regclass('public.bank_transactions') IS NOT NULL
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
      /*
       * 【P4-5 退款配对例外：token 优先、列兜底】
       * B/D 悬空配对是**已识别的业务形态**（退款流水落 B＝企业已付银行未付；银行退款落 D＝银行已付企业未付），
       * 同额两侧并存时会被形态判据误当「假平衡」。配对标识取法（按优先级）：
       *   ① item.reason 里的 token  [refundPair=<refund_record_id>]        ← 迁移前只有它
       *   ② fund_flows.refund_record_id（A/B 侧经 item.flow_id 关联；C/D 侧经
       *      bank_transactions.matched_flow_id 关联到退款流水）            ← 迁移后有列
       *   两种形态并存原因：后端先落 token、迁移后补列，同一批数据可能两种都有；
       *   COALESCE 保证「有 token 用 token、无 token 用列」。
       *   ② 用 to_jsonb(f) ->> 'refund_record_id' 读取：列**不存在时自然为 NULL**，
       *   不会解析期报错（这是 CASE+to_regclass 方案无效的教训）。
       * 命中：同 statement、同 amount、两侧（A|B vs C|D）pair_key 相同 → 两侧都不计入配对。
       * **这不是白名单/基线**；**配对完整性由 A32 独立兜底**（孤儿退款）。
       */
      WITH item_key AS (
          SELECT k.item_id, k.statement_id, k.amount, k.category,
                 COALESCE(
                     substring(k.reason from '\[refundPair=([^]]+)\]'),
                     (SELECT to_jsonb(f) ->> 'refund_record_id'
                        FROM fund_flows f WHERE f.flow_id = k.flow_id),
                     (SELECT to_jsonb(f) ->> 'refund_record_id'
                        FROM bank_transactions b
                        JOIN fund_flows f ON f.flow_id = b.matched_flow_id
                       WHERE b.transaction_id = k.transaction_id)
                 ) AS pair_key
            FROM reconciliation_items k
           WHERE k.deleted = 0
             -- 人工处置后该条不再参与形态判据（P4-3）：cleared_flag=true ← RESOLVE / EXEMPT
             -- **不是白名单/基线**，是「已人工处置」的事实标记。
             AND k.cleared_flag = false
             -- 兼容保留：历史数据/旧接口可能只写前缀未置 flag
             AND COALESCE(k.reason, '') NOT LIKE 'EXEMPTED(%'
      )
      SELECT k.statement_id, k.amount
        FROM item_key k
       WHERE NOT EXISTS (
             SELECT 1 FROM item_key p
              WHERE p.statement_id = k.statement_id
                AND p.amount = k.amount
                AND p.item_id <> k.item_id
                AND (p.category IN ('A', 'B')) <> (k.category IN ('A', 'B'))
                AND k.pair_key IS NOT NULL
                AND p.pair_key = k.pair_key)
       GROUP BY k.statement_id, k.amount
      HAVING count(*) FILTER (WHERE k.category IN ('A', 'B')) > 0
         AND count(*) FILTER (WHERE k.category IN ('C', 'D')) > 0
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
SELECT CASE WHEN (SELECT count(*) FROM reconciliation_statements WHERE deleted = 0 AND status = 'DRAFT') = 0
            THEN '[INFO] A27 skipped_no_data 分母=0（无 DRAFT 调节表）'
            WHEN count(*) = 0 THEN '[PASS] A27 DRAFT 汇总与未清 items 聚合一致 rows=0 分母=' || (SELECT count(*) FROM reconciliation_statements WHERE deleted = 0 AND status = 'DRAFT')
            ELSE '[FAIL] A27 DRAFT 汇总与未清 items 聚合一致 rows=' || count(*) || ' 分母=' || (SELECT count(*) FROM reconciliation_statements WHERE deleted = 0 AND status = 'DRAFT')
       END AS a27_line,
       CASE WHEN (SELECT count(*) FROM reconciliation_statements WHERE deleted = 0 AND status = 'DRAFT') = 0 THEN 0 ELSE count(*) END AS a27_n,
       (SELECT count(*) FROM reconciliation_statements WHERE deleted = 0 AND status = 'DRAFT') AS a27_den
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
--   【口径收窄 2026-10-26】A30/A31/A32/A33 一律要求 **fund_flows.refund_record_id IS NOT NULL**：
--      **真实类别冲突** —— PaymentServiceImpl:157 把 flow_category=2 也用于「采购付款」，
--      采购付款一旦落库，A31 的流水侧会永久偏大。真正的退款流水必然带 refund_record_id，
--      故按数据形态收窄到「真退款」。**这不是白名单**（不看金额/账号/时间等任何白名单式条件）。
--      根治见 backlog **NEW-21**（采购付款改用 flow_category=6）。
--   A30 只判**存在性**（该订单有没有退款流水）；**金额一致性交给 A33 逐单对拍**
--      （多笔部分退款时 单笔金额 ≠ 订单退款额，A30 若比金额必然误报）。
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
SELECT CASE WHEN (SELECT count(*) FROM orders WHERE deleted = 0 AND COALESCE(refund_amount,0) > 0) = 0
            THEN '[INFO] A30 skipped_no_data 分母=0（无含退款额的订单）'
            WHEN count(*) = 0 THEN '[PASS] A30 有退款额但无退款流水 orders=0 分母=' || (SELECT count(*) FROM orders WHERE deleted = 0 AND COALESCE(refund_amount,0) > 0)
            ELSE '[FAIL] A30 有退款额但无退款流水 orders=' || count(*) || ' 分母=' || (SELECT count(*) FROM orders WHERE deleted = 0 AND COALESCE(refund_amount,0) > 0)
       END AS a30_line,
       CASE WHEN (SELECT count(*) FROM orders WHERE deleted = 0 AND COALESCE(refund_amount,0) > 0) = 0 THEN 0 ELSE count(*) END AS a30_n,
       (SELECT count(*) FROM orders WHERE deleted = 0 AND COALESCE(refund_amount,0) > 0) AS a30_den
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
            -- 只判「存在退款流水」；**金额一致性由 A33 逐单对拍**（多笔部分退款时单笔金额≠订单退款额）
            AND f.refund_record_id IS NOT NULL)
\gset

-- A31：按日退款流水合计 vs 订单退款合计（orders 无 account_id → 退化为「当日全库合计」口径）
--   流水侧同样要求 refund_record_id IS NOT NULL：category=2 被采购付款复用（PaymentServiceImpl:157），
--   不加会把采购付款算进退款合计（根治见 NEW-21）
SELECT CASE WHEN (SELECT count(DISTINCT business_date) FROM fund_flows WHERE deleted=0 AND status=2 AND flow_category=2 AND refund_record_id IS NOT NULL) = 0
            THEN '[INFO] A31 skipped_no_data 分母=0（无退款流水日期）'
            WHEN count(*) = 0 THEN '[PASS] A31 当日退款流水合计=订单退款合计 days=0 分母=' || (SELECT count(DISTINCT business_date) FROM fund_flows WHERE deleted=0 AND status=2 AND flow_category=2 AND refund_record_id IS NOT NULL)
            ELSE '[FAIL] A31 当日退款流水合计=订单退款合计 days=' || count(*) || ' 分母=' || (SELECT count(DISTINCT business_date) FROM fund_flows WHERE deleted=0 AND status=2 AND flow_category=2 AND refund_record_id IS NOT NULL)
       END AS a31_line,
       CASE WHEN (SELECT count(DISTINCT business_date) FROM fund_flows WHERE deleted=0 AND status=2 AND flow_category=2 AND refund_record_id IS NOT NULL) = 0 THEN 0 ELSE count(*) END AS a31_n,
       (SELECT count(DISTINCT business_date) FROM fund_flows WHERE deleted=0 AND status=2 AND flow_category=2 AND refund_record_id IS NOT NULL) AS a31_den
  FROM (
      SELECT COALESCE(fl.d, od.d) AS d,
             COALESCE(fl.amt, 0) AS flow_amt,
             COALESCE(od.amt, 0) AS order_amt
        FROM (SELECT f.business_date AS d, SUM(f.amount) AS amt
                FROM fund_flows f
               WHERE f.deleted = 0 AND f.status = 2 AND f.flow_direction = 2
                 AND f.flow_category = ANY (string_to_array(:'REFUND_FLOW_CATEGORIES', ',')::int[])
                 AND f.refund_record_id IS NOT NULL
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
-- A33（**参与判定**）：逐单退款对拍 —— orders.refund_amount vs SUM(退款流水)
--   单位：两边都是**分**（refund_amount numeric(12,2) 但按分存）→ 直比，禁止 ×100。
--   口径：orders.deleted=0 且 refund_amount>0；流水取 deleted=0、status=2、flow_category=2（退款）。
--   期望 0 笔不符；>0 即 FAIL。
-- ------------------------------------------------------------
\if :refund_ready
SELECT CASE WHEN (SELECT count(*) FROM orders WHERE deleted = 0 AND COALESCE(refund_amount,0) > 0) = 0
            THEN '[INFO] A33 skipped_no_data 分母=0（无含退款额的订单）'
            WHEN count(*) = 0 THEN '[PASS] A33 逐单退款对拍(订单退款额=退款流水合计) orders=0 分母=' || (SELECT count(*) FROM orders WHERE deleted = 0 AND COALESCE(refund_amount,0) > 0)
            ELSE '[FAIL] A33 逐单退款对拍(订单退款额=退款流水合计) orders=' || count(*) || ' 分母=' || (SELECT count(*) FROM orders WHERE deleted = 0 AND COALESCE(refund_amount,0) > 0)
       END AS a33_line,
       CASE WHEN (SELECT count(*) FROM orders WHERE deleted = 0 AND COALESCE(refund_amount,0) > 0) = 0 THEN 0 ELSE count(*) END AS a33_n,
       (SELECT count(*) FROM orders WHERE deleted = 0 AND COALESCE(refund_amount,0) > 0) AS a33_den
  FROM orders o
  LEFT JOIN (SELECT f.order_id, SUM(f.amount) AS amt
               FROM fund_flows f
              WHERE f.deleted = 0 AND f.status = 2 AND f.flow_category = 2
                AND f.refund_record_id IS NOT NULL
              GROUP BY f.order_id) r ON r.order_id = o.order_id
 WHERE o.deleted = 0
   AND o.refund_amount > 0
   AND COALESCE(r.amt, 0) <> o.refund_amount::bigint
\gset
\else
\set a33_line '[INFO] A33 跳过（退款 flow_category 常量未配置 / 表未创建）'
\set a33_n 0
\endif

-- ------------------------------------------------------------
-- A32（**参与判定**）：孤儿退款流水
--   口径（判据定案）：**「待匹配」是合法状态**（退款当天 CSV 尚未导入），所以只看
--     business_date < CURRENT_DATE - 1（昨天及更早）；此时仍「既无银行退款流水可配、
--     也不在未清 A/B/C/D 明细里」的退款流水才算孤儿。
--   【2026-10-26 N19-1 定案】A32 **只查银行户（account_type = 1）**。这是**长期设计**：
--     现金户不在调节表框架内 —— 现金户的差异由缴款单 expected_amount / actual_amount /
--     difference_amount（短款/长款）闭环，**不产生 recon_items**；现金退款由 NEW-19 的
--     缴款单配对闭环（A36/A37 另判）。**不设移除计划。**
--     现金户（account_type=2）的 B 类因此不进孤儿统计，改由 A32-DIST 的 cash-B 桶单独展示。
--   期望 0；>0 即 FAIL。
-- ------------------------------------------------------------
SELECT ((:'REFUND_FLOW_CATEGORIES' <> '')
        AND to_regclass('public.bank_transactions') IS NOT NULL
        AND to_regclass('public.reconciliation_items') IS NOT NULL
        AND to_regclass('public.reconciliation_statements') IS NOT NULL
        AND EXISTS (SELECT 1 FROM information_schema.columns
                     WHERE table_schema = 'public' AND table_name = 'bank_transactions'
                       AND column_name IN ('is_refund', 'direction', 'amount', 'business_date', 'deleted')
                     GROUP BY table_name HAVING count(*) = 5)
        AND EXISTS (SELECT 1 FROM information_schema.columns
                     WHERE table_schema = 'public' AND table_name = 'reconciliation_items'
                       AND column_name IN ('statement_id', 'amount', 'cleared_flag', 'deleted')
                     GROUP BY table_name HAVING count(*) = 4)
        AND EXISTS (SELECT 1 FROM information_schema.columns
                     WHERE table_schema = 'public' AND table_name = 'reconciliation_statements'
                       AND column_name IN ('statement_id', 'status', 'deleted')
                     GROUP BY table_name HAVING count(*) = 3)
        AND to_regclass('public.bank_accounts') IS NOT NULL
        AND EXISTS (SELECT 1 FROM information_schema.columns
                     WHERE table_schema = 'public' AND table_name = 'bank_accounts'
                       AND column_name IN ('account_id', 'account_type', 'deleted')
                     GROUP BY table_name HAVING count(*) = 3)) AS refund_cross_ready
\gset

\if :refund_cross_ready
SELECT CASE WHEN count(*) = 0
            THEN '[PASS] A32 孤儿退款流水(昨天及更早仍未匹配/未入明细) rows=0'
            ELSE '[FAIL] A32 孤儿退款流水(昨天及更早仍未匹配/未入明细) rows=' || count(*)
                 || ' —— 存在 ' || count(*) || ' 条退款流水既无银行退款可配、也不在未清明细中（孤儿），禁止接受'
       END AS a32_line,
       count(*) AS a32_n
  FROM fund_flows f
 WHERE f.deleted = 0
   AND f.status = 2
   AND f.flow_category = 2
   -- 收窄到「真退款」：category=2 也被采购付款复用（NEW-21 前），真退款必有 refund_record_id
   AND f.refund_record_id IS NOT NULL
   AND f.business_date < CURRENT_DATE - 1
   -- 长期设计（N19-1 定案）：只查银行户（account_type=1）；现金户不在调节表框架内，
   --   其差异由缴款单 expected/actual/difference 闭环，**不产生 recon_items**；不设移除计划
   AND COALESCE((SELECT ba.account_type FROM bank_accounts ba
                  WHERE ba.account_id = f.account_id AND ba.deleted = 0), 1) = 1
   AND NOT EXISTS (SELECT 1 FROM bank_transactions b
                    WHERE b.deleted = 0 AND b.is_refund = true AND b.direction = 2
                      AND b.amount = f.amount AND b.business_date = f.business_date)
   AND NOT EXISTS (SELECT 1 FROM reconciliation_items i
                    WHERE i.deleted = 0 AND i.cleared_flag = false AND i.amount = f.amount
                      AND i.statement_id IN (SELECT statement_id FROM reconciliation_statements
                                              WHERE deleted = 0 AND status <> 'CLOSED'))
\gset

-- A32-DIST（信息性，不参与 TOTAL）：退款流水匹配状态分布
--   六个桶互斥、合计 = total：
--     matched   = 银行有「同额同日」的退款流水可配
--     cash-B    = **现金户**（account_type=2）且未匹配 → 由缴款单短款/长款闭环（**长期设计，不计孤儿**）
--     pending-B = 银行户、未匹配、同额 B 项在未清明细中
--     pending-D = 银行户、未匹配、同额 D 项在未清明细中
--     awaiting  = 银行户、未匹配、仍在时间窗内（今天/昨天，CSV 未导入 —— 合法「待匹配」）
--     orphan    = 银行户、未匹配、超出时间窗、且不在未清明细中（= A32 命中集）
SELECT '[INFO] A32-DIST 退款匹配状态分布 matched=' || count(*) FILTER (WHERE x.matched)
       || ' cash-B(现金户待缴款单闭环)=' || count(*) FILTER (WHERE NOT x.matched AND x.acct_type = 2)
       || ' pending-B=' || count(*) FILTER (WHERE NOT x.matched AND x.acct_type = 1 AND x.pending_b)
       || ' pending-D=' || count(*) FILTER (WHERE NOT x.matched AND x.acct_type = 1 AND NOT x.pending_b AND x.pending_d)
       || ' awaiting(待匹配窗口内)=' || count(*) FILTER (WHERE NOT x.matched AND x.acct_type = 1 AND NOT x.pending_b
                                                        AND NOT x.pending_d AND NOT x.orphan_raw)
       || ' orphan=' || count(*) FILTER (WHERE NOT x.matched AND x.acct_type = 1 AND NOT x.pending_b
                                          AND NOT x.pending_d AND x.orphan_raw)
       || ' total=' || count(*) AS a32d_line,
       count(*) AS a32d_n
  FROM (
      SELECT f.flow_id,
             COALESCE((SELECT ba.account_type FROM bank_accounts ba
                        WHERE ba.account_id = f.account_id AND ba.deleted = 0), 1) AS acct_type,
             EXISTS (SELECT 1 FROM bank_transactions b
                      WHERE b.deleted = 0 AND b.is_refund = true AND b.direction = 2
                        AND b.amount = f.amount AND b.business_date = f.business_date) AS matched,
             EXISTS (SELECT 1 FROM reconciliation_items i
                      WHERE i.deleted = 0 AND i.cleared_flag = false AND i.amount = f.amount
                        AND i.category = 'B'
                        AND i.statement_id IN (SELECT statement_id FROM reconciliation_statements
                                                WHERE deleted = 0 AND status <> 'CLOSED')) AS pending_b,
             EXISTS (SELECT 1 FROM reconciliation_items i
                      WHERE i.deleted = 0 AND i.cleared_flag = false AND i.amount = f.amount
                        AND i.category = 'D'
                        AND i.statement_id IN (SELECT statement_id FROM reconciliation_statements
                                                WHERE deleted = 0 AND status <> 'CLOSED')) AS pending_d,
             (f.business_date < CURRENT_DATE - 1
              AND NOT EXISTS (SELECT 1 FROM bank_transactions b
                               WHERE b.deleted = 0 AND b.is_refund = true AND b.direction = 2
                                 AND b.amount = f.amount AND b.business_date = f.business_date)
              AND NOT EXISTS (SELECT 1 FROM reconciliation_items i
                               WHERE i.deleted = 0 AND i.cleared_flag = false AND i.amount = f.amount
                                 AND i.statement_id IN (SELECT statement_id FROM reconciliation_statements
                                                         WHERE deleted = 0 AND status <> 'CLOSED'))) AS orphan_raw
        FROM fund_flows f
       WHERE f.deleted = 0 AND f.status = 2 AND f.flow_category = 2
         AND f.refund_record_id IS NOT NULL   -- 与 A32 同口径，保证六桶合计=total
  ) x
\gset
\else
\set a32_line '[INFO] A32 跳过（表未创建）'
\set a32_n 0
\set a32d_line '[INFO] A32-DIST 跳过（表未创建）'
\set a32d_n 0
\endif

-- ============================================================
-- A34【预留编号，落地前不执行】
--   用途：NEW-23 储值充值双向断言
--     · A34-入：recharge_records 已支付但无 flow_category=3 的等额入账流水
--     · A34-退：recharge_records.refund_amount > 0 但无 flow_category=4 的等额退款流水
--   阻塞原因（2026-10-26）：充值功能未启用（recharge_records=0 行、充值类流水=0），
--     且 3/4 的类别与字段需在 NEW-23 落盘时由后端定义；常量未定前不猜值、不写空断言。
--   落地后：把本注释替换为实际断言，并计入「判定项」计数（22 → 24）。
--   ⚠️ 本占位**不含任何 SELECT**，不产生输出行、不影响 INFO/判定计数。
-- ============================================================

-- ------------------------------------------------------------
-- A35（信息性，不参与 TOTAL）：采购付款类别计数（NEW-21 迁移进度）
--   语义：确认采购付款已从 flow_category=2 迁到 **6**（NEW-21）。
--   **当前预期 rows=0**（扫描实测：采购付款路径从未产生流水）；
--   **等采购付款首次落库后该值 >0 属正常** → 因此只做 INFO、绝不判定。
--   （纵深防御保留：A30/A31/A32/A33 的 category=2 旁边都有 refund_record_id IS NOT NULL，
--     即使 NEW-21 未落地、采购付款仍写 2，也不会污染退款侧勾稽。）
-- ------------------------------------------------------------
SELECT (to_regclass('public.fund_flows') IS NOT NULL
        AND EXISTS (SELECT 1 FROM information_schema.columns
                     WHERE table_schema = 'public' AND table_name = 'fund_flows'
                       AND column_name IN ('flow_category', 'deleted')
                     GROUP BY table_name HAVING count(*) = 2)) AS purchase_cat_ready
\gset

\if :purchase_cat_ready
SELECT '[INFO] A35 采购付款流水(category=6) rows=' || count(*) AS a35_line,
       count(*) AS a35_n
  FROM fund_flows
 WHERE deleted = 0
   AND flow_category = 6
\gset
\else
\set a35_line '[INFO] A35 跳过（表未创建）'
\set a35_n 0
\endif

-- ------------------------------------------------------------
-- A41（信息性，不参与 TOTAL）：近 24h「对账跳过现金户」留痕计数
--   语义：N19-1/N19-2 定案「现金户不在调节表框架内」，入口直接返回不建 statement，
--         但每次跳过都会写审计 RECON_SKIP_CASH（NEW-15 留痕）。
--         本项只观测「跳过动作是否真的在发生」，不做判定（跳过是**正常设计行为**，
--         数量随操作频率变化，>0 属健康、=0 说明近 24h 没人对现金户发起对账）。
--   依赖：finance_audit_logs.operation_type / create_time（缺表或列 → [INFO] 跳过）
-- ------------------------------------------------------------
SELECT (to_regclass('public.finance_audit_logs') IS NOT NULL
        AND EXISTS (SELECT 1 FROM information_schema.columns
                     WHERE table_schema = 'public' AND table_name = 'finance_audit_logs'
                       AND column_name IN ('operation_type', 'create_time')
                     GROUP BY table_name HAVING count(*) = 2)) AS recon_skip_ready
\gset

\if :recon_skip_ready
SELECT '[INFO] A41 近24h RECON_SKIP_CASH rows=' || count(*) AS a41_line,
       count(*) AS a41_n
  FROM finance_audit_logs
 WHERE operation_type = 'RECON_SKIP_CASH'
   AND create_time >= now() - interval '24 hours'
\gset
\else
\set a41_line '[INFO] A41 跳过（表未创建）'
\set a41_n 0
\endif

-- ============================================================
-- N19-2 四项（A36 / A37 / A38 / A39）
--   共同约定：**每条都带分母**；分母 = 0 时输出 `[INFO] Axx skipped_no_data 分母=0`，
--   绝不在「没有可比数据」时输出 PASS（避免「空集全绿」的假安全）。
--   A36/A37/A39 参与判定（进 TOTAL）；A38 为 INFO（闭环率/待确认只观测）。
-- ============================================================

-- ------------------------------------------------------------
-- A36（**参与判定**；允许长期 skipped，Owner 2026-10-27 确认）
--   为什么可能长期 skipped：口径要求「business_date < CURRENT_DATE-1（昨天及更早）」的现金退款；
--   当前业务实态下这类数据为空（E2E 与种子都只造当天数据）→ 长期输出 skipped_no_data（分母=0）。
--   **这是可接受的长期状态**：一旦出现「昨天及更早」的现金退款，本断言自动生效并开始判定；
--   **不要为了让它变绿去造跨天/跨日班次或倒填日期**（那会扭曲业务实态）。
--
-- A36（**参与判定**）：现金户退款流水未被任何缴款单闭环
--   口径：account_type=2 的退款流水（category=2/direction=2/status=2/deleted=0），
--         business_date < CURRENT_DATE-1（昨天及更早；当天仍在窗口内），且同时：
--           · remark 无 [slipPair=…] token（缴款单配对标记；当前后端尚无该 token → 恒真）
--           · refund_shift_attribution 无该 refund_record_id 的归属记录
--           · 不在未清 A/B/C/D 明细中（进入明细即视为已进入核对流程）
--   分母 = 上述「现金户退款流水」总数（不加后三个条件）。
-- ------------------------------------------------------------
SELECT (to_regclass('public.fund_flows') IS NOT NULL
        AND to_regclass('public.bank_accounts') IS NOT NULL
        AND to_regclass('public.refund_shift_attribution') IS NOT NULL
        AND to_regclass('public.reconciliation_items') IS NOT NULL
        AND to_regclass('public.reconciliation_statements') IS NOT NULL
        AND EXISTS (SELECT 1 FROM information_schema.columns
                     WHERE table_schema='public' AND table_name='fund_flows'
                       AND column_name IN ('account_id','flow_category','flow_direction','status','deleted','business_date','refund_record_id','remark')
                     GROUP BY table_name HAVING count(*) = 8)
        AND EXISTS (SELECT 1 FROM information_schema.columns
                     WHERE table_schema='public' AND table_name='refund_shift_attribution'
                       AND column_name IN ('refund_record_id','shift_id','slip_id')
                     GROUP BY table_name HAVING count(*) = 3)) AS cash_refund_ready
\gset

\if :cash_refund_ready
WITH cash_refund AS (
    SELECT f.flow_id, f.amount, f.business_date, f.refund_record_id, f.remark
      FROM fund_flows f
      JOIN bank_accounts ba ON ba.account_id = f.account_id AND ba.deleted = 0
     WHERE f.deleted = 0
       AND f.status = 2
       AND f.flow_category = 2
       AND f.flow_direction = 2
       AND f.refund_record_id IS NOT NULL
       AND ba.account_type = 2
), aged AS (
    SELECT * FROM cash_refund WHERE business_date < CURRENT_DATE - 1
), viol AS (
    SELECT c.flow_id
      FROM aged c
     WHERE COALESCE(c.remark, '') NOT LIKE '%[slipPair=%'
       AND NOT EXISTS (SELECT 1 FROM refund_shift_attribution a
                        WHERE a.refund_record_id = c.refund_record_id
                          AND (a.slip_id IS NOT NULL OR a.shift_id IS NOT NULL))
       AND NOT EXISTS (SELECT 1 FROM reconciliation_items i
                        WHERE i.deleted = 0 AND i.cleared_flag = false AND i.amount = c.amount
                          AND i.statement_id IN (SELECT statement_id FROM reconciliation_statements
                                                  WHERE deleted = 0 AND status <> 'CLOSED'))
)
SELECT CASE WHEN (SELECT count(*) FROM aged) = 0
            THEN '[INFO] A36 skipped_no_data 分母=0（无「超过 1 天窗口」的现金退款；现金退款总量='
                 || (SELECT count(*) FROM cash_refund) || '）'
            WHEN (SELECT count(*) FROM viol) = 0
            THEN '[PASS] A36 现金退款未闭环 rows=0 分母=' || (SELECT count(*) FROM aged)
                 || '（现金退款总量=' || (SELECT count(*) FROM cash_refund) || '）'
            ELSE '[FAIL] A36 现金退款未闭环 rows=' || (SELECT count(*) FROM viol)
                 || ' 分母=' || (SELECT count(*) FROM aged)
                 || '（现金退款总量=' || (SELECT count(*) FROM cash_refund) || '）'
                 || ' —— 存在 ' || (SELECT count(*) FROM viol) || ' 笔现金退款既无缴款单闭环、也不在未清明细中，禁止接受'
       END AS a36_line,
       (SELECT count(*) FROM viol) AS a36_n,
       (SELECT count(*) FROM aged) AS a36_den
\gset
\else
\set a36_line '[INFO] A36 跳过（表未创建）'
\set a36_n 0
\set a36_den 0
\endif

-- ------------------------------------------------------------
-- A37（**参与判定**）：退款流水 refund_time 落不进任何班次窗口
--   口径：退款流水 refund_time IS NOT NULL，但 pos_shifts 无
--         start_time <= refund_time <= end_time（deleted=0）的窗口 → 班次归属断链。
--   时区：refund_time 为 timestamptz、pos_shifts 为 timestamp；
--         统一按 **Asia/Shanghai** 本地时间比较（写死时区，避免依赖会话 TZ）。
--   分母 = refund_time IS NOT NULL 的退款流水数。
-- ------------------------------------------------------------
SELECT (to_regclass('public.fund_flows') IS NOT NULL
        AND to_regclass('public.pos_shifts') IS NOT NULL
        AND EXISTS (SELECT 1 FROM information_schema.columns
                     WHERE table_schema='public' AND table_name='fund_flows'
                       AND column_name IN ('refund_time','flow_category','flow_direction','status','deleted')
                     GROUP BY table_name HAVING count(*) = 5)
        AND EXISTS (SELECT 1 FROM information_schema.columns
                     WHERE table_schema='public' AND table_name='pos_shifts'
                       AND column_name IN ('start_time','end_time','deleted')
                     GROUP BY table_name HAVING count(*) = 3)) AS refund_window_ready
\gset

\if :refund_window_ready
WITH rf AS (
    SELECT f.flow_id, f.refund_time
      FROM fund_flows f
     WHERE f.deleted = 0 AND f.status = 2 AND f.flow_category = 2 AND f.flow_direction = 2
       AND f.refund_time IS NOT NULL
), viol AS (
    SELECT r.flow_id
      FROM rf r
     WHERE NOT EXISTS (
             SELECT 1 FROM pos_shifts s
              WHERE s.deleted = 0
                 AND s.start_time IS NOT NULL
                 AND (r.refund_time AT TIME ZONE 'Asia/Shanghai') BETWEEN s.start_time AND COALESCE(s.end_time, (NOW() AT TIME ZONE 'Asia/Shanghai')))
)
SELECT CASE WHEN (SELECT count(*) FROM rf) = 0
            THEN '[INFO] A37 skipped_no_data 分母=0（无 refund_time 已落的退款流水）'
            WHEN (SELECT count(*) FROM viol) = 0
            THEN '[PASS] A37 退款落不进班次窗口 rows=0 分母=' || (SELECT count(*) FROM rf)
            ELSE '[FAIL] A37 退款落不进班次窗口 rows=' || (SELECT count(*) FROM viol)
                 || ' 分母=' || (SELECT count(*) FROM rf)
                 || ' —— 存在 ' || (SELECT count(*) FROM viol) || ' 笔退款的 refund_time 不在任何班次窗口内，禁止接受'
       END AS a37_line,
       (SELECT count(*) FROM viol) AS a37_n,
       (SELECT count(*) FROM rf)   AS a37_den
\gset
\else
\set a37_line '[INFO] A37 跳过（表未创建）'
\set a37_n 0
\set a37_den 0
\endif

-- ------------------------------------------------------------
-- A38（信息性，不参与 TOTAL）：现金退款闭环率 + 待人工确认笔数
--   闭环率 = 已闭环现金退款数 / 现金退款流水总数
--     已闭环 = 有 [slipPair] token / 有 attribution 记录 / 在未清明细中
--   待人工确认 = 近 24h 审计 RECON_SHIFT_BOUNDARY_PENDING 条数（班次边界待确认）
--   分母 0 → [INFO] skipped_no_data
-- ------------------------------------------------------------
\if :cash_refund_ready
WITH cash_refund AS (
    SELECT f.flow_id, f.amount, f.refund_record_id, f.remark, f.business_date
      FROM fund_flows f
      JOIN bank_accounts ba ON ba.account_id = f.account_id AND ba.deleted = 0
     WHERE f.deleted = 0 AND f.status = 2 AND f.flow_category = 2 AND f.flow_direction = 2
       AND f.refund_record_id IS NOT NULL
       AND ba.account_type = 2
), closed AS (
    SELECT c.flow_id
      FROM cash_refund c
     WHERE COALESCE(c.remark, '') LIKE '%[slipPair=%'
        OR EXISTS (SELECT 1 FROM refund_shift_attribution a
                    WHERE a.refund_record_id = c.refund_record_id
                      AND (a.slip_id IS NOT NULL OR a.shift_id IS NOT NULL))
        OR EXISTS (SELECT 1 FROM reconciliation_items i
                    WHERE i.deleted = 0 AND i.cleared_flag = false AND i.amount = c.amount
                      AND i.statement_id IN (SELECT statement_id FROM reconciliation_statements
                                              WHERE deleted = 0 AND status <> 'CLOSED'))
)
SELECT CASE WHEN (SELECT count(*) FROM cash_refund WHERE business_date < CURRENT_DATE - 1) = 0
            THEN '[INFO] A38 skipped_no_data 分母=0（无「超过 1 天窗口」的现金退款；全量='
                 || (SELECT count(*) FROM cash_refund) || '）'
            ELSE '[INFO] A38 现金退款闭环率='
                 || round(100.0 * (SELECT count(*) FROM closed WHERE flow_id IN (SELECT flow_id FROM cash_refund WHERE business_date < CURRENT_DATE - 1))
                          / (SELECT count(*) FROM cash_refund WHERE business_date < CURRENT_DATE - 1), 1)
                 || '% (' || (SELECT count(*) FROM closed WHERE flow_id IN (SELECT flow_id FROM cash_refund WHERE business_date < CURRENT_DATE - 1))
                 || '/' || (SELECT count(*) FROM cash_refund WHERE business_date < CURRENT_DATE - 1) || '；全量现金退款='
                 || (SELECT count(*) FROM cash_refund) || ')'
       END
       || ' 待人工确认(近24h RECON_SHIFT_BOUNDARY_PENDING)=' ||
          (SELECT count(*) FROM finance_audit_logs
            WHERE operation_type = 'RECON_SHIFT_BOUNDARY_PENDING'
              AND create_time >= now() - interval '24 hours') AS a38_line,
       (SELECT count(*) FROM cash_refund) AS a38_den
\gset
\else
\set a38_line '[INFO] A38 跳过（表未创建）'
\set a38_den 0
\endif

-- ------------------------------------------------------------
-- A39 所需常量：**班次边界带分钟数**（默认 10）
--   ⚠️ **与后端配置 finance.cash.shift-boundary-minutes 保持一致，改动需同步**；
--      脚本内只此一处定义，禁止在 SQL 里散落硬编码数字。
\set SHIFT_BOUNDARY_MINUTES 10

-- ------------------------------------------------------------
-- A39（**参与判定**）：缴款单恒等式
--   对 deleted=0 的缴款单，两条恒等式都要成立：
--     ① difference_net_fen = (actual_amount − reserve_fen) − (expected_amount − refund_component_fen)
--        差异1 裁定（Owner）：**备用金 reserve_fen 必须从计现额里扣掉**（交班显示「应交 = 抽屉总额 − 备用金」），
--        因此净差异按上式判定；若沿用旧式 actual − (expected − rcf)，1000 元备用金会以假长款回到净差异里。
--        difference_amount（actual_amount − expected_amount）**保持旧口径不动**（兼容历史报表），
--        两者差值 = reserve_fen + refund_component_fen，可作交叉校验。
--     ② refund_component_fen = **期望退款合计**，期望值 = Σ 满足以下**任一**条件的现金退款流水
--        （**且退款的账户门店 = 缴款单门店**：bank_accounts.store_id IS NOT DISTINCT FROM deposit_slips.store_id；
--          不加这条，整日班次窗口会把其它门店的退款也算进来 —— 实测 slip 232(store=2) 曾因此误红）：
--        (a) **自动计入**：refund_time 落在该缴款单对应班次 [start_time, end_time] 内，
--            且**不在边界带**（距 start ≤ N 分钟 或 距 end ≤ N 分钟者剔除；N = :SHIFT_BOUNDARY_MINUTES）
--        (b) **人工归属**：refund_shift_attribution 存在该 refund_record_id 且 shift_id = 该班次
--        —— 依据：N19-2② 的自动计入**不写归属表**（E2E 实测 slip 246：refund_component_fen=2500、
--           attribution 行数=0）。若②只看归属表会恒红，故按「自动 ∪ 人工」取期望值。
--   为什么剔除边界带：班次边界的退款归属到哪个班次本身不确定，后端同样排除边界带（待人工确认，
--   对应审计 RECON_SHIFT_BOUNDARY_PENDING / A38 的待确认计数）。
--   **as-of-create 语义**：② 的期望合计只累加 `refund_time <= slip.create_time` 的退款
--     （自动计入与人工归属**两条路同一时点截断**）—— 因为 rcf 是缴款单**生成时刻的快照**，
--     A39 校验的是「生成逻辑算得对不对」，不是「缴款单是否已过时」。
--   分母 = difference_net_fen IS NOT NULL **且班次窗口已结束**（pos_shifts.end_time < now()）的缴款单。
--   **三项排除各自含义（全部显式输出，不隐藏）**：
--     · still_open=<k>  ：班次窗口未结束 → 快照必滞后于 live 合计，不判（E2E：slip 231/shift 291 全天窗）。
--     · legacy_null=<n> ：N19-2 之前的老单 difference_net_fen 为 NULL（该列未落盘），不判。
--     · stale=<m>       ：窗口内有 **create_time 之后**才发生的退款（store 一致、非边界带）→
--                         缴款单「已过时」的业务信号；**只观测不判定**（E2E：slip 249 的 flow 5636）。
--   输出串统一：`分母=<d>（still_open=<k>；legacy_null=<n>；stale=<m>）`。
-- ------------------------------------------------------------
SELECT (to_regclass('public.deposit_slips') IS NOT NULL
        AND to_regclass('public.refund_shift_attribution') IS NOT NULL
        AND to_regclass('public.fund_flows') IS NOT NULL
        AND EXISTS (SELECT 1 FROM information_schema.columns
                     WHERE table_schema='public' AND table_name='deposit_slips'
                       AND column_name IN ('expected_amount','actual_amount','refund_component_fen','difference_net_fen','shift_id','deleted')
                     GROUP BY table_name HAVING count(*) = 6)) AS slip_identity_ready
\gset

\if :slip_identity_ready
WITH slip AS (
    SELECT s.slip_id, s.store_id, s.shift_id, s.expected_amount, s.actual_amount,
           COALESCE(s.refund_component_fen, 0) AS rcf, s.difference_net_fen, COALESCE(s.reserve_fen, 0) AS reserve_fen,
           s.create_time,             -- **快照时点**：rcf 是这一时刻算出来的
           sh.end_time AS shift_end   -- 班次窗口结束时间（判定集要用：窗口已结束才判）
      FROM deposit_slips s
      LEFT JOIN pos_shifts sh ON sh.shift_id = s.shift_id AND sh.deleted = 0
     WHERE s.deleted = 0
), cash_refund AS (
    -- 现金户退款流水（与 A36/A38 同口径），并带出账户所属门店用于「门店归属」约束
    SELECT f.flow_id, f.amount, f.refund_record_id, f.refund_time, ba.store_id AS store_id
      FROM fund_flows f
      JOIN bank_accounts ba ON ba.account_id = f.account_id AND ba.deleted = 0
     WHERE f.deleted = 0 AND f.status = 2 AND f.flow_category = 2 AND f.flow_direction = 2
       AND f.refund_record_id IS NOT NULL
       AND ba.account_type = 2
), expected AS (
    -- 期望退款合计 = 自动计入（窗内且不在边界带）∪ 人工归属（attribution.shift_id = 该班次）
    --   **且必须门店一致**：退款流水所属门店（bank_accounts.store_id）与缴款单 store_id 相同。
    --   否则整日班次窗口（如 test 数据 00:00–23:59 的 shift）会把**其它门店**的退款也算进来。
    --   ⚠️ pos_shifts 表**没有 store_id 列**（information_schema 实测），门店归属只能经
    --      fund_flows.account_id → bank_accounts.store_id 拿到。
    SELECT s.slip_id, COALESCE(SUM(f.amount), 0) AS amt
      FROM slip s
      JOIN cash_refund f
        ON (f.store_id IS NOT DISTINCT FROM s.store_id)
       -- 【2026-10-28 Owner 裁定】as-of-create 截断**只约束「自动窗口」分支**，不约束「人工归属」分支：
       --   · as-of 原意：防止**自动窗口**把迟到的退款算进已关班的缴款单（防「快照 vs 实时」必然红）；
       --   · 人工归属是解决边界情况的**逃生口**（±边界带功能本身）：财务已确认归属，系统不能说不算。
       --   ⇒ 自动窗口：refund_time <= slip.create_time；人工归属：**不受 as-of 约束**且优先级更高。
       AND (
             (
               f.refund_time IS NOT NULL
               AND (f.refund_time AT TIME ZONE 'Asia/Shanghai') <= s.create_time
               AND EXISTS (SELECT 1 FROM pos_shifts ps
                            WHERE ps.shift_id = s.shift_id AND ps.deleted = 0
                              AND ps.start_time IS NOT NULL AND ps.end_time IS NOT NULL
                              AND (f.refund_time AT TIME ZONE 'Asia/Shanghai')
                                  BETWEEN ps.start_time + make_interval(mins => :'SHIFT_BOUNDARY_MINUTES'::int)
                                      AND ps.end_time   - make_interval(mins => :'SHIFT_BOUNDARY_MINUTES'::int))
             )
             OR
             -- 人工归属：只看该退款是否被归属到本班次，不再叠加 refund_time <= create_time
             EXISTS (SELECT 1 FROM refund_shift_attribution a
                      WHERE a.refund_record_id = f.refund_record_id
                        AND a.shift_id = s.shift_id)
           )
     GROUP BY s.slip_id
), stale AS (
    -- INFO 用：窗口内有「生成之后」才发生的退款（store 一致、非边界带）→ 缴款单已过时（只观测不判定）
    SELECT DISTINCT s.slip_id
      FROM slip s
      JOIN cash_refund f
        ON (f.store_id IS NOT DISTINCT FROM s.store_id)
       AND f.refund_time IS NOT NULL
       AND (f.refund_time AT TIME ZONE 'Asia/Shanghai') > s.create_time
       AND EXISTS (SELECT 1 FROM pos_shifts ps
                    WHERE ps.shift_id = s.shift_id AND ps.deleted = 0
                      AND ps.start_time IS NOT NULL AND ps.end_time IS NOT NULL
                      AND (f.refund_time AT TIME ZONE 'Asia/Shanghai')
                          BETWEEN ps.start_time + make_interval(mins => :'SHIFT_BOUNDARY_MINUTES'::int)
                              AND ps.end_time   - make_interval(mins => :'SHIFT_BOUNDARY_MINUTES'::int))
), judged AS (
    -- 判定集：N19-2 新口径（difference_net_fen 非 NULL）**且班次窗口已结束**
    --   窗口未结束时 refund_component_fen 是「生成时快照」，必然滞后于按窗口现算的 live 合计，
    --   不属于错误 → 不进分母，但以 still_open 计数显式输出（不隐藏）。
    SELECT s.*
      FROM slip s
     WHERE s.difference_net_fen IS NOT NULL
       AND s.shift_end IS NOT NULL
       AND s.shift_end < (now() AT TIME ZONE 'Asia/Shanghai')
), viol AS (
    SELECT s.slip_id
      FROM judged s
      LEFT JOIN expected e ON e.slip_id = s.slip_id
 WHERE s.difference_net_fen <> (s.actual_amount - s.reserve_fen) - (s.expected_amount - s.rcf)
        OR s.rcf <> COALESCE(e.amt, 0)
)
SELECT CASE WHEN (SELECT count(*) FROM judged) = 0
            THEN '[INFO] A39 skipped_no_data 分母=0（无「窗口已结束」的 N19-2 缴款单；still_open='
                 || (SELECT count(*) FROM slip WHERE difference_net_fen IS NOT NULL
                      AND (shift_end IS NULL OR shift_end >= (now() AT TIME ZONE 'Asia/Shanghai')))
                 || '；legacy_null=' || (SELECT count(*) FROM slip WHERE difference_net_fen IS NULL)
                 || '；stale=' || (SELECT count(*) FROM stale) || '）'
            WHEN (SELECT count(*) FROM viol) = 0
            THEN '[PASS] A39 缴款单恒等式 rows=0 分母=' || (SELECT count(*) FROM judged)
                 || '（still_open=' || (SELECT count(*) FROM slip WHERE difference_net_fen IS NOT NULL
                      AND (shift_end IS NULL OR shift_end >= (now() AT TIME ZONE 'Asia/Shanghai')))
                 || '；legacy_null=' || (SELECT count(*) FROM slip WHERE difference_net_fen IS NULL)
                 || '；stale=' || (SELECT count(*) FROM stale) || '）'
            ELSE '[FAIL] A39 缴款单恒等式 rows=' || (SELECT count(*) FROM viol)
                 || ' 分母=' || (SELECT count(*) FROM judged)
                 || '（still_open=' || (SELECT count(*) FROM slip WHERE difference_net_fen IS NOT NULL
                      AND (shift_end IS NULL OR shift_end >= (now() AT TIME ZONE 'Asia/Shanghai')))
                 || '；legacy_null=' || (SELECT count(*) FROM slip WHERE difference_net_fen IS NULL)
                 || '；stale=' || (SELECT count(*) FROM stale) || '）'
                 || ' —— 存在 ' || (SELECT count(*) FROM viol) || ' 张缴款单恒等式不成立，禁止接受'
       END AS a39_line,
       (SELECT count(*) FROM viol) AS a39_n,
       (SELECT count(*) FROM judged) AS a39_den
\gset
\else
\set a39_line '[INFO] A39 跳过（表未创建）'
\set a39_n 0
\set a39_den 0
\endif

-- ------------------------------------------------------------
-- A42（**参与判定**）：差异1 备用金换算一致
--   reserve_fen 必须等于 ROUND(pos_shifts.opening_cash * 100)（opening_cash 单位=元，numeric(10,2)）
--   守卫：opening_cash 非空；列/表缺失 → INFO 跳过（不计入 TOTAL）
-- ------------------------------------------------------------
SELECT (to_regclass('public.deposit_slips') IS NOT NULL
        AND to_regclass('public.pos_shifts') IS NOT NULL
        AND EXISTS (SELECT 1 FROM information_schema.columns
                     WHERE table_schema='public' AND table_name='deposit_slips'
                       AND column_name='reserve_fen')
        AND EXISTS (SELECT 1 FROM information_schema.columns
                     WHERE table_schema='public' AND table_name='pos_shifts'
                       AND column_name='opening_cash')) AS reserve_ready
\gset
\if :reserve_ready
SELECT CASE WHEN (SELECT count(*) FROM deposit_slips WHERE deleted = 0 AND shift_id IS NOT NULL) = 0
            THEN '[INFO] A42 skipped_no_data 分母=0（无带班次的缴款单）'
            WHEN count(*) = 0 THEN '[PASS] A42 备用金换算一致 rows=0 分母=' || (SELECT count(*) FROM deposit_slips WHERE deleted = 0 AND shift_id IS NOT NULL)
            ELSE '[FAIL] A42 备用金换算不一致 rows=' || count(*) || ' 分母=' || (SELECT count(*) FROM deposit_slips WHERE deleted = 0 AND shift_id IS NOT NULL)
       END AS a42_line,
       CASE WHEN (SELECT count(*) FROM deposit_slips WHERE deleted = 0 AND shift_id IS NOT NULL) = 0 THEN 0 ELSE count(*) END AS a42_n,
       (SELECT count(*) FROM deposit_slips WHERE deleted = 0 AND shift_id IS NOT NULL) AS a42_den
  FROM deposit_slips s
  JOIN pos_shifts ps ON ps.shift_id = s.shift_id
 WHERE s.deleted = 0
   AND ps.opening_cash IS NOT NULL
   AND COALESCE(s.reserve_fen, 0) <> ROUND(ps.opening_cash * 100)::bigint
\gset
\else
\set a42_line '[INFO] A42 跳过（reserve_fen / opening_cash 不存在）'
\set a42_n 0
\endif

-- ------------------------------------------------------------
-- A43（**参与判定**）：DEPOSITED 缴款单必须有配对的存行内部转账流水
--   **与 A14 互为反向覆盖，两个方向都要有：**
--     · A14 以**流水**为起点：存在 [depositSlip=X] 键 → 该键下必须出/入各 1 条且净额 0；
--     · A43 以**单据**为起点：status=DEPOSITED 的单 → 必须有与之配对、成对且净额 0 的流水。
--   只保留 A14 会漏「单还在、流水被删/冲正」；只保留 A43 会漏「流水成对但单已不存在」。
--   守卫：deposit_slips / fund_flows 缺失 → INFO 跳过（不计入 TOTAL）。
-- ------------------------------------------------------------
SELECT (to_regclass('public.deposit_slips') IS NOT NULL
        AND to_regclass('public.fund_flows') IS NOT NULL) AS a43_ready
\gset
\if :a43_ready
SELECT CASE WHEN (SELECT count(*) FROM deposit_slips WHERE deleted = 0 AND status = 'DEPOSITED') = 0
            THEN '[INFO] A43 skipped_no_data 分母=0（无 DEPOSITED 缴款单）'
            WHEN count(*) = 0 THEN '[PASS] A43 DEPOSITED 缴款单缺配对存行流水 rows=0 分母=' || (SELECT count(*) FROM deposit_slips WHERE deleted = 0 AND status = 'DEPOSITED')
            ELSE '[FAIL] A43 DEPOSITED 缴款单缺配对存行流水 rows=' || count(*) || ' 分母=' || (SELECT count(*) FROM deposit_slips WHERE deleted = 0 AND status = 'DEPOSITED')
       END AS a43_line,
       CASE WHEN (SELECT count(*) FROM deposit_slips WHERE deleted = 0 AND status = 'DEPOSITED') = 0 THEN 0 ELSE count(*) END AS a43_n,
       (SELECT count(*) FROM deposit_slips WHERE deleted = 0 AND status = 'DEPOSITED') AS a43_den
  FROM deposit_slips s
 WHERE s.deleted = 0
   AND s.status = 'DEPOSITED'
   AND NOT EXISTS (
       SELECT 1 FROM (
           SELECT split_part(split_part(f.remark, '[depositSlip=', 2), ']', 1) AS slip_no,
                  count(*) AS cnt,
                  sum(CASE WHEN f.flow_direction = 2 THEN f.amount ELSE -f.amount END) AS net
             FROM fund_flows f
            WHERE f.deleted = 0
              AND f.remark LIKE '%[depositSlip=%'
            GROUP BY 1
       ) k
        WHERE k.slip_no = s.slip_no
          AND k.cnt = 2
          AND k.net = 0)
\gset
\else
\set a43_line '[INFO] A43 跳过（deposit_slips / fund_flows 不存在）'
\set a43_n 0
\endif

-- ------------------------------------------------------------
-- A44（**参与判定**）：role_permissions.permission_id 必须等于 permissions.permission_id_str
--   **授权必须用 permission_id_str（形如 PERM_FINANCE_DEPOSIT_SLIP_RECEIVE）**；
--   用权限码字符串（finance:deposit-slip:receive）插入的行，在 UserMapper.getUserRolePermissions
--   的 join（rp.PERMISSION_ID = p.permission_id_str）上匹配不上 → **静默失效（接口 403）**。
--   分母 = role_permissions 有效行数；分母=0 → INFO 跳过。
-- ------------------------------------------------------------
SELECT (to_regclass('public.role_permissions') IS NOT NULL
        AND to_regclass('public.permissions') IS NOT NULL) AS a44_ready
\gset
\if :a44_ready
SELECT CASE WHEN (SELECT count(*) FROM role_permissions WHERE COALESCE(deleted,0) = 0) = 0
            THEN '[INFO] A44 skipped_no_data 分母=0（无 role_permissions 有效行）'
            WHEN count(*) = 0 THEN '[PASS] A44 授权 permission_id 形态合法 rows=0 分母=' || (SELECT count(*) FROM role_permissions WHERE COALESCE(deleted,0) = 0)
            ELSE '[FAIL] A44 授权 permission_id 形态非法(不等于任何 permission_id_str) rows=' || count(*)
                 || ' 分母=' || (SELECT count(*) FROM role_permissions WHERE COALESCE(deleted,0) = 0)
                 || ' —— 这类孤儿授权等于静默失效，禁止接受'
       END AS a44_line,
       CASE WHEN (SELECT count(*) FROM role_permissions WHERE COALESCE(deleted,0) = 0) = 0
            THEN 0 ELSE count(*) END AS a44_n
  FROM role_permissions rp
 WHERE COALESCE(rp.deleted,0) = 0
   AND NOT EXISTS (SELECT 1 FROM permissions p WHERE p.permission_id_str = rp.permission_id)
\gset
\else
\set a44_line '[INFO] A44 跳过（role_permissions / permissions 不存在）'
\set a44_n 0
\endif

-- ------------------------------------------------------------
-- A45（**参与判定**）：role_permissions 的**引用可解析性**（与 A44 互补）
--   A44 查「形态自洽」：permission_id 是否等于某个 permission_id_str；
--   A45 查「引用可解析」：role_permissions 的两侧外键是否都能落到实体表——
--     · permission 侧：permission_id 必须能在 permissions.permission_id_str 命中（与 A44 重叠，双保险）；
--     · **role 侧（A45 独有）**：role_id 必须能在 roles.role_id_str 命中，否则该授权同样被 join 丢弃。
--   分母 = role_permissions 有效行数；分母=0 → INFO 跳过。
-- ------------------------------------------------------------
SELECT (to_regclass('public.role_permissions') IS NOT NULL
        AND to_regclass('public.permissions') IS NOT NULL
        AND to_regclass('public.roles') IS NOT NULL) AS a45_ready
\gset
\if :a45_ready
SELECT CASE WHEN (SELECT count(*) FROM role_permissions WHERE COALESCE(deleted,0) = 0) = 0
            THEN '[INFO] A45 skipped_no_data 分母=0（无 role_permissions 有效行）'
            WHEN count(*) = 0 THEN '[PASS] A45 授权引用可解析 rows=0 分母=' || (SELECT count(*) FROM role_permissions WHERE COALESCE(deleted,0) = 0)
            ELSE '[FAIL] A45 授权引用不可解析 rows=' || count(*)
                 || ' 分母=' || (SELECT count(*) FROM role_permissions WHERE COALESCE(deleted,0) = 0)
                 || ' —— permission_id 或 role_id 落不到实体表，授权会被 join 丢弃，禁止接受'
       END AS a45_line,
       CASE WHEN (SELECT count(*) FROM role_permissions WHERE COALESCE(deleted,0) = 0) = 0
            THEN 0 ELSE count(*) END AS a45_n
  FROM role_permissions rp
 WHERE COALESCE(rp.deleted,0) = 0
   AND ( NOT EXISTS (SELECT 1 FROM permissions p WHERE p.permission_id_str = rp.permission_id)
         OR NOT EXISTS (SELECT 1 FROM roles r WHERE r.role_id_str = rp.role_id) )
\gset
\else
\set a45_line '[INFO] A45 跳过（role_permissions / permissions / roles 不存在）'
\set a45_n 0
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
), skipped AS (
    SELECT
      (SELECT count(*) FROM orders WHERE deleted = 0 AND COALESCE(paid_amount, 0) > 0) AS a1_den,
      (SELECT count(*) FROM recon_runs WHERE status = 'FINALIZED') AS a10_den,
      (SELECT count(*) FROM reconciliation_statements WHERE deleted = 0 AND status = 'DRAFT') AS a27_den,
      (SELECT count(*) FROM orders WHERE deleted = 0 AND COALESCE(refund_amount, 0) > 0) AS a30_den,
      (SELECT count(DISTINCT business_date) FROM fund_flows WHERE deleted = 0 AND status = 2 AND flow_category = 2 AND refund_record_id IS NOT NULL) AS a31_den,
      (SELECT count(*) FROM orders WHERE deleted = 0 AND COALESCE(refund_amount, 0) > 0) AS a33_den,
      (SELECT count(*) FROM deposit_slips WHERE deleted = 0 AND shift_id IS NOT NULL) AS a42_den,
      (SELECT count(*) FROM deposit_slips WHERE deleted = 0 AND status = 'DEPOSITED') AS a43_den
)
SELECT line
  FROM (
    SELECT 1 AS ord,
           CASE WHEN (SELECT a1_den FROM skipped) = 0
                THEN '[INFO] A1 skipped_no_data 分母=0（无已支付订单 paid_amount>0）'
                WHEN n = 0 THEN '[PASS] A1 已支付订单缺 SALE 资金流水 rows=0 分母=' || (SELECT a1_den FROM skipped)
                ELSE '[FAIL] A1 已支付订单缺 SALE 资金流水 rows=' || n || ' 分母=' || (SELECT a1_den FROM skipped)
           END AS line
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
    -- A32 参与判定：孤儿退款流水（>0 进 TOTAL）
    SELECT 32,
           :'a32_line'
    UNION ALL
    -- A33 参与判定：逐单退款对拍（>0 进 TOTAL）
    SELECT 33,
           :'a33_line'
    UNION ALL
    -- A32-DIST 信息性：退款匹配状态分布（不进 TOTAL）
    SELECT 34,
           :'a32d_line'
    UNION ALL
    -- A35 信息性：采购付款流水(category=6) 计数（NEW-21 迁移进度；不进 TOTAL）
    SELECT 35,
           :'a35_line'
    UNION ALL
    -- A41 信息性：近24h RECON_SKIP_CASH（现金户跳过多，不进 TOTAL）
    SELECT 36,
           :'a41_line'
    UNION ALL
    -- A36 参与判定：现金退款未闭环（>0 进 TOTAL）
    SELECT 38,
           :'a36_line'
    UNION ALL
    -- A37 参与判定：退款落不进班次窗口（>0 进 TOTAL）
    SELECT 39,
           :'a37_line'
    UNION ALL
    -- A38 信息性：现金退款闭环率 + 待人工确认（不进 TOTAL）
    SELECT 40,
           :'a38_line'
    UNION ALL
    -- A39 参与判定：缴款单恒等式（>0 进 TOTAL）
    SELECT 41,
           :'a39_line'
    UNION ALL
    -- A42 参与判定：备用金换算一致（>0 进 TOTAL）
    SELECT 43,
           :'a42_line'
    UNION ALL
    -- A43 参与判定：DEPOSITED 单缺配对存行流水（>0 进 TOTAL）
    SELECT 44,
           :'a43_line'
    UNION ALL
    -- A44 参与判定：授权 permission_id 形态合法（>0 进 TOTAL）
    SELECT 45,
           :'a44_line'
    UNION ALL
    -- A45 参与判定：授权引用可解析（>0 进 TOTAL）
    SELECT 46,
           :'a45_line'
    UNION ALL
    SELECT 42,
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
             + CASE WHEN :a31_n > 0 THEN 1 ELSE 0 END
             + CASE WHEN :a32_n > 0 THEN 1 ELSE 0 END
             + CASE WHEN :a33_n > 0 THEN 1 ELSE 0 END
             + CASE WHEN :a36_n > 0 THEN 1 ELSE 0 END
             + CASE WHEN :a37_n > 0 THEN 1 ELSE 0 END
             + CASE WHEN :a39_n > 0 THEN 1 ELSE 0 END
              + CASE WHEN :a42_n > 0 THEN 1 ELSE 0 END
              + CASE WHEN :a43_n > 0 THEN 1 ELSE 0 END
              + CASE WHEN :a44_n > 0 THEN 1 ELSE 0 END
              + CASE WHEN :a45_n > 0 THEN 1 ELSE 0 END)
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
-- RECON_AMBIGUITY_RESOLVED / RECON_AMBIGUITY_EXEMPTED）；兼容排除 'EXEMPTED(' 前缀（旧数据）；
-- 并排除「同 statement + 同额 + 两侧同 refundPair」的退款配对（已知业务形态，配对完整性由 A32 兜底）；
-- refundPair 取值**token 优先、列兜底**：[refundPair=<id>] 缺失时用 fund_flows.refund_record_id
-- （A/B 侧经 flow_id、C/D 侧经 bank_transactions.matched_flow_id 关联）。
-- ------------------------------------------------------------
\if :ambiguity_shape_ready
\echo ''
\echo '-- A19 明细（前 20）--'
WITH item_key AS (
    SELECT k.item_id, k.statement_id, k.amount, k.category,
           COALESCE(
               substring(k.reason from '\[refundPair=([^]]+)\]'),
               (SELECT to_jsonb(f) ->> 'refund_record_id'
                  FROM fund_flows f WHERE f.flow_id = k.flow_id),
               (SELECT to_jsonb(f) ->> 'refund_record_id'
                  FROM bank_transactions b
                  JOIN fund_flows f ON f.flow_id = b.matched_flow_id
                 WHERE b.transaction_id = k.transaction_id)
           ) AS pair_key
      FROM reconciliation_items k
     WHERE k.deleted = 0
       AND k.cleared_flag = false
       AND COALESCE(k.reason, '') NOT LIKE 'EXEMPTED(%'
)
SELECT k.statement_id,
       k.amount,
       count(*) FILTER (WHERE k.category IN ('A', 'B')) AS ab_cnt,
       count(*) FILTER (WHERE k.category IN ('C', 'D')) AS cd_cnt
  FROM item_key k
 WHERE NOT EXISTS (
       SELECT 1 FROM item_key p
        WHERE p.statement_id = k.statement_id
          AND p.amount = k.amount
          AND p.item_id <> k.item_id
          AND (p.category IN ('A', 'B')) <> (k.category IN ('A', 'B'))
          AND k.pair_key IS NOT NULL
          AND p.pair_key = k.pair_key)
 GROUP BY k.statement_id, k.amount
HAVING count(*) FILTER (WHERE k.category IN ('A', 'B')) > 0
   AND count(*) FILTER (WHERE k.category IN ('C', 'D')) > 0
 ORDER BY k.statement_id, k.amount
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
            -- 与判定同口径：只判存在性（金额一致性由 A33 对拍）+ 收窄到真退款
            AND f.refund_record_id IS NOT NULL)
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
                 AND f.refund_record_id IS NOT NULL
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

-- ------------------------------------------------------------
-- A32 明细（前 20）：孤儿退款流水（昨天及更早，既无可配银行退款、也不在未清明细）
-- ------------------------------------------------------------
\if :refund_cross_ready
\echo ''
\echo '-- A32 明细（前 20）--'
SELECT f.flow_id, f.flow_no, f.order_id, f.account_id, f.amount, f.business_date, f.create_time,
       left(COALESCE(f.remark, ''), 60) AS remark_snippet
  FROM fund_flows f
 WHERE f.deleted = 0 AND f.status = 2 AND f.flow_category = 2
   AND f.business_date < CURRENT_DATE - 1
   AND NOT EXISTS (SELECT 1 FROM bank_transactions b
                    WHERE b.deleted = 0 AND b.is_refund = true AND b.direction = 2
                      AND b.amount = f.amount AND b.business_date = f.business_date)
   AND NOT EXISTS (SELECT 1 FROM reconciliation_items i
                    WHERE i.deleted = 0 AND i.cleared_flag = false AND i.amount = f.amount
                      AND i.statement_id IN (SELECT statement_id FROM reconciliation_statements
                                              WHERE deleted = 0 AND status <> 'CLOSED'))
 ORDER BY f.business_date DESC, f.flow_id DESC
 LIMIT 20;
\else
\echo ''
\echo '-- A32 明细跳过（表未创建）--'
\endif

-- ------------------------------------------------------------
-- A33 明细（前 20）：订单退款额 ≠ 退款流水合计（差值 = 流水合计 - 订单退款额，单位分）
-- ------------------------------------------------------------
\if :refund_ready
\echo ''
\echo '-- A33 明细（前 20）--'
SELECT o.order_id, o.refund_amount::bigint AS order_refund_fen,
       COALESCE(r.amt, 0) AS flow_refund_fen,
       COALESCE(r.amt, 0) - o.refund_amount::bigint AS diff_fen,
       COALESCE(r.cnt, 0) AS flow_rows
  FROM orders o
  LEFT JOIN (SELECT f.order_id, SUM(f.amount) AS amt, count(*) AS cnt
               FROM fund_flows f
              WHERE f.deleted = 0 AND f.status = 2 AND f.flow_category = 2
                AND f.refund_record_id IS NOT NULL
              GROUP BY f.order_id) r ON r.order_id = o.order_id
 WHERE o.deleted = 0 AND o.refund_amount > 0
   AND COALESCE(r.amt, 0) <> o.refund_amount::bigint
 ORDER BY abs(COALESCE(r.amt, 0) - o.refund_amount::bigint) DESC
 LIMIT 20;
\else
\echo ''
\echo '-- A33 明细跳过（退款 flow_category 常量未配置 / 表未创建）--'
\endif

COMMIT;
