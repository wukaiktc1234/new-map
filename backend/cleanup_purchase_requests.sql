-- 清理脏的采购申请测试数据
DELETE FROM purchase_request_item WHERE request_id IN (SELECT request_id FROM purchase_request WHERE title LIKE '%?%' OR department_name LIKE '%?%' OR applicant_name LIKE '%?%');
DELETE FROM purchase_request WHERE title LIKE '%?%' OR department_name LIKE '%?%' OR applicant_name LIKE '%?%';

-- 同时清理E2E测试产生的非真实数据（admin作为申请人、部门编码而非ID）
DELETE FROM purchase_request_item WHERE request_id IN (SELECT request_id FROM purchase_request WHERE applicant_id = '1' OR department_id LIKE 'DEPT_%');
DELETE FROM purchase_request WHERE applicant_id = '1' OR department_id LIKE 'DEPT_%';

-- 查看剩余数据
SELECT request_id, request_no, title, applicant_id, applicant_name, department_id, department_name, status, total_amount FROM purchase_request WHERE deleted = 0 ORDER BY request_no;
