package com.foodtraceability.dto.finance.deposit;

import java.time.LocalDateTime;

/** 缴款单视图（SL2/SL3）。金额一律「分」。 */
public class DepositSlipVO {

    private Long slipId;
    private String slipNo;
    private Long storeId;
    private Long shiftId;
    private Long cashierEmployeeId;
    private String cashierName;
    private Long collectorUserId;
    private String collectorName;
    private Long expectedAmount;
    private Long actualAmount;
    /** 应交 − 实收：正=短款，负=长款（只记录在单据上，绝不改 pos_shifts.closing_cash） */
    private Long differenceAmount;
    private Long cashAccountId;
    private Long bankAccountId;
    private Long depositBankTxnId;
    private String depositRefNo;
    private String status;
    private LocalDateTime submittedAt;
    private LocalDateTime receivedAt;
    private LocalDateTime depositedAt;
    private LocalDateTime verifiedAt;
    private LocalDateTime closedAt;
    private Long reopenOf;
    private String remark;
    private Long createUserId;
    private String createdBy;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;

    public Long getSlipId() { return slipId; }
    public void setSlipId(Long slipId) { this.slipId = slipId; }

    public String getSlipNo() { return slipNo; }
    public void setSlipNo(String slipNo) { this.slipNo = slipNo; }

    public Long getStoreId() { return storeId; }
    public void setStoreId(Long storeId) { this.storeId = storeId; }

    public Long getShiftId() { return shiftId; }
    public void setShiftId(Long shiftId) { this.shiftId = shiftId; }

    public Long getCashierEmployeeId() { return cashierEmployeeId; }
    public void setCashierEmployeeId(Long cashierEmployeeId) { this.cashierEmployeeId = cashierEmployeeId; }

    public String getCashierName() { return cashierName; }
    public void setCashierName(String cashierName) { this.cashierName = cashierName; }

    public Long getCollectorUserId() { return collectorUserId; }
    public void setCollectorUserId(Long collectorUserId) { this.collectorUserId = collectorUserId; }

    public String getCollectorName() { return collectorName; }
    public void setCollectorName(String collectorName) { this.collectorName = collectorName; }

    public Long getExpectedAmount() { return expectedAmount; }
    public void setExpectedAmount(Long expectedAmount) { this.expectedAmount = expectedAmount; }

    public Long getActualAmount() { return actualAmount; }
    public void setActualAmount(Long actualAmount) { this.actualAmount = actualAmount; }

    public Long getDifferenceAmount() { return differenceAmount; }
    public void setDifferenceAmount(Long differenceAmount) { this.differenceAmount = differenceAmount; }

    public Long getCashAccountId() { return cashAccountId; }
    public void setCashAccountId(Long cashAccountId) { this.cashAccountId = cashAccountId; }

    public Long getBankAccountId() { return bankAccountId; }
    public void setBankAccountId(Long bankAccountId) { this.bankAccountId = bankAccountId; }

    public Long getDepositBankTxnId() { return depositBankTxnId; }
    public void setDepositBankTxnId(Long depositBankTxnId) { this.depositBankTxnId = depositBankTxnId; }

    public String getDepositRefNo() { return depositRefNo; }
    public void setDepositRefNo(String depositRefNo) { this.depositRefNo = depositRefNo; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public LocalDateTime getSubmittedAt() { return submittedAt; }
    public void setSubmittedAt(LocalDateTime submittedAt) { this.submittedAt = submittedAt; }

    public LocalDateTime getReceivedAt() { return receivedAt; }
    public void setReceivedAt(LocalDateTime receivedAt) { this.receivedAt = receivedAt; }

    public LocalDateTime getDepositedAt() { return depositedAt; }
    public void setDepositedAt(LocalDateTime depositedAt) { this.depositedAt = depositedAt; }

    public LocalDateTime getVerifiedAt() { return verifiedAt; }
    public void setVerifiedAt(LocalDateTime verifiedAt) { this.verifiedAt = verifiedAt; }

    public LocalDateTime getClosedAt() { return closedAt; }
    public void setClosedAt(LocalDateTime closedAt) { this.closedAt = closedAt; }

    public Long getReopenOf() { return reopenOf; }
    public void setReopenOf(Long reopenOf) { this.reopenOf = reopenOf; }

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
}
