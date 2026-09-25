-- ============================================================
-- 菜品分类删除逻辑异常 - 孤儿数据清理脚本
-- ============================================================
-- 背景：
--   用户报告删除分类"测试1"时报错"该分类下还有2个菜品"，
--   但菜品管理中看不到该分类的菜品。
--
-- 根因分析：
--   1. foods 表通过 category_id 字段直接关联 food_categories（一对多关系）
--   2. 实际上 foods 表中确实有 2 个菜品（麻婆豆腐 foodId=4、西红柿炒蛋 foodId=10）
--      引用了 categoryId=9（"测试1"分类），且 deleted=0
--   3. 后端删除校验逻辑正确，但用户在菜品管理页面可能因筛选条件未看到这些菜品
--
-- 说明：
--   - food_categories 表没有 food_count 字段，foodCount 在 VO 中动态计算
--   - 没有单独的 food_categories 关联表，foods.category_id 直接关联
--   - 本脚本为只读查询 + 可选清理，请根据实际情况执行
-- ============================================================

-- ============================================================
-- 第一步：查找孤儿数据（foods 表中 category_id 指向已删除或不存在的分类）
-- ============================================================

-- 1.1 查找 foods 表中 category_id 指向已逻辑删除分类的菜品
SELECT f.food_id, f.food_code, f.food_name, f.category_id, f.status, f.deleted,
       fc.category_name, fc.deleted AS category_deleted
FROM foods f
LEFT JOIN food_categories fc ON f.category_id = fc.category_id
WHERE f.deleted = 0
  AND (fc.category_id IS NULL OR fc.deleted = 1)
ORDER BY f.food_id;

-- 1.2 查找 foods 表中所有已逻辑删除但仍引用某分类的菜品（可选查看）
SELECT f.food_id, f.food_code, f.food_name, f.category_id, f.deleted,
       fc.category_name
FROM foods f
LEFT JOIN food_categories fc ON f.category_id = fc.category_id
WHERE f.deleted = 1
  AND f.category_id IS NOT NULL
ORDER BY f.food_id;

-- 1.3 统计每个分类下的实时菜品数量（用于核对分类树显示的 foodCount）
SELECT fc.category_id, fc.category_name, fc.parent_id, fc.status, fc.deleted,
       (SELECT COUNT(*) FROM foods f WHERE f.category_id = fc.category_id AND f.deleted = 0) AS actual_food_count
FROM food_categories fc
ORDER BY fc.parent_id, fc.sort_order, fc.category_id;


-- ============================================================
-- 第二步：清理孤儿数据（请确认后再执行，建议先备份）
-- ============================================================

-- 2.1 【可选】将孤儿菜品的 category_id 置为 NULL（保留菜品，解除分类关联）
-- 适用场景：分类已被删除，但菜品仍需保留
-- !!!执行前请确认！！！
-- UPDATE foods
-- SET category_id = NULL,
--     update_time = CURRENT_TIMESTAMP
-- WHERE deleted = 0
--   AND category_id IS NOT NULL
--   AND category_id NOT IN (
--       SELECT category_id FROM food_categories WHERE deleted = 0
--   );

-- 2.2 【可选】将孤儿菜品转移到指定分类（例如"未分类"或"其他"分类）
-- 适用场景：希望保留菜品的分类归属
-- !!!执行前请确认目标分类ID正确！！！
-- UPDATE foods
-- SET category_id = <目标分类ID>,
--     update_time = CURRENT_TIMESTAMP
-- WHERE deleted = 0
--   AND category_id IS NOT NULL
--   AND category_id NOT IN (
--       SELECT category_id FROM food_categories WHERE deleted = 0
--   );

-- 2.3 【可选】逻辑删除孤儿菜品（如果菜品已无用途）
-- !!!执行前请确认！！！
-- UPDATE foods
-- SET deleted = 1,
--     update_time = CURRENT_TIMESTAMP
-- WHERE deleted = 0
--   AND category_id IS NOT NULL
--   AND category_id NOT IN (
--       SELECT category_id FROM food_categories WHERE deleted = 0
--   );


-- ============================================================
-- 第三步：清理已删除分类下的已删除菜品（物理删除，可选）
-- ============================================================
-- 说明：如果 foods 表中存在 deleted=1 的菜品引用了已删除的分类，
--       这些数据属于历史残留，可以物理删除以释放空间。
-- !!!执行前请确认！！！
-- DELETE FROM foods
-- WHERE deleted = 1
--   AND category_id IS NOT NULL
--   AND category_id NOT IN (
--       SELECT category_id FROM food_categories
--   );


-- ============================================================
-- 第四步：针对本次"测试1"分类（categoryId=9）的专项查询
-- ============================================================

-- 4.1 查看"测试1"分类（categoryId=9）下所有菜品（包括已删除的）
SELECT f.food_id, f.food_code, f.food_name, f.category_id, f.status, f.deleted,
       f.create_time, f.update_time
FROM foods f
WHERE f.category_id = 9
ORDER BY f.food_id;

-- 4.2 查看"测试1"分类（categoryId=9）下未删除的菜品（删除校验会统计这些）
SELECT f.food_id, f.food_code, f.food_name, f.category_id, f.status, f.deleted
FROM foods f
WHERE f.category_id = 9
  AND f.deleted = 0
ORDER BY f.food_id;

-- 4.3 【可选】如果确认要删除"测试1"分类（categoryId=9），
--     需要先处理其下的 2 个菜品（foodId=4 麻婆豆腐, foodId=10 西红柿炒蛋）：
-- 方案A：转移到其他分类（推荐）
-- UPDATE foods SET category_id = <目标分类ID>, update_time = CURRENT_TIMESTAMP WHERE category_id = 9 AND deleted = 0;
-- 方案B：逻辑删除这些菜品
-- UPDATE foods SET deleted = 1, update_time = CURRENT_TIMESTAMP WHERE category_id = 9 AND deleted = 0;
-- 然后即可删除分类：
-- UPDATE food_categories SET deleted = 1, update_time = CURRENT_TIMESTAMP WHERE category_id = 9;


-- ============================================================
-- 备注：food_categories 表无需更新 food_count 字段
-- ============================================================
-- 本项目中 foodCount 是在 CategoryVO 中通过 foodNewMapper.countByCategoryId()
-- 动态计算的，food_categories 表本身没有 food_count 字段，
-- 因此无需执行 UPDATE food_categories SET food_count = ... 这类操作。
