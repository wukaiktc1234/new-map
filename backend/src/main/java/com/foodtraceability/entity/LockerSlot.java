package com.foodtraceability.entity;

import com.baomidou.mybatisplus.annotation.*;
import java.time.LocalDateTime;

/**
 * 取餐柜格子实体类
 * @author example
 * @since 2026-01-08
 */
@TableName("locker_slot")
public class LockerSlot {
    /**
     * 主键ID
     */
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;
    /**
     * 取餐柜ID
     */
    @TableField("locker_id")
    private Long lockerId;
    /**
     * 取餐柜编号
     */
    @TableField("locker_code")
    private String lockerCode;
    /**
     * 格子编号
     */
    @TableField("slot_code")
    private String slotCode;
    /**
     * 格子类型：SMALL（小）、MEDIUM（中）、LARGE（大）
     */
    @TableField("slot_type")
    private String slotType;
    /**
     * 格子状态：AVAILABLE（可用）、OCCUPIED（占用）、MAINTENANCE（维护中）、DISABLED（禁用）
     */
    @TableField("status")
    private String status;
    /**
     * 当前订单ID
     */
    @TableField("order_id")
    private Long orderId;
    /**
     * 取餐码
     */
    @TableField("pickup_code")
    private String pickupCode;
    /**
     * 放入时间
     */
    @TableField("put_time")
    private LocalDateTime putTime;
    /**
     * 预计取餐时间
     */
    @TableField("expected_pickup_time")
    private LocalDateTime expectedPickupTime;
    /**
     * 取餐时间
     */
    @TableField("pickup_time")
    private LocalDateTime pickupTime;
    /**
     * 放入人
     */
    @TableField("put_operator")
    private String putOperator;
    /**
     * 取餐人
     */
    @TableField("pickup_operator")
    private String pickupOperator;
    /**
     * 超时时间（分钟）
     */
    @TableField("timeout_minutes")
    private Integer timeoutMinutes;
    /**
     * 备注
     */
    @TableField("remark")
    private String remark;
    /**
     * 创建时间
     */
    @TableField(value = "create_time", fill = FieldFill.INSERT)
    private LocalDateTime createTime;
    /**
     * 更新时间
     */
    @TableField(value = "update_time", fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;
    /**
     * 创建人
     */
    @TableField(value = "create_by", fill = FieldFill.INSERT)
    private String createBy;
    /**
     * 更新人
     */
    @TableField(value = "update_by", fill = FieldFill.INSERT_UPDATE)
    private String updateBy;
    /**
     * 逻辑删除标志
     */
    @TableLogic
    @TableField("deleted")
    private Integer deleted;

    // Getter and Setter methods
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getLockerId() {
        return lockerId;
    }

    public void setLockerId(Long lockerId) {
        this.lockerId = lockerId;
    }

    public String getLockerCode() {
        return lockerCode;
    }

    public void setLockerCode(String lockerCode) {
        this.lockerCode = lockerCode;
    }

    public String getSlotCode() {
        return slotCode;
    }

    public void setSlotCode(String slotCode) {
        this.slotCode = slotCode;
    }

    public String getSlotType() {
        return slotType;
    }

    public void setSlotType(String slotType) {
        this.slotType = slotType;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public Long getOrderId() {
        return orderId;
    }

    public void setOrderId(Long orderId) {
        this.orderId = orderId;
    }

    public String getPickupCode() {
        return pickupCode;
    }

    public void setPickupCode(String pickupCode) {
        this.pickupCode = pickupCode;
    }

    public LocalDateTime getPutTime() {
        return putTime;
    }

    public void setPutTime(LocalDateTime putTime) {
        this.putTime = putTime;
    }

    public LocalDateTime getExpectedPickupTime() {
        return expectedPickupTime;
    }

    public void setExpectedPickupTime(LocalDateTime expectedPickupTime) {
        this.expectedPickupTime = expectedPickupTime;
    }

    public LocalDateTime getPickupTime() {
        return pickupTime;
    }

    public void setPickupTime(LocalDateTime pickupTime) {
        this.pickupTime = pickupTime;
    }

    public String getPutOperator() {
        return putOperator;
    }

    public void setPutOperator(String putOperator) {
        this.putOperator = putOperator;
    }

    public String getPickupOperator() {
        return pickupOperator;
    }

    public void setPickupOperator(String pickupOperator) {
        this.pickupOperator = pickupOperator;
    }

    public Integer getTimeoutMinutes() {
        return timeoutMinutes;
    }

    public void setTimeoutMinutes(Integer timeoutMinutes) {
        this.timeoutMinutes = timeoutMinutes;
    }

    public String getRemark() {
        return remark;
    }

    public void setRemark(String remark) {
        this.remark = remark;
    }

    public LocalDateTime getCreateTime() {
        return createTime;
    }

    public void setCreateTime(LocalDateTime createTime) {
        this.createTime = createTime;
    }

    public LocalDateTime getUpdateTime() {
        return updateTime;
    }

    public void setUpdateTime(LocalDateTime updateTime) {
        this.updateTime = updateTime;
    }

    public String getCreateBy() {
        return createBy;
    }

    public void setCreateBy(String createBy) {
        this.createBy = createBy;
    }

    public String getUpdateBy() {
        return updateBy;
    }

    public void setUpdateBy(String updateBy) {
        this.updateBy = updateBy;
    }

    public Integer getDeleted() {
        return deleted;
    }

    public void setDeleted(Integer deleted) {
        this.deleted = deleted;
    }

    public LockerSlot() {
    }

    @java.lang.Override
    public java.lang.String toString() {
        return "LockerSlot(id=" + this.getId() + ", lockerId=" + this.getLockerId() + ", lockerCode=" + this.getLockerCode() + ", slotCode=" + this.getSlotCode() + ", slotType=" + this.getSlotType() + ", status=" + this.getStatus() + ", orderId=" + this.getOrderId() + ", pickupCode=" + this.getPickupCode() + ", putTime=" + this.getPutTime() + ", expectedPickupTime=" + this.getExpectedPickupTime() + ", pickupTime=" + this.getPickupTime() + ", putOperator=" + this.getPutOperator() + ", pickupOperator=" + this.getPickupOperator() + ", timeoutMinutes=" + this.getTimeoutMinutes() + ", remark=" + this.getRemark() + ", createTime=" + this.getCreateTime() + ", updateTime=" + this.getUpdateTime() + ", createBy=" + this.getCreateBy() + ", updateBy=" + this.getUpdateBy() + ", deleted=" + this.getDeleted() + ")";
    }

    @java.lang.Override
    public boolean equals(final java.lang.Object o) {
        if (o == this) return true;
        if (!(o instanceof LockerSlot)) return false;
        final LockerSlot other = (LockerSlot) o;
        if (!other.canEqual((java.lang.Object) this)) return false;
        final java.lang.Object this$id = this.getId();
        final java.lang.Object other$id = other.getId();
        if (this$id == null ? other$id != null : !this$id.equals(other$id)) return false;
        final java.lang.Object this$lockerId = this.getLockerId();
        final java.lang.Object other$lockerId = other.getLockerId();
        if (this$lockerId == null ? other$lockerId != null : !this$lockerId.equals(other$lockerId)) return false;
        final java.lang.Object this$orderId = this.getOrderId();
        final java.lang.Object other$orderId = other.getOrderId();
        if (this$orderId == null ? other$orderId != null : !this$orderId.equals(other$orderId)) return false;
        final java.lang.Object this$timeoutMinutes = this.getTimeoutMinutes();
        final java.lang.Object other$timeoutMinutes = other.getTimeoutMinutes();
        if (this$timeoutMinutes == null ? other$timeoutMinutes != null : !this$timeoutMinutes.equals(other$timeoutMinutes)) return false;
        final java.lang.Object this$deleted = this.getDeleted();
        final java.lang.Object other$deleted = other.getDeleted();
        if (this$deleted == null ? other$deleted != null : !this$deleted.equals(other$deleted)) return false;
        final java.lang.Object this$lockerCode = this.getLockerCode();
        final java.lang.Object other$lockerCode = other.getLockerCode();
        if (this$lockerCode == null ? other$lockerCode != null : !this$lockerCode.equals(other$lockerCode)) return false;
        final java.lang.Object this$slotCode = this.getSlotCode();
        final java.lang.Object other$slotCode = other.getSlotCode();
        if (this$slotCode == null ? other$slotCode != null : !this$slotCode.equals(other$slotCode)) return false;
        final java.lang.Object this$slotType = this.getSlotType();
        final java.lang.Object other$slotType = other.getSlotType();
        if (this$slotType == null ? other$slotType != null : !this$slotType.equals(other$slotType)) return false;
        final java.lang.Object this$status = this.getStatus();
        final java.lang.Object other$status = other.getStatus();
        if (this$status == null ? other$status != null : !this$status.equals(other$status)) return false;
        final java.lang.Object this$pickupCode = this.getPickupCode();
        final java.lang.Object other$pickupCode = other.getPickupCode();
        if (this$pickupCode == null ? other$pickupCode != null : !this$pickupCode.equals(other$pickupCode)) return false;
        final java.lang.Object this$putTime = this.getPutTime();
        final java.lang.Object other$putTime = other.getPutTime();
        if (this$putTime == null ? other$putTime != null : !this$putTime.equals(other$putTime)) return false;
        final java.lang.Object this$expectedPickupTime = this.getExpectedPickupTime();
        final java.lang.Object other$expectedPickupTime = other.getExpectedPickupTime();
        if (this$expectedPickupTime == null ? other$expectedPickupTime != null : !this$expectedPickupTime.equals(other$expectedPickupTime)) return false;
        final java.lang.Object this$pickupTime = this.getPickupTime();
        final java.lang.Object other$pickupTime = other.getPickupTime();
        if (this$pickupTime == null ? other$pickupTime != null : !this$pickupTime.equals(other$pickupTime)) return false;
        final java.lang.Object this$putOperator = this.getPutOperator();
        final java.lang.Object other$putOperator = other.getPutOperator();
        if (this$putOperator == null ? other$putOperator != null : !this$putOperator.equals(other$putOperator)) return false;
        final java.lang.Object this$pickupOperator = this.getPickupOperator();
        final java.lang.Object other$pickupOperator = other.getPickupOperator();
        if (this$pickupOperator == null ? other$pickupOperator != null : !this$pickupOperator.equals(other$pickupOperator)) return false;
        final java.lang.Object this$remark = this.getRemark();
        final java.lang.Object other$remark = other.getRemark();
        if (this$remark == null ? other$remark != null : !this$remark.equals(other$remark)) return false;
        final java.lang.Object this$createTime = this.getCreateTime();
        final java.lang.Object other$createTime = other.getCreateTime();
        if (this$createTime == null ? other$createTime != null : !this$createTime.equals(other$createTime)) return false;
        final java.lang.Object this$updateTime = this.getUpdateTime();
        final java.lang.Object other$updateTime = other.getUpdateTime();
        if (this$updateTime == null ? other$updateTime != null : !this$updateTime.equals(other$updateTime)) return false;
        final java.lang.Object this$createBy = this.getCreateBy();
        final java.lang.Object other$createBy = other.getCreateBy();
        if (this$createBy == null ? other$createBy != null : !this$createBy.equals(other$createBy)) return false;
        final java.lang.Object this$updateBy = this.getUpdateBy();
        final java.lang.Object other$updateBy = other.getUpdateBy();
        if (this$updateBy == null ? other$updateBy != null : !this$updateBy.equals(other$updateBy)) return false;
        return true;
    }

    protected boolean canEqual(final java.lang.Object other) {
        return other instanceof LockerSlot;
    }

    @java.lang.Override
    public int hashCode() {
        final int PRIME = 59;
        int result = 1;
        final java.lang.Object $id = this.getId();
        result = result * PRIME + ($id == null ? 43 : $id.hashCode());
        final java.lang.Object $lockerId = this.getLockerId();
        result = result * PRIME + ($lockerId == null ? 43 : $lockerId.hashCode());
        final java.lang.Object $orderId = this.getOrderId();
        result = result * PRIME + ($orderId == null ? 43 : $orderId.hashCode());
        final java.lang.Object $timeoutMinutes = this.getTimeoutMinutes();
        result = result * PRIME + ($timeoutMinutes == null ? 43 : $timeoutMinutes.hashCode());
        final java.lang.Object $deleted = this.getDeleted();
        result = result * PRIME + ($deleted == null ? 43 : $deleted.hashCode());
        final java.lang.Object $lockerCode = this.getLockerCode();
        result = result * PRIME + ($lockerCode == null ? 43 : $lockerCode.hashCode());
        final java.lang.Object $slotCode = this.getSlotCode();
        result = result * PRIME + ($slotCode == null ? 43 : $slotCode.hashCode());
        final java.lang.Object $slotType = this.getSlotType();
        result = result * PRIME + ($slotType == null ? 43 : $slotType.hashCode());
        final java.lang.Object $status = this.getStatus();
        result = result * PRIME + ($status == null ? 43 : $status.hashCode());
        final java.lang.Object $pickupCode = this.getPickupCode();
        result = result * PRIME + ($pickupCode == null ? 43 : $pickupCode.hashCode());
        final java.lang.Object $putTime = this.getPutTime();
        result = result * PRIME + ($putTime == null ? 43 : $putTime.hashCode());
        final java.lang.Object $expectedPickupTime = this.getExpectedPickupTime();
        result = result * PRIME + ($expectedPickupTime == null ? 43 : $expectedPickupTime.hashCode());
        final java.lang.Object $pickupTime = this.getPickupTime();
        result = result * PRIME + ($pickupTime == null ? 43 : $pickupTime.hashCode());
        final java.lang.Object $putOperator = this.getPutOperator();
        result = result * PRIME + ($putOperator == null ? 43 : $putOperator.hashCode());
        final java.lang.Object $pickupOperator = this.getPickupOperator();
        result = result * PRIME + ($pickupOperator == null ? 43 : $pickupOperator.hashCode());
        final java.lang.Object $remark = this.getRemark();
        result = result * PRIME + ($remark == null ? 43 : $remark.hashCode());
        final java.lang.Object $createTime = this.getCreateTime();
        result = result * PRIME + ($createTime == null ? 43 : $createTime.hashCode());
        final java.lang.Object $updateTime = this.getUpdateTime();
        result = result * PRIME + ($updateTime == null ? 43 : $updateTime.hashCode());
        final java.lang.Object $createBy = this.getCreateBy();
        result = result * PRIME + ($createBy == null ? 43 : $createBy.hashCode());
        final java.lang.Object $updateBy = this.getUpdateBy();
        result = result * PRIME + ($updateBy == null ? 43 : $updateBy.hashCode());
        return result;
    }
}
