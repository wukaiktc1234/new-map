package com.foodtraceability.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.math.BigDecimal;

@TableName("sales_order_detail")
public class SalesOrderDetail {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long orderId;
    private Long productId;
    private String productName;
    private String productCode;
    private Integer quantity;
    private String unit;
    private BigDecimal unitPrice;
    private BigDecimal totalPrice;
    private String inventoryCode;
    private Long storeId;
    private String createdBy;
    private String updatedBy;

    public SalesOrderDetail() {
    }

    public Long getId() {
        return this.id;
    }

    public Long getOrderId() {
        return this.orderId;
    }

    public Long getProductId() {
        return this.productId;
    }

    public String getProductName() {
        return this.productName;
    }

    public String getProductCode() {
        return this.productCode;
    }

    public Integer getQuantity() {
        return this.quantity;
    }

    public String getUnit() {
        return this.unit;
    }

    public BigDecimal getUnitPrice() {
        return this.unitPrice;
    }

    public BigDecimal getTotalPrice() {
        return this.totalPrice;
    }

    public String getInventoryCode() {
        return this.inventoryCode;
    }

    public Long getStoreId() {
        return this.storeId;
    }

    public String getCreatedBy() {
        return this.createdBy;
    }

    public String getUpdatedBy() {
        return this.updatedBy;
    }

    public void setId(final Long id) {
        this.id = id;
    }

    public void setOrderId(final Long orderId) {
        this.orderId = orderId;
    }

    public void setProductId(final Long productId) {
        this.productId = productId;
    }

    public void setProductName(final String productName) {
        this.productName = productName;
    }

    public void setProductCode(final String productCode) {
        this.productCode = productCode;
    }

    public void setQuantity(final Integer quantity) {
        this.quantity = quantity;
    }

    public void setUnit(final String unit) {
        this.unit = unit;
    }

    public void setUnitPrice(final BigDecimal unitPrice) {
        this.unitPrice = unitPrice;
    }

    public void setTotalPrice(final BigDecimal totalPrice) {
        this.totalPrice = totalPrice;
    }

    public void setInventoryCode(final String inventoryCode) {
        this.inventoryCode = inventoryCode;
    }

    public void setStoreId(final Long storeId) {
        this.storeId = storeId;
    }

    public void setCreatedBy(final String createdBy) {
        this.createdBy = createdBy;
    }

    public void setUpdatedBy(final String updatedBy) {
        this.updatedBy = updatedBy;
    }

    @java.lang.Override
    public boolean equals(final java.lang.Object o) {
        if (o == this) return true;
        if (!(o instanceof SalesOrderDetail)) return false;
        final SalesOrderDetail other = (SalesOrderDetail) o;
        if (!other.canEqual((java.lang.Object) this)) return false;
        final java.lang.Object this$id = this.getId();
        final java.lang.Object other$id = other.getId();
        if (this$id == null ? other$id != null : !this$id.equals(other$id)) return false;
        final java.lang.Object this$orderId = this.getOrderId();
        final java.lang.Object other$orderId = other.getOrderId();
        if (this$orderId == null ? other$orderId != null : !this$orderId.equals(other$orderId)) return false;
        final java.lang.Object this$productId = this.getProductId();
        final java.lang.Object other$productId = other.getProductId();
        if (this$productId == null ? other$productId != null : !this$productId.equals(other$productId)) return false;
        final java.lang.Object this$quantity = this.getQuantity();
        final java.lang.Object other$quantity = other.getQuantity();
        if (this$quantity == null ? other$quantity != null : !this$quantity.equals(other$quantity)) return false;
        final java.lang.Object this$storeId = this.getStoreId();
        final java.lang.Object other$storeId = other.getStoreId();
        if (this$storeId == null ? other$storeId != null : !this$storeId.equals(other$storeId)) return false;
        final java.lang.Object this$productName = this.getProductName();
        final java.lang.Object other$productName = other.getProductName();
        if (this$productName == null ? other$productName != null : !this$productName.equals(other$productName)) return false;
        final java.lang.Object this$productCode = this.getProductCode();
        final java.lang.Object other$productCode = other.getProductCode();
        if (this$productCode == null ? other$productCode != null : !this$productCode.equals(other$productCode)) return false;
        final java.lang.Object this$unit = this.getUnit();
        final java.lang.Object other$unit = other.getUnit();
        if (this$unit == null ? other$unit != null : !this$unit.equals(other$unit)) return false;
        final java.lang.Object this$unitPrice = this.getUnitPrice();
        final java.lang.Object other$unitPrice = other.getUnitPrice();
        if (this$unitPrice == null ? other$unitPrice != null : !this$unitPrice.equals(other$unitPrice)) return false;
        final java.lang.Object this$totalPrice = this.getTotalPrice();
        final java.lang.Object other$totalPrice = other.getTotalPrice();
        if (this$totalPrice == null ? other$totalPrice != null : !this$totalPrice.equals(other$totalPrice)) return false;
        final java.lang.Object this$inventoryCode = this.getInventoryCode();
        final java.lang.Object other$inventoryCode = other.getInventoryCode();
        if (this$inventoryCode == null ? other$inventoryCode != null : !this$inventoryCode.equals(other$inventoryCode)) return false;
        final java.lang.Object this$createdBy = this.getCreatedBy();
        final java.lang.Object other$createdBy = other.getCreatedBy();
        if (this$createdBy == null ? other$createdBy != null : !this$createdBy.equals(other$createdBy)) return false;
        final java.lang.Object this$updatedBy = this.getUpdatedBy();
        final java.lang.Object other$updatedBy = other.getUpdatedBy();
        if (this$updatedBy == null ? other$updatedBy != null : !this$updatedBy.equals(other$updatedBy)) return false;
        return true;
    }

