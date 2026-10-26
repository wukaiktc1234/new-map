package com.foodtraceability.entity.finance.recon;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

import java.time.LocalDateTime;

/**
 * N19-2④：退款单 → 班次归属（表 refund_shift_attribution，迁移 V20261026_005）。
 *
 * <p>用途：现金户对账时，退款究竟落在哪个班次/哪张缴款单需要人工确认；
 * {@code refund_record_id} 唯一（一个退款单只归属一次），重复确认走覆盖 + 审计。</p>
 */
@TableName("refund_shift_attribution")
public class RefundShiftAttribution {

    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    @TableField("refund_record_id")
    private Long refundRecordId;

    @TableField("shift_id")
    private Long shiftId;

    @TableField("slip_id")
    private Long slipId;

    @TableField("operator_id")
    private Long operatorId;

    @TableField("reason")
    private String reason;

    @TableField("confirmed_at")
    private java.time.OffsetDateTime confirmedAt;

    @TableField("update_time")
    private java.time.OffsetDateTime updateTime;

    @TableField("version")
    private Integer version;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getRefundRecordId() { return refundRecordId; }
    public void setRefundRecordId(Long refundRecordId) { this.refundRecordId = refundRecordId; }
    public Long getShiftId() { return shiftId; }
    public void setShiftId(Long shiftId) { this.shiftId = shiftId; }
    public Long getSlipId() { return slipId; }
    public void setSlipId(Long slipId) { this.slipId = slipId; }
    public Long getOperatorId() { return operatorId; }
    public void setOperatorId(Long operatorId) { this.operatorId = operatorId; }
    public String getReason() { return reason; }
    public void setReason(String reason) { this.reason = reason; }
    public java.time.OffsetDateTime getConfirmedAt() { return confirmedAt; }
    public void setConfirmedAt(java.time.OffsetDateTime confirmedAt) { this.confirmedAt = confirmedAt; }
    public java.time.OffsetDateTime getUpdateTime() { return updateTime; }
    public void setUpdateTime(java.time.OffsetDateTime updateTime) { this.updateTime = updateTime; }
    public Integer getVersion() { return version; }
    public void setVersion(Integer version) { this.version = version; }
}
