package com.foodtraceability.dto;

import java.math.BigDecimal;

/**
 * Canonical Order Item Command（统一订单项创建语义合同）
 *
 * 金额单位：元（BigDecimal）。canonical service 内部统一转分（Long）。
 */
public class CanonicalOrderItemCommand {

    /** 菜品/产品ID */
    private String foodId;

    /** 菜品/产品名称 */
    private String foodName;

    /** 单价（元，BigDecimal） */
    private BigDecimal unitPrice;

    /** 数量 */
    private Integer quantity;

    /** 菜品类型（可选） */
    private String dishType;

    // ==================== Constructors ====================

    public CanonicalOrderItemCommand() {
    }

    // ==================== Getters ====================

    public String getFoodId() { return foodId; }
    public String getFoodName() { return foodName; }
    public BigDecimal getUnitPrice() { return unitPrice; }
    public Integer getQuantity() { return quantity; }
    public String getDishType() { return dishType; }

    // ==================== Setters ====================

    public void setFoodId(String foodId) { this.foodId = foodId; }
    public void setFoodName(String foodName) { this.foodName = foodName; }
    public void setUnitPrice(BigDecimal unitPrice) { this.unitPrice = unitPrice; }
    public void setQuantity(Integer quantity) { this.quantity = quantity; }
    public void setDishType(String dishType) { this.dishType = dishType; }
}
