package com.foodtraceability.entity.finance.recon;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 对账 P0 · 对账批次（设计 §5.4，表 recon_runs）。
 *
 * <p><b>金额字段单位一律为「分」(BIGINT)</b>。</p>
 */
@TableName("recon_runs")
public class ReconRun {

    @TableId(value = "run_id", type = IdType.AUTO)
    private Long runId;

    @TableField("run_key")
    private String runKey;

    @TableField("account_id")
    private Long accountId;

    @TableField("store_id")
    private Long storeId;

    @TableField("business_date")
    private LocalDate businessDate;

    @TableField("status")
    private String status;

    @TableField("internal_count")
    private Integer internalCount;

    @TableField("internal_amount")
    private Long internalAmount;

    @TableField("external_count")
    private Integer externalCount;

    @TableField("external_amount")
    private Long externalAmount;

    @TableField("matched_count")
    private Integer matchedCount;

    @TableField("matched_amount")
    private Long matchedAmount;

    @TableField("difference_count")
    private Integer differenceCount;

    @TableField("rebuild_of")
    private Long rebuildOf;

    @TableField("started_at")
    private LocalDateTime startedAt;

    @TableField("finished_at")
    private LocalDateTime finishedAt;

    @TableField("operator_id")
    private Long operatorId;

    @TableField("create_time")
    private LocalDateTime createTime;

    @TableField("update_time")
    private LocalDateTime updateTime;

    public Long getRunId() { return runId; }
    public void setRunId(Long runId) { this.runId = runId; }

    public String getRunKey() { return runKey; }
    public void setRunKey(String runKey) { this.runKey = runKey; }

    public Long getAccountId() { return accountId; }
    public void setAccountId(Long accountId) { this.accountId = accountId; }

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
