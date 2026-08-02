package com.foodtraceability.dto.product;

import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;

/**
 * 菜品可做份数预测-原料明细
 */
@Schema(description = "菜品可做份数预测-原料明细")
public class StockForecastItemVO {

    @Schema(description = "原料ID")
    private Long materialId;

    @Schema(description = "原料名称")
    private String materialName;

    @Schema(description = "单位")
    private String unit;

    @Schema(description = "可用库存")
    private BigDecimal availableStock;

    @Schema(description = "安全库存")
    private BigDecimal safetyStock;

    @Schema(description = "理论每份用量（BOM）")
    private BigDecimal recipeQty;

    @Schema(description = "实际每份用量（含损耗，自校准）")
    private BigDecimal actualQtyPerServing;

    @Schema(description = "隐含损耗率（实际/理论 - 1，如 0.2=20%）")
    private BigDecimal impliedLossRate;

    @Schema(description = "该原料可做份数")
    private Integer maxServingsByThis;

    @Schema(description = "是否为瓶颈原料")
    private Boolean isBottleneck;

    public Long getMaterialId() { return materialId; }
    public void setMaterialId(Long materialId) { this.materialId = materialId; }
    public String getMaterialName() { return materialName; }
    public void setMaterialName(String materialName) { this.materialName = materialName; }
    public String getUnit() { return unit; }
    public void setUnit(String unit) { this.unit = unit; }
    public BigDecimal getAvailableStock() { return availableStock; }
    public void setAvailableStock(BigDecimal availableStock) { this.availableStock = availableStock; }
    public BigDecimal getSafetyStock() { return safetyStock; }
    public void setSafetyStock(BigDecimal safetyStock) { this.safetyStock = safetyStock; }
    public BigDecimal getRecipeQty() { return recipeQty; }
    public void setRecipeQty(BigDecimal recipeQty) { this.recipeQty = recipeQty; }
    public BigDecimal getActualQtyPerServing() { return actualQtyPerServing; }
    public void setActualQtyPerServing(BigDecimal actualQtyPerServing) { this.actualQtyPerServing = actualQtyPerServing; }
    public BigDecimal getImpliedLossRate() { return impliedLossRate; }
    public void setImpliedLossRate(BigDecimal impliedLossRate) { this.impliedLossRate = impliedLossRate; }
    public Integer getMaxServingsByThis() { return maxServingsByThis; }
    public void setMaxServingsByThis(Integer maxServingsByThis) { this.maxServingsByThis = maxServingsByThis; }
    public Boolean getIsBottleneck() { return isBottleneck; }
    public void setIsBottleneck(Boolean isBottleneck) { this.isBottleneck = isBottleneck; }
}
