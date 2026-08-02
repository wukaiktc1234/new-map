package com.foodtraceability.entity;

import com.baomidou.mybatisplus.annotation.*;
import io.swagger.v3.oas.annotations.media.Schema;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 订单项实体类
 * 用于管理订单中的具体产品信息
 */
@TableName("order_items_legacy")
@Schema(description = "POS历史订单项实体（已迁移至独立表，与新订单核心系统隔离）")
public class OrderItem {
    /**
     * 订单项ID，主键
     */
    @TableId(value = "order_item_id", type = IdType.ASSIGN_ID)
    @Schema(description = "订单项ID", example = "OI20241201001")
    private String orderItemId;
    /**
     * 订单ID
     */
    @TableField("order_id")
    @Schema(description = "订单ID", example = "O20241201001")
    private String orderId;
    /**
     * 产品ID
     */
    @TableField("food_id")
    @Schema(description = "产品ID", example = "F001")
    private String foodId;
    /**
     * 产品名称
     */
    @TableField("food_name")
    @Schema(description = "产品名称", example = "有机苹果")
    private String foodName;
    /**
     * 产品单价（元）
     */
    @TableField("unit_price")
    @Schema(description = "产品单价（元）", example = "12.50")
    private BigDecimal unitPrice;
    /**
     * 购买数量
     */
    @TableField("quantity")
    @Schema(description = "购买数量", example = "2")
    private Integer quantity;
    /**
     * 小计金额（元）
     */
    @TableField("subtotal_amount")
    @Schema(description = "小计金额（元）", example = "25.00")
    private BigDecimal subtotalAmount;
    /**
     * 产品图片URL
     */
    @TableField("food_image_url")
    @Schema(description = "产品图片URL", example = "/uploads/food/apple001.jpg")
    private String foodImageUrl;
    /**
     * 产品规格
     */
    @TableField("specification")
    @Schema(description = "产品规格", example = "200g/个")
    private String specification;
    /**
     * 创建时间
     */
    @TableField(value = "create_time", fill = FieldFill.INSERT)
    @Schema(description = "创建时间")
    private LocalDateTime createTime;
    /**
     * 更新时间
     */
    @TableField(value = "update_time", fill = FieldFill.INSERT_UPDATE)
    @Schema(description = "更新时间")
    private LocalDateTime updateTime;
    /**
     * 逻辑删除标记（0-正常，1-删除）
     */
    @TableLogic
    @TableField("deleted")
    @Schema(description = "删除标记", example = "0")
    private Integer deleted;

    public OrderItem() {
    }

    /**
     * 订单项ID，主键
     */
    public String getOrderItemId() {
        return this.orderItemId;
    }

    /**
     * 订单ID
     */
    public String getOrderId() {
        return this.orderId;
    }

    /**
     * 产品ID
     */
    public String getFoodId() {
        return this.foodId;
    }

    /**
     * 产品名称
     */
    public String getFoodName() {
        return this.foodName;
    }

    /**
     * 产品单价（元）
     */
    public BigDecimal getUnitPrice() {
        return this.unitPrice;
    }

    /**
     * 购买数量
     */
    public Integer getQuantity() {
        return this.quantity;
    }

    /**
     * 小计金额（元）
     */
    public BigDecimal getSubtotalAmount() {
        return this.subtotalAmount;
    }

    /**
     * 产品图片URL
     */
    public String getFoodImageUrl() {
        return this.foodImageUrl;
    }

    /**
     * 产品规格
     */
    public String getSpecification() {
        return this.specification;
    }

    /**
     * 创建时间
     */
    public LocalDateTime getCreateTime() {
        return this.createTime;
    }

    /**
     * 更新时间
     */
    public LocalDateTime getUpdateTime() {
        return this.updateTime;
    }

    /**
     * 逻辑删除标记（0-正常，1-删除）
     */
    public Integer getDeleted() {
        return this.deleted;
    }

    /**
     * 订单项ID，主键
     */
    public void setOrderItemId(final String orderItemId) {
        this.orderItemId = orderItemId;
    }

    /**
     * 订单ID
     */
    public void setOrderId(final String orderId) {
        this.orderId = orderId;
    }

    /**
     * 产品ID
     */
    public void setFoodId(final String foodId) {
        this.foodId = foodId;
    }

    /**
     * 产品名称
     */
    public void setFoodName(final String foodName) {
        this.foodName = foodName;
    }

    /**
     * 产品单价（元）
     */
    public void setUnitPrice(final BigDecimal unitPrice) {
        this.unitPrice = unitPrice;
    }

    /**
     * 购买数量
     */
    public void setQuantity(final Integer quantity) {
        this.quantity = quantity;
    }

    /**
     * 小计金额（元）
     */
    public void setSubtotalAmount(final BigDecimal subtotalAmount) {
        this.subtotalAmount = subtotalAmount;
    }

    /**
     * 产品图片URL
     */
    public void setFoodImageUrl(final String foodImageUrl) {
        this.foodImageUrl = foodImageUrl;
    }

    /**
     * 产品规格
     */
    public void setSpecification(final String specification) {
        this.specification = specification;
    }

    /**
     * 创建时间
     */
    public void setCreateTime(final LocalDateTime createTime) {
        this.createTime = createTime;
    }

    /**
     * 更新时间
     */
    public void setUpdateTime(final LocalDateTime updateTime) {
        this.updateTime = updateTime;
    }

    /**
     * 逻辑删除标记（0-正常，1-删除）
     */
    public void setDeleted(final Integer deleted) {
        this.deleted = deleted;
    }

