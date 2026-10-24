package com.foodtraceability.dto.finance.recon;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/** 勾稽主体视图（§5.5）。金额单位「分」。 */
public class ReconItemVO {

    private Long itemId;
    private Long runId;
    private String side;
    private String internalRef;
    private String externalRef;
    private String paymentMethods;
    private Long amount;
    private LocalDateTime txnTime;
    private String counterpartyName;
    private String subjectStatus;
    private LocalDateTime matchedSince;
    private String matchLevel;
    private String matchGroup;
    private String suggestedExternalRef;
    private BigDecimal confidence;
    private LocalDateTime createTime;
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
