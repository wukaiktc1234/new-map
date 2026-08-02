package com.foodtraceability.dataservice;

import com.foodtraceability.dto.store.operation.vo.RecruitmentApprovalVO;

import java.util.List;
import java.util.Map;

/**
 * 招聘审批数据服务接口
 * 提供招聘审批的二级缓存(L1本地+L2 Redis)和批量查询功能
 * 支持按岗位、审批人等维度查询
 * 缓存键格式: recruitment_approval:basic:{approvalId}
 */
public interface RecruitmentApprovalDataService {

    /**
     * 批量获取招聘审批基本信息
     *
     * @param approvalIds 审批记录ID列表
     * @return 审批ID到视图对象的映射
     */
    Map<String, RecruitmentApprovalVO> batchGetApprovalBasicInfo(List<String> approvalIds);

    /**
     * 获取单个招聘审批基本信息
     *
     * @param approvalId 审批记录ID
     * @return 招聘审批视图对象
     */
    RecruitmentApprovalVO getApprovalBasicInfo(String approvalId);

    /**
     * 根据招聘岗位ID查询审批记录列表
     *
     * @param postId 岗位ID
     * @return 该岗位下的审批记录列表
     */
    List<RecruitmentApprovalVO> getApprovalsByPostId(String postId);

    /**
     * 获取当前用户待审批的记录列表
     *
     * @param approverId   当前审批人ID
     * @param currentLevel 当前审批层级
     * @return 待审批记录列表
     */
    List<RecruitmentApprovalVO> getPendingApprovalsForUser(String approverId, Integer currentLevel);

    /**
     * 清除指定审批记录的缓存
     *
     * @param approvalId 审批记录ID
     */
    void clearApprovalCache(String approvalId);
}
