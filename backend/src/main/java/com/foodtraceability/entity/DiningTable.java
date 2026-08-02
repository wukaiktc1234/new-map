package com.foodtraceability.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.time.LocalDateTime;

@TableName("dining_table")
public class DiningTable {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String tableNumber;
    private String tableName;
    private Integer capacity;
    private String area;
    private String status;
    private String qrCode;
    private Long currentOrderId;
    private Integer guestCount;
    private LocalDateTime seatedAt;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public DiningTable() {
    }

    public Long getId() {
        return this.id;
    }

    public String getTableNumber() {
        return this.tableNumber;
    }

    public String getTableName() {
        return this.tableName;
    }

    public Integer getCapacity() {
        return this.capacity;
    }

    public String getArea() {
        return this.area;
    }

    public String getStatus() {
        return this.status;
    }

    public String getQrCode() {
        return this.qrCode;
    }

    public Long getCurrentOrderId() {
        return this.currentOrderId;
    }

    public Integer getGuestCount() {
        return this.guestCount;
    }

    public LocalDateTime getSeatedAt() {
        return this.seatedAt;
    }

    public LocalDateTime getCreatedAt() {
        return this.createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return this.updatedAt;
    }

    public void setId(final Long id) {
        this.id = id;
    }

    public void setTableNumber(final String tableNumber) {
        this.tableNumber = tableNumber;
    }

    public void setTableName(final String tableName) {
        this.tableName = tableName;
    }

    public void setCapacity(final Integer capacity) {
        this.capacity = capacity;
    }

    public void setArea(final String area) {
        this.area = area;
    }

    public void setStatus(final String status) {
        this.status = status;
    }

    public void setQrCode(final String qrCode) {
        this.qrCode = qrCode;
    }

    public void setCurrentOrderId(final Long currentOrderId) {
        this.currentOrderId = currentOrderId;
    }

    public void setGuestCount(final Integer guestCount) {
        this.guestCount = guestCount;
    }

    public void setSeatedAt(final LocalDateTime seatedAt) {
        this.seatedAt = seatedAt;
    }

