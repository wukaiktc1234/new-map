package com.foodtraceability.entity.finance.recon;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 对账 P1 · 银行存款余额调节表主表（设计 §5.10.1，表 reconciliation_statements）。
 *
 * <p><b>金额字段单位一律为「分」(BIGINT)</b>：book/bank/adjusted 余额与四类未达合计均为分。</p>
 *
 * <p>硬门禁（§4.3）：{@code isBalanced=false} 只允许 DRAFT，禁止 SUBMIT/APPROVED；
 * APPROVED 要求 approvedBy ≠ submittedBy 且审核人不得持 ROLE_TELLER。</p>
 */
@TableName("reconciliation_statements")
public class ReconciliationStatement {

    @TableId(value = "statement_id", type = IdType.AUTO)
    private Long statementId;

    /** 编号 BS<yyyyMM><NNNN> */
    @TableField("statement_no")
    private String statementNo;

    @TableField("account_id")
    private Long accountId;

    @TableField("store_id")
    private Long storeId;

    @TableField("period_start")
    private LocalDate periodStart;

    @TableField("period_end")
    private LocalDate periodEnd;

    /** 企业账面余额（分） */
    @TableField("book_balance")
    private Long bookBalance;

    /** 银行对账单余额（分） */
    @TableField("bank_balance")
    private Long bankBalance;

    /** 未达 A：企业已收、银行未收（分） */
    @TableField("uncleared_in_credit")
    private Long unclearedInCredit;

    /** 未达 B：企业已付、银行未付（分） */
    @TableField("uncleared_in_debit")
    private Long unclearedInDebit;

    /** 未达 C：银行已收、企业未收（分） */
    @TableField("uncleared_out_credit")
    private Long unclearedOutCredit;

    /** 未达 D：银行已付、企业未付（分） */
    @TableField("uncleared_out_debit")
    private Long unclearedOutDebit;

    /** 调节后企业余额（分）= book + C − D */
    @TableField("adjusted_book_balance")
    private Long adjustedBookBalance;

    /** 调节后银行余额（分）= bank + A − B */
    @TableField("adjusted_bank_balance")
    private Long adjustedBankBalance;

    @TableField("is_balanced")
    private Boolean isBalanced;

    /** DRAFT / SUBMITTED / APPROVED */
    @TableField("status")
    private String status;

    @TableField("submitted_by")
    private Long submittedBy;

    @TableField("submitted_at")
    private LocalDateTime submittedAt;

    @TableField("approved_by")
    private Long approvedBy;

    @TableField("approved_at")
    private LocalDateTime approvedAt;

    @TableField("remark")
    private String remark;

    @TableField("create_user_id")
    private Long createUserId;

    @TableField("created_by")
    private String createdBy;

    @TableField("create_time")
    private LocalDateTime createTime;

    @TableField("update_time")
    private LocalDateTime updateTime;

    @TableField("version")
    private Integer version;

    @TableField("deleted")
    private Integer deleted;

    public Long getStatementId() { return statementId; }
    public void setStatementId(Long statementId) { this.statementId = statementId; }

    public String getStatementNo() { return statementNo; }
    public void setStatementNo(String statementNo) { this.statementNo = statementNo; }

    public Long getAccountId() { return accountId; }
    public void setAccountId(Long accountId) { this.accountId = accountId; }

    public Long getStoreId() { return storeId; }
    public void setStoreId(Long storeId) { this.storeId = storeId; }

    public LocalDate getPeriodStart() { return periodStart; }
    public void setPeriodStart(LocalDate periodStart) { this.periodStart = periodStart; }

    public LocalDate getPeriodEnd() { return periodEnd; }
    public void setPeriodEnd(LocalDate periodEnd) { this.periodEnd = periodEnd; }

    public Long getBookBalance() { return bookBalance; }
    public void setBookBalance(Long bookBalance) { this.bookBalance = bookBalance; }

    public Long getBankBalance() { return bankBalance; }
    public void setBankBalance(Long bankBalance) { this.bankBalance = bankBalance; }

    public Long getUnclearedInCredit() { return unclearedInCredit; }
    public void setUnclearedInCredit(Long unclearedInCredit) { this.unclearedInCredit = unclearedInCredit; }

    public Long getUnclearedInDebit() { return unclearedInDebit; }
    public void setUnclearedInDebit(Long unclearedInDebit) { this.unclearedInDebit = unclearedInDebit; }

    public Long getUnclearedOutCredit() { return unclearedOutCredit; }
    public void setUnclearedOutCredit(Long unclearedOutCredit) { this.unclearedOutCredit = unclearedOutCredit; }

    public Long getUnclearedOutDebit() { return unclearedOutDebit; }
    public void setUnclearedOutDebit(Long unclearedOutDebit) { this.unclearedOutDebit = unclearedOutDebit; }

    public Long getAdjustedBookBalance() { return adjustedBookBalance; }
    public void setAdjustedBookBalance(Long adjustedBookBalance) { this.adjustedBookBalance = adjustedBookBalance; }

    public Long getAdjustedBankBalance() { return adjustedBankBalance; }
    public void setAdjustedBankBalance(Long adjustedBankBalance) { this.adjustedBankBalance = adjustedBankBalance; }

    public Boolean getIsBalanced() { return isBalanced; }
    public void setIsBalanced(Boolean isBalanced) { this.isBalanced = isBalanced; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public Long getSubmittedBy() { return submittedBy; }
    public void setSubmittedBy(Long submittedBy) { this.submittedBy = submittedBy; }

    public LocalDateTime getSubmittedAt() { return submittedAt; }
    public void setSubmittedAt(LocalDateTime submittedAt) { this.submittedAt = submittedAt; }

    public Long getApprovedBy() { return approvedBy; }
    public void setApprovedBy(Long approvedBy) { this.approvedBy = approvedBy; }

    public LocalDateTime getApprovedAt() { return approvedAt; }
    public void setApprovedAt(LocalDateTime approvedAt) { this.approvedAt = approvedAt; }

    public String getRemark() { return remark; }
    public void setRemark(String remark) { this.remark = remark; }

    public Long getCreateUserId() { return createUserId; }
    public void setCreateUserId(Long createUserId) { this.createUserId = createUserId; }

    public String getCreatedBy() { return createdBy; }
    public void setCreatedBy(String createdBy) { this.createdBy = createdBy; }

    public LocalDateTime getCreateTime() { return createTime; }
    public void setCreateTime(LocalDateTime createTime) { this.createTime = createTime; }

    public LocalDateTime getUpdateTime() { return updateTime; }
    public void setUpdateTime(LocalDateTime updateTime) { this.updateTime = updateTime; }

    public Integer getVersion() { return version; }
    public void setVersion(Integer version) { this.version = version; }

    public Integer getDeleted() { return deleted; }
    public void setDeleted(Integer deleted) { this.deleted = deleted; }
}
