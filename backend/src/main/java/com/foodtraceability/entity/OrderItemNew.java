package com.foodtraceability.entity;

import com.baomidou.mybatisplus.annotation.*;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDateTime;

/**
 * 订单明细实体类
 * 记录订单中每个商品/套餐的详细信息
 */
@TableName("order_items")
@Schema(description = "订单明细实体")
public class OrderItemNew {

    /** 明细ID */
    @TableId(value = "item_id", type = IdType.ASSIGN_UUID)
    @Schema(description = "明细ID", example = "OI1783341970544")
    private String itemId;

    /** 关联订单ID */
    @TableField("order_id")
    @Schema(description = "关联订单ID", example = "O1783341970544")
    private String orderId;

    /**
     * 产品类型：
     * 1单品 2套餐
     */
    @TableField("product_type")
    @Schema(description = "产品类型: 1单品 2套餐", example = "1")
    private Integer productType;

    /** 单品ID（product_type=1时使用） */
    @TableField("food_id")
    @Schema(description = "单品ID")
    private Long foodId;

    /** 套餐ID（product_type=2时使用） */
    @TableField("combo_id")
    @Schema(description = "套餐ID")
    private Long comboId;

    /** 商品名称（冗余方便查询） */
    @TableField("product_name")
    @Schema(description = "商品名称", example = "红烧肉")
    private String productName;

    /** 规格（如：大/中/小） */
    @TableField("specification")
    @Schema(description = "规格", example = "大份")
    private String specification;

    /** 单价（分） */
    @TableField("unit_price")
    @Schema(description = "单价（分）", example = "3800")
    private Long unitPrice;

    /** 数量 */
    @TableField("quantity")
    @Schema(description = "数量", example = "2")
    private Integer quantity;

    /** 小计金额（分） */
    @TableField("amount")
    @Schema(description = "小计金额（分）", example = "7600")
    private Long amount;

    /** 单项优惠（分） */
    @TableField("discount_amount")
    @Schema(description = "单项优惠（分）", example = "0")
    private Long discountAmount;

    /** 备注（如"不要辣"） */
    @TableField("remark")
    @Schema(description = "备注", example = "不要辣")
    private String remark;

    /**
     * 厨房状态：
     * 0待制作 1制作中 2已完成 3已上菜 4已退款
     */
    @TableField("kitchen_status")
    @Schema(description = "厨房状态: 0待制作 1制作中 2已完成 3已上菜 4已退款", example = "0")
    private Integer kitchenStatus;

    /** 创建时间 */
    @TableField(value = "create_time", fill = FieldFill.INSERT)
    @Schema(description = "创建时间")
    private LocalDateTime createTime;

    /** 更新时间 */
    @TableField(value = "update_time", fill = FieldFill.INSERT_UPDATE)
    @Schema(description = "更新时间")
    private LocalDateTime updateTime;

    /** 逻辑删除标记 */
    @TableLogic
    @TableField("deleted")
    @Schema(description = "逻辑删除标记", example = "0")
    private Integer deleted;

    // Getter方法
    public String getItemId() { return itemId; }
    public String getOrderId() { return orderId; }
    public Integer getProductType() { return productType; }
    public Long getFoodId() { return foodId; }
    public Long getComboId() { return comboId; }
    public String getProductName() { return productName; }
    public String getSpecification() { return specification; }
    public Long getUnitPrice() { return unitPrice; }
    public Integer getQuantity() { return quantity; }
    public Long getAmount() { return amount; }
    public Long getDiscountAmount() { return discountAmount; }
    public String getRemark() { return remark; }
    public Integer getKitchenStatus() { return kitchenStatus; }
    public LocalDateTime getCreateTime() { return createTime; }
    public LocalDateTime getUpdateTime() { return updateTime; }
    public Integer getDeleted() { return deleted; }

    // Setter方法
    public void setItemId(String itemId) { this.itemId = itemId; }
    public void setOrderId(String orderId) { this.orderId = orderId; }
    public void setProductType(Integer productType) { this.productType = productType; }
    public void setFoodId(Long foodId) { this.foodId = foodId; }
    public void setComboId(Long comboId) { this.comboId = comboId; }
    public void setProductName(String productName) { this.productName = productName; }
    public void setSpecification(String specification) { this.specification = specification; }
    public void setUnitPrice(Long unitPrice) { this.unitPrice = unitPrice; }
    public void setQuantity(Integer quantity) { this.quantity = quantity; }
    public void setAmount(Long amount) { this.amount = amount; }
    public void setDiscountAmount(Long discountAmount) { this.discountAmount = discountAmount; }
    public void setRemark(String remark) { this.remark = remark; }
    public void setKitchenStatus(Integer kitchenStatus) { this.kitchenStatus = kitchenStatus; }
    public void setCreateTime(LocalDateTime createTime) { this.createTime = createTime; }
    public void setUpdateTime(LocalDateTime updateTime) { this.updateTime = updateTime; }
    public void setDeleted(Integer deleted) { this.deleted = deleted; }
}
