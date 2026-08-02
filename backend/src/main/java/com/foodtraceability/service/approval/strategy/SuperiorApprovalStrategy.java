package com.foodtraceability.service.approval.strategy;

import com.foodtraceability.dto.approval.ApprovalActionDTO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.List;

/**
 * 直属上级审批策略
 *
 * <p>取申请人的直属上级（User.reportTo 字段）作为审批人。
 * 适用场景：员工请假、报销等需要直属上级审批的业务。
 *
 * <p><b>当前实现状态</b>：User 实体暂未包含 reportTo 字段，
 * 当前 resolveApprovers 返回空列表并打印警告日志。
 * 待 User 实体新增 reportTo 字段后，需补充从 UserService 获取申请人直属上级的逻辑。
 *
 * <p>TODO: 待 User 实体新增 reportTo 字段后，补充完整的直属上级解析逻辑。
 */
@Component
public class SuperiorApprovalStrategy implements ApprovalStrategy {

    private static final Logger log = LoggerFactory.getLogger(SuperiorApprovalStrategy.class);
    private static final String STRATEGY_TYPE = "superior";

    @Override
    public String getStrategyType() {
        return STRATEGY_TYPE;
    }

    @Override
    public List<String> resolveApprovers(ApprovalActionDTO actionDTO, String nodeId, String approverValue) {
        // User 实体当前未包含 reportTo 字段，无法解析直属上级
        // 返回空列表不阻断审批流程，由调用方按需处理（如回退到其他策略或人工指定）
        log.warn("直属上级审批策略暂未实现：User 实体未包含 reportTo 字段，"
                        + "businessId={}, businessType={}, nodeId={}",
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
