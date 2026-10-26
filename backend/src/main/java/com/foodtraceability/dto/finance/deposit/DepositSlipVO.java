package com.foodtraceability.dto.finance.deposit;

import java.time.LocalDateTime;

/** 缴款单视图（SL2/SL3）。金额一律「分」。 */
public class DepositSlipVO {

    private Long slipId;
    private String slipNo;
    /** TD-36：业务日期（= 班次 end_time 的日期 / 交班日） */
    private java.time.LocalDate businessDate;
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
    /** N19-2：本次存行中属于退款/冲回的金额（分） */
    private Long refundComponentFen;
    /** N19-2：净差异 = actual − (expected − refund_component) */
    private Long differenceNetFen;
    /** N19-3b：班次窗口未结束的缴款单数（与巡检 A39 still_open 同义） */
    private Integer stillOpenCount;
    /** N19-3b：本单窗口内「生成之后才发生」的退款笔数（与巡检 A39 stale 同义） */
    private Integer staleCount;
    /** N19-3b：difference_net_fen 为 NULL 的历史缴款单数 */
    private Integer legacyNullCount;
    /** N19-3a③：该门店现金户下 refund_time 为 NULL 的退款流水笔数（未归属任何班次，待回填） */
    private Integer missingRefundTimeCount;
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

    public java.time.LocalDate getBusinessDate() { return businessDate; }
    public void setBusinessDate(java.time.LocalDate businessDate) { this.businessDate = businessDate; }

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

    public Long getRefundComponentFen() { return refundComponentFen; }
    public void setRefundComponentFen(Long refundComponentFen) { this.refundComponentFen = refundComponentFen; }

    public Long getDifferenceNetFen() { return differenceNetFen; }
    public void setDifferenceNetFen(Long differenceNetFen) { this.differenceNetFen = differenceNetFen; }

    public Integer getStillOpenCount() { return stillOpenCount; }
    public void setStillOpenCount(Integer stillOpenCount) { this.stillOpenCount = stillOpenCount; }

    public Integer getStaleCount() { return staleCount; }
    public void setStaleCount(Integer staleCount) { this.staleCount = staleCount; }

    public Integer getLegacyNullCount() { return legacyNullCount; }
    public void setLegacyNullCount(Integer legacyNullCount) { this.legacyNullCount = legacyNullCount; }

    public Integer getMissingRefundTimeCount() { return missingRefundTimeCount; }
    public void setMissingRefundTimeCount(Integer missingRefundTimeCount) { this.missingRefundTimeCount = missingRefundTimeCount; }

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
