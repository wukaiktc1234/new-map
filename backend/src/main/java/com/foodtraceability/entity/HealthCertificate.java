package com.foodtraceability.entity;

import com.baomidou.mybatisplus.annotation.IdType;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

import java.time.LocalDate;

/**
 * 健康证实体类
 * 用于存储健康证基本信息
 */
@TableName("health_certificate")
public class HealthCertificate {
    /**
     * 健康证ID
     */
    @TableId(type = IdType.ASSIGN_ID)
    private String id;
    
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
     * 门店ID
     */
    private String storeId;

    /**
     * 门店名称
     */
    private String storeName;
    
    /**
     * 健康证号
     */
    private String certificateNumber;
    
    /**
     * 签发日期
     */
    private LocalDate issueDate;
    
    /**
     * 到期日期
     */
    private LocalDate expiryDate;
    
    /**
     * 健康证状态
     * valid: 有效
     * expiring: 即将过期
     * expired: 已过期
     */
    private String status;
    
    /**
     * 签发机构
     */
    private String issuer;
    
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
    private String expenseStatus;
    
    /**
     * 审核状态
     * draft: 草稿
     * pending: 待审核
     * approved: 已通过
     * rejected: 已拒绝
     */
    private String approvalStatus;
    
    /**
     * 审核人
     */
    private String approvalBy;
    
    /**
     * 审核日期
     */
    private LocalDate approvalDate;
    
    /**
     * 拒绝原因
     */
    private String rejectReason;
    
    /**
     * 提交日期
     */
    private LocalDate submissionDate;
    
    /**
     * 提交人
     */
    private String submittedBy;
    
    /**
     * 健康证图片路径
     */
    private String certificateImage;
    
    /**
     * 最后一次报销ID
     */
    private Long lastExpenseId;
    
    /**
     * 操作人
     */
    private String operator;
    
    /**
     * 操作时间
     */
    private LocalDate operationTime;
    
    /**
     * 剩余天数
     */
    private Integer expiryDays;
    
    /**
     * 软删除标记
     */
    private Boolean deleted;
    
    /**
     * 删除时间
     */
    private LocalDate deletedAt;
    
    /**
     * 删除人
     */
    private String deletedBy;

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
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

    public String getStoreId() {
        return storeId;
    }

    public void setStoreId(String storeId) {
        this.storeId = storeId;
    }

    public String getStoreName() {
        return storeName;
    }

    public void setStoreName(String storeName) {
        this.storeName = storeName;
    }

    public String getCertificateNumber() {
        return certificateNumber;
    }

    public void setCertificateNumber(String certificateNumber) {
        this.certificateNumber = certificateNumber;
    }

    public LocalDate getIssueDate() {
        return issueDate;
    }

    public void setIssueDate(LocalDate issueDate) {
        this.issueDate = issueDate;
    }

    public LocalDate getExpiryDate() {
        return expiryDate;
    }

    public void setExpiryDate(LocalDate expiryDate) {
        this.expiryDate = expiryDate;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getIssuer() {
        return issuer;
    }

    public void setIssuer(String issuer) {
        this.issuer = issuer;
    }

    public String getNote() {
        return note;
    }

    public void setNote(String note) {
        this.note = note;
    }

    public String getExpenseStatus() {
        return expenseStatus;
    }

    public void setExpenseStatus(String expenseStatus) {
        this.expenseStatus = expenseStatus;
    }

    public String getApprovalStatus() {
        return approvalStatus;
    }

    public void setApprovalStatus(String approvalStatus) {
        this.approvalStatus = approvalStatus;
    }

    public String getApprovalBy() {
        return approvalBy;
    }

    public void setApprovalBy(String approvalBy) {
        this.approvalBy = approvalBy;
    }

    public LocalDate getApprovalDate() {
        return approvalDate;
    }

    public void setApprovalDate(LocalDate approvalDate) {
        this.approvalDate = approvalDate;
    }

    public String getRejectReason() {
        return rejectReason;
    }

    public void setRejectReason(String rejectReason) {
        this.rejectReason = rejectReason;
    }

    public LocalDate getSubmissionDate() {
        return submissionDate;
    }

    public void setSubmissionDate(LocalDate submissionDate) {
        this.submissionDate = submissionDate;
    }

    public String getSubmittedBy() {
        return submittedBy;
    }

    public void setSubmittedBy(String submittedBy) {
        this.submittedBy = submittedBy;
    }

    public String getCertificateImage() {
        return certificateImage;
    }

    public void setCertificateImage(String certificateImage) {
        this.certificateImage = certificateImage;
    }

    public Long getLastExpenseId() {
        return lastExpenseId;
    }

    public void setLastExpenseId(Long lastExpenseId) {
        this.lastExpenseId = lastExpenseId;
    }

    public String getOperator() {
        return operator;
    }

    public void setOperator(String operator) {
        this.operator = operator;
    }

    public LocalDate getOperationTime() {
        return operationTime;
    }

    public void setOperationTime(LocalDate operationTime) {
        this.operationTime = operationTime;
    }

    public Integer getExpiryDays() {
        return expiryDays;
    }

    public void setExpiryDays(Integer expiryDays) {
        this.expiryDays = expiryDays;
    }

    public Boolean getDeleted() {
        return deleted;
    }

    public void setDeleted(Boolean deleted) {
        this.deleted = deleted;
    }

    public LocalDate getDeletedAt() {
        return deletedAt;
    }

    public void setDeletedAt(LocalDate deletedAt) {
        this.deletedAt = deletedAt;
    }

    public String getDeletedBy() {
        return deletedBy;
    }

    public void setDeletedBy(String deletedBy) {
        this.deletedBy = deletedBy;
    }
}
