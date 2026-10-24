package com.foodtraceability.entity.finance.recon;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 对账 P1 · 未达账项明细（设计 §5.10.2，表 reconciliation_items）。
 *
 * <p>四类：A 企业已收银行未收 / B 企业已付银行未付（内部侧，flowId 必填）；
 * C 银行已收企业未收 / D 银行已付企业未付（外部侧，transactionId 必填）。</p>
 *
 * <p><b>amount 单位「分」</b>，恒正数，方向由 category 决定。</p>
 */
@TableName("reconciliation_items")
public class ReconciliationItem {

    @TableId(value = "item_id", type = IdType.AUTO)
    private Long itemId;

    @TableField("statement_id")
    private Long statementId;

    /** A/B/C/D */
    @TableField("category")
    private String category;

    /** 内部侧引用 → fund_flows.flow_id */
    @TableField("flow_id")
    private Long flowId;

    /** 外部侧引用 → bank_transactions.transaction_id */
    @TableField("transaction_id")
    private Long transactionId;

    /** 未达金额（分，正数） */
    @TableField("amount")
    private Long amount;

    @TableField("business_date")
    private LocalDate businessDate;

    @TableField("period_start")
    private LocalDate periodStart;

    @TableField("period_end")
    private LocalDate periodEnd;

    @TableField("owner_user_id")
    private Long ownerUserId;

    @TableField("owner_name")
    private String ownerName;

    @TableField("reason")
    private String reason;

    @TableField("cleared_flag")
    private Boolean clearedFlag;

    @TableField("cleared_statement_id")
    private Long clearedStatementId;

    @TableField("create_time")
    private LocalDateTime createTime;

    @TableField("update_time")
    private LocalDateTime updateTime;

    @TableField("deleted")
    private Integer deleted;

    public Long getItemId() { return itemId; }
    public void setItemId(Long itemId) { this.itemId = itemId; }

    public Long getStatementId() { return statementId; }
    public void setStatementId(Long statementId) { this.statementId = statementId; }

    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }

    public Long getFlowId() { return flowId; }
    public void setFlowId(Long flowId) { this.flowId = flowId; }

    public Long getTransactionId() { return transactionId; }
    public void setTransactionId(Long transactionId) { this.transactionId = transactionId; }

    public Long getAmount() { return amount; }
    public void setAmount(Long amount) { this.amount = amount; }

    public LocalDate getBusinessDate() { return businessDate; }
    public void setBusinessDate(LocalDate businessDate) { this.businessDate = businessDate; }

    public LocalDate getPeriodStart() { return periodStart; }
    public void setPeriodStart(LocalDate periodStart) { this.periodStart = periodStart; }

    public LocalDate getPeriodEnd() { return periodEnd; }
    public void setPeriodEnd(LocalDate periodEnd) { this.periodEnd = periodEnd; }

    public Long getOwnerUserId() { return ownerUserId; }
    public void setOwnerUserId(Long ownerUserId) { this.ownerUserId = ownerUserId; }

    public String getOwnerName() { return ownerName; }
    public void setOwnerName(String ownerName) { this.ownerName = ownerName; }

    public String getReason() { return reason; }
    public void setReason(String reason) { this.reason = reason; }

    public Boolean getClearedFlag() { return clearedFlag; }
    public void setClearedFlag(Boolean clearedFlag) { this.clearedFlag = clearedFlag; }

    public Long getClearedStatementId() { return clearedStatementId; }
    public void setClearedStatementId(Long clearedStatementId) { this.clearedStatementId = clearedStatementId; }

    public LocalDateTime getCreateTime() { return createTime; }
    public void setCreateTime(LocalDateTime createTime) { this.createTime = createTime; }

    public LocalDateTime getUpdateTime() { return updateTime; }
    public void setUpdateTime(LocalDateTime updateTime) { this.updateTime = updateTime; }

    public Integer getDeleted() { return deleted; }
    public void setDeleted(Integer deleted) { this.deleted = deleted; }
}
