-- 修复岗位编制人数默认值
-- 背景：历史岗位数据的 head_count 均为 0，导致前端岗位管理页面编制人数显示为 0。
-- 规则：将编制人数设置为 max(在职人数, 1)，确保每个岗位至少有 1 个编制。

-- 确保 head_count 列存在（兼容列缺失场景）
DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM information_schema.columns
        WHERE table_name = 'positions' AND column_name = 'head_count'
    ) THEN
        ALTER TABLE positions ADD COLUMN head_count INTEGER DEFAULT 0;
        COMMENT ON COLUMN positions.head_count IS '编制人数';
    END IF;
END $$;

-- 修复编制人数：取在职人数与 1 的较大值
UPDATE positions
SET head_count = GREATEST(COALESCE(employee_count, 0), 1)
WHERE deleted = 0
  AND (head_count IS NULL OR head_count = 0);
