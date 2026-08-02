package com.foodtraceability.dto.product;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.io.Serializable;
import java.math.BigDecimal;

/**
 * 配方/BOM创建请求DTO
 */
@Schema(description = "配方创建请求")
public class RecipeCreateDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    @NotNull(message = "菜品ID不能为空")
    @Schema(description = "关联菜品ID", example = "1", requiredMode = Schema.RequiredMode.REQUIRED)
    private Long foodId;

    @NotNull(message = "原料ID不能为空")
    @Schema(description = "原料ID（关联仓储的物料表）", example = "100", requiredMode = Schema.RequiredMode.REQUIRED)
    private Long materialId;

    @Schema(description = "原料名称（冗余存储）", example = "五花肉")
    private String materialName;

    @Schema(description = "规格", example = "500g/块")
    private String specification;

    @NotNull(message = "标准用量不能为空")
    @Positive(message = "标准用量必须大于0")
    @Schema(description = "标准用量", example = "200", requiredMode = Schema.RequiredMode.REQUIRED)
    private BigDecimal requiredQuantity;

    @Schema(description = "单位", example = "克")
    private String unit;

    @Schema(description = "损耗率(%)", example = "5.00")
    private BigDecimal lossRate;

    @Schema(description = "原料单价（分）", example = "80")
    private Long unitCost;

    public Long getFoodId() { return foodId; }
    public void setFoodId(Long foodId) { this.foodId = foodId; }

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

    public Long getUnitCost() { return unitCost; }
    public void setUnitCost(Long unitCost) { this.unitCost = unitCost; }
}
