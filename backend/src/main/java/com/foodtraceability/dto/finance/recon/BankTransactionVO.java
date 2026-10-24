package com.foodtraceability.dto.finance.recon;

import java.time.LocalDate;
import java.time.LocalDateTime;

/** 外部流水只读视图（§1.5 I6） */
public class BankTransactionVO {
    private Long transactionId;
    private Long batchId;
    private Long accountId;
    private String sourceType;
    private String externalTxnId;
    private String merchantOrderNo;
    private Integer direction;
    private Boolean isRefund;
    private Long amount;
    private Long fee;
    private LocalDateTime txnTime;
    private LocalDate businessDate;
    private String counterpartyName;
    private String counterpartyAccount;
    private String summary;
    private String dedupeBasis;
    private String matchStatus;
    private Boolean needsReview;
    private Long createUserId;
    private LocalDateTime createTime;

    public Long getTransactionId() { return transactionId; }
    public void setTransactionId(Long v) { this.transactionId = v; }
    public Long getBatchId() { return batchId; }
    public void setBatchId(Long v) { this.batchId = v; }
    public Long getAccountId() { return accountId; }
    public void setAccountId(Long v) { this.accountId = v; }
    public String getSourceType() { return sourceType; }
    public void setSourceType(String v) { this.sourceType = v; }
    public String getExternalTxnId() { return externalTxnId; }
    public void setExternalTxnId(String v) { this.externalTxnId = v; }
    public String getMerchantOrderNo() { return merchantOrderNo; }
    public void setMerchantOrderNo(String v) { this.merchantOrderNo = v; }
    public Integer getDirection() { return direction; }
    public void setDirection(Integer v) { this.direction = v; }
    public Boolean getIsRefund() { return isRefund; }
    public void setIsRefund(Boolean v) { this.isRefund = v; }
    public Long getAmount() { return amount; }
    public void setAmount(Long v) { this.amount = v; }
    public Long getFee() { return fee; }
    public void setFee(Long v) { this.fee = v; }
    public LocalDateTime getTxnTime() { return txnTime; }
    public void setTxnTime(LocalDateTime v) { this.txnTime = v; }
    public LocalDate getBusinessDate() { return businessDate; }
    public void setBusinessDate(LocalDate v) { this.businessDate = v; }
    public String getCounterpartyName() { return counterpartyName; }
    public void setCounterpartyName(String v) { this.counterpartyName = v; }
    public String getCounterpartyAccount() { return counterpartyAccount; }
    public void setCounterpartyAccount(String v) { this.counterpartyAccount = v; }
    public String getSummary() { return summary; }
    public void setSummary(String v) { this.summary = v; }
    public String getDedupeBasis() { return dedupeBasis; }
    public void setDedupeBasis(String v) { this.dedupeBasis = v; }
    public String getMatchStatus() { return matchStatus; }
    public void setMatchStatus(String v) { this.matchStatus = v; }
    public Boolean getNeedsReview() { return needsReview; }
    public void setNeedsReview(Boolean v) { this.needsReview = v; }
    public Long getCreateUserId() { return createUserId; }
    public void setCreateUserId(Long v) { this.createUserId = v; }
    public LocalDateTime getCreateTime() { return createTime; }
    public void setCreateTime(LocalDateTime v) { this.createTime = v; }
}
