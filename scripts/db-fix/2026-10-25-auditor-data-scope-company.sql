SET client_encoding='UTF8';
-- P3-4 拍板（Owner 放行）：auditor 的 data_scope 由 self 改为 company。
-- 依据：设计稿 §7.1 角色矩阵 `finance:bank:view` 对审计明确写「只读全量」；self 与矩阵矛盾，属数据缺陷。
UPDATE roles SET data_scope = 'company', update_time = now() WHERE role_code = 'auditor' AND data_scope = 'self';
\echo '=== 执行后回查（教训 §52：必须验证实际查询结果）==='
SELECT role_id, role_code, data_scope FROM roles WHERE role_code IN ('auditor','finance_director','finance_manager','store_manager');