package com.foodtraceability.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.foodtraceability.entity.finance.FinanceApproval;

import java.util.List;

/**
 * 财务审批Mapper接口
 * @author example
 * @since 2025-12-06
 */
public interface FinanceApprovalMapper extends BaseMapper<FinanceApproval> {

    /**
     * 根据业务ID和业务类型查询审批记录
     * @param businessId 业务ID
     * @param businessType 业务类型
     * @return 审批记录列表
     */
    List<FinanceApproval> selectByBusinessIdAndType(Long businessId, String businessType);

    /**
     * 根据申请人ID查询审批记录
     * @param applicantId 申请人ID
     * @return 审批记录列表
     */
    List<FinanceApproval> selectByApplicantId(Long applicantId);

    /**
     * 根据审批人ID查询待审批记录
     * @param approverId 审批人ID
     * @return 待审批记录列表
     */
    List<FinanceApproval> selectPendingApprovalsByApproverId(Long approverId);

    /**
     * 获取审批记录详情
     * @param approvalId 审批ID
     * @return 审批记录详情
     */
    FinanceApproval selectApprovalDetail(Long approvalId);
}
