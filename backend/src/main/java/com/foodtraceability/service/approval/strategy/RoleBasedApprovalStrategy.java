package com.foodtraceability.service.approval.strategy;

import com.foodtraceability.dto.approval.ApprovalActionDTO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.List;

/**
 * 基于角色的审批策略
 *
 * <p>取当前门店拥有特定角色编码的用户作为审批人。
 * 适用场景：店长审批、财务审批等基于角色的审批。
 *
 * <p>approverValue 为角色编码（如 "store_manager"、"finance"）。
 *
 * <p><b>当前实现状态</b>：UserService 暂未提供 queryUserIdsByRoleCode 方法，
 * 当前 resolveApprovers 返回空列表并打印警告日志。
 * 待 UserService 新增该方法后，补充角色用户查询逻辑。
 *
 * <p>TODO: 待 UserService 实现 queryUserIdsByRoleCode 后，补充完整的角色用户解析逻辑。
 */
@Component
public class RoleBasedApprovalStrategy implements ApprovalStrategy {

    private static final Logger log = LoggerFactory.getLogger(RoleBasedApprovalStrategy.class);
    private static final String STRATEGY_TYPE = "role_based";

    @Override
    public String getStrategyType() {
        return STRATEGY_TYPE;
    }

    @Override
    public List<String> resolveApprovers(ApprovalActionDTO actionDTO, String nodeId, String approverValue) {
        if (approverValue == null || approverValue.isEmpty()) {
            return Collections.emptyList();
        }
        // UserService 当前未提供 queryUserIdsByRoleCode 方法
        // 返回空列表不阻断审批流程，由调用方按需处理（如回退到其他策略或人工指定）
        log.warn("基于角色的审批策略暂未实现：UserService 未提供 queryUserIdsByRoleCode 方法，"
                        + "roleCode={}, businessId={}, businessType={}, nodeId={}",
                approverValue,
                actionDTO != null ? actionDTO.getBusinessId() : null,
                actionDTO != null ? actionDTO.getBusinessType() : null,
                nodeId);
        return Collections.emptyList();
    }

    @Override
    public boolean canApprove(ApprovalActionDTO actionDTO, String userId, String approverValue) {
        List<String> approvers = resolveApprovers(actionDTO, null, approverValue);
        return approvers.contains(userId);
    }
}
