package com.foodtraceability.dto.product;

import io.swagger.v3.oas.annotations.media.Schema;

import java.io.Serializable;

/**
 * 套餐明细视图对象VO
 */
@Schema(description = "套餐明细视图对象")
public class ComboIngredientVO implements Serializable {

    private static final long serialVersionUID = 1L;

    @Schema(description = "配料ID", example = "1")
    private Long ingredientId;

    @Schema(description = "关联套餐ID", example = "1")
    private Long comboId;

    @Schema(description = "菜品ID", example = "5")
    private Long foodId;

    @Schema(description = "菜品名称", example = "红烧肉")
    private String foodName;

    @Schema(description = "菜品编码", example = "FD001")
    private String foodCode;

    @Schema(description = "数量", example = "1")
    private Integer quantity;

    @Schema(description = "单位", example = "份")
    private String unit;

    @Schema(description = "单价（分）", example = "3800")
    private Long unitPrice;

    @Schema(description = "小计（分）", example = "3800")
    private Long subtotal;

    @Schema(description = "是否必选")
    private Boolean isRequired;

    @Schema(description = "最多可选几样", example = "2")
    private Integer maxSelect;

    @Schema(description = "排序权重", example = "0")
    private Integer sortOrder;

    // Getter和Setter方法
    public Long getIngredientId() { return ingredientId; }
    public void setIngredientId(Long ingredientId) { this.ingredientId = ingredientId; }

    public Long getComboId() { return comboId; }
    public void setComboId(Long comboId) { this.comboId = comboId; }

    public Long getFoodId() { return foodId; }
    public void setFoodId(Long foodId) { this.foodId = foodId; }

    public String getFoodName() { return foodName; }
    public void setFoodName(String foodName) { this.foodName = foodName; }

    public String getFoodCode() { return foodCode; }
    public void setFoodCode(String foodCode) { this.foodCode = foodCode; }

    public Integer getQuantity() { return quantity; }
    public void setQuantity(Integer quantity) { this.quantity = quantity; }

    public String getUnit() { return unit; }
    public void setUnit(String unit) { this.unit = unit; }

    public Long getUnitPrice() { return unitPrice; }
    public void setUnitPrice(Long unitPrice) { this.unitPrice = unitPrice; }

    public Long getSubtotal() { return subtotal; }
    public void setSubtotal(Long subtotal) { this.subtotal = subtotal; }

    public Boolean getIsRequired() { return isRequired; }
    public void setIsRequired(Boolean isRequired) { this.isRequired = isRequired; }

    public Integer getMaxSelect() { return maxSelect; }
    public void setMaxSelect(Integer maxSelect) { this.maxSelect = maxSelect; }

    public Integer getSortOrder() { return sortOrder; }
    public void setSortOrder(Integer sortOrder) { this.sortOrder = sortOrder; }
}
