-- ============================================================
-- 初始化采购订单默认审批流程
-- 为采购订单模块提供开箱即用的单节点审批流程（财务经理审批）
-- ============================================================

INSERT INTO approval_workflows (workflow_id, workflow_name, business_type, template_code, enabled, nodes, conditions)
SELECT 'purchase_order_default_workflow',
       '采购订单默认审批流程',
       'purchase_order',
       'standard-chain',
       TRUE,
       '[{"nodeId":"node_1","nodeName":"财务经理审批","approverType":"role","approverValue":"finance_manager"}]',
       '[]'
WHERE NOT EXISTS (
    SELECT 1 FROM approval_workflows WHERE business_type = 'purchase_order' AND enabled = TRUE AND deleted = 0
);
