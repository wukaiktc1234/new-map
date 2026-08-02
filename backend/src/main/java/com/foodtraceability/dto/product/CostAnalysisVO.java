package com.foodtraceability.dto.product;

import io.swagger.v3.oas.annotations.media.Schema;

import java.io.Serializable;

/**
 * 成本分析视图对象VO
 */
@Schema(description = "成本分析视图对象")
public class CostAnalysisVO implements Serializable {

    private static final long serialVersionUID = 1L;

    @Schema(description = "产品ID", example = "1")
    private Long productId;

    @Schema(description = "产品编码", example = "FD001")
    private String productCode;

    @Schema(description = "产品名称", example = "红烧肉")
    private String productName;

    @Schema(description = "产品类型: FOOD-菜品, COMBO-套餐")
    private String productType;

    @Schema(description = "分类名称", example = "热菜")
    private String categoryName;

    @Schema(description = "规格", example = "大份")
    private String specification;

    @Schema(description = "单位", example = "份")
    private String unit;

    @Schema(description = "售价（分）", example = "3800")
    private Long salePrice;

    @Schema(description = "成本价（分）", example = "1500")
    private Long costPrice;

    @Schema(description = "毛利（分）", example = "2300")
    private Long profit;

    @Schema(description = "毛利率(%)", example = "60.53")
    private Double profitRate;

    @Schema(description = "销量", example = "156")
    private Integer salesCount;

    @Schema(description = "销售额（分）", example = "592800")
    private Long salesAmount;

    @Schema(description = "总成本（分）", example = "234000")
    private Long totalCost;

    @Schema(description = "总毛利（分）", example = "358800")
    private Long totalProfit;

    @Schema(description = "状态: 1在售 2停售", example = "1")
    private Integer status;

    @Schema(description = "周销售量（份）- 近7天累计销量", example = "42")
    private Integer weeklySales;

    @Schema(description = "周转率(%) = 周销售量 / 当前库存 * 100", example = "210.0")
    private Double turnoverRate;

    // Getter和Setter方法
    public Long getProductId() { return productId; }
    public void setProductId(Long productId) { this.productId = productId; }

    public String getProductCode() { return productCode; }
    public void setProductCode(String productCode) { this.productCode = productCode; }

    public String getProductName() { return productName; }
    public void setProductName(String productName) { this.productName = productName; }

    public String getProductType() { return productType; }
    public void setProductType(String productType) { this.productType = productType; }

    public String getCategoryName() { return categoryName; }
    public void setCategoryName(String categoryName) { this.categoryName = categoryName; }

    public String getSpecification() { return specification; }
    public void setSpecification(String specification) { this.specification = specification; }

    public String getUnit() { return unit; }
    public void setUnit(String unit) { this.unit = unit; }

    public Long getSalePrice() { return salePrice; }
    public void setSalePrice(Long salePrice) { this.salePrice = salePrice; }

    public Long getCostPrice() { return costPrice; }
    public void setCostPrice(Long costPrice) { this.costPrice = costPrice; }

    public Long getProfit() { return profit; }
    public void setProfit(Long profit) { this.profit = profit; }

    public Double getProfitRate() { return profitRate; }
    public void setProfitRate(Double profitRate) { this.profitRate = profitRate; }

    public Integer getSalesCount() { return salesCount; }
    public void setSalesCount(Integer salesCount) { this.salesCount = salesCount; }

    public Long getSalesAmount() { return salesAmount; }
    public void setSalesAmount(Long salesAmount) { this.salesAmount = salesAmount; }

    public Long getTotalCost() { return totalCost; }
    public void setTotalCost(Long totalCost) { this.totalCost = totalCost; }

    public Long getTotalProfit() { return totalProfit; }
    public void setTotalProfit(Long totalProfit) { this.totalProfit = totalProfit; }

    public Integer getStatus() { return status; }
    public void setStatus(Integer status) { this.status = status; }

    public Integer getWeeklySales() { return weeklySales; }
    public void setWeeklySales(Integer weeklySales) { this.weeklySales = weeklySales; }

    public Double getTurnoverRate() { return turnoverRate; }
    public void setTurnoverRate(Double turnoverRate) { this.turnoverRate = turnoverRate; }
}
