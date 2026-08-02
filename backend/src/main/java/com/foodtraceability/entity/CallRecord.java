package com.foodtraceability.entity;

import com.baomidou.mybatisplus.annotation.*;
import java.time.LocalDateTime;

@TableName("call_record")
public class CallRecord {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String orderId;
    private String orderNumber;
    private String tableNumber;
    private String orderType;
    private Integer itemCount;
    private String status;
    private Integer callCount;
    private LocalDateTime firstCallTime;
    private LocalDateTime lastCallTime;
    private LocalDateTime pickTime;
    private Integer waitSeconds;
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;

    public CallRecord() {
    }

    public Long getId() {
        return this.id;
    }

    public String getOrderId() {
        return this.orderId;
    }

    public String getOrderNumber() {
        return this.orderNumber;
    }

    public String getTableNumber() {
        return this.tableNumber;
    }

    public String getOrderType() {
        return this.orderType;
    }

    public Integer getItemCount() {
        return this.itemCount;
    }

    public String getStatus() {
        return this.status;
    }

    public Integer getCallCount() {
        return this.callCount;
    }

    public LocalDateTime getFirstCallTime() {
        return this.firstCallTime;
    }

    public LocalDateTime getLastCallTime() {
        return this.lastCallTime;
    }

    public LocalDateTime getPickTime() {
        return this.pickTime;
    }

    public Integer getWaitSeconds() {
        return this.waitSeconds;
    }

    public LocalDateTime getCreateTime() {
        return this.createTime;
    }

    public LocalDateTime getUpdateTime() {
        return this.updateTime;
    }

    public void setId(final Long id) {
        this.id = id;
    }

    public void setOrderId(final String orderId) {
        this.orderId = orderId;
    }

    public void setOrderNumber(final String orderNumber) {
        this.orderNumber = orderNumber;
    }

    public void setTableNumber(final String tableNumber) {
        this.tableNumber = tableNumber;
    }

    public void setOrderType(final String orderType) {
        this.orderType = orderType;
    }

    public void setItemCount(final Integer itemCount) {
        this.itemCount = itemCount;
    }

    public void setStatus(final String status) {
        this.status = status;
    }

    public void setCallCount(final Integer callCount) {
        this.callCount = callCount;
    }

    public void setFirstCallTime(final LocalDateTime firstCallTime) {
        this.firstCallTime = firstCallTime;
    }

    public void setLastCallTime(final LocalDateTime lastCallTime) {
        this.lastCallTime = lastCallTime;
    }

    public void setPickTime(final LocalDateTime pickTime) {
        this.pickTime = pickTime;
    }

    public void setWaitSeconds(final Integer waitSeconds) {
        this.waitSeconds = waitSeconds;
    }

    public void setCreateTime(final LocalDateTime createTime) {
        this.createTime = createTime;
    }

    public void setUpdateTime(final LocalDateTime updateTime) {
        this.updateTime = updateTime;
    }

    @java.lang.Override
    public boolean equals(final java.lang.Object o) {
        if (o == this) return true;
        if (!(o instanceof CallRecord)) return false;
        final CallRecord other = (CallRecord) o;
        if (!other.canEqual((java.lang.Object) this)) return false;
        final java.lang.Object this$id = this.getId();
        final java.lang.Object other$id = other.getId();
        if (this$id == null ? other$id != null : !this$id.equals(other$id)) return false;
        final java.lang.Object this$itemCount = this.getItemCount();
        final java.lang.Object other$itemCount = other.getItemCount();
        if (this$itemCount == null ? other$itemCount != null : !this$itemCount.equals(other$itemCount)) return false;
        final java.lang.Object this$callCount = this.getCallCount();
        final java.lang.Object other$callCount = other.getCallCount();
        if (this$callCount == null ? other$callCount != null : !this$callCount.equals(other$callCount)) return false;
        final java.lang.Object this$waitSeconds = this.getWaitSeconds();
        final java.lang.Object other$waitSeconds = other.getWaitSeconds();
        if (this$waitSeconds == null ? other$waitSeconds != null : !this$waitSeconds.equals(other$waitSeconds)) return false;
        final java.lang.Object this$orderId = this.getOrderId();
        final java.lang.Object other$orderId = other.getOrderId();
        if (this$orderId == null ? other$orderId != null : !this$orderId.equals(other$orderId)) return false;
        final java.lang.Object this$orderNumber = this.getOrderNumber();
        final java.lang.Object other$orderNumber = other.getOrderNumber();
        if (this$orderNumber == null ? other$orderNumber != null : !this$orderNumber.equals(other$orderNumber)) return false;
        final java.lang.Object this$tableNumber = this.getTableNumber();
        final java.lang.Object other$tableNumber = other.getTableNumber();
        if (this$tableNumber == null ? other$tableNumber != null : !this$tableNumber.equals(other$tableNumber)) return false;
        final java.lang.Object this$orderType = this.getOrderType();
        final java.lang.Object other$orderType = other.getOrderType();
        if (this$orderType == null ? other$orderType != null : !this$orderType.equals(other$orderType)) return false;
        final java.lang.Object this$status = this.getStatus();
        final java.lang.Object other$status = other.getStatus();
        if (this$status == null ? other$status != null : !this$status.equals(other$status)) return false;
        final java.lang.Object this$firstCallTime = this.getFirstCallTime();
        final java.lang.Object other$firstCallTime = other.getFirstCallTime();
        if (this$firstCallTime == null ? other$firstCallTime != null : !this$firstCallTime.equals(other$firstCallTime)) return false;
        final java.lang.Object this$lastCallTime = this.getLastCallTime();
        final java.lang.Object other$lastCallTime = other.getLastCallTime();
        if (this$lastCallTime == null ? other$lastCallTime != null : !this$lastCallTime.equals(other$lastCallTime)) return false;
        final java.lang.Object this$pickTime = this.getPickTime();
        final java.lang.Object other$pickTime = other.getPickTime();
        if (this$pickTime == null ? other$pickTime != null : !this$pickTime.equals(other$pickTime)) return false;
        final java.lang.Object this$createTime = this.getCreateTime();
        final java.lang.Object other$createTime = other.getCreateTime();
        if (this$createTime == null ? other$createTime != null : !this$createTime.equals(other$createTime)) return false;
        final java.lang.Object this$updateTime = this.getUpdateTime();
        final java.lang.Object other$updateTime = other.getUpdateTime();
        if (this$updateTime == null ? other$updateTime != null : !this$updateTime.equals(other$updateTime)) return false;
        return true;
    }

