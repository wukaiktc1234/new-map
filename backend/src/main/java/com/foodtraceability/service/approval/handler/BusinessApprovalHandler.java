package com.foodtraceability.service.approval.handler;

/**
 * 业务审批处理器接口
 * 让各业务模块（采购订单、采购合同、报销等）注册处理器，
 * 在审批通过/驳回时自动联动业务实体状态变更。
 *
 * <p>设计原则：
 * <ul>
 *   <li>每种 businessType 对应一个 handler 实现</li>
 *   <li>通过 Spring @Component 自动注册，运行时按 businessType 路由</li>
 *   <li>handler 实现应委托给已有的业务 Service（如 PurchaseOrderService.approveOrder）
 *       避免重复实现业务逻辑</li>
 * </ul>
 *
 * <p>这是 RBAC+动态审批链的第二层（业务状态联动）：
 * 审批流程本身只记录审批日志，业务实体的状态变更由各业务模块的 handler 完成。
 */
public interface BusinessApprovalHandler {

    /**
     * 获取该处理器对应的业务类型
     * @return 业务类型标识（如 "purchase_order"、"purchase_contract"）
     */
    String getBusinessType();

    /**
     * 审批通过时触发，更新业务实体状态
     * @param businessId 业务ID
     * @param comment 审批意见
     * @param operatorId 操作人ID
     */
    void onApproved(String businessId, String comment, String operatorId);

    /**
     * 审批驳回时触发，更新业务实体状态
     * @param businessId 业务ID
     * @param comment 驳回原因
     * @param operatorId 操作人ID
     */
    void onRejected(String businessId, String comment, String operatorId);
}
