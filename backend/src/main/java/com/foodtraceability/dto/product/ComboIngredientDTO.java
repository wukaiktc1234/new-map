package com.foodtraceability.dto.product;

import io.swagger.v3.oas.annotations.media.Schema;

import java.io.Serializable;

/**
 * 套餐明细/配料DTO
 */
@Schema(description = "套餐明细")
public class ComboIngredientDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    @Schema(description = "菜品ID", example = "5", requiredMode = Schema.RequiredMode.REQUIRED)
    private Long foodId;

    @Schema(description = "数量", example = "1")
    private Integer quantity = 1;

    @Schema(description = "单位", example = "份")
    private String unit;

    @Schema(description = "是否必选")
    private Boolean isRequired = true;

    @Schema(description = "最多可选几样（当isRequired为false时有效）", example = "2")
    private Integer maxSelect;

    @Schema(description = "排序权重", example = "0")
    private Integer sortOrder = 0;

    public Long getFoodId() { return foodId; }
    public void setFoodId(Long foodId) { this.foodId = foodId; }

    public Integer getQuantity() { return quantity; }
    public void setQuantity(Integer quantity) { this.quantity = quantity; }

    public String getUnit() { return unit; }
    public void setUnit(String unit) { this.unit = unit; }

    public Boolean getIsRequired() { return isRequired; }
    public void setIsRequired(Boolean isRequired) { this.isRequired = isRequired; }

    public Integer getMaxSelect() { return maxSelect; }
    public void setMaxSelect(Integer maxSelect) { this.maxSelect = maxSelect; }

    public Integer getSortOrder() { return sortOrder; }
    public void setSortOrder(Integer sortOrder) { this.sortOrder = sortOrder; }
}
