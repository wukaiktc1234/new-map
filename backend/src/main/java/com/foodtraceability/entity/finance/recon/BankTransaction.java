package com.foodtraceability.entity.finance.recon;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 对账 P0 · 外部流水事实表（设计 §5.3 权威定义，表 bank_transactions）。
 *
 * <p><b>金额字段单位一律为「分」(BIGINT)</b>：元→分换算只允许复用 Java 侧的唯一实现
 * （见后续 FinanceMoney 工具），禁止 double/float。</p>
 */
@TableName(value = "bank_transactions")
public class BankTransaction {

    @TableId(value = "transaction_id", type = IdType.AUTO)
    private Long transactionId;

    @TableField(value = "batch_id")
    private Long batchId;

    @TableField(value = "account_id")
    private Long accountId;

    @TableField(value = "source_type")
    private String sourceType;

    @TableField(value = "external_txn_id")
    private String externalTxnId;

    @TableField(value = "merchant_order_no")
    private String merchantOrderNo;

    @TableField(value = "direction")
    private Integer direction;

    @TableField(value = "is_refund")
    private Boolean isRefund;

    @TableField(value = "amount")
    private Long amount;

    @TableField(value = "fee")
    private Long fee;

    @TableField(value = "txn_time")
    private java.time.LocalDateTime txnTime;

    @TableField(value = "business_date")
    private java.time.LocalDate businessDate;

    @TableField(value = "balance_snapshot")
    private Long balanceSnapshot;

    @TableField(value = "counterparty_name")
    private String counterpartyName;

    @TableField(value = "counterparty_account")
    private String counterpartyAccount;

    @TableField(value = "summary")
    private String summary;

    @TableField(value = "status_raw")
    private String statusRaw;

    @TableField(value = "settle_status")
    private String settleStatus;

    @TableField(value = "dedupe_basis")
    private String dedupeBasis;

    @TableField(value = "raw_hash")
    private String rawHash;

    @TableField(value = "match_status")
    private String matchStatus;

    @TableField(value = "matched_flow_id")
    private Long matchedFlowId;

    @TableField(value = "needs_review")
    private Boolean needsReview;

    @TableField(value = "create_user_id")
    private Long createUserId;

    @TableField(value = "create_time")
    private java.time.LocalDateTime createTime;

    @TableField(value = "update_time")
    private java.time.LocalDateTime updateTime;

    @TableField(value = "deleted")
    private Integer deleted;

    @TableField(value = "version")
    private Integer version;

    public Long getTransactionId() { return transactionId; }
    public void setTransactionId(Long transactionId) { this.transactionId = transactionId; }

    public Long getBatchId() { return batchId; }
    public void setBatchId(Long batchId) { this.batchId = batchId; }

    public Long getAccountId() { return accountId; }
    public void setAccountId(Long accountId) { this.accountId = accountId; }

    public String getSourceType() { return sourceType; }
    public void setSourceType(String sourceType) { this.sourceType = sourceType; }

    public String getExternalTxnId() { return externalTxnId; }
    public void setExternalTxnId(String externalTxnId) { this.externalTxnId = externalTxnId; }

    public String getMerchantOrderNo() { return merchantOrderNo; }
    public void setMerchantOrderNo(String merchantOrderNo) { this.merchantOrderNo = merchantOrderNo; }

    public Integer getDirection() { return direction; }
    public void setDirection(Integer direction) { this.direction = direction; }

    public Boolean getIsRefund() { return isRefund; }
    public void setIsRefund(Boolean isRefund) { this.isRefund = isRefund; }

    public Long getAmount() { return amount; }
    public void setAmount(Long amount) { this.amount = amount; }

    public Long getFee() { return fee; }
    public void setFee(Long fee) { this.fee = fee; }

    public java.time.LocalDateTime getTxnTime() { return txnTime; }
    public void setTxnTime(java.time.LocalDateTime txnTime) { this.txnTime = txnTime; }

    public java.time.LocalDate getBusinessDate() { return businessDate; }
    public void setBusinessDate(java.time.LocalDate businessDate) { this.businessDate = businessDate; }

    public Long getBalanceSnapshot() { return balanceSnapshot; }
    public void setBalanceSnapshot(Long balanceSnapshot) { this.balanceSnapshot = balanceSnapshot; }

    public String getCounterpartyName() { return counterpartyName; }
    public void setCounterpartyName(String counterpartyName) { this.counterpartyName = counterpartyName; }

    public String getCounterpartyAccount() { return counterpartyAccount; }
    public void setCounterpartyAccount(String counterpartyAccount) { this.counterpartyAccount = counterpartyAccount; }

    public String getSummary() { return summary; }
    public void setSummary(String summary) { this.summary = summary; }

    public String getStatusRaw() { return statusRaw; }
    public void setStatusRaw(String statusRaw) { this.statusRaw = statusRaw; }

    public String getSettleStatus() { return settleStatus; }
    public void setSettleStatus(String settleStatus) { this.settleStatus = settleStatus; }

    public String getDedupeBasis() { return dedupeBasis; }
    public void setDedupeBasis(String dedupeBasis) { this.dedupeBasis = dedupeBasis; }

    public String getRawHash() { return rawHash; }
    public void setRawHash(String rawHash) { this.rawHash = rawHash; }

    public String getMatchStatus() { return matchStatus; }
    public void setMatchStatus(String matchStatus) { this.matchStatus = matchStatus; }

    public Long getMatchedFlowId() { return matchedFlowId; }
    public void setMatchedFlowId(Long matchedFlowId) { this.matchedFlowId = matchedFlowId; }

    public Boolean getNeedsReview() { return needsReview; }
    public void setNeedsReview(Boolean needsReview) { this.needsReview = needsReview; }

    public Long getCreateUserId() { return createUserId; }
    public void setCreateUserId(Long createUserId) { this.createUserId = createUserId; }

    public java.time.LocalDateTime getCreateTime() { return createTime; }
    public void setCreateTime(java.time.LocalDateTime createTime) { this.createTime = createTime; }

    public java.time.LocalDateTime getUpdateTime() { return updateTime; }
    public void setUpdateTime(java.time.LocalDateTime updateTime) { this.updateTime = updateTime; }

    public Integer getDeleted() { return deleted; }
    public void setDeleted(Integer deleted) { this.deleted = deleted; }

    public Integer getVersion() { return version; }
    public void setVersion(Integer version) { this.version = version; }
}