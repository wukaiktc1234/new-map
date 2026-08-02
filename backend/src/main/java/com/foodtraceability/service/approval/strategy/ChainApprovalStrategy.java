package com.foodtraceability.service.approval.strategy;

import com.foodtraceability.dto.approval.ApprovalActionDTO;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 链式审批策略
 *
 * <p>按预设名单顺序传递审批。approverValue 为用户ID列表，用逗号分隔
 * （如 "user1,user2,user3"）。
 *
 * <p>适用场景：固定流程的多级审批（如大额采购需经采购员→店长→区域经理）。
 */
@Component
public class ChainApprovalStrategy implements ApprovalStrategy {

    private static final String STRATEGY_TYPE = "chain";

    @Override
    public String getStrategyType() {
        return STRATEGY_TYPE;
    }

    @Override
    public List<String> resolveApprovers(ApprovalActionDTO actionDTO, String nodeId, String approverValue) {
        if (approverValue == null || approverValue.isEmpty()) {
            return Collections.emptyList();
        }
        // approverValue 格式："user1,user2,user3"
        return Arrays.stream(approverValue.split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .collect(Collectors.toList());
    }

    @Override
    public boolean canApprove(ApprovalActionDTO actionDTO, String userId, String approverValue) {
        List<String> approvers = resolveApprovers(actionDTO, null, approverValue);
        return approvers.contains(userId);
    }
}
