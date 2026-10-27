package com.foodtraceability.entity.finance.recon;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

import java.time.LocalDate;
import java.time.OffsetDateTime;

/** NEW-34：存行日汇总人工确认记录（覆盖式追加，保留历史行）。 */
@TableName("deposit_daily_confirmations")
public class DepositDailyConfirmation {

    @TableId(type = IdType.AUTO)
    private Long id;
    private LocalDate businessDate;
    private Long accountId;
    private Long confirmedBy;
    private String confirmedByName;
    private OffsetDateTime confirmedAt;
    private Long totalCashCountedFen;
    private Long bankInflowFen;
    private Integer unmatchedCount;
    private String snapshotHash;
    private String snapshotJson;
    private OffsetDateTime createTime;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public LocalDate getBusinessDate() { return businessDate; }
    public void setBusinessDate(LocalDate businessDate) { this.businessDate = businessDate; }
    public Long getAccountId() { return accountId; }
    public void setAccountId(Long accountId) { this.accountId = accountId; }
    public Long getConfirmedBy() { return confirmedBy; }
    public void setConfirmedBy(Long confirmedBy) { this.confirmedBy = confirmedBy; }
    public String getConfirmedByName() { return confirmedByName; }
    public void setConfirmedByName(String confirmedByName) { this.confirmedByName = confirmedByName; }
    public OffsetDateTime getConfirmedAt() { return confirmedAt; }
    public void setConfirmedAt(OffsetDateTime confirmedAt) { this.confirmedAt = confirmedAt; }
    public Long getTotalCashCountedFen() { return totalCashCountedFen; }
    public void setTotalCashCountedFen(Long v) { this.totalCashCountedFen = v; }
    public Long getBankInflowFen() { return bankInflowFen; }
    public void setBankInflowFen(Long bankInflowFen) { this.bankInflowFen = bankInflowFen; }
    public Integer getUnmatchedCount() { return unmatchedCount; }
    public void setUnmatchedCount(Integer unmatchedCount) { this.unmatchedCount = unmatchedCount; }
    public String getSnapshotHash() { return snapshotHash; }
    public void setSnapshotHash(String snapshotHash) { this.snapshotHash = snapshotHash; }
    public String getSnapshotJson() { return snapshotJson; }
    public void setSnapshotJson(String snapshotJson) { this.snapshotJson = snapshotJson; }
    public OffsetDateTime getCreateTime() { return createTime; }
    public void setCreateTime(OffsetDateTime createTime) { this.createTime = createTime; }
}