    @java.lang.Override
    public boolean equals(final java.lang.Object o) {
        if (o == this) return true;
        if (!(o instanceof OrderItem)) return false;
        final OrderItem other = (OrderItem) o;
        if (!other.canEqual((java.lang.Object) this)) return false;
        final java.lang.Object this$quantity = this.getQuantity();
        final java.lang.Object other$quantity = other.getQuantity();
        if (this$quantity == null ? other$quantity != null : !this$quantity.equals(other$quantity)) return false;
        final java.lang.Object this$deleted = this.getDeleted();
        final java.lang.Object other$deleted = other.getDeleted();
        if (this$deleted == null ? other$deleted != null : !this$deleted.equals(other$deleted)) return false;
        final java.lang.Object this$orderItemId = this.getOrderItemId();
        final java.lang.Object other$orderItemId = other.getOrderItemId();
        if (this$orderItemId == null ? other$orderItemId != null : !this$orderItemId.equals(other$orderItemId)) return false;
        final java.lang.Object this$orderId = this.getOrderId();
        final java.lang.Object other$orderId = other.getOrderId();
        if (this$orderId == null ? other$orderId != null : !this$orderId.equals(other$orderId)) return false;
        final java.lang.Object this$foodId = this.getFoodId();
        final java.lang.Object other$foodId = other.getFoodId();
        if (this$foodId == null ? other$foodId != null : !this$foodId.equals(other$foodId)) return false;
        final java.lang.Object this$foodName = this.getFoodName();
        final java.lang.Object other$foodName = other.getFoodName();
        if (this$foodName == null ? other$foodName != null : !this$foodName.equals(other$foodName)) return false;
        final java.lang.Object this$unitPrice = this.getUnitPrice();
        final java.lang.Object other$unitPrice = other.getUnitPrice();
        if (this$unitPrice == null ? other$unitPrice != null : !this$unitPrice.equals(other$unitPrice)) return false;
        final java.lang.Object this$subtotalAmount = this.getSubtotalAmount();
        final java.lang.Object other$subtotalAmount = other.getSubtotalAmount();
        if (this$subtotalAmount == null ? other$subtotalAmount != null : !this$subtotalAmount.equals(other$subtotalAmount)) return false;
        final java.lang.Object this$foodImageUrl = this.getFoodImageUrl();
        final java.lang.Object other$foodImageUrl = other.getFoodImageUrl();
        if (this$foodImageUrl == null ? other$foodImageUrl != null : !this$foodImageUrl.equals(other$foodImageUrl)) return false;
        final java.lang.Object this$specification = this.getSpecification();
        final java.lang.Object other$specification = other.getSpecification();
        if (this$specification == null ? other$specification != null : !this$specification.equals(other$specification)) return false;
        final java.lang.Object this$createTime = this.getCreateTime();
        final java.lang.Object other$createTime = other.getCreateTime();
        if (this$createTime == null ? other$createTime != null : !this$createTime.equals(other$createTime)) return false;
        final java.lang.Object this$updateTime = this.getUpdateTime();
        final java.lang.Object other$updateTime = other.getUpdateTime();
        if (this$updateTime == null ? other$updateTime != null : !this$updateTime.equals(other$updateTime)) return false;
        return true;
    }

    protected boolean canEqual(final java.lang.Object other) {
        return other instanceof OrderItem;
    }

    @java.lang.Override
    public int hashCode() {
        final int PRIME = 59;
        int result = 1;
        final java.lang.Object $quantity = this.getQuantity();
        result = result * PRIME + ($quantity == null ? 43 : $quantity.hashCode());
        final java.lang.Object $deleted = this.getDeleted();
        result = result * PRIME + ($deleted == null ? 43 : $deleted.hashCode());
        final java.lang.Object $orderItemId = this.getOrderItemId();
        result = result * PRIME + ($orderItemId == null ? 43 : $orderItemId.hashCode());
        final java.lang.Object $orderId = this.getOrderId();
        result = result * PRIME + ($orderId == null ? 43 : $orderId.hashCode());
        final java.lang.Object $foodId = this.getFoodId();
        result = result * PRIME + ($foodId == null ? 43 : $foodId.hashCode());
        final java.lang.Object $foodName = this.getFoodName();
        result = result * PRIME + ($foodName == null ? 43 : $foodName.hashCode());
        final java.lang.Object $unitPrice = this.getUnitPrice();
        result = result * PRIME + ($unitPrice == null ? 43 : $unitPrice.hashCode());
        final java.lang.Object $subtotalAmount = this.getSubtotalAmount();
        result = result * PRIME + ($subtotalAmount == null ? 43 : $subtotalAmount.hashCode());
        final java.lang.Object $foodImageUrl = this.getFoodImageUrl();
        result = result * PRIME + ($foodImageUrl == null ? 43 : $foodImageUrl.hashCode());
        final java.lang.Object $specification = this.getSpecification();
        result = result * PRIME + ($specification == null ? 43 : $specification.hashCode());
        final java.lang.Object $createTime = this.getCreateTime();
        result = result * PRIME + ($createTime == null ? 43 : $createTime.hashCode());
        final java.lang.Object $updateTime = this.getUpdateTime();
        result = result * PRIME + ($updateTime == null ? 43 : $updateTime.hashCode());
        return result;
    }

    @java.lang.Override
    public java.lang.String toString() {
        return "OrderItem(orderItemId=" + this.getOrderItemId() + ", orderId=" + this.getOrderId() + ", foodId=" + this.getFoodId() + ", foodName=" + this.getFoodName() + ", unitPrice=" + this.getUnitPrice() + ", quantity=" + this.getQuantity() + ", subtotalAmount=" + this.getSubtotalAmount() + ", foodImageUrl=" + this.getFoodImageUrl() + ", specification=" + this.getSpecification() + ", createTime=" + this.getCreateTime() + ", updateTime=" + this.getUpdateTime() + ", deleted=" + this.getDeleted() + ")";
    }
}
