package com.foodtraceability.dto.product;

import io.swagger.v3.oas.annotations.media.Schema;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 配方/BOM视图对象VO
 */
@Schema(description = "配方视图对象")
public class RecipeVO implements Serializable {

    private static final long serialVersionUID = 1L;

    @Schema(description = "配方ID", example = "1")
    private Long recipeId;

    @Schema(description = "关联菜品ID", example = "1")
    private Long foodId;

    @Schema(description = "菜品名称", example = "红烧肉")
    private String foodName;

    @Schema(description = "原料ID", example = "100")
    private Long materialId;

    @Schema(description = "原料名称", example = "五花肉")
    private String materialName;

    @Schema(description = "规格", example = "500g/块")
    private String specification;

    @Schema(description = "标准用量", example = "200")
    private BigDecimal requiredQuantity;

    @Schema(description = "单位", example = "克")
    private String unit;

    @Schema(description = "损耗率(%)", example = "5.00")
    private BigDecimal lossRate;

    @Schema(description = "实际用量（含损耗）", example = "210")
    private BigDecimal actualQuantity;

    @Schema(description = "原料单价（分）", example = "80")
    private Long unitCost;

    @Schema(description = "该项成本（分）", example = "168")
    private Long subtotalCost;

    @Schema(description = "成本占比(%)", example = "35.2")
    private Double costRatio;

    @Schema(description = "创建时间")
    private LocalDateTime createTime;

    @Schema(description = "更新时间")
    private LocalDateTime updateTime;

    // Getter和Setter方法
    public Long getRecipeId() { return recipeId; }
    public void setRecipeId(Long recipeId) { this.recipeId = recipeId; }

    public Long getFoodId() { return foodId; }
    public void setFoodId(Long foodId) { this.foodId = foodId; }

    public String getFoodName() { return foodName; }
    public void setFoodName(String foodName) { this.foodName = foodName; }

    public Long getMaterialId() { return materialId; }
    public void setMaterialId(Long materialId) { this.materialId = materialId; }

    public String getMaterialName() { return materialName; }
    public void setMaterialName(String materialName) { this.materialName = materialName; }

    public String getSpecification() { return specification; }
    public void setSpecification(String specification) { this.specification = specification; }

    public BigDecimal getRequiredQuantity() { return requiredQuantity; }
    public void setRequiredQuantity(BigDecimal requiredQuantity) { this.requiredQuantity = requiredQuantity; }

    public String getUnit() { return unit; }
    public void setUnit(String unit) { this.unit = unit; }

    public BigDecimal getLossRate() { return lossRate; }
    public void setLossRate(BigDecimal lossRate) { this.lossRate = lossRate; }

    public BigDecimal getActualQuantity() { return actualQuantity; }
    public void setActualQuantity(BigDecimal actualQuantity) { this.actualQuantity = actualQuantity; }

    public Long getUnitCost() { return unitCost; }
    public void setUnitCost(Long unitCost) { this.unitCost = unitCost; }

    public Long getSubtotalCost() { return subtotalCost; }
    public void setSubtotalCost(Long subtotalCost) { this.subtotalCost = subtotalCost; }

    public Double getCostRatio() { return costRatio; }
    public void setCostRatio(Double costRatio) { this.costRatio = costRatio; }

    public LocalDateTime getCreateTime() { return createTime; }
    public void setCreateTime(LocalDateTime createTime) { this.createTime = createTime; }

    public LocalDateTime getUpdateTime() { return updateTime; }
    public void setUpdateTime(LocalDateTime updateTime) { this.updateTime = updateTime; }
}
