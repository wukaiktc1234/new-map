package com.foodtraceability.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.foodtraceability.entity.HealthCertificate;

import java.util.List;

/**
 * 健康证管理Service接口
 * 用于健康证相关的业务逻辑处理
 */
public interface HealthCertificateService extends IService<HealthCertificate> {
    
    /**
     * 根据员工ID获取健康证信息
     * @param employeeId 员工ID
     * @return 健康证信息
     */
    HealthCertificate getByEmployeeId(String employeeId);
    
    /**
     * 根据门店获取健康证列表
     * @param store 门店名称
     * @return 健康证列表
     */
    List<HealthCertificate> getByStore(String store);
    
    /**
     * 更新健康证状态
     * @param id 健康证ID
     * @param status 状态
     * @return 更新结果
     */
    boolean updateStatus(String id, String status);
    
    /**
     * 批量更新健康证状态
     * @param ids 健康证ID列表
     * @param status 状态
     * @return 更新结果
     */
    boolean batchUpdateStatus(List<String> ids, String status);
    
    /**
     * 更新健康证报销状态
     * @param id 健康证ID
     * @param expenseStatus 报销状态
     * @return 更新结果
     */
    boolean updateExpenseStatus(String id, String expenseStatus);
    
    /**
     * 计算并更新所有健康证的剩余天数和状态
     */
    void calculateAndUpdateStatus();
    
    /**
     * 发送健康证到期提醒
     * @param days 提前天数
     * @return 发送结果
     */
    boolean sendExpiryReminders(int days);
    
    /**
     * 提交健康证审核
     * @param id 健康证ID
     * @param submittedBy 提交人
     * @return 提交结果
     */
    boolean submitForApproval(String id, String submittedBy);
    
    /**
     * 审核健康证
     * @param id 健康证ID
     * @param approvalStatus 审核状态
     * @param approvedBy 审核人
     * @param rejectReason 拒绝原因
     * @return 审核结果
     */
    boolean approveHealthCertificate(String id, String approvalStatus, String approvedBy, String rejectReason);
    
    /**
     * 检查健康证是否可以申请报销
     * @param id 健康证ID
     * @return 是否可以申请报销
     */
    boolean canApplyForExpense(String id);
}
