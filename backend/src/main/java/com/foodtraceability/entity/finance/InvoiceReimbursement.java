package com.foodtraceability.entity.finance;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;

import java.io.Serializable;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 发票报销申请实体
 *
 * <p>Sprint 3.1 P0 F-001：从 entity/ 根目录迁移至 entity/finance/，金额字段
 * BigDecimal → Long（分），时间字段 created_at/updated_at → create_time/update_time，
 * 主键字段 id → reimbursement_id（雪花算法），与 FinanceInvoice 职责边界明确：</p>
 * <ul>
 *   <li>FinanceInvoice：进项/销项发票管理（外部发票）</li>
 *   <li>InvoiceReimbursement：内部员工报销申请（业务单据）</li>
 * </ul>
 */
@TableName("invoice_reimbursement")
public class InvoiceReimbursement implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 报销单ID（雪花算法） */
    @TableId(value = "reimbursement_id", type = IdType.ASSIGN_ID)
    private Long reimbursementId;

    /** 租户ID */
    @TableField("tenant_id")
    private Long tenantId;

    /** 报销单号（自动生成：RE + yyyyMMdd + 4 位序号） */
    @TableField("reimbursement_no")
    private String reimbursementNo;

    /** 申请人ID */
    @TableField("applicant_id")
    private Long applicantId;

    /** 申请人姓名（冗余字段，便于查询展示） */
    @TableField("applicant_name")
    private String applicantName;

    /** 部门ID */
    @TableField("department_id")
    private Long departmentId;

    /** 部门名称（冗余字段） */
    @TableField("department_name")
    private String departmentName;

    /** 报销类型（差旅费/招待费/办公费/交通费/通讯费/其他） */
    @TableField("reimbursement_type")
    private String reimbursementType;

    /** 报销总金额（单位：分） */
    @TableField("total_amount")
    private Long totalAmount;

    /** 审批通过金额（单位：分） */
    @TableField("approved_amount")
    private Long approvedAmount;

    /**
     * 单据状态
     * 0-草稿 1-已审批 2-已付款 3-已取消 4-已拒绝
     */
    @TableField("status")
    private Integer status;

    /** 申请日期 */
    @TableField("apply_date")
    private LocalDate applyDate;

    /** 审批日期 */
    @TableField("approve_date")
    private LocalDate approveDate;

    /** 审批人ID */
    @TableField("approver_id")
    private Long approverId;

    /** 审批人姓名 */
    @TableField("approver_name")
    private String approverName;

    /** 拒绝/取消原因 */
    @TableField("reject_reason")
    private String rejectReason;

    /** 备注 */
    @TableField("remark")
    private String remark;

    /**
     * 付款状态
     * 0-未付款 1-已付款
     */
    @TableField("payment_status")
    private Integer paymentStatus;

    /** 付款日期 */
    @TableField("payment_date")
    private LocalDate paymentDate;

    /** 付款凭证号 */
    @TableField("payment_voucher_no")
    private String paymentVoucherNo;

    /** 创建人ID */
    @TableField("created_by")
    private Long createdBy;

    /** 创建时间 */
    @TableField(value = "create_time", fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    /** 更新人ID */
    @TableField("updated_by")
    private Long updatedBy;

    /** 更新时间 */
    @TableField(value = "update_time", fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;

    /** 逻辑删除标识（0-未删除 1-已删除） */
    @TableLogic
    @TableField("deleted")
    private Integer deleted;

    public InvoiceReimbursement() {
    }

    public Long getReimbursementId() {
        return reimbursementId;
    }

    public void setReimbursementId(Long reimbursementId) {
        this.reimbursementId = reimbursementId;
    }

    public Long getTenantId() {
        return tenantId;
    }

    public void setTenantId(Long tenantId) {
        this.tenantId = tenantId;
    }

    public String getReimbursementNo() {
        return reimbursementNo;
    }

    public void setReimbursementNo(String reimbursementNo) {
        this.reimbursementNo = reimbursementNo;
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

    public Long getDepartmentId() {
        return departmentId;
    }

    public void setDepartmentId(Long departmentId) {
        this.departmentId = departmentId;
    }

    public String getDepartmentName() {
        return departmentName;
    }

    public void setDepartmentName(String departmentName) {
        this.departmentName = departmentName;
    }

    public String getReimbursementType() {
        return reimbursementType;
    }

    public void setReimbursementType(String reimbursementType) {
        this.reimbursementType = reimbursementType;
    }

    public Long getTotalAmount() {
        return totalAmount;
    }

    public void setTotalAmount(Long totalAmount) {
        this.totalAmount = totalAmount;
    }

    public Long getApprovedAmount() {
        return approvedAmount;
    }

    public void setApprovedAmount(Long approvedAmount) {
        this.approvedAmount = approvedAmount;
    }

    public Integer getStatus() {
        return status;
    }

    public void setStatus(Integer status) {
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

    public String getRejectReason() {
        return rejectReason;
    }

    public void setRejectReason(String rejectReason) {
        this.rejectReason = rejectReason;
    }

    public String getRemark() {
        return remark;
    }

    public void setRemark(String remark) {
        this.remark = remark;
    }

    public Integer getPaymentStatus() {
        return paymentStatus;
    }

    public void setPaymentStatus(Integer paymentStatus) {
        this.paymentStatus = paymentStatus;
    }

    public LocalDate getPaymentDate() {
        return paymentDate;
    }

    public void setPaymentDate(LocalDate paymentDate) {
        this.paymentDate = paymentDate;
    }

    public String getPaymentVoucherNo() {
        return paymentVoucherNo;
    }

    public void setPaymentVoucherNo(String paymentVoucherNo) {
        this.paymentVoucherNo = paymentVoucherNo;
    }

    public Long getCreatedBy() {
        return createdBy;
    }

    public void setCreatedBy(Long createdBy) {
        this.createdBy = createdBy;
    }

    public LocalDateTime getCreateTime() {
        return createTime;
    }

    public void setCreateTime(LocalDateTime createTime) {
        this.createTime = createTime;
    }

    public Long getUpdatedBy() {
        return updatedBy;
    }

    public void setUpdatedBy(Long updatedBy) {
        this.updatedBy = updatedBy;
    }

    public LocalDateTime getUpdateTime() {
        return updateTime;
    }

    public void setUpdateTime(LocalDateTime updateTime) {
        this.updateTime = updateTime;
    }

    public Integer getDeleted() {
        return deleted;
    }

    public void setDeleted(Integer deleted) {
        this.deleted = deleted;
    }
}
