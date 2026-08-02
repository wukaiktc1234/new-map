package com.foodtraceability.dto.finance;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 发票报销视图对象VO
 *
 * <p>Sprint 3.1 P0 F-001：用于 Controller 返回报销单完整信息，
 * 包含报销单所有字段 + items（明细列表） + approvalRecords（审批记录列表），
 * 金额字段为 Long（分）。</p>
 */
public class InvoiceReimbursementVO {

    /** 报销单ID */
    private Long reimbursementId;

    /** 报销单号 */
    private String reimbursementNo;

    /** 申请人ID */
    private Long applicantId;

    /** 申请人姓名 */
    private String applicantName;

    /** 部门ID */
    private Long departmentId;

    /** 部门名称 */
    private String departmentName;

    /** 报销类型 */
    private String reimbursementType;

    /** 报销总金额（单位：分） */
    private Long totalAmount;

    /** 审批通过金额（单位：分） */
    private Long approvedAmount;

    /**
     * 单据状态
     * 0-草稿 1-已审批 2-已付款 3-已取消
     */
    private Integer status;

    /** 状态名称 */
    private String statusName;

    /** 申请日期 */
    private LocalDate applyDate;

    /** 审批日期 */
    private LocalDate approveDate;

    /** 审批人ID */
    private Long approverId;

    /** 审批人姓名 */
    private String approverName;

    /** 拒绝/取消原因 */
    private String rejectReason;

    /** 备注 */
    private String remark;

    /**
     * 付款状态
     * 0-未付款 1-已付款
     */
    private Integer paymentStatus;

    /** 付款状态名称 */
    private String paymentStatusName;

    /** 付款日期 */
    private LocalDate paymentDate;

    /** 付款凭证号 */
    private String paymentVoucherNo;

    /** 创建人ID */
    private Long createdBy;

    /** 创建时间 */
    private LocalDateTime createTime;

    /** 更新人ID */
    private Long updatedBy;

    /** 更新时间 */
    private LocalDateTime updateTime;

    /** 报销明细列表 */
    private List<InvoiceReimbursementItemVO> items;

    /** 审批记录列表 */
    private List<InvoiceReimbursementApprovalRecordVO> approvalRecords;

    public InvoiceReimbursementVO() {
    }

    public Long getReimbursementId() {
        return reimbursementId;
    }

    public void setReimbursementId(Long reimbursementId) {
        this.reimbursementId = reimbursementId;
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

    public String getStatusName() {
        return statusName;
    }

    public void setStatusName(String statusName) {
        this.statusName = statusName;
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

    public String getPaymentStatusName() {
        return paymentStatusName;
    }

    public void setPaymentStatusName(String paymentStatusName) {
        this.paymentStatusName = paymentStatusName;
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

    public List<InvoiceReimbursementItemVO> getItems() {
        return items;
    }

    public void setItems(List<InvoiceReimbursementItemVO> items) {
        this.items = items;
    }

    public List<InvoiceReimbursementApprovalRecordVO> getApprovalRecords() {
        return approvalRecords;
    }

    public void setApprovalRecords(List<InvoiceReimbursementApprovalRecordVO> approvalRecords) {
        this.approvalRecords = approvalRecords;
    }

    /**
     * 报销明细视图对象（内嵌）
     */
    public static class InvoiceReimbursementItemVO {

        /** 明细ID */
        private Long itemId;

        /** 报销单ID */
        private Long reimbursementId;

        /** 关联发票ID */
        private Long relatedInvoiceId;

        /** 凭证ID */
        private Long voucherId;

        /** 凭证编号 */
        private String voucherNo;

        /** 凭证类型 */
        private String voucherType;

        /** 明细金额（单位：分） */
        private Long amount;

        /** 审批通过金额（单位：分） */
        private Long approvedAmount;

        /** 费用类型 */
        private String expenseType;

        /** 费用说明 */
        private String expenseDescription;

        public Long getItemId() {
            return itemId;
        }

        public void setItemId(Long itemId) {
            this.itemId = itemId;
        }

        public Long getReimbursementId() {
            return reimbursementId;
        }

        public void setReimbursementId(Long reimbursementId) {
            this.reimbursementId = reimbursementId;
        }

        public Long getRelatedInvoiceId() {
            return relatedInvoiceId;
        }

        public void setRelatedInvoiceId(Long relatedInvoiceId) {
            this.relatedInvoiceId = relatedInvoiceId;
        }

        public Long getVoucherId() {
            return voucherId;
        }

        public void setVoucherId(Long voucherId) {
            this.voucherId = voucherId;
        }

        public String getVoucherNo() {
            return voucherNo;
        }

        public void setVoucherNo(String voucherNo) {
            this.voucherNo = voucherNo;
        }

        public String getVoucherType() {
            return voucherType;
        }

        public void setVoucherType(String voucherType) {
            this.voucherType = voucherType;
        }

        public Long getAmount() {
            return amount;
        }

        public void setAmount(Long amount) {
            this.amount = amount;
        }

        public Long getApprovedAmount() {
            return approvedAmount;
        }

        public void setApprovedAmount(Long approvedAmount) {
            this.approvedAmount = approvedAmount;
        }

        public String getExpenseType() {
            return expenseType;
        }

        public void setExpenseType(String expenseType) {
            this.expenseType = expenseType;
        }

        public String getExpenseDescription() {
            return expenseDescription;
        }

        public void setExpenseDescription(String expenseDescription) {
            this.expenseDescription = expenseDescription;
        }
    }

    /**
     * 报销审批记录视图对象（内嵌）
     */
    public static class InvoiceReimbursementApprovalRecordVO {

        /** 审批记录ID */
        private Long recordId;

        /** 报销单ID */
        private Long reimbursementId;

        /** 操作人ID */
        private Long operatorId;

        /** 操作人姓名 */
        private String operatorName;

        /** 操作动作（submit/approve/reject/cancel/pay） */
        private String action;

        /** 操作备注 */
        private String remark;

        /** 操作时间 */
        private LocalDateTime operateTime;

        public Long getRecordId() {
            return recordId;
        }

        public void setRecordId(Long recordId) {
            this.recordId = recordId;
        }

        public Long getReimbursementId() {
            return reimbursementId;
        }

        public void setReimbursementId(Long reimbursementId) {
            this.reimbursementId = reimbursementId;
        }

        public Long getOperatorId() {
            return operatorId;
        }

        public void setOperatorId(Long operatorId) {
            this.operatorId = operatorId;
        }

        public String getOperatorName() {
            return operatorName;
        }

        public void setOperatorName(String operatorName) {
            this.operatorName = operatorName;
        }

        public String getAction() {
            return action;
        }

        public void setAction(String action) {
            this.action = action;
        }

        public String getRemark() {
            return remark;
        }

        public void setRemark(String remark) {
            this.remark = remark;
        }

        public LocalDateTime getOperateTime() {
            return operateTime;
        }

        public void setOperateTime(LocalDateTime operateTime) {
            this.operateTime = operateTime;
        }
    }
}
