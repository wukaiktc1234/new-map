package com.foodtraceability.service.approval;

import com.baomidou.mybatisplus.extension.service.IService;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.foodtraceability.entity.approval.EmployeeApproval;
import com.foodtraceability.dto.approval.EmployeeApprovalCreateDTO;
import com.foodtraceability.dto.approval.EmployeeApprovalQueryDTO;
import com.foodtraceability.dto.approval.vo.ApprovalDetailVO;
import com.foodtraceability.dto.approval.vo.ApprovalStatsVO;
import com.foodtraceability.dto.approval.vo.EmployeeApprovalVO;

import java.util.List;
import java.util.Map;

/**
 * 员工日常审批服务接口
 * 提供请假、加班、换班、出差、报销、领用等日常审批的统一管理
 */
public interface EmployeeApprovalService extends IService<EmployeeApproval> {

    /**
     * 分页查询当前员工的审批列表（我发起的）
     * @param query 查询条件
     * @param employeeId 当前员工ID
     * @return 分页审批列表
     */
    IPage<EmployeeApprovalVO> getMyApprovalPage(EmployeeApprovalQueryDTO query, String employeeId);

    /**
     * 分页查询待当前员工审批的列表（待我审批的）
     * @param query 查询条件
     * @param reviewerId 审批人ID
     * @return 分页待审批列表
     */
    IPage<EmployeeApprovalVO> getPendingReviewPage(EmployeeApprovalQueryDTO query, String reviewerId);

    /**
     * 获取审批详情（含上下文数据和风险预警）
     * @param approvalId 审批ID
     * @return 审批详情
     */
    ApprovalDetailVO getApprovalDetail(String approvalId);

    /**
     * 提交新的审批申请
     * @param dto 审批创建DTO
     * @param employeeId 申请人ID
     * @return 创建后的审批信息
     */
    EmployeeApprovalVO submitApproval(EmployeeApprovalCreateDTO dto, String employeeId);

    /**
     * 审批操作：通过
     * @param approvalId 审批ID
     * @param reviewerId 审批人ID
     * @param comment 审批意见
     */
    void approveApproval(String approvalId, String reviewerId, String comment);

    /**
     * 审批操作：驳回
     * @param approvalId 审批ID
     * @param reviewerId 审批人ID
     * @param comment 驳回原因
     */
    void rejectApproval(String approvalId, String reviewerId, String comment);

    /**
     * 撤回审批申请（仅pending状态允许撤回）
     * @param approvalId 审批ID
     * @param employeeId 申请人ID
     */
    void withdrawApproval(String approvalId, String employeeId);

    /**
     * 催办审批（向审批人发送催办通知）
     * @param approvalId 审批ID
     * @param employeeId 催办人ID（申请人）
     */
    void urgeApproval(String approvalId, String employeeId);

    /**
     * 获取当前员工的审批统计数据
     * @param employeeId 员工ID
     * @return 审批统计数据
     */
    ApprovalStatsVO getMyApprovalStats(String employeeId);

    /**
     * 批量查询审批基本信息
     * @param approvalIds 审批ID列表
     * @return 审批基本信息Map，key为approvalId
     */
    Map<String, EmployeeApprovalVO> batchGetBasicInfo(List<String> approvalIds);
}
