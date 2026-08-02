package com.foodtraceability.dto.finance;

import java.math.BigDecimal;
import java.util.Date;

/**
 * 财务审批视图对象VO
 *
 * <p>Sprint 3.2 P1 F-033：财务审批列表查询时，关联展示发票报销信息。
 * 基础字段与 FinanceApproval 实体对齐；关联字段（reimbursementNo/totalAmount/departmentName）
 * 仅当 businessType=INVOICE_REIMBURSEMENT 时填充，由 Service 层批量关联查询组装。</p>
 */
public class FinanceApprovalVO {

    /** 审批ID */
    private Long approvalId;

    /** 业务类型（INVOICE_REIMBURSEMENT/PURCHASE/EXPENSE/PAYMENT/LOAN 等） */
    private String businessType;

    /** 业务ID */
    private Long businessId;

    /** 审批金额（元，BigDecimal） */
    private BigDecimal amount;

    /** 审批状态（DRAFT/SUBMITTED/APPROVED/REJECTED/CANCELLED） */
    private String status;

    /** 审批标题 */
    private String title;

    /** 审批内容 */
    private String content;

    /** 申请人ID */
    private Long applicantId;

    /** 申请人姓名 */
    private String applicantName;

    /** 审批人ID */
    private Long approverId;

    /** 审批人姓名 */
    private String approverName;

    /** 审批意见 */
    private String approvalComment;

    /** 申请时间 */
    private Date applyTime;

    /** 审批时间 */
    private Date approveTime;

    /** 创建时间 */
    private Date createTime;

    // ============ 关联字段（INVOICE_REIMBURSEMENT 类型专用，批量查询填充） ============

    /** 报销单号（关联 InvoiceReimbursement.reimbursementNo） */
    private String reimbursementNo;

    /** 报销总金额（关联 InvoiceReimbursement.totalAmount，单位：分） */
    private Long totalAmount;

    /** 部门名称（关联 InvoiceReimbursement.departmentName） */
    private String departmentName;

    public FinanceApprovalVO() {
    }

    public Long getApprovalId() {
        return approvalId;
    }

    public void setApprovalId(Long approvalId) {
        this.approvalId = approvalId;
    }

    public String getBusinessType() {
        return businessType;
    }

    public void setBusinessType(String businessType) {
        this.businessType = businessType;
    }

    public Long getBusinessId() {
        return businessId;
    }

    public void setBusinessId(Long businessId) {
        this.businessId = businessId;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public void setAmount(BigDecimal amount) {
        this.amount = amount;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public Long getApplicantId() {
        return applicantId;
    }

    public void setApplicantId(Long applicantId) {
        this.applicantId = applicantId;
    }

    public String getApplicantName() {
        return applicantName;
    }

    public void setApplicantName(String applicantName) {
        this.applicantName = applicantName;
    }

    public Long getApproverId() {
        return approverId;
    }

    public void setApproverId(Long approverId) {
        this.approverId = approverId;
    }

    public String getApproverName() {
        return approverName;
    }

    public void setApproverName(String approverName) {
        this.approverName = approverName;
    }

    public String getApprovalComment() {
        return approvalComment;
    }

    public void setApprovalComment(String approvalComment) {
        this.approvalComment = approvalComment;
    }

    public Date getApplyTime() {
        return applyTime;
    }

    public void setApplyTime(Date applyTime) {
        this.applyTime = applyTime;
    }

    public Date getApproveTime() {
        return approveTime;
    }

    public void setApproveTime(Date approveTime) {
        this.approveTime = approveTime;
    }

    public Date getCreateTime() {
        return createTime;
    }

    public void setCreateTime(Date createTime) {
        this.createTime = createTime;
    }

    public String getReimbursementNo() {
        return reimbursementNo;
    }

    public void setReimbursementNo(String reimbursementNo) {
        this.reimbursementNo = reimbursementNo;
    }

    public Long getTotalAmount() {
        return totalAmount;
    }

    public void setTotalAmount(Long totalAmount) {
        this.totalAmount = totalAmount;
    }

    public String getDepartmentName() {
        return departmentName;
    }

    public void setDepartmentName(String departmentName) {
        this.departmentName = departmentName;
    }
}
