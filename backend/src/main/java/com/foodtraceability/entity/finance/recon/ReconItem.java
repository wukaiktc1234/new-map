package com.foodtraceability.entity.finance.recon;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 对账 P0 · 勾稽主体（设计 §5.5，表 recon_items）。matched_since = 进入 MATCHED 的时刻（NULL 表示非 MATCHED）。
 *
 * <p><b>金额字段单位一律为「分」(BIGINT)</b>。</p>
 */
@TableName("recon_items")
public class ReconItem {

    @TableId(value = "item_id", type = IdType.AUTO)
    private Long itemId;

    @TableField("run_id")
    private Long runId;

    @TableField("side")
    private String side;

    @TableField("internal_ref")
    private String internalRef;

    @TableField("external_ref")
    private String externalRef;

    @TableField("payment_methods")
    private String paymentMethods;

    @TableField("amount")
    private Long amount;

    @TableField("txn_time")
    private LocalDateTime txnTime;

    @TableField("counterparty_name")
    private String counterpartyName;

    @TableField("subject_status")
    private String subjectStatus;

    @TableField("matched_since")
    private LocalDateTime matchedSince;

    @TableField("match_level")
    private String matchLevel;

    @TableField("match_group")
    private String matchGroup;

    @TableField("suggested_external_ref")
    private String suggestedExternalRef;

    @TableField("confidence")
    private BigDecimal confidence;

    @TableField("create_time")
    private LocalDateTime createTime;

    @TableField("update_time")
    private LocalDateTime updateTime;

    public Long getItemId() { return itemId; }
    public void setItemId(Long itemId) { this.itemId = itemId; }

    public Long getRunId() { return runId; }
    public void setRunId(Long runId) { this.runId = runId; }

    public String getSide() { return side; }
    public void setSide(String side) { this.side = side; }

    public String getInternalRef() { return internalRef; }
    public void setInternalRef(String internalRef) { this.internalRef = internalRef; }

    public String getExternalRef() { return externalRef; }
    public void setExternalRef(String externalRef) { this.externalRef = externalRef; }

    public String getPaymentMethods() { return paymentMethods; }
    public void setPaymentMethods(String paymentMethods) { this.paymentMethods = paymentMethods; }

    public Long getAmount() { return amount; }
    public void setAmount(Long amount) { this.amount = amount; }

    public LocalDateTime getTxnTime() { return txnTime; }
    public void setTxnTime(LocalDateTime txnTime) { this.txnTime = txnTime; }

    public String getCounterpartyName() { return counterpartyName; }
    public void setCounterpartyName(String counterpartyName) { this.counterpartyName = counterpartyName; }

    public String getSubjectStatus() { return subjectStatus; }
    public void setSubjectStatus(String subjectStatus) { this.subjectStatus = subjectStatus; }

    public LocalDateTime getMatchedSince() { return matchedSince; }
    public void setMatchedSince(LocalDateTime matchedSince) { this.matchedSince = matchedSince; }

    public String getMatchLevel() { return matchLevel; }
    public void setMatchLevel(String matchLevel) { this.matchLevel = matchLevel; }

    public String getMatchGroup() { return matchGroup; }
    public void setMatchGroup(String matchGroup) { this.matchGroup = matchGroup; }

    public String getSuggestedExternalRef() { return suggestedExternalRef; }
    public void setSuggestedExternalRef(String suggestedExternalRef) { this.suggestedExternalRef = suggestedExternalRef; }

    public BigDecimal getConfidence() { return confidence; }
    public void setConfidence(BigDecimal confidence) { this.confidence = confidence; }

    public LocalDateTime getCreateTime() { return createTime; }
    public void setCreateTime(LocalDateTime createTime) { this.createTime = createTime; }

    public LocalDateTime getUpdateTime() { return updateTime; }
    public void setUpdateTime(LocalDateTime updateTime) { this.updateTime = updateTime; }
}
