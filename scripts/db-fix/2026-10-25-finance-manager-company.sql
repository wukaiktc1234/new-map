SET client_encoding='UTF8';
-- 拍板 1：finance_manager 设计职责 = 统筹全部门店日常对账 → data_scope=company（Owner 放行）
UPDATE roles SET data_scope = 'company', update_time = now() WHERE role_code = 'finance_manager' AND data_scope <> 'company';
-- qa_approver 补归属门店（若需要 department 场景另建 qa_manager_store，见 WORKER_LOG §56.9）
-- location_id=1 仅作员工归属画像（JWT locationId claim、门店业务域读取），
-- 不参与 data_scope=company 的全局判定；若将来把 finance_manager 改回 department，此字段会生效。
UPDATE users SET location_id = 1 WHERE username = 'qa_approver' AND location_id IS NULL;
\echo '=== 拍板 1 回查（按鉴权真实 JOIN）==='
SELECT u.username, u.location_id, COALESCE(string_agg(r.role_code||'('||r.data_scope||')', ',' ORDER BY r.role_code),'(none)') AS resolved
  FROM users u LEFT JOIN user_roles ur ON ur.user_id=u.user_id::text AND ur.deleted=0
  LEFT JOIN roles r ON r.role_id_str=ur.role_id AND r.deleted=0
 WHERE u.username IN ('qa_admin','qa_approver','qa_auditor','qa_store_b') GROUP BY u.username, u.location_id ORDER BY u.username;