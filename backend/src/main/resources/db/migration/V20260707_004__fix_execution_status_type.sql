-- ============================================================
-- 生产环境 scheduled_task.execution_status 类型修复
-- 版本: V20260707_004
-- 说明: 将 execution_status 从 character varying 统一为 INTEGER，
--       与 ScheduledTask 实体及 MyBatis-Plus 更新语句中的整型参数匹配
-- ============================================================

-- 幂等保护：仅当 execution_status 仍为 character varying 时才执行类型转换
-- 若已是 INTEGER（如其他建表流程已统一类型），则跳过 USING 中的正则匹配
DO $$
BEGIN
    IF EXISTS (
        SELECT 1 FROM information_schema.columns
        WHERE table_name = 'scheduled_task' AND column_name = 'execution_status'
          AND data_type = 'character varying'
    ) THEN
        -- 先移除字符串默认值，避免类型转换时默认值无法转成 INTEGER 报错
        ALTER TABLE scheduled_task ALTER COLUMN execution_status DROP DEFAULT;
        ALTER TABLE scheduled_task ALTER COLUMN execution_status TYPE INTEGER
            USING CASE
                WHEN execution_status IS NULL THEN 0
                WHEN execution_status ~ '^[0-9]+$' THEN execution_status::INTEGER
                ELSE 0
            END;
    END IF;
END $$;

ALTER TABLE scheduled_task ALTER COLUMN execution_status SET DEFAULT 0;
