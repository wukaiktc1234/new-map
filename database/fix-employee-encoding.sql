-- 修复员工表中文乱码数据
-- 这些是测试数据，需要手动更新为正确的中文

USE food_traceability;

-- 更新员工表的测试数据
UPDATE employees SET EMPLOYEE_NAME = '张财务', gender = 'male' WHERE EMPLOYEE_CODE = 'EMP_FIN_001';
UPDATE employees SET EMPLOYEE_NAME = '李会计', gender = 'female' WHERE EMPLOYEE_CODE = 'EMP_FIN_002';
UPDATE employees SET EMPLOYEE_NAME = '王采购', gender = 'male' WHERE EMPLOYEE_CODE = 'EMP_PUR_001';
UPDATE employees SET EMPLOYEE_NAME = '赵采购', gender = 'female' WHERE EMPLOYEE_CODE = 'EMP_PUR_002';
UPDATE employees SET EMPLOYEE_NAME = '钱运营', gender = 'male' WHERE EMPLOYEE_CODE = 'EMP_OPS_001';
UPDATE employees SET EMPLOYEE_NAME = '孙运营', gender = 'female' WHERE EMPLOYEE_CODE = 'EMP_OPS_002';
UPDATE employees SET EMPLOYEE_NAME = '李厨师', gender = 'male' WHERE EMPLOYEE_CODE = 'EMP_KIT_001';
UPDATE employees SET EMPLOYEE_NAME = '周厨师', gender = 'female' WHERE EMPLOYEE_CODE = 'EMP_KIT_002';
UPDATE employees SET EMPLOYEE_NAME = '吴服务', gender = 'male' WHERE EMPLOYEE_CODE = 'EMP_SRV_001';
UPDATE employees SET EMPLOYEE_NAME = '郑服务', gender = 'female' WHERE EMPLOYEE_CODE = 'EMP_SRV_002';

-- 更新入职档案表的测试数据
UPDATE onboarding_archive SET candidate_name = '张三' WHERE employee_code = 'EMP20260004';

-- 更新邀请码记录表的测试数据
UPDATE invitation_send_record SET bound_name = '张三' WHERE employee_code = 'EMP20260004';

-- 验证结果
SELECT '=== 员工表修复结果 ===' as info;
SELECT EMPLOYEE_CODE, EMPLOYEE_NAME, gender FROM employees WHERE EMPLOYEE_CODE LIKE 'EMP_%' LIMIT 10;

SELECT '=== 入职档案修复结果 ===' as info;
SELECT id, candidate_name, employee_code FROM onboarding_archive WHERE deleted = 0;

SELECT '=== 邀请码修复结果 ===' as info;
SELECT id, bound_name, employee_code FROM invitation_send_record LIMIT 5;
