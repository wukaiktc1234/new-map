-- ============================================================
-- 2026-10-23-fund-flow-status-normalize.sql
-- 目的：给 fund_flows.status 建立可用口径（C3）
--   现状：列 integer DEFAULT 0，活库 50 行全是 0；0 不属任何已定义状态，
--         FundFlow 实体原先没有 status 字段，前端详情只能显示「未知」。
-- 口径（docs/alignment/finance-system-state-machine.md 3.7）：
--   1 = 待处理(pending)  2 = 已完成(completed)  3 = 已取消(cancelled)
-- 做法：
--   1) 0/NULL/越界值 → 1（待处理；流水创建后的初态）
--   2) 列默认值改为 1、加 NOT NULL
--   3) 补 CHECK 约束（1..3），防止再写入无法渲染的状态码
-- 可重复执行：先判类型/是否存在，重复跑只输出 [skip]。
-- 用法：psql -h localhost -U postgres -d food_traceability -f 本文件
-- ============================================================

BEGIN;

DO $$
DECLARE
    v_type   text;
    v_null   text;
    v_def    text;
    v_rows   bigint;
BEGIN
    SELECT data_type, is_nullable, column_default
      INTO v_type, v_null, v_def
      FROM information_schema.columns
     WHERE table_schema = 'public'
       AND table_name   = 'fund_flows'
       AND column_name  = 'status';

    IF v_type IS NULL THEN
        RAISE NOTICE '[skip] fund_flows.status 不存在，跳过（本脚本不负责建列）';
        RETURN;
    END IF;

    RAISE NOTICE '[info] status 当前类型=% 可空=% 默认=%', v_type, v_null, COALESCE(v_def, 'NULL');

    -- 1) 值归一：0 / NULL / 越界 → 1（待处理）
    UPDATE public.fund_flows
       SET status      = 1,
           update_time = CURRENT_TIMESTAMP
     WHERE status IS NULL
        OR status < 1
        OR status > 3;

    GET DIAGNOSTICS v_rows = ROW_COUNT;
    RAISE NOTICE '[info] 状态归一更新 % 行（0/NULL/越界 → 1 待处理）', v_rows;

    -- 2) 默认值 + NOT NULL
    EXECUTE 'ALTER TABLE public.fund_flows ALTER COLUMN status SET DEFAULT 1';
    IF v_null = 'YES' THEN
        EXECUTE 'ALTER TABLE public.fund_flows ALTER COLUMN status SET NOT NULL';
        RAISE NOTICE '[ok] status 已设为 NOT NULL';
    ELSE
        RAISE NOTICE '[skip] status 已是 NOT NULL';
    END IF;

    -- 3) 值域约束
    IF NOT EXISTS (
        SELECT 1 FROM pg_constraint
         WHERE conname  = 'ck_fund_flows_status_range'
           AND conrelid = 'public.fund_flows'::regclass
    ) THEN
        EXECUTE 'ALTER TABLE public.fund_flows
                 ADD CONSTRAINT ck_fund_flows_status_range
                 CHECK (status BETWEEN 1 AND 3)';
        RAISE NOTICE '[ok] 已添加约束 ck_fund_flows_status_range (1..3)';
    ELSE
        RAISE NOTICE '[skip] 约束 ck_fund_flows_status_range 已存在';
    END IF;

    EXECUTE 'COMMENT ON COLUMN public.fund_flows.status IS '
         || quote_literal('流水状态：1-待处理 2-已完成 3-已取消（约束 ck_fund_flows_status_range 1..3）。2026-10-23 由 0（无对应状态）归一为 1。');
END $$;

-- 自检
SELECT status, COUNT(*) AS rows FROM public.fund_flows GROUP BY status ORDER BY status;

COMMIT;