    protected boolean canEqual(final java.lang.Object other) {
        return other instanceof CallRecord;
    }

    @java.lang.Override
    public int hashCode() {
        final int PRIME = 59;
        int result = 1;
        final java.lang.Object $id = this.getId();
        result = result * PRIME + ($id == null ? 43 : $id.hashCode());
        final java.lang.Object $itemCount = this.getItemCount();
        result = result * PRIME + ($itemCount == null ? 43 : $itemCount.hashCode());
        final java.lang.Object $callCount = this.getCallCount();
        result = result * PRIME + ($callCount == null ? 43 : $callCount.hashCode());
        final java.lang.Object $waitSeconds = this.getWaitSeconds();
        result = result * PRIME + ($waitSeconds == null ? 43 : $waitSeconds.hashCode());
        final java.lang.Object $orderId = this.getOrderId();
        result = result * PRIME + ($orderId == null ? 43 : $orderId.hashCode());
        final java.lang.Object $orderNumber = this.getOrderNumber();
        result = result * PRIME + ($orderNumber == null ? 43 : $orderNumber.hashCode());
        final java.lang.Object $tableNumber = this.getTableNumber();
        result = result * PRIME + ($tableNumber == null ? 43 : $tableNumber.hashCode());
        final java.lang.Object $orderType = this.getOrderType();
        result = result * PRIME + ($orderType == null ? 43 : $orderType.hashCode());
        final java.lang.Object $status = this.getStatus();
        result = result * PRIME + ($status == null ? 43 : $status.hashCode());
        final java.lang.Object $firstCallTime = this.getFirstCallTime();
        result = result * PRIME + ($firstCallTime == null ? 43 : $firstCallTime.hashCode());
        final java.lang.Object $lastCallTime = this.getLastCallTime();
        result = result * PRIME + ($lastCallTime == null ? 43 : $lastCallTime.hashCode());
        final java.lang.Object $pickTime = this.getPickTime();
        result = result * PRIME + ($pickTime == null ? 43 : $pickTime.hashCode());
        final java.lang.Object $createTime = this.getCreateTime();
        result = result * PRIME + ($createTime == null ? 43 : $createTime.hashCode());
        final java.lang.Object $updateTime = this.getUpdateTime();
        result = result * PRIME + ($updateTime == null ? 43 : $updateTime.hashCode());
        return result;
    }

    @java.lang.Override
    public java.lang.String toString() {
        return "CallRecord(id=" + this.getId() + ", orderId=" + this.getOrderId() + ", orderNumber=" + this.getOrderNumber() + ", tableNumber=" + this.getTableNumber() + ", orderType=" + this.getOrderType() + ", itemCount=" + this.getItemCount() + ", status=" + this.getStatus() + ", callCount=" + this.getCallCount() + ", firstCallTime=" + this.getFirstCallTime() + ", lastCallTime=" + this.getLastCallTime() + ", pickTime=" + this.getPickTime() + ", waitSeconds=" + this.getWaitSeconds() + ", createTime=" + this.getCreateTime() + ", updateTime=" + this.getUpdateTime() + ")";
    }
}
