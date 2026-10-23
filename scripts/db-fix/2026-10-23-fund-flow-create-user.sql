-- ============================================================
-- fund_flows 审计合规：create_user_id 不留 null（模块 1.3，2026-10-23）
--
-- 背景：资金流水必须能追溯到操作人。历史上该列允许 NULL，已有 71 条流水 create_user_id 为 null，
--       管理端无法判断「谁记的账」。本次：
--         1) 列默认值设为 0（服务端仍会写真实操作人；取不到时显式写 0 并 WARN）
--         2) 回填历史 NULL → 0
--         3) 加 NOT NULL 约束，杜绝再次出现 null
--
-- 幂等：可反复执行（SET DEFAULT 重复设置无害；UPDATE 只命中 null；NOT NULL 幂等判断）。
-- 用法：psql -h localhost -p 5432 -U postgres -d food_traceability -f <本文件>
-- ============================================================

-- 1) 默认值 0（新增流水即便漏填也有值）
ALTER TABLE fund_flows ALTER COLUMN create_user_id SET DEFAULT 0;

-- 2) 回填历史 null
UPDATE fund_flows SET create_user_id = 0 WHERE create_user_id IS NULL;

-- 3) NOT NULL（先判断，避免重复执行报错）
DO $$
BEGIN
    IF EXISTS (
        SELECT 1 FROM information_schema.columns
        WHERE table_name = 'fund_flows'
          AND column_name = 'create_user_id'
          AND is_nullable = 'YES'
    ) THEN
        ALTER TABLE fund_flows ALTER COLUMN create_user_id SET NOT NULL;
    END IF;
END $$;

-- 自查：
-- SELECT COUNT(*) FILTER (WHERE create_user_id IS NULL) AS nulls, COUNT(*) FROM fund_flows;
