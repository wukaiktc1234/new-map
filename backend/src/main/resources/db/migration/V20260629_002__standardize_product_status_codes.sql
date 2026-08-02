-- ============================================
-- 产品中心表 - 统一状态码到项目标准
-- 版本: V20260629_002
-- 说明: 项目统一标准为 1启用/在售(active), 0停用/停售(inactive), 2售罄(soldout)
--       但产品中心早期实现使用了错误约定：
--         - food_categories.status: 1启用/2停用  → 应为 1启用/0停用
--         - dish_combos.status:     1在售/2停售  → 应为 1在售/0停售
--         - foods.status:           1在售/2停售/3售罄 → 应为 1在售/0停售/2售罄
--       本迁移脚本将历史数据迁移到新约定，并补充列注释
-- 影响表: foods, food_categories, dish_combos
-- 注意: foods 表必须先迁移 3→2（售罄），再迁移 2→0（停售），顺序不可调整
-- ============================================

-- ============================================================
-- 1. 数据迁移：foods 表
-- ============================================================

-- 1.1 售罄：3 → 2（必须先执行，否则会被 1.2 覆盖）
UPDATE foods SET status = 2 WHERE status = 3;

-- 1.2 停售：2 → 0
UPDATE foods SET status = 0 WHERE status = 2;

-- 1.3 补充列注释
COMMENT ON COLUMN foods.status IS '菜品状态: 1在售(active), 0停售(inactive), 2售罄(soldout)';

-- ============================================================
-- 2. 数据迁移：food_categories 表
-- ============================================================

-- 2.1 停用：2 → 0
UPDATE food_categories SET status = 0 WHERE status = 2;

-- 2.2 补充列注释
COMMENT ON COLUMN food_categories.status IS '分类状态: 1启用(active), 0停用(inactive)';

-- ============================================================
-- 3. 数据迁移：dish_combos 表
-- ============================================================

-- 3.1 停售：2 → 0
UPDATE dish_combos SET status = 0 WHERE status = 2;

-- 3.2 补充列注释
COMMENT ON COLUMN dish_combos.status IS '套餐状态: 1在售(active), 0停售(inactive)';
