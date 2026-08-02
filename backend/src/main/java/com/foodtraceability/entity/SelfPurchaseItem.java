package com.foodtraceability.entity;

import com.baomidou.mybatisplus.annotation.*;
import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 自采商品明细实体类
 * 用于管理自采记录中的具体商品信息
 */
@TableName("self_purchase_item")
@Schema(description = "自采商品明细实体")
public class SelfPurchaseItem {
    
    /**
     * 自采商品明细ID，主键
     */
    @TableId(value = "self_purchase_item_id", type = IdType.ASSIGN_ID)
    @Schema(description = "自采商品明细ID", example = "SPI20251205001")
    private String selfPurchaseItemId;
    
    /**
     * 自采记录ID
     */
    @TableField("self_purchase_id")
    @Schema(description = "自采记录ID", example = "SP20251205001")
    private String selfPurchaseId;
    
    /**
     * 产品ID
     */
    @TableField("product_id")
    @Schema(description = "产品ID", example = "1")
    private Long productId;
    
    /**
     * 产品名称
     */
    @TableField("product_name")
    @Schema(description = "产品名称", example = "有机苹果")
    private String productName;
    
    /**
     * 产品规格
     */
    @TableField("specification")
    @Schema(description = "产品规格", example = "200g/个")
    private String specification;
    
    /**
     * 计量单位
     */
    @TableField("unit")
    @Schema(description = "计量单位", example = "个")
    private String unit;
    
    /**
     * 采购数量
     */
    @TableField("quantity")
    @Schema(description = "采购数量", example = "10")
    private Integer quantity;
    
    /**
     * 采购单价（元）
     */
    @TableField("unit_price")
    @Schema(description = "采购单价（元）", example = "12.50")
    private BigDecimal unitPrice;
    
    /**
     * 小计金额（元）
     */
    @TableField("subtotal_amount")
    @Schema(description = "小计金额（元）", example = "125.00")
    private BigDecimal subtotalAmount;
    
    /**
     * 入库状态（pending-待入库，completed-已入库）
     */
    @TableField("status")
    @Schema(description = "入库状态", example = "pending")
    private String status;
    
    /**
     * 追溯码生成状态（pending-待生成，completed-已生成）
     */
    @TableField("trace_code_status")
    @Schema(description = "追溯码生成状态", example = "pending")
    private String traceCodeStatus;
    
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

    // Getter and Setter methods
    public String getSelfPurchaseItemId() {
        return selfPurchaseItemId;
    }

    public void setSelfPurchaseItemId(String selfPurchaseItemId) {
        this.selfPurchaseItemId = selfPurchaseItemId;
    }

    public String getSelfPurchaseId() {
        return selfPurchaseId;
    }

    public void setSelfPurchaseId(String selfPurchaseId) {
        this.selfPurchaseId = selfPurchaseId;
    }

    public Long getProductId() {
        return productId;
    }

    public void setProductId(Long productId) {
        this.productId = productId;
    }

    public String getProductName() {
        return productName;
    }

    public void setProductName(String productName) {
        this.productName = productName;
    }

    public String getSpecification() {
        return specification;
    }

    public void setSpecification(String specification) {
        this.specification = specification;
    }

    public String getUnit() {
        return unit;
    }

    public void setUnit(String unit) {
        this.unit = unit;
    }

    public Integer getQuantity() {
        return quantity;
    }

    public void setQuantity(Integer quantity) {
        this.quantity = quantity;
    }

    public BigDecimal getUnitPrice() {
        return unitPrice;
    }

    public void setUnitPrice(BigDecimal unitPrice) {
        this.unitPrice = unitPrice;
    }

    public BigDecimal getSubtotalAmount() {
        return subtotalAmount;
    }

    public void setSubtotalAmount(BigDecimal subtotalAmount) {
        this.subtotalAmount = subtotalAmount;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getTraceCodeStatus() {
        return traceCodeStatus;
    }

    public void setTraceCodeStatus(String traceCodeStatus) {
        this.traceCodeStatus = traceCodeStatus;
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

    public Integer getDeleted() {
        return deleted;
    }

    public void setDeleted(Integer deleted) {
        this.deleted = deleted;
    }
}
