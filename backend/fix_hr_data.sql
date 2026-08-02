-- 修复HR模块中的脏数据
-- 1. 将position_id=4的岗位修正为"会计"
UPDATE positions
SET position_code = 'POS_ACCOUNTANT',
    position_name = '会计',
    level = 'P2',
    description = '负责财务核算、凭证管理等工作'
WHERE position_id = 4;

-- 2. 删除重复的会计岗位（ID=1744），并先将引用它的员工调整到position_id=4
UPDATE employees
SET position_id = 4
WHERE position_id = 1744;

DELETE FROM positions WHERE position_id = 1744;

-- 3. 修正员工D、E的姓名
UPDATE employees
SET employee_name = '员工D'
WHERE employee_id = 2080155339590037506;

UPDATE employees
SET employee_name = '员工E'
WHERE employee_id = 2080155339590037507;

-- 验证结果
SELECT position_id, position_code, position_name FROM positions WHERE position_id IN (4, 1744);
SELECT employee_id, employee_code, employee_name, position_id FROM employees WHERE employee_id IN (2080155339590037506, 2080155339590037507);
