-- ============================================================
-- 2026-10-23-fund-flow-status-complete.sql
-- 模块 2.2：历史资金流水状态统一为「已完成」
--
-- 【业务裁决（2026-10-23，财务口径）】
--   fund_flows 里现有的历史流水都是**已实际发生并到账/付出**的记账事实
--   （银行账户余额已随之变动、凭证/付款单/销售入账均已落库），
--   因此状态语义应为 2-已完成，而不是 1-待处理。
--   1-待处理 只应属于「已录入但尚未确认/可能取消」的草稿态；
--   历史数据里并没有这种草稿，全部置 2 是纠正而不是臆造。
--   保留 CHECK(1..3) 约束（ck_fund_flows_status_range），不放开值域。
--
-- 【何时要再跑一次（2026-10-23 Owner 裁决后的运维约定）】
--   FundFlowServiceImpl.create() 目前仍把新流水初值写成 1-待处理
--   （Java 侧改 2 由 call-number 负责），所以**每次跑完 E2E / 造数 / 批量补账之后**
--   都要再执行一次本脚本，把新产生的 status=1 收敛回 2-已完成，否则界面又会出现
--   「待处理」的历史流水，与 Owner「资金流水状态=已完成」的口径不一致。
--   本脚本幂等：没有 1 可收敛时更新 0 行，重复执行安全。
--
-- 行为：
--   1) 执行前打印 status 分布快照（BEFORE）
--   2) UPDATE fund_flows SET status = 2 WHERE status = 1（含历史 NULL，防御性处理）
--   3) 执行后打印分布快照（AFTER）并断言：不存在 status=1 / status IS NULL
--   4) 幂等：再跑一次 UPDATE 命中 0 行，分布不变
--
-- 用法：psql -h localhost -U postgres -d food_traceability -f 本文件
-- 回滚（如需）：UPDATE fund_flows SET status = 1 WHERE status = 2;  -- 会一并改回本次之前就是 2 的行
-- ============================================================

BEGIN;

-- ---------- BEFORE：分布快照 ----------
SELECT 'BEFORE' AS phase, status, COUNT(*) AS rows
  FROM public.fund_flows
 GROUP BY status
 ORDER BY status NULLS FIRST;

SELECT 'BEFORE' AS phase,
       COUNT(*)                                        AS total_rows,
       COUNT(*) FILTER (WHERE status IS NULL)          AS status_null,
       COUNT(*) FILTER (WHERE status = 1)              AS status_1_pending,
       COUNT(*) FILTER (WHERE status = 2)              AS status_2_completed,
       COUNT(*) FILTER (WHERE status = 3)              AS status_3_cancelled
  FROM public.fund_flows;

-- ---------- 更新 ----------
DO $$
DECLARE
    v_rows bigint;
BEGIN
    UPDATE public.fund_flows
       SET status      = 2,
           update_time = CURRENT_TIMESTAMP
     WHERE status = 1
        OR status IS NULL;

    GET DIAGNOSTICS v_rows = ROW_COUNT;
    RAISE NOTICE '[info] 状态统一为 2-已完成：更新 % 行（幂等重跑应为 0）', v_rows;

    -- CHECK(1..3) 约束保留；若历史库里被删过则补回，保证后续不能写出越界状态
    IF NOT EXISTS (
        SELECT 1 FROM pg_constraint
         WHERE conname  = 'ck_fund_flows_status_range'
           AND conrelid = 'public.fund_flows'::regclass
    ) THEN
        ALTER TABLE public.fund_flows
            ADD CONSTRAINT ck_fund_flows_status_range CHECK (status BETWEEN 1 AND 3);
        RAISE NOTICE '[ok] 补回约束 ck_fund_flows_status_range (1..3)';
    ELSE
        RAISE NOTICE '[skip] 约束 ck_fund_flows_status_range 已存在（保留不动）';
    END IF;
END $$;

-- ---------- AFTER：分布快照 + 断言 ----------
SELECT 'AFTER' AS phase, status, COUNT(*) AS rows
  FROM public.fund_flows
 GROUP BY status
 ORDER BY status NULLS FIRST;

SELECT 'AFTER' AS phase,
       COUNT(*)                                        AS total_rows,
       COUNT(*) FILTER (WHERE status IS NULL)          AS status_null,
       COUNT(*) FILTER (WHERE status = 1)              AS status_1_pending,
       COUNT(*) FILTER (WHERE status = 2)              AS status_2_completed,
       COUNT(*) FILTER (WHERE status = 3)              AS status_3_cancelled,
       CASE WHEN COUNT(*) FILTER (WHERE status IS NULL OR status = 1) = 0
            THEN 'PASS: 无 1/NULL 残留'
            ELSE 'FAIL: 仍有 1/NULL 残留' END         AS assertion
  FROM public.fund_flows;

COMMENT ON COLUMN public.fund_flows.status IS
    '流水状态：1-待处理 2-已完成 3-已取消（CHECK 1..3）。2026-10-23 业务裁决：历史流水均已实际到账，统一置 2-已完成。';

COMMIT;
