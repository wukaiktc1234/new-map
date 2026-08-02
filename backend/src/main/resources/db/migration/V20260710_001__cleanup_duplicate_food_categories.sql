-- =====================================================
-- 清理 food_categories 表重复数据并添加唯一约束
-- 版本: V20260710_001
-- 背景: BaseDatabaseInitializer 每次启动都执行 V6.0.0 脚本，
--       该脚本 INSERT INTO food_categories ... ON CONFLICT DO NOTHING
--       未指定冲突列，category_id 自增主键永远不冲突，
--       导致每次启动都插入 6 条重复分类记录（共 12 套 72 条）。
-- 修复策略:
--   1. 物理删除重复分类（保留每个 category_name+parent_id 组合的最小 category_id）
--   2. 修复 foods 表中引用了不存在/已删除分类的记录
--   3. 添加部分唯一索引防止未来重复（仅对 deleted=0 的记录生效）
-- =====================================================

-- 步骤1: 修复 foods 表中引用了不存在分类的记录（category_id=7 不存在）
-- 清蒸鲈鱼(FD002) 和 水煮鱼(FD009) 的 category_id=7 该分类不存在，修复为热菜(category_id=1)
UPDATE foods
SET category_id = 1
WHERE category_id = 7
  AND food_code IN ('FD002', 'FD009');

-- 步骤2: 物理删除重复分类记录
-- 保留规则: 每个 (category_name, parent_id) 组合保留最小 category_id 的记录
-- 注意: foods 表中没有引用这些重复 ID（已通过步骤1修复 FD002/FD009 的 category_id=7）
DELETE FROM food_categories
WHERE category_id NOT IN (
    SELECT MIN(category_id)
    FROM food_categories
    WHERE deleted = 0
    GROUP BY category_name, parent_id
)
AND deleted = 0;

-- 步骤3: 添加部分唯一索引（仅对未删除记录生效，允许同名分类存在于已删除记录中）
-- 索引名遵循项目规范: uk_{table}_{field1}_{field2}
CREATE UNIQUE INDEX IF NOT EXISTS uk_food_categories_name_parent
    ON food_categories (category_name, parent_id)
    WHERE deleted = 0;

-- 步骤4: 验证清理结果（应剩余 6 条顶级分类：热菜/凉菜/主食/汤类/饮品/甜品）
-- 注意: 此查询仅用于人工验证，不影响迁移执行
-- SELECT category_id, category_name, parent_id, deleted FROM food_categories WHERE deleted = 0 ORDER BY category_id;
