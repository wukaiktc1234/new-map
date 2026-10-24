package com.foodtraceability.dto.finance.recon;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 跑批视图（R1 结果 / R2 列表 / R3 详情）。
 *
 * <p>金额字段单位一律为「分」。matchRate = matched_amount / internal_amount * 100（保留 2 位），
 * internal_amount 为 0 时为 null（不返回默认值 0，避免误读为「勾稽率 0%」）。</p>
 */
public class ReconRunVO {

    private Long runId;
    private String runKey;
    private Long accountId;
    private String accountName;
    private Long storeId;
    private LocalDate businessDate;
    private String status;
    private Integer internalCount;
    private Long internalAmount;
    private Integer externalCount;
    private Long externalAmount;
    private Integer matchedCount;
    private Long matchedAmount;
    private BigDecimal matchRate;
    private Integer differenceCount;
    private Long rebuildOf;
    private LocalDateTime startedAt;
    private LocalDateTime finishedAt;
    private Long operatorId;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;

    public Long getRunId() { return runId; }
    public void setRunId(Long runId) { this.runId = runId; }

    public String getRunKey() { return runKey; }
    public void setRunKey(String runKey) { this.runKey = runKey; }

    public Long getAccountId() { return accountId; }
    public void setAccountId(Long accountId) { this.accountId = accountId; }

    public String getAccountName() { return accountName; }
    public void setAccountName(String accountName) { this.accountName = accountName; }

    public Long getStoreId() { return storeId; }
    public void setStoreId(Long storeId) { this.storeId = storeId; }

    public LocalDate getBusinessDate() { return businessDate; }
    public void setBusinessDate(LocalDate businessDate) { this.businessDate = businessDate; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public Integer getInternalCount() { return internalCount; }
    public void setInternalCount(Integer internalCount) { this.internalCount = internalCount; }

    public Long getInternalAmount() { return internalAmount; }
    public void setInternalAmount(Long internalAmount) { this.internalAmount = internalAmount; }

    public Integer getExternalCount() { return externalCount; }
    public void setExternalCount(Integer externalCount) { this.externalCount = externalCount; }

    public Long getExternalAmount() { return externalAmount; }
    public void setExternalAmount(Long externalAmount) { this.externalAmount = externalAmount; }

    public Integer getMatchedCount() { return matchedCount; }
    public void setMatchedCount(Integer matchedCount) { this.matchedCount = matchedCount; }

    public Long getMatchedAmount() { return matchedAmount; }
    public void setMatchedAmount(Long matchedAmount) { this.matchedAmount = matchedAmount; }

    public BigDecimal getMatchRate() { return matchRate; }
    public void setMatchRate(BigDecimal matchRate) { this.matchRate = matchRate; }

    public Integer getDifferenceCount() { return differenceCount; }
    public void setDifferenceCount(Integer differenceCount) { this.differenceCount = differenceCount; }

    public Long getRebuildOf() { return rebuildOf; }
    public void setRebuildOf(Long rebuildOf) { this.rebuildOf = rebuildOf; }

    public LocalDateTime getStartedAt() { return startedAt; }
    public void setStartedAt(LocalDateTime startedAt) { this.startedAt = startedAt; }

    public LocalDateTime getFinishedAt() { return finishedAt; }
    public void setFinishedAt(LocalDateTime finishedAt) { this.finishedAt = finishedAt; }

    public Long getOperatorId() { return operatorId; }
    public void setOperatorId(Long operatorId) { this.operatorId = operatorId; }

    public LocalDateTime getCreateTime() { return createTime; }
    public void setCreateTime(LocalDateTime createTime) { this.createTime = createTime; }

    public LocalDateTime getUpdateTime() { return updateTime; }
    public void setUpdateTime(LocalDateTime updateTime) { this.updateTime = updateTime; }
}
