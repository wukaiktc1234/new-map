package com.foodtraceability.service.approval;

import com.foodtraceability.entity.approval.EmployeeApproval;
import com.foodtraceability.dto.approval.vo.RiskWarningVO;

import java.util.List;
import java.util.Map;

/**
 * 风控规则引擎接口
 * 用于评估审批申请的风险等级，内置24条规则覆盖6种审批类型
 */
public interface RiskRuleEngine {

    /**
     * 评估单个审批申请的风险等级
     * @param approval 审批记录
     * @param contextData 审批上下文数据（根据type不同而不同）
     * @return 风险预警信息，无风险时返回null；多条规则命中时返回最高级别预警
     */
    RiskWarningVO evaluate(EmployeeApproval approval, Object contextData);

    /**
     * 批量评估多个审批申请的风险等级
     * @param approvals 审批记录列表
     * @return 风险预警Map，key为approvalId，无风险的审批不包含在结果中
     */
    Map<String, RiskWarningVO> batchEvaluate(List<EmployeeApproval> approvals);
}
