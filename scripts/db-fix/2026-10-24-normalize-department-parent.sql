-- =============================================================================
-- 2026-10-24  R6 修复：departments.parent_id 的「0 / NULL」双轨归一
--
-- 背景（代码缺陷，已同步修复）：
--   * createDepartment 把 parentId==null 归一为 0 再落库；
--   * updateDepartment 旧实现未归一，改单时会把 NULL 写回，导致同一张表里
--     「0=顶级」与「NULL=顶级」并存；
--   * buildDepartmentTree 把 0 和 NULL 都当根，但 getChildrenDepartments(0) 只查
--     parent_id = 0，漏掉 NULL 根 → 两个接口对「顶级」的集合口径不一致。
--
-- 本脚本做存量数据归一（幂等：重复执行第二次影响 0 行）。
-- 影响列：仅 departments.parent_id、departments.level；不改任何业务表结构。
--
-- 建议先备份：
--   pg_dump -h localhost -p 5432 -U postgres -d food_traceability \
--     --data-only --table=departments -f backups/departments-pre-normalize.sql
--
-- 执行：
--   psql -h localhost -p 5432 -U postgres -d food_traceability \
--     -f scripts/db-fix/2026-10-24-normalize-department-parent.sql
-- =============================================================================

\set ON_ERROR_STOP on

\echo '== BEFORE：parent_id 分布 =='
SELECT count(*) FILTER (WHERE parent_id IS NULL) AS null_parent_cnt,
       count(*) FILTER (WHERE parent_id = 0)      AS zero_parent_cnt,
       count(*) FILTER (WHERE parent_id > 0)      AS has_parent_cnt,
       count(*)                                  AS total_cnt
FROM departments;

BEGIN;

-- 1) 顶级行 parent_id 归一为 0（含已逻辑删除行，保持数据一致）
UPDATE departments
   SET parent_id = 0
 WHERE parent_id IS NULL;

-- 2) 顶级行 level 必须为 1（修复 parent_id 归一前遗留的层级脏值）
UPDATE departments
   SET level = 1
 WHERE parent_id = 0
   AND (level IS NULL OR level <> 1);

COMMIT;

\echo '== AFTER：parent_id 分布 =='
SELECT count(*) FILTER (WHERE parent_id IS NULL) AS null_parent_cnt,
       count(*) FILTER (WHERE parent_id = 0)      AS zero_parent_cnt,
       count(*) FILTER (WHERE parent_id > 0)      AS has_parent_cnt,
       count(*)                                  AS total_cnt
FROM departments;

\echo '== 校验：应输出 OK =='
SELECT CASE
         WHEN count(*) FILTER (WHERE parent_id IS NULL) = 0
          AND count(*) FILTER (WHERE parent_id = 0 AND (level IS NULL OR level <> 1)) = 0
         THEN 'OK: parent_id NULL 已归一为 0，且顶级行 level 均为 1'
         ELSE 'FAIL: 仍存在 parent_id IS NULL 或顶级行 level <> 1'
       END AS result
FROM departments;

-- 回滚（若需）：UPDATE departments SET parent_id = NULL WHERE department_id = <原NULL行ID>;
-- 幂等说明：第二次执行时两条 UPDATE 的 WHERE 均无命中，影响 0 行，分布不变。
