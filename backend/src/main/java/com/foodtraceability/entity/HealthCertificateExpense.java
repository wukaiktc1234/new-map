package com.foodtraceability.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

import java.time.LocalDate;


/**
 * 健康证费用报销实体类
 * 用于存储健康证费用报销相关信息
 */
@TableName("health_certificate_expense")
public class HealthCertificateExpense {
    /**
     * 报销ID
     */
    @TableId(type = IdType.AUTO)
    private Long id;
    
    /**
     * 健康证ID
     */
    private String healthCertificateId;
    
    /**
     * 员工ID
     */
    private String employeeId;
    
    /**
     * 员工姓名
     */
    private String employeeName;
    
    /**
     * 所属门店
     */
    private String store;
    
    /**
     * 报销金额
     */
    private Double amount;
    
    /**
     * 发票类型
     */
    private String invoiceType;
    
    /**
     * 发票号码
     */
    private String invoiceNumber;
    
    /**
     * 发票日期
     */
    private LocalDate invoiceDate;
    
    /**
     * 发票附件路径列表
     */
    private String invoiceAttachments;
    
    /**
     * 报销事由
     */
    private String reason;
    
    /**
     * 备注
     */
    private String note;
    
    /**
     * 报销状态
     * pending: 待审批
     * approved: 已审批
     * reimbursed: 已报销
     * rejected: 已拒绝
     */
    private String status;
    
    /**
     * 申请日期
     */
    private LocalDate applyDate;
    
    /**
     * 审批日期
     */
    private LocalDate approveDate;
    
    /**
     * 报销日期
     */
    private LocalDate reimburseDate;
    
    /**
     * 拒绝原因
     */
    private String rejectReason;
    
    /**
     * 报销类型
     * new: 新办
     * renewal: 续期
     */
    private String expenseType;
    
    /**
     * 审批历史（JSON格式存储）
     */
    private String approvalHistory;
    
    /**
     * 创建人
     */
    private String creator;
    
    /**
     * 创建时间
     */
    private LocalDate createTime;
    
    /**
     * 最后更新人
     */
    private String updater;
    
    /**
     * 最后更新时间
     */
    private LocalDate updateTime;
    
    /**
     * 支付重试次数
     */
    private Integer paymentRetryCount = 0;
    
    /**
     * 支付状态
     * pending: 待支付
     * success: 支付成功
     * failed: 支付失败
     * manual: 人工处理中
     */
    private String paymentStatus;
    
    /**
     * 支付方式
     * auto: 自动支付
     * manual: 人工支付
     * batch: 批量支付
     */
    private String paymentMethod;
    
    /**
     * 支付流水号
     */
    private String paymentTransactionId;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getHealthCertificateId() {
        return healthCertificateId;
    }

    public void setHealthCertificateId(String healthCertificateId) {
        this.healthCertificateId = healthCertificateId;
    }

    public String getEmployeeId() {
        return employeeId;
    }

    public void setEmployeeId(String employeeId) {
        this.employeeId = employeeId;
    }

    public String getEmployeeName() {
        return employeeName;
    }

    public void setEmployeeName(String employeeName) {
        this.employeeName = employeeName;
    }

    public String getStore() {
        return store;
    }

    public void setStore(String store) {
        this.store = store;
    }

    public Double getAmount() {
        return amount;
    }

    public void setAmount(Double amount) {
        this.amount = amount;
    }

    public String getInvoiceType() {
        return invoiceType;
    }

    public void setInvoiceType(String invoiceType) {
        this.invoiceType = invoiceType;
    }

    public String getInvoiceNumber() {
        return invoiceNumber;
    }

    public void setInvoiceNumber(String invoiceNumber) {
        this.invoiceNumber = invoiceNumber;
    }

    public LocalDate getInvoiceDate() {
        return invoiceDate;
    }

    public void setInvoiceDate(LocalDate invoiceDate) {
        this.invoiceDate = invoiceDate;
    }

    public String getInvoiceAttachments() {
        return invoiceAttachments;
    }

    public void setInvoiceAttachments(String invoiceAttachments) {
        this.invoiceAttachments = invoiceAttachments;
    }

    public String getReason() {
        return reason;
    }

    public void setReason(String reason) {
        this.reason = reason;
    }

    public String getNote() {
        return note;
    }

    public void setNote(String note) {
        this.note = note;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public LocalDate getApplyDate() {
        return applyDate;
    }

    public void setApplyDate(LocalDate applyDate) {
        this.applyDate = applyDate;
    }

    public LocalDate getApproveDate() {
        return approveDate;
    }

    public void setApproveDate(LocalDate approveDate) {
        this.approveDate = approveDate;
    }

    public LocalDate getReimburseDate() {
        return reimburseDate;
    }

    public void setReimburseDate(LocalDate reimburseDate) {
        this.reimburseDate = reimburseDate;
    }

    public String getRejectReason() {
        return rejectReason;
    }

    public void setRejectReason(String rejectReason) {
        this.rejectReason = rejectReason;
    }

    public String getExpenseType() {
        return expenseType;
    }

    public void setExpenseType(String expenseType) {
        this.expenseType = expenseType;
    }

    public String getApprovalHistory() {
        return approvalHistory;
    }

    public void setApprovalHistory(String approvalHistory) {
        this.approvalHistory = approvalHistory;
    }

    public String getCreator() {
        return creator;
    }

    public void setCreator(String creator) {
        this.creator = creator;
    }

    public LocalDate getCreateTime() {
        return createTime;
    }

    public void setCreateTime(LocalDate createTime) {
        this.createTime = createTime;
    }

    public String getUpdater() {
        return updater;
    }

    public void setUpdater(String updater) {
        this.updater = updater;
    }

    public LocalDate getUpdateTime() {
        return updateTime;
    }

    public void setUpdateTime(LocalDate updateTime) {
        this.updateTime = updateTime;
    }

    public Integer getPaymentRetryCount() {
        return paymentRetryCount;
    }

    public void setPaymentRetryCount(Integer paymentRetryCount) {
        this.paymentRetryCount = paymentRetryCount;
    }

    public String getPaymentStatus() {
        return paymentStatus;
    }

    public void setPaymentStatus(String paymentStatus) {
        this.paymentStatus = paymentStatus;
    }

    public String getPaymentMethod() {
        return paymentMethod;
    }

    public void setPaymentMethod(String paymentMethod) {
        this.paymentMethod = paymentMethod;
    }

    public String getPaymentTransactionId() {
        return paymentTransactionId;
    }

    public void setPaymentTransactionId(String paymentTransactionId) {
        this.paymentTransactionId = paymentTransactionId;
    }
}
