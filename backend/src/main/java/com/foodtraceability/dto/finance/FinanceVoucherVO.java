package com.foodtraceability.dto.finance;

import java.io.Serializable;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 记账凭证VO
 * 包含凭证头信息和分录明细列表
 */
public class FinanceVoucherVO implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 凭证ID */
    private Long voucherId;

    /** 凭证号 */
    private String voucherNo;

    /** 凭证日期 */
    private LocalDate voucherDate;

    /** 凭证类型 */
    private Integer voucherType;

    /** 凭证类型名称 */
    private String voucherTypeName;

    /** 凭证状态 */
    private Integer voucherStatus;

    /** 凭证状态名称 */
    private String voucherStatusName;

    /** 借方合计（单位：分） */
    private Long totalDebit;

    /** 贷方合计（单位：分） */
    private Long totalCredit;

    /** 借方合计（元，用于显示） */
    private String totalDebitDisplay;

    /** 贷方合计（元，用于显示） */
    private String totalCreditDisplay;

    /** 附件数量 */
    private Integer attachmentCount;

    /** 关联单据号 */
    private String referenceNo;

    /** 来源类型 */
    private Integer sourceType;

    /** 来源单据ID */
    private Long sourceId;

    /** 备注 */
    private String remark;

    /** 创建人ID */
    private Long createUserId;

    /** 创建人名称 */
    private String createUserName;

    /** 审核人ID */
    private Long approveUserId;

    /** 审核人名称 */
    private String approveUserName;

    /** 审核时间 */
    private LocalDateTime approveTime;

    /** 创建时间 */
    private LocalDateTime createTime;

    /** 更新时间 */
    private LocalDateTime updateTime;

    /** 分录明细列表 */
    private List<FinanceVoucherDetailVO> details;

    // getter和setter方法
    public Long getVoucherId() { return voucherId; }
    public void setVoucherId(Long voucherId) { this.voucherId = voucherId; }
    public String getVoucherNo() { return voucherNo; }
    public void setVoucherNo(String voucherNo) { this.voucherNo = voucherNo; }
    public LocalDate getVoucherDate() { return voucherDate; }
    public void setVoucherDate(LocalDate voucherDate) { this.voucherDate = voucherDate; }
    public Integer getVoucherType() { return voucherType; }
    public void setVoucherType(Integer voucherType) { this.voucherType = voucherType; }
    public String getVoucherTypeName() { return voucherTypeName; }
    public void setVoucherTypeName(String voucherTypeName) { this.voucherTypeName = voucherTypeName; }
    public Integer getVoucherStatus() { return voucherStatus; }
    public void setVoucherStatus(Integer voucherStatus) { this.voucherStatus = voucherStatus; }
    public String getVoucherStatusName() { return voucherStatusName; }
    public void setVoucherStatusName(String voucherStatusName) { this.voucherStatusName = voucherStatusName; }
    public Long getTotalDebit() { return totalDebit; }
    public void setTotalDebit(Long totalDebit) { this.totalDebit = totalDebit; }
    public Long getTotalCredit() { return totalCredit; }
    public void setTotalCredit(Long totalCredit) { this.totalCredit = totalCredit; }
    public String getTotalDebitDisplay() { return totalDebitDisplay; }
    public void setTotalDebitDisplay(String totalDebitDisplay) { this.totalDebitDisplay = totalDebitDisplay; }
    public String getTotalCreditDisplay() { return totalCreditDisplay; }
    public void setTotalCreditDisplay(String totalCreditDisplay) { this.totalCreditDisplay = totalCreditDisplay; }
    public Integer getAttachmentCount() { return attachmentCount; }
    public void setAttachmentCount(Integer attachmentCount) { this.attachmentCount = attachmentCount; }
    public String getReferenceNo() { return referenceNo; }
    public void setReferenceNo(String referenceNo) { this.referenceNo = referenceNo; }
    public Integer getSourceType() { return sourceType; }
    public void setSourceType(Integer sourceType) { this.sourceType = sourceType; }
    public Long getSourceId() { return sourceId; }
    public void setSourceId(Long sourceId) { this.sourceId = sourceId; }
    public String getRemark() { return remark; }
    public void setRemark(String remark) { this.remark = remark; }
    public Long getCreateUserId() { return createUserId; }
    public void setCreateUserId(Long createUserId) { this.createUserId = createUserId; }
    public String getCreateUserName() { return createUserName; }
    public void setCreateUserName(String createUserName) { this.createUserName = createUserName; }
    public Long getApproveUserId() { return approveUserId; }
    public void setApproveUserId(Long approveUserId) { this.approveUserId = approveUserId; }
    public String getApproveUserName() { return approveUserName; }
    public void setApproveUserName(String approveUserName) { this.approveUserName = approveUserName; }
    public LocalDateTime getApproveTime() { return approveTime; }
    public void setApproveTime(LocalDateTime approveTime) { this.approveTime = approveTime; }
    public LocalDateTime getCreateTime() { return createTime; }
    public void setCreateTime(LocalDateTime createTime) { this.createTime = createTime; }
    public LocalDateTime getUpdateTime() { return updateTime; }
    public void setUpdateTime(LocalDateTime updateTime) { this.updateTime = updateTime; }
    public List<FinanceVoucherDetailVO> getDetails() { return details; }
    public void setDetails(List<FinanceVoucherDetailVO> details) { this.details = details; }
}
