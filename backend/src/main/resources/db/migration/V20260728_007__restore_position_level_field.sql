-- ============================================================
-- 迁移脚本: V20260728_007__restore_position_level_field.sql
-- 任务: 恢复 positions 表的 level 字段，避免启动时表结构校验失败
-- 说明: 职级体系字段暂不做删除处理，先保持数据库与校验逻辑一致
-- ============================================================

-- 恢复 positions 表的 level 字段
ALTER TABLE IF EXISTS positions
    ADD COLUMN IF NOT EXISTS level VARCHAR(50);

COMMENT ON COLUMN positions.level IS '职级';

-- 为历史数据填充默认值，避免 NULL 导致校验或查询异常
UPDATE positions
SET level = '初级'
WHERE deleted = 0
  AND (level IS NULL OR level = '');
