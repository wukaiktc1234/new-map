-- 修复店长/组长在采购申请中的数据权限范围
-- 业务说明：采购申请按部门维度提交，当前测试数据 store_id 为空。
-- 将 store_manager / team_leader 的 data_scope 由 store 调整为 department，
-- 使其能够查看本部门采购申请，避免上线后因门店维度无数据而产生权限"空窗"。

UPDATE roles
SET data_scope = 'department',
    update_time = CURRENT_TIMESTAMP
WHERE role_code IN ('store_manager', 'team_leader')
  AND data_scope = 'store';