    public void setCreatedAt(final LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public void setUpdatedAt(final LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }

    @java.lang.Override
    public boolean equals(final java.lang.Object o) {
        if (o == this) return true;
        if (!(o instanceof DiningTable)) return false;
        final DiningTable other = (DiningTable) o;
        if (!other.canEqual((java.lang.Object) this)) return false;
        final java.lang.Object this$id = this.getId();
        final java.lang.Object other$id = other.getId();
        if (this$id == null ? other$id != null : !this$id.equals(other$id)) return false;
        final java.lang.Object this$capacity = this.getCapacity();
        final java.lang.Object other$capacity = other.getCapacity();
        if (this$capacity == null ? other$capacity != null : !this$capacity.equals(other$capacity)) return false;
        final java.lang.Object this$currentOrderId = this.getCurrentOrderId();
        final java.lang.Object other$currentOrderId = other.getCurrentOrderId();
        if (this$currentOrderId == null ? other$currentOrderId != null : !this$currentOrderId.equals(other$currentOrderId)) return false;
        final java.lang.Object this$guestCount = this.getGuestCount();
        final java.lang.Object other$guestCount = other.getGuestCount();
        if (this$guestCount == null ? other$guestCount != null : !this$guestCount.equals(other$guestCount)) return false;
        final java.lang.Object this$tableNumber = this.getTableNumber();
        final java.lang.Object other$tableNumber = other.getTableNumber();
        if (this$tableNumber == null ? other$tableNumber != null : !this$tableNumber.equals(other$tableNumber)) return false;
        final java.lang.Object this$tableName = this.getTableName();
        final java.lang.Object other$tableName = other.getTableName();
        if (this$tableName == null ? other$tableName != null : !this$tableName.equals(other$tableName)) return false;
        final java.lang.Object this$area = this.getArea();
        final java.lang.Object other$area = other.getArea();
        if (this$area == null ? other$area != null : !this$area.equals(other$area)) return false;
        final java.lang.Object this$status = this.getStatus();
        final java.lang.Object other$status = other.getStatus();
        if (this$status == null ? other$status != null : !this$status.equals(other$status)) return false;
        final java.lang.Object this$qrCode = this.getQrCode();
        final java.lang.Object other$qrCode = other.getQrCode();
        if (this$qrCode == null ? other$qrCode != null : !this$qrCode.equals(other$qrCode)) return false;
        final java.lang.Object this$seatedAt = this.getSeatedAt();
        final java.lang.Object other$seatedAt = other.getSeatedAt();
        if (this$seatedAt == null ? other$seatedAt != null : !this$seatedAt.equals(other$seatedAt)) return false;
        final java.lang.Object this$createdAt = this.getCreatedAt();
        final java.lang.Object other$createdAt = other.getCreatedAt();
        if (this$createdAt == null ? other$createdAt != null : !this$createdAt.equals(other$createdAt)) return false;
        final java.lang.Object this$updatedAt = this.getUpdatedAt();
        final java.lang.Object other$updatedAt = other.getUpdatedAt();
        if (this$updatedAt == null ? other$updatedAt != null : !this$updatedAt.equals(other$updatedAt)) return false;
        return true;
    }

    protected boolean canEqual(final java.lang.Object other) {
        return other instanceof DiningTable;
    }

    @java.lang.Override
    public int hashCode() {
        final int PRIME = 59;
        int result = 1;
        final java.lang.Object $id = this.getId();
        result = result * PRIME + ($id == null ? 43 : $id.hashCode());
        final java.lang.Object $capacity = this.getCapacity();
        result = result * PRIME + ($capacity == null ? 43 : $capacity.hashCode());
        final java.lang.Object $currentOrderId = this.getCurrentOrderId();
        result = result * PRIME + ($currentOrderId == null ? 43 : $currentOrderId.hashCode());
        final java.lang.Object $guestCount = this.getGuestCount();
        result = result * PRIME + ($guestCount == null ? 43 : $guestCount.hashCode());
        final java.lang.Object $tableNumber = this.getTableNumber();
        result = result * PRIME + ($tableNumber == null ? 43 : $tableNumber.hashCode());
        final java.lang.Object $tableName = this.getTableName();
        result = result * PRIME + ($tableName == null ? 43 : $tableName.hashCode());
        final java.lang.Object $area = this.getArea();
        result = result * PRIME + ($area == null ? 43 : $area.hashCode());
        final java.lang.Object $status = this.getStatus();
        result = result * PRIME + ($status == null ? 43 : $status.hashCode());
        final java.lang.Object $qrCode = this.getQrCode();
        result = result * PRIME + ($qrCode == null ? 43 : $qrCode.hashCode());
        final java.lang.Object $seatedAt = this.getSeatedAt();
        result = result * PRIME + ($seatedAt == null ? 43 : $seatedAt.hashCode());
        final java.lang.Object $createdAt = this.getCreatedAt();
        result = result * PRIME + ($createdAt == null ? 43 : $createdAt.hashCode());
        final java.lang.Object $updatedAt = this.getUpdatedAt();
        result = result * PRIME + ($updatedAt == null ? 43 : $updatedAt.hashCode());
        return result;
    }

    @java.lang.Override
    public java.lang.String toString() {
        return "DiningTable(id=" + this.getId() + ", tableNumber=" + this.getTableNumber() + ", tableName=" + this.getTableName() + ", capacity=" + this.getCapacity() + ", area=" + this.getArea() + ", status=" + this.getStatus() + ", qrCode=" + this.getQrCode() + ", currentOrderId=" + this.getCurrentOrderId() + ", guestCount=" + this.getGuestCount() + ", seatedAt=" + this.getSeatedAt() + ", createdAt=" + this.getCreatedAt() + ", updatedAt=" + this.getUpdatedAt() + ")";
    }
}
