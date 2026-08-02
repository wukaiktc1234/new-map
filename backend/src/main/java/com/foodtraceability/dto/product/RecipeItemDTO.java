package com.foodtraceability.dto.product;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.io.Serializable;

/**
 * 菜品原料明细项DTO
 * 用于创建/更新菜品时传递原料关联明细，对应 dish_recipes 表
 */
@Schema(description = "菜品原料明细项")
public class RecipeItemDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    @NotNull(message = "原料ID不能为空")
    @Schema(description = "原料ID", example = "100")
    private Long materialId;

    @Schema(description = "原料名称", example = "五花肉")
    private String materialName;

    @Positive(message = "数量必须大于0")
    @Schema(description = "所需数量", example = "200")
    private Double quantity;

    @Schema(description = "单位", example = "克")
    private String unit;

    @Schema(description = "单价（分）", example = "80")
    private Long unitPrice;

    @Schema(description = "损耗率（百分比）", example = "0")
    private Double lossRate;

    public RecipeItemDTO() {
    }

    public Long getMaterialId() {
        return materialId;
    }

    public void setMaterialId(Long materialId) {
        this.materialId = materialId;
    }

    public String getMaterialName() {
        return materialName;
    }

    public void setMaterialName(String materialName) {
        this.materialName = materialName;
    }

    public Double getQuantity() {
        return quantity;
    }

    public void setQuantity(Double quantity) {
        this.quantity = quantity;
    }

    public String getUnit() {
        return unit;
    }

    public void setUnit(String unit) {
        this.unit = unit;
    }

    public Long getUnitPrice() {
        return unitPrice;
    }

    public void setUnitPrice(Long unitPrice) {
        this.unitPrice = unitPrice;
    }

    public Double getLossRate() {
        return lossRate;
    }

    public void setLossRate(Double lossRate) {
        this.lossRate = lossRate;
    }
}
