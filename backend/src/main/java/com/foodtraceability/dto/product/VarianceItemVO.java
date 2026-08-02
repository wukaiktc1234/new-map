package com.foodtraceability.dto.product;

import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;

/**
 * 原料理论 vs 实际消耗差异分析（方案E）
 * 用于揪出超耗/损耗高的原料，优化配方与管控浪费。
 */
@Schema(description = "原料消耗差异分析项")
public class VarianceItemVO {

    @Schema(description = "原料ID")
    private Long materialId;

    @Schema(description = "原料名称")
    private String materialName;

    @Schema(description = "单位")
    private String unit;

    @Schema(description = "理论消耗量（近N天售出份数×BOM用量）")
    private BigDecimal theoreticalConsumed;

    @Schema(description = "实际消耗量（近N天库存减少量，含损耗）")
    private BigDecimal actualConsumed;

    @Schema(description = "差异量（实际-理论）")
    private BigDecimal varianceQty;

    @Schema(description = "差异率（差异/理论，如 0.25=25%）")
    private BigDecimal varianceRate;

    @Schema(description = "售出份数（近N天，含该原料的菜）")
    private BigDecimal soldServings;

    public Long getMaterialId() { return materialId; }
    public void setMaterialId(Long materialId) { this.materialId = materialId; }
    public String getMaterialName() { return materialName; }
    public void setMaterialName(String materialName) { this.materialName = materialName; }
    public String getUnit() { return unit; }
    public void setUnit(String unit) { this.unit = unit; }
    public BigDecimal getTheoreticalConsumed() { return theoreticalConsumed; }
    public void setTheoreticalConsumed(BigDecimal theoreticalConsumed) { this.theoreticalConsumed = theoreticalConsumed; }
    public BigDecimal getActualConsumed() { return actualConsumed; }
    public void setActualConsumed(BigDecimal actualConsumed) { this.actualConsumed = actualConsumed; }
    public BigDecimal getVarianceQty() { return varianceQty; }
    public void setVarianceQty(BigDecimal varianceQty) { this.varianceQty = varianceQty; }
    public BigDecimal getVarianceRate() { return varianceRate; }
    public void setVarianceRate(BigDecimal varianceRate) { this.varianceRate = varianceRate; }
    public BigDecimal getSoldServings() { return soldServings; }
    public void setSoldServings(BigDecimal soldServings) { this.soldServings = soldServings; }
}
