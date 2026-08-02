package com.foodtraceability.entity;

import com.baomidou.mybatisplus.annotation.*;
import com.fasterxml.jackson.annotation.JsonFormat;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@TableName("purchase_request_item")
public class PurchaseRequestItem {
    @TableId(value = "item_id", type = IdType.ASSIGN_UUID)
    private String itemId;
    @TableField("request_id")
    private String requestId;
    @TableField("food_id")
    private String foodId;
    @TableField("food_name")
    private String foodName;
    @TableField("food_code")
    private String foodCode;
    @TableField("specification")
    private String specification;
    @TableField("quantity")
    private BigDecimal quantity;
    @TableField("unit")
    private String unit;
    @TableField("estimated_price")
    private BigDecimal estimatedPrice;
    @TableField("subtotal_amount")
    private BigDecimal subtotalAmount;
    @TableField("remark")
    private String remark;
    @TableField("planned_receiver_type")
    private String plannedReceiverType;
    @TableField("planned_store_id")
    private String plannedStoreId;
    @TableField("planned_warehouse_id")
    private Long plannedWarehouseId;
    @TableField("create_time")
    @JsonFormat(pattern = "yyyy-MM-dd\'T\'HH:mm:ss")
    private LocalDateTime createTime;
    @TableField("update_time")
    @JsonFormat(pattern = "yyyy-MM-dd\'T\'HH:mm:ss")
    private LocalDateTime updateTime;
    @TableLogic
    private Integer deleted;

    public PurchaseRequestItem() {
    }

    public String getItemId() {
        return this.itemId;
    }

    public String getRequestId() {
        return this.requestId;
    }

    public String getFoodId() {
        return this.foodId;
    }

    public String getFoodName() {
        return this.foodName;
    }

    public String getFoodCode() {
        return this.foodCode;
    }

    public String getSpecification() {
        return this.specification;
    }

    public BigDecimal getQuantity() {
        return this.quantity;
    }

    public String getUnit() {
        return this.unit;
    }

    public BigDecimal getEstimatedPrice() {
        return this.estimatedPrice;
    }

    public BigDecimal getSubtotalAmount() {
        return this.subtotalAmount;
    }

    public String getRemark() {
        return this.remark;
    }

    public String getPlannedReceiverType() {
        return this.plannedReceiverType;
    }

    public String getPlannedStoreId() {
        return this.plannedStoreId;
    }

    public Long getPlannedWarehouseId() {
        return this.plannedWarehouseId;
    }

    public LocalDateTime getCreateTime() {
        return this.createTime;
    }

    public LocalDateTime getUpdateTime() {
        return this.updateTime;
    }

    public Integer getDeleted() {
        return this.deleted;
    }

    public void setItemId(final String itemId) {
        this.itemId = itemId;
    }

    public void setRequestId(final String requestId) {
        this.requestId = requestId;
    }

    public void setFoodId(final String foodId) {
        this.foodId = foodId;
    }

    public void setFoodName(final String foodName) {
        this.foodName = foodName;
    }

    public void setFoodCode(final String foodCode) {
        this.foodCode = foodCode;
    }

    public void setSpecification(final String specification) {
        this.specification = specification;
    }

    public void setQuantity(final BigDecimal quantity) {
        this.quantity = quantity;
    }

    public void setUnit(final String unit) {
        this.unit = unit;
    }

    public void setEstimatedPrice(final BigDecimal estimatedPrice) {
        this.estimatedPrice = estimatedPrice;
    }

    public void setSubtotalAmount(final BigDecimal subtotalAmount) {
        this.subtotalAmount = subtotalAmount;
    }

    public void setRemark(final String remark) {
        this.remark = remark;
    }

    public void setPlannedReceiverType(final String plannedReceiverType) {
        this.plannedReceiverType = plannedReceiverType;
    }

    public void setPlannedStoreId(final String plannedStoreId) {
        this.plannedStoreId = plannedStoreId;
    }

    public void setPlannedWarehouseId(final Long plannedWarehouseId) {
        this.plannedWarehouseId = plannedWarehouseId;
    }

    @JsonFormat(pattern = "yyyy-MM-dd\'T\'HH:mm:ss")
    public void setCreateTime(final LocalDateTime createTime) {
        this.createTime = createTime;
    }

    @JsonFormat(pattern = "yyyy-MM-dd\'T\'HH:mm:ss")
    public void setUpdateTime(final LocalDateTime updateTime) {
        this.updateTime = updateTime;
    }

    public void setDeleted(final Integer deleted) {
        this.deleted = deleted;
    }

