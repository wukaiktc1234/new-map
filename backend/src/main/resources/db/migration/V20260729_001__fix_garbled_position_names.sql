-- ============================================================
-- 迁移脚本: V20260729_001__fix_garbled_position_names.sql
-- 任务: 修复岗位表中名称乱码（含 ?）的数据，清理无员工关联的脏数据
-- ============================================================

-- 1. 删除岗位名称为乱码且无员工关联的岗位（避免页面显示 ?????）
DELETE FROM positions
WHERE position_name LIKE '%?%'
  AND deleted = 0
  AND NOT EXISTS (
      SELECT 1
      FROM employees e
      WHERE e.position_id = positions.position_id
        AND e.deleted = 0
        AND e.status IN (1, 2)
  );

-- 2. 对于仍有员工关联但名称乱码的岗位，使用 position_code 兜底回显名称
UPDATE positions
SET position_name = COALESCE(NULLIF(position_code, ''), '未知岗位')
WHERE position_name LIKE '%?%'
  AND deleted = 0;
