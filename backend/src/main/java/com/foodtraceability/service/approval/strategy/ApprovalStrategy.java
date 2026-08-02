package com.foodtraceability.service.approval.strategy;

import com.foodtraceability.dto.approval.ApprovalActionDTO;

import java.util.List;

/**
 * 审批策略接口（策略模式）
 *
 * <p>不同组织模式 / 业务场景使用不同的审批策略：
 * <ul>
 *   <li>SuperiorApprovalStrategy: 取申请人的直属上级（report_to 字段）</li>
 *   <li>RoleBasedApprovalStrategy: 取当前门店拥有特定角色的人</li>
 *   <li>ChainApprovalStrategy: 按预设名单顺序传递</li>
 * </ul>
 *
 * <p>客户在后台切换"组织模式"时，系统底层只是切换了使用的策略实现类。
 * 这是 RBAC+动态审批链的第四层（策略模式）。
 */
public interface ApprovalStrategy {

    /**
     * 获取策略类型标识
     * @return 策略类型（如 "superior"、"role_based"、"chain"）
     */
    String getStrategyType();

    /**
     * 解析当前节点的实际审批人
     * @param actionDTO 审批操作参数
     * @param nodeId 节点ID
     * @param approverValue 节点配置的审批人值（角色编码/上级编码/用户ID等）
     * @return 实际审批人ID列表（会签场景可能有多个）
     */
    List<String> resolveApprovers(ApprovalActionDTO actionDTO, String nodeId, String approverValue);

    /**
     * 判断当前用户是否有权审批
     * @param actionDTO 审批操作参数
     * @param userId 当前用户ID
     * @param approverValue 节点配置的审批人值
     * @return 是否有权审批
     */
    boolean canApprove(ApprovalActionDTO actionDTO, String userId, String approverValue);
}
