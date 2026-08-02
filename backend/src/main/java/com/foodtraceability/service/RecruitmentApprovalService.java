package com.foodtraceability.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.foodtraceability.dto.store.operation.RecruitmentApprovalCreateDTO;
import com.foodtraceability.dto.store.operation.RecruitmentApprovalUpdateDTO;
import com.foodtraceability.dto.store.operation.vo.RecruitmentApprovalVO;

import java.util.List;

/**
 * 招聘审批服务接口
 * 提供门店自主招聘的三级审批流程管理功能
 * 支持提交申请、审批通过/驳回、自动升级检测等核心业务逻辑
 */
public interface RecruitmentApprovalService {

    /**
     * 提交招聘申请
     * 店长提交后进入审批流程，支持自动升级检测（薪资超标/编制超额时升级到更高层级）
     *
     * @param dto             申请DTO（含应聘者信息、建议薪资、面试评分等）
     * @param storeManagerId  提交人ID（店长）
     * @return 创建的审批记录视图对象
     */
    RecruitmentApprovalVO submitApplication(RecruitmentApprovalCreateDTO dto, String storeManagerId);

    /**
     * 审批通过
     * 当前层级审批通过后，检查是否需要升级到下一级或终审通过
     *
     * @param approvalId 审批记录ID
     * @param approverId 审批人ID
     */
    void approve(String approvalId, String approverId);

    /**
     * 审批驳回
     * 驳回原因不能少于10个字符
     *
     * @param approvalId 审批记录ID
     * @param approverId 审批人ID
     * @param dto        驳回DTO（含驳回原因）
     */
    void reject(String approvalId, String approverId, RecruitmentApprovalUpdateDTO dto);

    /**
     * 查询我提交的申请列表
     *
     * @param submitterId 提交人ID
     * @param page        页码
     * @param size        每页大小
     * @return 分页结果
     */
    IPage<RecruitmentApprovalVO> getMyApplications(String submitterId, Integer page, Integer size);

    /**
     * 查询当前用户待审批的记录
     *
     * @param approverId   审批人ID
     * @param currentLevel 当前审批层级
     * @return 待审批记录列表
     */
    List<RecruitmentApprovalVO> getPendingApprovals(String approverId, Integer currentLevel);

    /**
     * 获取审批详情
     *
     * @param approvalId 审批记录ID
     * @return 审批记录视图对象
     */
    RecruitmentApprovalVO getApprovalDetail(String approvalId);
}
