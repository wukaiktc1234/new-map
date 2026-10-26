package com.foodtraceability.entity.finance.recon;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 对账 P0 · 现金交款单（设计 §5.7，表 deposit_slips）。
 *
 * <p><b>金额字段单位一律为「分」(BIGINT)</b>。</p>
 */
@TableName("deposit_slips")
public class DepositSlip {

    @TableId(value = "slip_id", type = IdType.AUTO)
    private Long slipId;

    @TableField("slip_no")
    private String slipNo;

    @TableField("store_id")
    private Long storeId;

    @TableField("shift_id")
    private Long shiftId;

    /**
     * 业务日期（交班日）：TD-36 —— 取所属班次 pos_shifts.end_time 的日期。
     *
     * <p>生成时由 {@code DepositSlipServiceImpl.generate} 接线写入（ts-purchase 负责）；
     * 历史行为 NULL，由 scripts/db-fix/2026-10-25-backfill-deposit-slip-business-date.sql 回填。</p>
     */
    @TableField("business_date")
    private LocalDate businessDate;

    @TableField("cashier_employee_id")
    private Long cashierEmployeeId;

    @TableField("cashier_name")
    private String cashierName;

    @TableField("collector_user_id")
    private Long collectorUserId;

    @TableField("collector_name")
    private String collectorName;

    @TableField("expected_amount")
    private Long expectedAmount;

    /** N19-2：本次存行中属于退款/冲回的金额（分，DB 列 refund_component_fen，默认 0） */
    private Long refundComponentFen;

    /** N19-2：扣除退款构成后的净差异（分，DB 列 difference_net_fen，未确认时为 null） */
    private Long differenceNetFen;

    @TableField("actual_amount")
    private Long actualAmount;

    @TableField("difference_amount")
    private Long differenceAmount;

    @TableField("cash_account_id")
    private Long cashAccountId;

    @TableField("bank_account_id")
    private Long bankAccountId;

    @TableField("deposit_bank_txn_id")
    private Long depositBankTxnId;

    @TableField("deposit_ref_no")
    private String depositRefNo;

    @TableField("status")
    private String status;

    @TableField("submitted_at")
    private LocalDateTime submittedAt;

    @TableField("received_at")
    private LocalDateTime receivedAt;

    @TableField("deposited_at")
    private LocalDateTime depositedAt;

    @TableField("verified_at")
    private LocalDateTime verifiedAt;

    @TableField("closed_at")
    private LocalDateTime closedAt;

    @TableField("reopen_of")
    private Long reopenOf;

    @TableField("remark")
    private String remark;

    @TableField("create_user_id")
    private Long createUserId;

    @TableField("created_by")
    private String createdBy;

    @TableField("create_time")
    private LocalDateTime createTime;

    @TableField("update_time")
    private LocalDateTime updateTime;

    @TableField("version")
    private Integer version;

    @TableField("deleted")
    private Integer deleted;

    public Long getSlipId() { return slipId; }
    public void setSlipId(Long slipId) { this.slipId = slipId; }

    public String getSlipNo() { return slipNo; }
    public void setSlipNo(String slipNo) { this.slipNo = slipNo; }

    public Long getStoreId() { return storeId; }
    public void setStoreId(Long storeId) { this.storeId = storeId; }

    public Long getShiftId() { return shiftId; }
    public void setShiftId(Long shiftId) { this.shiftId = shiftId; }

    public LocalDate getBusinessDate() { return businessDate; }
    public void setBusinessDate(LocalDate businessDate) { this.businessDate = businessDate; }

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

    public Long getRefundComponentFen() { return refundComponentFen; }
    public void setRefundComponentFen(Long refundComponentFen) { this.refundComponentFen = refundComponentFen; }

    public Long getDifferenceNetFen() { return differenceNetFen; }
    public void setDifferenceNetFen(Long differenceNetFen) { this.differenceNetFen = differenceNetFen; }

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

    public Integer getVersion() { return version; }
    public void setVersion(Integer version) { this.version = version; }

    public Integer getDeleted() { return deleted; }
    public void setDeleted(Integer deleted) { this.deleted = deleted; }
}