    protected boolean canEqual(final java.lang.Object other) {
        return other instanceof SalesOrderDetail;
    }

    @java.lang.Override
    public int hashCode() {
        final int PRIME = 59;
        int result = 1;
        final java.lang.Object $id = this.getId();
        result = result * PRIME + ($id == null ? 43 : $id.hashCode());
        final java.lang.Object $orderId = this.getOrderId();
        result = result * PRIME + ($orderId == null ? 43 : $orderId.hashCode());
        final java.lang.Object $productId = this.getProductId();
        result = result * PRIME + ($productId == null ? 43 : $productId.hashCode());
        final java.lang.Object $quantity = this.getQuantity();
        result = result * PRIME + ($quantity == null ? 43 : $quantity.hashCode());
        final java.lang.Object $storeId = this.getStoreId();
        result = result * PRIME + ($storeId == null ? 43 : $storeId.hashCode());
        final java.lang.Object $productName = this.getProductName();
        result = result * PRIME + ($productName == null ? 43 : $productName.hashCode());
        final java.lang.Object $productCode = this.getProductCode();
        result = result * PRIME + ($productCode == null ? 43 : $productCode.hashCode());
        final java.lang.Object $unit = this.getUnit();
        result = result * PRIME + ($unit == null ? 43 : $unit.hashCode());
        final java.lang.Object $unitPrice = this.getUnitPrice();
        result = result * PRIME + ($unitPrice == null ? 43 : $unitPrice.hashCode());
        final java.lang.Object $totalPrice = this.getTotalPrice();
        result = result * PRIME + ($totalPrice == null ? 43 : $totalPrice.hashCode());
        final java.lang.Object $inventoryCode = this.getInventoryCode();
        result = result * PRIME + ($inventoryCode == null ? 43 : $inventoryCode.hashCode());
        final java.lang.Object $createdBy = this.getCreatedBy();
        result = result * PRIME + ($createdBy == null ? 43 : $createdBy.hashCode());
        final java.lang.Object $updatedBy = this.getUpdatedBy();
        result = result * PRIME + ($updatedBy == null ? 43 : $updatedBy.hashCode());
        return result;
    }

    @java.lang.Override
    public java.lang.String toString() {
        return "SalesOrderDetail(id=" + this.getId() + ", orderId=" + this.getOrderId() + ", productId=" + this.getProductId() + ", productName=" + this.getProductName() + ", productCode=" + this.getProductCode() + ", quantity=" + this.getQuantity() + ", unit=" + this.getUnit() + ", unitPrice=" + this.getUnitPrice() + ", totalPrice=" + this.getTotalPrice() + ", inventoryCode=" + this.getInventoryCode() + ", storeId=" + this.getStoreId() + ", createdBy=" + this.getCreatedBy() + ", updatedBy=" + this.getUpdatedBy() + ")";
    }
}
