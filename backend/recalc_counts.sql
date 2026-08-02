-- 根据实际员工数据重新计算部门和岗位的在岗人数

-- 1. 重置所有部门的 employee_count
UPDATE departments SET employee_count = 0;

-- 2. 按 department_id 统计在职/试用期员工数并更新
WITH dept_counts AS (
  SELECT department_id, COUNT(*) AS cnt
  FROM employees
  WHERE status IN (1, 2) AND deleted = 0 AND department_id IS NOT NULL
  GROUP BY department_id
)
UPDATE departments d
SET employee_count = dc.cnt
FROM dept_counts dc
WHERE d.department_id = dc.department_id;

-- 3. 重置所有岗位的 employee_count
UPDATE positions SET employee_count = 0;

-- 4. 按 position_id 统计在职/试用期员工数并更新
WITH pos_counts AS (
  SELECT position_id, COUNT(*) AS cnt
  FROM employees
  WHERE status IN (1, 2) AND deleted = 0 AND position_id IS NOT NULL
  GROUP BY position_id
)
UPDATE positions p
SET employee_count = pc.cnt
FROM pos_counts pc
WHERE p.position_id = pc.position_id;

-- 验证
SELECT d.department_id, d.dept_name, d.employee_count
FROM departments d
WHERE d.deleted = 0
ORDER BY d.department_id;
