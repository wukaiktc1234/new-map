package com.foodtraceability.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.foodtraceability.entity.HealthCertificateExpense;

import java.util.List;

/**
 * 健康证报销管理Service接口
 * 用于健康证报销相关的业务逻辑处理
 */
public interface HealthCertificateExpenseService extends IService<HealthCertificateExpense> {
    
    /**
     * 根据健康证ID获取报销记录
     * @param healthCertificateId 健康证ID
     * @return 报销记录列表
     */
    List<HealthCertificateExpense> getByHealthCertificateId(String healthCertificateId);
    
    /**
     * 根据员工ID获取报销记录
     * @param employeeId 员工ID
     * @return 报销记录列表
     */
    List<HealthCertificateExpense> getByEmployeeId(String employeeId);
    
    /**
     * 根据门店获取报销记录
     * @param store 门店名称
     * @return 报销记录列表
     */
    List<HealthCertificateExpense> getByStore(String store);
    
    /**
     * 更新报销状态
     * @param id 报销ID
     * @param status 状态
     * @return 更新结果
     */
    boolean updateStatus(Long id, String status);

    /**
     * 提交报销申请
     * @param expense 报销申请信息
     * @return 提交结果
     */
    boolean submitExpense(HealthCertificateExpense expense);

    /**
     * 人事初审报销申请
     * @param id 报销ID
     * @param status 审批状态
     * @param approver 审批人
     * @param comment 审批意见
     * @return 审批结果
     */
    boolean hrReviewExpense(Long id, String status, String approver, String comment);

    /**
     * 财务审核报销申请
     * @param id 报销ID
     * @param status 审批状态
     * @param approver 审批人
     * @param comment 审批意见
     * @return 审批结果
     */
    boolean financeReviewExpense(Long id, String status, String approver, String comment);

    /**
     * 财务报销处理
     * @param id 报销ID
     * @param reimburseDate 报销日期
     * @return 处理结果
     */
    boolean reimburseExpense(Long id, String reimburseDate);

    /**
     * 批量拒绝报销申请
     * @param ids 报销ID列表
     * @param reason 拒绝原因
     * @param approver 审批人
     * @return 处理结果
     */
    boolean batchRejectExpense(List<Long> ids, String reason, String approver);

    /**
     * 上传发票附件
     * @param id 报销ID
     * @param attachmentPath 附件路径
     * @return 上传结果
     */
    boolean uploadInvoiceAttachment(Long id, String attachmentPath);
    
    /**
     * 获取待人事初审的报销申请
     * @return 报销申请列表
     */
    List<HealthCertificateExpense> getPendingHrReviewExpenses();
    
    /**
     * 获取待财务审核的报销申请
     * @return 报销申请列表
     */
    List<HealthCertificateExpense> getPendingFinanceReviewExpenses();
    
    /**
     * 获取已报销的记录
     * @param startDate 开始日期
     * @param endDate 结束日期
     * @return 报销记录列表
     */
    List<HealthCertificateExpense> getReimbursedExpenses(String startDate, String endDate);
}
