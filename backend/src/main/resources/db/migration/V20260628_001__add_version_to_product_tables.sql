-- ============================================
-- 产品中心表 - 添加乐观锁 version 列
-- 版本: V20260628_001
-- 说明: 实体层已标注 @Version，需同步数据库
-- 影响表: foods, food_categories, dish_combos, dish_recipes
-- ============================================

-- 菜品表
ALTER TABLE foods ADD COLUMN IF NOT EXISTS version INTEGER NOT NULL DEFAULT 0;
COMMENT ON COLUMN foods.version IS '乐观锁版本号';

-- 分类表
ALTER TABLE food_categories ADD COLUMN IF NOT EXISTS version INTEGER NOT NULL DEFAULT 0;
COMMENT ON COLUMN food_categories.version IS '乐观锁版本号';

-- 套餐表
ALTER TABLE dish_combos ADD COLUMN IF NOT EXISTS version INTEGER NOT NULL DEFAULT 0;
COMMENT ON COLUMN dish_combos.version IS '乐观锁版本号';

-- 配方表
ALTER TABLE dish_recipes ADD COLUMN IF NOT EXISTS version INTEGER NOT NULL DEFAULT 0;
COMMENT ON COLUMN dish_recipes.version IS '乐观锁版本号';
