-- ============================================================
-- 迁移脚本: V20260728_009__remove_duplicate_positions.sql
-- 任务: 清理重复导入的职位数据，保留员工实际关联的职位，
--       确保岗位管理页面人数统计与员工管理一致。
-- ============================================================

-- 删除通过 POS_ 前缀批量导入的重复职位（且当前无员工关联）
DELETE FROM positions
WHERE position_code LIKE 'POS\_%' ESCAPE '\'
  AND NOT EXISTS (
      SELECT 1
      FROM employees e
      WHERE e.position_id = positions.position_id
        AND e.deleted = 0
  );