    @java.lang.Override
    public boolean equals(final java.lang.Object o) {
        if (o == this) return true;
        if (!(o instanceof PurchaseRequestItem)) return false;
        final PurchaseRequestItem other = (PurchaseRequestItem) o;
        if (!other.canEqual((java.lang.Object) this)) return false;
        final java.lang.Object this$deleted = this.getDeleted();
        final java.lang.Object other$deleted = other.getDeleted();
        if (this$deleted == null ? other$deleted != null : !this$deleted.equals(other$deleted)) return false;
        final java.lang.Object this$itemId = this.getItemId();
        final java.lang.Object other$itemId = other.getItemId();
        if (this$itemId == null ? other$itemId != null : !this$itemId.equals(other$itemId)) return false;
        final java.lang.Object this$requestId = this.getRequestId();
        final java.lang.Object other$requestId = other.getRequestId();
        if (this$requestId == null ? other$requestId != null : !this$requestId.equals(other$requestId)) return false;
        final java.lang.Object this$foodId = this.getFoodId();
        final java.lang.Object other$foodId = other.getFoodId();
        if (this$foodId == null ? other$foodId != null : !this$foodId.equals(other$foodId)) return false;
        final java.lang.Object this$foodName = this.getFoodName();
        final java.lang.Object other$foodName = other.getFoodName();
        if (this$foodName == null ? other$foodName != null : !this$foodName.equals(other$foodName)) return false;
        final java.lang.Object this$foodCode = this.getFoodCode();
        final java.lang.Object other$foodCode = other.getFoodCode();
        if (this$foodCode == null ? other$foodCode != null : !this$foodCode.equals(other$foodCode)) return false;
        final java.lang.Object this$specification = this.getSpecification();
        final java.lang.Object other$specification = other.getSpecification();
        if (this$specification == null ? other$specification != null : !this$specification.equals(other$specification)) return false;
        final java.lang.Object this$quantity = this.getQuantity();
        final java.lang.Object other$quantity = other.getQuantity();
        if (this$quantity == null ? other$quantity != null : !this$quantity.equals(other$quantity)) return false;
        final java.lang.Object this$unit = this.getUnit();
        final java.lang.Object other$unit = other.getUnit();
        if (this$unit == null ? other$unit != null : !this$unit.equals(other$unit)) return false;
        final java.lang.Object this$estimatedPrice = this.getEstimatedPrice();
        final java.lang.Object other$estimatedPrice = other.getEstimatedPrice();
        if (this$estimatedPrice == null ? other$estimatedPrice != null : !this$estimatedPrice.equals(other$estimatedPrice)) return false;
        final java.lang.Object this$subtotalAmount = this.getSubtotalAmount();
        final java.lang.Object other$subtotalAmount = other.getSubtotalAmount();
        if (this$subtotalAmount == null ? other$subtotalAmount != null : !this$subtotalAmount.equals(other$subtotalAmount)) return false;
        final java.lang.Object this$remark = this.getRemark();
        final java.lang.Object other$remark = other.getRemark();
        if (this$remark == null ? other$remark != null : !this$remark.equals(other$remark)) return false;
        final java.lang.Object this$createTime = this.getCreateTime();
        final java.lang.Object other$createTime = other.getCreateTime();
        if (this$createTime == null ? other$createTime != null : !this$createTime.equals(other$createTime)) return false;
        final java.lang.Object this$updateTime = this.getUpdateTime();
        final java.lang.Object other$updateTime = other.getUpdateTime();
        if (this$updateTime == null ? other$updateTime != null : !this$updateTime.equals(other$updateTime)) return false;
        return true;
    }

    protected boolean canEqual(final java.lang.Object other) {
        return other instanceof PurchaseRequestItem;
    }

    @java.lang.Override
    public int hashCode() {
        final int PRIME = 59;
        int result = 1;
        final java.lang.Object $deleted = this.getDeleted();
        result = result * PRIME + ($deleted == null ? 43 : $deleted.hashCode());
        final java.lang.Object $itemId = this.getItemId();
        result = result * PRIME + ($itemId == null ? 43 : $itemId.hashCode());
        final java.lang.Object $requestId = this.getRequestId();
        result = result * PRIME + ($requestId == null ? 43 : $requestId.hashCode());
        final java.lang.Object $foodId = this.getFoodId();
        result = result * PRIME + ($foodId == null ? 43 : $foodId.hashCode());
        final java.lang.Object $foodName = this.getFoodName();
        result = result * PRIME + ($foodName == null ? 43 : $foodName.hashCode());
        final java.lang.Object $foodCode = this.getFoodCode();
        result = result * PRIME + ($foodCode == null ? 43 : $foodCode.hashCode());
        final java.lang.Object $specification = this.getSpecification();
        result = result * PRIME + ($specification == null ? 43 : $specification.hashCode());
        final java.lang.Object $quantity = this.getQuantity();
        result = result * PRIME + ($quantity == null ? 43 : $quantity.hashCode());
        final java.lang.Object $unit = this.getUnit();
        result = result * PRIME + ($unit == null ? 43 : $unit.hashCode());
        final java.lang.Object $estimatedPrice = this.getEstimatedPrice();
        result = result * PRIME + ($estimatedPrice == null ? 43 : $estimatedPrice.hashCode());
        final java.lang.Object $subtotalAmount = this.getSubtotalAmount();
        result = result * PRIME + ($subtotalAmount == null ? 43 : $subtotalAmount.hashCode());
        final java.lang.Object $remark = this.getRemark();
        result = result * PRIME + ($remark == null ? 43 : $remark.hashCode());
        final java.lang.Object $createTime = this.getCreateTime();
        result = result * PRIME + ($createTime == null ? 43 : $createTime.hashCode());
        final java.lang.Object $updateTime = this.getUpdateTime();
        result = result * PRIME + ($updateTime == null ? 43 : $updateTime.hashCode());
        return result;
    }

    @java.lang.Override
    public java.lang.String toString() {
        return "PurchaseRequestItem(itemId=" + this.getItemId() + ", requestId=" + this.getRequestId() + ", foodId=" + this.getFoodId() + ", foodName=" + this.getFoodName() + ", foodCode=" + this.getFoodCode() + ", specification=" + this.getSpecification() + ", quantity=" + this.getQuantity() + ", unit=" + this.getUnit() + ", estimatedPrice=" + this.getEstimatedPrice() + ", subtotalAmount=" + this.getSubtotalAmount() + ", remark=" + this.getRemark() + ", createTime=" + this.getCreateTime() + ", updateTime=" + this.getUpdateTime() + ", deleted=" + this.getDeleted() + ")";
    }
}
