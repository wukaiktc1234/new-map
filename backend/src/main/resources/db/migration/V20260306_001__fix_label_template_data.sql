-- 修复标签模板表中的size字段为空的记录（PostgreSQL兼容）
-- 使用label_width和label_height字段填充size字段

-- 确保表中有size字段（V1.0.0.6创建的表中可能缺少该字段）
DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM information_schema.columns
        WHERE table_name = 'label_template' AND column_name = 'size'
    ) THEN
        ALTER TABLE label_template ADD COLUMN size TEXT;
        COMMENT ON COLUMN label_template.size IS '标签尺寸配置(JSON)';
    END IF;
END $$;

-- 确保表中有background_color字段（V1.0.0.6创建的表中可能缺少该字段）
DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM information_schema.columns
        WHERE table_name = 'label_template' AND column_name = 'background_color'
    ) THEN
        ALTER TABLE label_template ADD COLUMN background_color VARCHAR(20);
        COMMENT ON COLUMN label_template.background_color IS '背景颜色';
    END IF;
END $$;

-- 填充size字段
UPDATE label_template
SET size = json_build_object('width', label_width, 'height', label_height)
WHERE size IS NULL;

-- 修复标签模板表中的elements字段为空的记录
-- 设置为空数组
UPDATE label_template
SET elements = '[]'
WHERE elements IS NULL;

-- 修复标签模板表中的background_color字段为空的记录
-- 设置为默认白色
UPDATE label_template
SET background_color = '#FFFFFF'
WHERE background_color IS NULL;
