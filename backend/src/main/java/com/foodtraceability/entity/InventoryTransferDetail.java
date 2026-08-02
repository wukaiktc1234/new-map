package com.foodtraceability.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.math.BigDecimal;
import java.util.Date;

/**
 * 调拨明细表实体类
 */
@TableName("inventory_transfer_detail")
public class InventoryTransferDetail {
    /**
     * 调拨明细ID
     */
    @TableId(type = IdType.AUTO)
    private Long id;
    /**
     * 调拨单ID
     */
    private Long transferId;
    /**
     * 产品ID
     */
    private Long productId;
    /**
     * 调拨数量
     */
    private BigDecimal quantity;
    /**
     * 计量单位
     */
    private String unit;
    /**
     * 创建时间
     */
    private Date createdAt;
    /**
     * 更新时间
     */
    private Date updatedAt;

    public InventoryTransferDetail() {
    }

    /**
     * 调拨明细ID
     */
    public Long getId() {
        return this.id;
    }

    /**
     * 调拨单ID
     */
    public Long getTransferId() {
        return this.transferId;
    }

    /**
     * 产品ID
     */
    public Long getProductId() {
        return this.productId;
    }

    /**
     * 调拨数量
     */
    public BigDecimal getQuantity() {
        return this.quantity;
    }

    /**
     * 计量单位
     */
    public String getUnit() {
        return this.unit;
    }

    /**
     * 创建时间
     */
    public Date getCreatedAt() {
        return this.createdAt;
    }

    /**
     * 更新时间
     */
    public Date getUpdatedAt() {
        return this.updatedAt;
    }

    /**
     * 调拨明细ID
     */
    public void setId(final Long id) {
        this.id = id;
    }

    /**
     * 调拨单ID
     */
    public void setTransferId(final Long transferId) {
        this.transferId = transferId;
    }

    /**
     * 产品ID
     */
    public void setProductId(final Long productId) {
        this.productId = productId;
    }

    /**
     * 调拨数量
     */
    public void setQuantity(final BigDecimal quantity) {
        this.quantity = quantity;
    }

    /**
     * 计量单位
     */
    public void setUnit(final String unit) {
        this.unit = unit;
    }

    /**
     * 创建时间
     */
    public void setCreatedAt(final Date createdAt) {
        this.createdAt = createdAt;
    }

    /**
     * 更新时间
     */
    public void setUpdatedAt(final Date updatedAt) {
        this.updatedAt = updatedAt;
    }

    @java.lang.Override
    public boolean equals(final java.lang.Object o) {
        if (o == this) return true;
        if (!(o instanceof InventoryTransferDetail)) return false;
        final InventoryTransferDetail other = (InventoryTransferDetail) o;
        if (!other.canEqual((java.lang.Object) this)) return false;
        final java.lang.Object this$id = this.getId();
        final java.lang.Object other$id = other.getId();
        if (this$id == null ? other$id != null : !this$id.equals(other$id)) return false;
        final java.lang.Object this$transferId = this.getTransferId();
        final java.lang.Object other$transferId = other.getTransferId();
        if (this$transferId == null ? other$transferId != null : !this$transferId.equals(other$transferId)) return false;
        final java.lang.Object this$productId = this.getProductId();
        final java.lang.Object other$productId = other.getProductId();
        if (this$productId == null ? other$productId != null : !this$productId.equals(other$productId)) return false;
        final java.lang.Object this$quantity = this.getQuantity();
        final java.lang.Object other$quantity = other.getQuantity();
        if (this$quantity == null ? other$quantity != null : !this$quantity.equals(other$quantity)) return false;
        final java.lang.Object this$unit = this.getUnit();
        final java.lang.Object other$unit = other.getUnit();
        if (this$unit == null ? other$unit != null : !this$unit.equals(other$unit)) return false;
        final java.lang.Object this$createdAt = this.getCreatedAt();
        final java.lang.Object other$createdAt = other.getCreatedAt();
        if (this$createdAt == null ? other$createdAt != null : !this$createdAt.equals(other$createdAt)) return false;
        final java.lang.Object this$updatedAt = this.getUpdatedAt();
        final java.lang.Object other$updatedAt = other.getUpdatedAt();
        if (this$updatedAt == null ? other$updatedAt != null : !this$updatedAt.equals(other$updatedAt)) return false;
        return true;
    }

    protected boolean canEqual(final java.lang.Object other) {
        return other instanceof InventoryTransferDetail;
    }

    @java.lang.Override
    public int hashCode() {
        final int PRIME = 59;
        int result = 1;
        final java.lang.Object $id = this.getId();
        result = result * PRIME + ($id == null ? 43 : $id.hashCode());
        final java.lang.Object $transferId = this.getTransferId();
        result = result * PRIME + ($transferId == null ? 43 : $transferId.hashCode());
        final java.lang.Object $productId = this.getProductId();
        result = result * PRIME + ($productId == null ? 43 : $productId.hashCode());
        final java.lang.Object $quantity = this.getQuantity();
        result = result * PRIME + ($quantity == null ? 43 : $quantity.hashCode());
        final java.lang.Object $unit = this.getUnit();
        result = result * PRIME + ($unit == null ? 43 : $unit.hashCode());
        final java.lang.Object $createdAt = this.getCreatedAt();
        result = result * PRIME + ($createdAt == null ? 43 : $createdAt.hashCode());
        final java.lang.Object $updatedAt = this.getUpdatedAt();
        result = result * PRIME + ($updatedAt == null ? 43 : $updatedAt.hashCode());
        return result;
    }

    @java.lang.Override
    public java.lang.String toString() {
        return "InventoryTransferDetail(id=" + this.getId() + ", transferId=" + this.getTransferId() + ", productId=" + this.getProductId() + ", quantity=" + this.getQuantity() + ", unit=" + this.getUnit() + ", createdAt=" + this.getCreatedAt() + ", updatedAt=" + this.getUpdatedAt() + ")";
    }
}
