package com.foodtraceability.dto.finance;

import java.io.Serializable;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 收支记录VO
 */
public class FinanceRecordVO implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 记录ID */
    private Long recordId;

    /** 记录编号 */
    private String recordNo;

    /** 收支类型 */
    private Integer recordType;

    /** 收支类型名称 */
    private String recordTypeName;

    /** 收支类别 */
    private Integer recordCategory;

    /** 收支类别名称 */
    private String recordCategoryName;

    /** 金额（单位：分） */
    private Long amount;

    /** 金额（元，用于显示） */
    private String amountDisplay;

    /** 支付方式 */
    private Integer paymentMethod;

    /** 支付方式名称 */
    private String paymentMethodName;

    /** 对应的会计科目ID */
    private Long accountSubjectId;

    /** 会计科目名称 */
    private String accountSubjectName;

    /** 对方单位/个人名称 */
    private String counterpartyName;

    /** 对方类型 */
    private Integer counterpartyType;

    /** 对方类型名称 */
    private String counterpartyTypeName;

    /** 业务发生日期 */
    private LocalDate businessDate;

    /** 记录日期 */
    private LocalDate recordDate;

    /** 关联的凭证ID */
    private Long voucherId;

    /** 凭证号 */
    private String voucherNo;

    /** 审批状态 */
    private Integer approvalStatus;

    /** 审批状态名称 */
    private String approvalStatusName;

    /** 审批人ID */
    private Long approveUserId;

    /** 审批时间 */
    private LocalDateTime approveTime;

    /** 备注 */
    private String remark;

    /** 创建人ID */
    private Long createUserId;

    /** 创建时间 */
    private LocalDateTime createTime;

    // getter和setter方法（使用简洁格式）
    public Long getRecordId() { return recordId; }
    public void setRecordId(Long recordId) { this.recordId = recordId; }
    public String getRecordNo() { return recordNo; }
    public void setRecordNo(String recordNo) { this.recordNo = recordNo; }
    public Integer getRecordType() { return recordType; }
    public void setRecordType(Integer recordType) { this.recordType = recordType; }
    public String getRecordTypeName() { return recordTypeName; }
    public void setRecordTypeName(String recordTypeName) { this.recordTypeName = recordTypeName; }
    public Integer getRecordCategory() { return recordCategory; }
    public void setRecordCategory(Integer recordCategory) { this.recordCategory = recordCategory; }
    public String getRecordCategoryName() { return recordCategoryName; }
    public void setRecordCategoryName(String recordCategoryName) { this.recordCategoryName = recordCategoryName; }
    public Long getAmount() { return amount; }
    public void setAmount(Long amount) { this.amount = amount; }
    public String getAmountDisplay() { return amountDisplay; }
    public void setAmountDisplay(String amountDisplay) { this.amountDisplay = amountDisplay; }
    public Integer getPaymentMethod() { return paymentMethod; }
    public void setPaymentMethod(Integer paymentMethod) { this.paymentMethod = paymentMethod; }
    public String getPaymentMethodName() { return paymentMethodName; }
    public void setPaymentMethodName(String paymentMethodName) { this.paymentMethodName = paymentMethodName; }
    public Long getAccountSubjectId() { return accountSubjectId; }
    public void setAccountSubjectId(Long accountSubjectId) { this.accountSubjectId = accountSubjectId; }
    public String getAccountSubjectName() { return accountSubjectName; }
    public void setAccountSubjectName(String accountSubjectName) { this.accountSubjectName = accountSubjectName; }
    public String getCounterpartyName() { return counterpartyName; }
    public void setCounterpartyName(String counterpartyName) { this.counterpartyName = counterpartyName; }
    public Integer getCounterpartyType() { return counterpartyType; }
    public void setCounterpartyType(Integer counterpartyType) { this.counterpartyType = counterpartyType; }
    public String getCounterpartyTypeName() { return counterpartyTypeName; }
    public void setCounterpartyTypeName(String counterpartyTypeName) { this.counterpartyTypeName = counterpartyTypeName; }
    public LocalDate getBusinessDate() { return businessDate; }
    public void setBusinessDate(LocalDate businessDate) { this.businessDate = businessDate; }
    public LocalDate getRecordDate() { return recordDate; }
    public void setRecordDate(LocalDate recordDate) { this.recordDate = recordDate; }
    public Long getVoucherId() { return voucherId; }
    public void setVoucherId(Long voucherId) { this.voucherId = voucherId; }
    public String getVoucherNo() { return voucherNo; }
    public void setVoucherNo(String voucherNo) { this.voucherNo = voucherNo; }
    public Integer getApprovalStatus() { return approvalStatus; }
    public void setApprovalStatus(Integer approvalStatus) { this.approvalStatus = approvalStatus; }
    public String getApprovalStatusName() { return approvalStatusName; }
    public void setApprovalStatusName(String approvalStatusName) { this.approvalStatusName = approvalStatusName; }
    public Long getApproveUserId() { return approveUserId; }
    public void setApproveUserId(Long approveUserId) { this.approveUserId = approveUserId; }
    public LocalDateTime getApproveTime() { return approveTime; }
    public void setApproveTime(LocalDateTime approveTime) { this.approveTime = approveTime; }
    public String getRemark() { return remark; }
    public void setRemark(String remark) { this.remark = remark; }
    public Long getCreateUserId() { return createUserId; }
    public void setCreateUserId(Long createUserId) { this.createUserId = createUserId; }
    public LocalDateTime getCreateTime() { return createTime; }
    public void setCreateTime(LocalDateTime createTime) { this.createTime = createTime; }
}
